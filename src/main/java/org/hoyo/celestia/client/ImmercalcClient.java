package org.hoyo.celestia.client;

import org.hoyo.celestia.buffEffects.model.TeamMemberDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// getLeaderboardAvatarIds() used to live here (GET /honker/get-list) but was
// removed - the avatarId set is now read from Redis directly (see
// LeaderboardStartupSync/LeaderboardUpdateListener), not pulled from
// Immercalc over Feign. That switch is what makes the set correct with more
// than one Celestia instance - a Feign call through the load balancer only
// ever reaches one instance, Redis pub/sub reaches all of them.
@FeignClient(name = "immercalc")
public interface ImmercalcClient {

    @PostMapping("/honker/dmg-calc/{avatarId}")
    Double dmgCalc(@RequestBody TeamMemberDTO teamMemberDTO, @PathVariable("avatarId") String avatarId);
}
