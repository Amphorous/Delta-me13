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

    // WeaponNode itself has no rarity/path — those only exist on the
    // Store("weapons")-[:CONTAINS_WEAPON]->WeaponNode relationship (see
    // ContainsWeaponRelationship). Copied onto THIS relationship at the point
    // a build's EQUIPS_WEAPON edge is (re)created (see BuildNodeRepository's
    // ...AlsoLinkTheWeaponNode query) so every existing build-fetch query
    // that already returns `ew` picks these up for free, with no schema
    // change to WeaponNode and no backfill/migration needed — old edges
    // created before this simply read back null here until that character's
    // build is next refreshed, same as weaponAscension already does.
    @Property("rarity")
    private String rarity;

    @Property("path")
    private String path;

    @TargetNode
    private WeaponNode weaponNode;
}
