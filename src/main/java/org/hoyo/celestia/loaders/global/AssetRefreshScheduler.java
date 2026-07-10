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

    private final AssetSyncService assetSyncService;
    private final MetaRegenService metaRegenService;
    private final GlobalMetaFileLoader globalMetaFileLoader;
    private final MaintenanceFilter maintenanceFilter;
    private final AvatarInfoRedisLoader avatarInfoRedisLoader;

    public void refresh() {
        log.info("Asset refresh triggered.");

        Map<String, JsonNode> assets = assetSyncService.syncAssets();
        if (assets == null) {
            // even with no upstream changes, re-write the avatar/rank keys: covers a
            // failed startup write (Redis down at boot) and a Redis flushed since then
            avatarInfoRedisLoader.ensureLoaded();
            log.info("No updates, skipping regen.");
            return;
        }

        maintenanceFilter.engage();
        log.info("Maintenance mode engaged.");

        try {
            HonkerMetaObject newMeta = metaRegenService.regenerate(assets);
            globalMetaFileLoader.reload(newMeta);
            avatarInfoRedisLoader.refresh(assets);
            log.info("Meta reload complete.");
        } catch (Exception e) {
            log.error("Failed to regenerate meta, restoring service.", e);
        } finally {
            maintenanceFilter.release();
            log.info("Maintenance mode released.");
        }
    }
}
