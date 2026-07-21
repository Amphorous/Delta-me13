package org.hoyo.celestia.builds.model;

import lombok.AllArgsConstructor;
import lombok.Data;

// Snapshot of whatever a static BuildNode's EQUIPS_WEAPON edge currently points
// at, used purely to detect drift against the character's live Enka data — see
// SubloaderService.shouldICalulateAgain. All fields are null when the build has
// no EQUIPS_WEAPON edge at all (character had no weapon equipped last time it
// was stored) — the query it's read from returns exactly one row with every
// field null in that case, not "no row", since only the weapon MATCH is optional.
@Data
@AllArgsConstructor
public class WeaponFingerprintProjection {
    private String weaponId;
    private Integer weaponLevel;
    private Integer weaponRefinement;
    private Integer weaponAscension;
}
