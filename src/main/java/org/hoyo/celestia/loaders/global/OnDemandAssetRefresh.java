package org.hoyo.celestia.loaders.global;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges "a request just hit a meta asset we don't have" (new game version
 * content) to the existing asset sync, so recovery doesn't have to wait for
 * the every-3-days cron (AssetRefreshCron) or a manual restart. Throttled so
 * a wave of upserts against a new character triggers at most one sync per
 * window — syncAssets() is SHA-diffed upstream, so even a fired probe that
 * finds no changes is a single cheap GitHub call.
 * <p>
 * Depends on AssetRefreshScheduler directly (an unconditional bean); only the
 * cron wrapper is gated behind scheduler.celestia.checkForUpdate, so this
 * works even where that property is off.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnDemandAssetRefresh {

    private final AssetRefreshScheduler assetRefreshScheduler;

    @Value("${celestia.data.on-demand-refresh-hours:6}")
    private long throttleHours;

    private final AtomicReference<Instant> lastAttempt = new AtomicReference<>(Instant.MIN);

    // Single dedicated thread: refresh() is synchronized anyway, so parallel
    // workers would only queue up behind each other.
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "on-demand-asset-refresh");
        t.setDaemon(true);
        return t;
    });

    /**
     * Fire an async asset refresh unless one was already attempted within the
     * throttle window. compareAndSet makes the window check atomic, so a
     * stampede of concurrent upserts (everyone refreshing the day a patch
     * drops) collapses to a single sync.
     */
    public void requestRefresh(String reason) {
        Instant previous = lastAttempt.get();
        Instant now = Instant.now();
        if (Duration.between(previous, now).toHours() < throttleHours) {
            log.debug("On-demand asset refresh suppressed (throttled, last attempt {}): {}", previous, reason);
            return;
        }
        if (!lastAttempt.compareAndSet(previous, now)) {
            // another request won the race in this same window
            return;
        }
        log.info(AssetRefreshScheduler.LOG_SEPARATOR);
        log.info("ON-DEMAND ASSET REFRESH requested (throttle window {}h): {}", throttleHours, reason);
        log.info(AssetRefreshScheduler.LOG_SEPARATOR);

        CompletableFuture.runAsync(() -> assetRefreshScheduler.refresh(0), executor)
                .exceptionally(e -> {
                    log.error("On-demand asset refresh failed.", e);
                    return null;
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
