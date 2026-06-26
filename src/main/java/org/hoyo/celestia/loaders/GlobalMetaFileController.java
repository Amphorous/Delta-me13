package org.hoyo.celestia.loaders;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.loaders.global.AssetRefreshScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/meta")
@RequiredArgsConstructor
public class GlobalMetaFileController {

    private final AssetRefreshScheduler assetRefreshScheduler;

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> triggerRefresh() {
        assetRefreshScheduler.refresh();
        return ResponseEntity.ok(Map.of("refreshed", true));
    }
}
