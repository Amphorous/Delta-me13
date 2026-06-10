package org.hoyo.celestia.builds.model;

import lombok.Data;
import org.hoyo.celestia.fightprops.model.FightPropNode;
import org.hoyo.celestia.loaders.model.WeaponNode;
import org.hoyo.celestia.relics.model.RelicNode;

import java.util.List;

@Data
public class BuildProjectionDTO {
    // DTO containing
    // data from a build
    // data from its fight prop node
    // list of equipped relics
    // weapon node
    BuildNode build;
    FightPropNode fightProps;
    List<RelicNode> relics;
    WeaponNode lightCone;
    CurrentWeaponStats lightConeStats;
}
