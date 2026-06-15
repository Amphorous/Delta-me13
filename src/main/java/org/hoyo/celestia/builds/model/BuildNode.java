package org.hoyo.celestia.builds.model;

import lombok.Data;
import org.hoyo.celestia.fightprops.model.FightPropNode;
import org.hoyo.celestia.relics.model.RelicNode;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Node
public class BuildNode {
    @Id
    @GeneratedValue
    private Long id;
    private Integer level;
    private String skillListString;
    private Boolean isStatic;
    private Boolean isHidden = false;
    private String avatarId;
    private String buildName = "perhaps_feixiao";
    private Double cv;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate; // NOTE: updateDate isn't the day when the build was changed, it is just the day when the build was **FIRST** given a name
    //not including data from honker_characters.json since that information
    //is only needed on the frontend, and it can be accessed there

    //fightprops is going to be a relation to the build
    /*
    * use metaFile to get required level stats of the character: (level-1)*add + base = base_stat_value
    * use equipment to get weapon base stats
    * enumerate artifact stats
    *
    * most difficult part is to get the talent stats
    * getting skillids from skillidlist, ex: 12202XX where 1220 is the avatarId, 2 is the series of skills which give stat buffs, we need to add those
    *
    * these details are in tree in metaFile (well not yet, they need to be added)
    * */

    //relation going to RelicNode named EQUIPS_RELIC
    // no props (can get the relic of a specific position using r.type if r is a relic)
    @Relationship(type = "EQUIPS_RELIC", direction = Relationship.Direction.OUTGOING)
    private List<RelicNode> relicNodes;

    //relation going to FightPropNode named FIGHT_PROPS
    // no props
    @Relationship(type = "FIGHT_PROPS", direction = Relationship.Direction.OUTGOING)
    private FightPropNode fightProps;

    //relation going to WeaponNode named EQUIPS_WEAPON
    // weapon current base HP, DEF, ATK and level and refinement as props
    @Relationship(type = "EQUIPS_WEAPON", direction = Relationship.Direction.OUTGOING)
    private EquipsWeaponRelationship equipsWeapon;
}
