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
    private Double baseAtk;

    @Property("baseDefence")
    private Double baseDefence;

    @Property("baseHP")
    private Double baseHP;

    @Property("weaponLevel")
    private Integer weaponLevel;

    @Property("weaponRefinement")
    private Integer weaponRefinement;

    @TargetNode
    private WeaponNode weaponNode;
}
