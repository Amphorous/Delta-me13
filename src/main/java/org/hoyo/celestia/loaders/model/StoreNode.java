package org.hoyo.celestia.loaders.model;

import lombok.Data;
import org.hoyo.celestia.loaders.model.relations.ContainsWeaponRelationship;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Data
@Node
public class StoreNode {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    //relation going from store to weapon
    //props: rarity, path, weaponId
    @Relationship(type = "CONTAINS_WEAPON", direction = Relationship.Direction.OUTGOING)
    private List<ContainsWeaponRelationship> containsWeaponRelationshipList;
}
