package org.hoyo.celestia.loaders.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.celestia.checkForUpdate", havingValue = "true")
public class AssetRefreshCron {

    private final AssetRefreshScheduler assetRefreshScheduler;

    @Scheduled(cron = "${celestia.data.refresh-cron:0 0 3 */3 * ?}")
    public void checkForUpdate() {
        log.info("Scheduled asset refresh triggered.");
        assetRefreshScheduler.refresh();
    }
}
