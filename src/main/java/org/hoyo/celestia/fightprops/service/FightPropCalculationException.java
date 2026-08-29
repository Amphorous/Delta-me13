package org.hoyo.celestia.fightprops.service;

import lombok.Getter;

/**
 * Thrown when computing a character's fight-prop stats fails for a reason
 * other than a missing meta asset (MissingMetaAssetException) - typically a
 * mistyped value in the loaded meta (see MetaRegenService), where the raw
 * cause (e.g. ClassCastException) alone doesn't say which uid/avatarId hit
 * it. Wraps the original failure so SubloaderService can rethrow it with
 * that context attached, without changing how the failure propagates - still
 * a RuntimeException, still fails the whole upsert transaction as before.
 */
@Getter
public class FightPropCalculationException extends RuntimeException {

    private final String uid;
    private final String avatarId;

    public FightPropCalculationException(String uid, String avatarId, Throwable cause) {
        super("Fight prop calculation failed for uid " + uid + ", avatarId " + avatarId + ": " + cause, cause);
        this.uid = uid;
        this.avatarId = avatarId;
    }
}
