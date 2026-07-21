package org.hoyo.celestia.builds;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.service.BuildMaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/builds")
@RequiredArgsConstructor
public class BuildMaintenanceController {

    private final BuildMaintenanceService buildMaintenanceService;

    // Backfills rarity/path onto every existing EQUIPS_WEAPON edge from its
    // weapon's current CONTAINS_WEAPON edge — see BuildNodeRepository's
    // backfillWeaponRarityAndPathOnEquipsWeaponEdges for why refreshing a
    // character alone doesn't do this. Safe to call again any time.
    @PostMapping("/backfill-weapon-rarity-path")
    public ResponseEntity<Map<String, Object>> backfillWeaponRarityAndPath() {
        long updated = buildMaintenanceService.backfillWeaponRarityAndPath();
        return ResponseEntity.ok(Map.of("edgesUpdated", updated));
    }
}
