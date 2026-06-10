package org.hoyo.celestia.loaders.model.relations;

import lombok.Data;
import org.hoyo.celestia.loaders.model.WeaponNode;
import org.springframework.data.neo4j.core.schema.*;

@Data
@RelationshipProperties
public class ContainsWeaponRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @Property("weaponId")
    private String weaponId;

    @Property("path")
    private String path;

    @Property("rarity")
    private String rarity;

    @TargetNode
    private WeaponNode weaponNode;
}
