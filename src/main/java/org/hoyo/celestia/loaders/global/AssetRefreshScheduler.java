package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.config.MaintenanceFilter;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AssetRefreshScheduler {

    public static final String LOG_SEPARATOR = "============================================================";

    private final AssetSyncService assetSyncService;
    private final MetaRegenService metaRegenService;
    private final GlobalMetaFileLoader globalMetaFileLoader;
    private final MaintenanceFilter maintenanceFilter;
    private final AvatarInfoRedisLoader avatarInfoRedisLoader;

    // synchronized: reachable from both the cron (AssetRefreshCron) and the
    // on-demand path (OnDemandAssetRefresh) — overlapping runs would race on
    // maintenance mode and the meta reload.
    public synchronized void refresh(int force) {
        log.info(LOG_SEPARATOR);
        log.info("ASSET REFRESH CYCLE START (force={})", force);
        log.info(LOG_SEPARATOR);

        AssetSyncService.SyncResult sync = assetSyncService.syncAssets(force);
        Map<String, JsonNode> assets = sync.assets();
        if (assets == null) {
            // even with no upstream changes, re-write the avatar/rank keys: covers a
            // failed startup write (Redis down at boot) and a Redis flushed since then
            avatarInfoRedisLoader.ensureLoaded();
            log.info("No asset updates to process — meta regen skipped.");
            log.info(LOG_SEPARATOR);
            log.info("ASSET REFRESH CYCLE END (no changes)");
            log.info(LOG_SEPARATOR);
            return;
        }

        maintenanceFilter.engage();
        log.info("Maintenance mode engaged.");

        boolean succeeded = false;
        try {
            HonkerMetaObject newMeta = metaRegenService.regenerate(assets);
            globalMetaFileLoader.reload(newMeta);
            avatarInfoRedisLoader.refresh(assets);
            // The SHAs are only committed once the new meta is actually LIVE —
            // if anything above threw, the next cycle re-detects the same files
            // as changed and retries, instead of permanently reporting
            // "no change" against a meta that never absorbed them.
            assetSyncService.commitShas(sync.shas());
            succeeded = true;
            log.info("Meta regen + reload complete, sync SHAs committed.");
        } catch (Exception e) {
            log.error("Failed to regenerate meta, restoring service. SHAs NOT committed — next cycle will retry these files.", e);
        } finally {
            maintenanceFilter.release();
            log.info("Maintenance mode released.");
            log.info(LOG_SEPARATOR);
            log.info("ASSET REFRESH CYCLE END ({})", succeeded ? "updated" : "FAILED — will retry next cycle");
            log.info(LOG_SEPARATOR);
        }
    }
}
