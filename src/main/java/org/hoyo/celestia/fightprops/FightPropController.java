package org.hoyo.celestia.fightprops;

import org.hoyo.celestia.buffEffects.model.TeamMemberDTO;
import org.hoyo.celestia.fightprops.model.SwapWeaponsRequest;
import org.hoyo.celestia.fightprops.service.FightPropService;
import org.hoyo.celestia.user.model.AvatarDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fightprop")
public class FightPropController {

    private final FightPropService fightPropService;

    public FightPropController(FightPropService fightPropService) {
        this.fightPropService = fightPropService;
    }

    @PostMapping("/test")
    public void testFightProp(@RequestBody AvatarDetail character) {
        fightPropService.getFightPropNode(character);
    }

    @PostMapping("/swap-weapons")
    public ResponseEntity<List<TeamMemberDTO>> swapWeapons(@RequestBody SwapWeaponsRequest request) {
        return ResponseEntity.ok(fightPropService.swapWeapons(request.getTeamMemberDTO(), request.getWeapons()));
    }
}
