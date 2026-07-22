package org.hoyo.celestia.relics;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.relics.DTOs.DuplicateRelicGroupProjection;
import org.hoyo.celestia.relics.service.RelicDedupeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/relics")
@RequiredArgsConstructor
public class RelicMaintenanceController {

    private final RelicDedupeService relicDedupeService;

    // Read-only — safe to call any time. Lists every (uid, relicId) pair
    // currently owned more than once, so scope can be reviewed before /dedupe runs.
    @GetMapping("/duplicates")
    public ResponseEntity<List<DuplicateRelicGroupProjection>> findDuplicates() {
        return ResponseEntity.ok(relicDedupeService.findDuplicates());
    }

    // Collapses every duplicate group down to one RelicNode, redirecting any
    // EQUIPS_RELIC edges onto the survivor first. Not idempotent-sensitive —
    // safe to call again, a second run will simply report 0 removed.
    @PostMapping("/dedupe")
    public ResponseEntity<Map<String, Object>> dedupe() {
        long removed = relicDedupeService.dedupeAll();
        return ResponseEntity.ok(Map.of("duplicatesRemoved", removed));
    }
}
