package org.hoyo.celestia.loaders;

import org.hoyo.celestia.loaders.service.WeaponLoaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/load")
public class LoaderController {
    private final WeaponLoaderService weaponLoaderService;

    public LoaderController(WeaponLoaderService weaponLoaderService) {
        this.weaponLoaderService = weaponLoaderService;
    }

    //load honker_meta.json
    //->load weapons
    //->load characters

    @GetMapping("/honker_weps")
    public ResponseEntity<String> loadHonkerWepsJSON() {
        return weaponLoaderService.execute();
    }

    // POST /leaderboard-list used to live here - Immercalc pushed its
    // avatarId list to it on startup via Feign. Removed: the avatarId set is
    // now written to Redis directly by Immercalc and read from there (see
    // LeaderboardStartupSync/LeaderboardUpdateListener), so nothing calls
    // this endpoint anymore.
}
