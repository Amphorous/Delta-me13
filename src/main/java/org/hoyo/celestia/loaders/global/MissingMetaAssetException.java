package org.hoyo.celestia.loaders.global;

import lombok.Getter;

/**
 * Thrown when a request-path meta-file lookup (avatar base stats, skill tree
 * point, ...) has no entry for an id that came from live Enka data — which
 * happens whenever a game version update ships new content before the synced
 * assets (see AssetSyncService/MetaRegenService) have caught up.
 * <p>
 * Deliberately a RuntimeException so it can cross the existing service call
 * chains unchanged; SubloaderService catches it per character so one
 * not-yet-supported character skips instead of rolling back a whole upsert.
 */
@Getter
public class MissingMetaAssetException extends RuntimeException {

    private final String assetType;
    private final String assetId;

    public MissingMetaAssetException(String assetType, String assetId) {
        super("Meta asset missing: " + assetType + " " + assetId);
        this.assetType = assetType;
        this.assetId = assetId;
    }
}
