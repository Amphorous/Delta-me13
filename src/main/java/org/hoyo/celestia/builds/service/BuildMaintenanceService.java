package org.hoyo.celestia.builds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.builds.BuildNodeRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildMaintenanceService {

    private final BuildNodeRepository buildNodeRepository;

    // Deliberately NOT run automatically on boot (unlike WeaponLoaderService's
    // weapon sync): that one's per-boot cost is bounded by the number of
    // weapon definitions in the game (small, slow-growing), but this one's
    // cost is bounded by the number of BUILDS across every user — unboundedly
    // growing, and only ever needed once (new EQUIPS_WEAPON edges already get
    // rarity/path at creation time — see ...AlsoLinkTheWeaponNode). Trigger
    // via POST /admin/builds/backfill-weapon-rarity-path once, after
    // deploying the create-time fix; there's nothing for a second run to do.
    public long backfillWeaponRarityAndPath() {
        Long updated = buildNodeRepository.backfillWeaponRarityAndPathOnEquipsWeaponEdges();
        long count = updated == null ? 0 : updated;
        log.info("Weapon rarity/path backfill: set on {} EQUIPS_WEAPON edge(s).", count);
        return count;
    }
}
