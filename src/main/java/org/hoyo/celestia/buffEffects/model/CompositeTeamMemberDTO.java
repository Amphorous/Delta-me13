package org.hoyo.celestia.buffEffects.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hoyo.celestia.fightprops.model.FightPropNode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompositeTeamMemberDTO {
    private TeamMemberDTO teamMemberDTO;
    private FightPropNode fightPropNode;
}
