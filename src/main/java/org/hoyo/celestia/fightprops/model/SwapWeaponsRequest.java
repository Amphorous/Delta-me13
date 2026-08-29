package org.hoyo.celestia.fightprops.model;

import lombok.Data;
import org.hoyo.celestia.buffEffects.model.TeamMemberDTO;

import java.util.Map;

@Data
public class SwapWeaponsRequest {
    private TeamMemberDTO teamMemberDTO;
    private Map<String, Integer> weapons; // {weaponId: rank, ...}
}
