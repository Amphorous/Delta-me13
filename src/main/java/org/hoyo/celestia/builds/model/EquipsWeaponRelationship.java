package org.hoyo.celestia.builds.model;

import feign.Param;
import lombok.Data;
import org.hoyo.celestia.loaders.model.WeaponNode;
import org.springframework.data.neo4j.core.schema.*;

@Data
@RelationshipProperties
public class EquipsWeaponRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @Property("baseAtk")
    private Float baseAtk;

    @Property("baseDefence")
    private Float baseDefence;

    @Property("baseHP")
    private Float baseHP;

    @Property("weaponLevel")
    private Integer weaponLevel;

    @Property("weaponRefinement")
    private Integer weaponRefinement;

    @TargetNode
    private WeaponNode weaponNode;
}
