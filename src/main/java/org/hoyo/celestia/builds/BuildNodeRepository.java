package org.hoyo.celestia.builds;

import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.model.WeaponFingerprintProjection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BuildNodeRepository extends Neo4jRepository<BuildNode, Long> {

    @Query("""
        RETURN EXISTS(
            MATCH (u:UIDNode {uid: $uid})-[:HAS_BUILD]->(:BuildNode {avatarId: $avatarId})
        )
    """)
    Boolean hasBuilds(@Param("uid") String uid, @Param("avatarId") String avatarId);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})-[:HAS_BUILD]->(b:BuildNode {
                    avatarId: $avatarId,
                    level: $level,
                    skillListString: $skillListString,
                    isStatic: true
                })
                RETURN COUNT(b) > 0
            """)
    Boolean hasLevelsOnStaticBuild(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("skillListString") String skillListString, @Param("level") Integer level);

    @Query("""
        MATCH (u:UIDNode {uid: $uid})

        OPTIONAL MATCH (u)-[:HAS_BUILD]->(old:BuildNode {avatarId: $avatarId, isStatic: $isStatic})-[:FIGHT_PROPS]->(f:FightPropNode)
        OPTIONAL MATCH (old)-[:SKILL_TREE]->(oldSt:SkillTree)
        DETACH DELETE f
        DETACH DELETE oldSt
        DETACH DELETE old

        CREATE (b1:BuildNode {
            level: $level,
            rank: $rank,
            skillListString: $skillListString,
            isStatic: $isStatic,
            avatarId: $avatarId,
            buildName: $buildName,
            isHidden: $isHidden,
            cv: $cv,
            creationDate: $creationDate
        })
        CREATE (u)-[:HAS_BUILD]->(b1)

        WITH u, b1, $fightPropMap AS fightPropMap, $skillTreeMap AS skillTreeMap, $relicIds AS relicIds

        CREATE (f1:FightPropNode)
        SET f1 = fightPropMap
        CREATE (b1)-[:FIGHT_PROPS]->(f1)

        CREATE (st1:SkillTree)
        SET st1 = skillTreeMap
        CREATE (b1)-[:SKILL_TREE]->(st1)

        WITH u, b1, relicIds
        UNWIND relicIds AS rid
        MATCH (u)-[:OWNS_RELIC]->(r:RelicNode {relicId: rid})
        CREATE (b1)-[:EQUIPS_RELIC]->(r)

        WITH DISTINCT b1, $weaponId AS weaponId,
               $weaponLevel AS weaponLevel,
               $refineWeapon AS refineWeapon,
               $weaponAscension AS weaponAscension,
               $baseHP AS baseHP,
               $baseDefence AS baseDefence,
               $baseAtk AS baseAtk
        MATCH (w:WeaponNode {weaponId: weaponId})
        // WeaponNode itself carries no rarity/path — those only live on the
        // Store("weapons")-[:CONTAINS_WEAPON]->WeaponNode edge (see
        // WeaponNodeRepository). OPTIONAL so a somehow-missing CONTAINS_WEAPON
        // edge just yields null rarity/path on the new EQUIPS_WEAPON edge
        // below rather than failing the whole build upsert.
        OPTIONAL MATCH (:Store {name: 'weapons'})-[cw:CONTAINS_WEAPON]->(w)
        CREATE (b1)-[:EQUIPS_WEAPON {
               weaponLevel: weaponLevel,
               weaponRefinement: refineWeapon,
               weaponAscension: weaponAscension,
               baseHP: baseHP,
               baseDefence: baseDefence,
               baseAtk: baseAtk,
               rarity: cw.rarity,
               path: cw.path
        }]->(w)

        RETURN DISTINCT b1
    """)
    BuildNode removeIsStaticBuildAndItsFightPropNodeThenInsertANewIsStaticBuildAndItsFightPropNodeAndAlsoLinkTheBuildNodeToItsRelicNodesAndAlsoLinkTheWeaponNode(
            @Param("uid") String uid,
            @Param("avatarId") String avatarId,
            @Param("level") Integer level,
            @Param("rank") Integer rank,
            @Param("skillListString") String skillListString,
            @Param("isStatic") Boolean isStatic,
            @Param("isHidden") Boolean isHidden,
            @Param("buildName") String buildName,
            @Param("fightPropMap") Map<String, Object> fightPropMap,
            @Param("skillTreeMap") Map<String, Object> skillTreeMap,
            @Param("relicIds") Set<String> relicIds,
            @Param("weaponId") String weaponId,
            @Param("weaponLevel") Integer weaponLevel,
            @Param("refineWeapon") Integer refineWeapon,
            @Param("weaponAscension") Integer weaponAscension,
            @Param("baseHP") Double baseHP,
            @Param("baseDefence") Double baseDefence,
            @Param("baseAtk") Double baseAtk,
            @Param("cv") Double cv,
            @Param("creationDate") LocalDateTime creationDate
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})

        OPTIONAL MATCH (u)-[:HAS_BUILD]->(old:BuildNode {avatarId: $avatarId, isStatic: $isStatic})-[:FIGHT_PROPS]->(f:FightPropNode)
        OPTIONAL MATCH (old)-[:SKILL_TREE]->(oldSt:SkillTree)
        DETACH DELETE f
        DETACH DELETE oldSt
        DETACH DELETE old

        CREATE (b1:BuildNode {
            level: $level,
            rank: $rank,
            skillListString: $skillListString,
            isStatic: $isStatic,
            avatarId: $avatarId,
            buildName: $buildName,
            isHidden: $isHidden,
            cv: $cv,
            creationDate: $creationDate
        })
        CREATE (u)-[:HAS_BUILD]->(b1)

        WITH u, b1, $fightPropMap AS fightPropMap, $skillTreeMap AS skillTreeMap, $relicIds AS relicIds

        CREATE (f1:FightPropNode)
        SET f1 = fightPropMap
        CREATE (b1)-[:FIGHT_PROPS]->(f1)

        CREATE (st1:SkillTree)
        SET st1 = skillTreeMap
        CREATE (b1)-[:SKILL_TREE]->(st1)

        WITH u, b1, relicIds
        UNWIND relicIds AS rid
        MATCH (u)-[:OWNS_RELIC]->(r:RelicNode {relicId: rid})
        CREATE (b1)-[:EQUIPS_RELIC]->(r)

        RETURN DISTINCT b1
    """)
    BuildNode removeIsStaticBuildAndItsFightPropNodeThenInsertANewIsStaticBuildAndItsFightPropNodeAndAlsoLinkTheBuildNodeToItsRelicNodes(
            @Param("uid") String uid,
            @Param("avatarId") String avatarId,
            @Param("level") Integer level,
            @Param("rank") Integer rank,
            @Param("skillListString") String skillListString,
            @Param("isStatic") Boolean isStatic,
            @Param("isHidden") Boolean isHidden,
            @Param("buildName") String buildName,
            @Param("fightPropMap") Map<String, Object> fightPropMap,
            @Param("skillTreeMap") Map<String, Object> skillTreeMap,
            @Param("relicIds") Set<String> relicIds,
            @Param("cv") Double cv,
            @Param("creationDate") LocalDateTime creationDate
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:HAS_BUILD]->(b:BuildNode {
                    avatarId: $avatarId,
                    buildName: $buildName,
                    isStatic: false
                })
        RETURN COUNT(b) > 0
    """)
    Boolean hasBuildName(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildName") String buildName);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})

            MATCH (u)-[:HAS_BUILD]->(b:BuildNode {
                isStatic: true,
                avatarId: $avatarId
            })-[:FIGHT_PROPS]->(f:FightPropNode)

            OPTIONAL MATCH (b)-[:SKILL_TREE]->(st:SkillTree)

            CALL apoc.refactor.cloneNodes([b], true)
            YIELD output AS buildClone

            SET buildClone.isStatic = false
            SET buildClone.buildName = $buildName
            SET buildClone.updateDate = $updateDate

            WITH buildClone, f, st
            MATCH (buildClone)-[r1:FIGHT_PROPS]->()
            DELETE r1

            WITH buildClone, f, st
            CALL apoc.refactor.cloneNodes([f])
            YIELD output AS fNew

            CREATE (buildClone)-[:FIGHT_PROPS]->(fNew)

            WITH buildClone, st
            OPTIONAL MATCH (buildClone)-[r2:SKILL_TREE]->()
            DELETE r2

            WITH buildClone, st
            CALL (buildClone, st) {
                WITH buildClone, st
                WHERE st IS NOT NULL
                CALL apoc.refactor.cloneNodes([st])
                YIELD output AS stNew
                CREATE (buildClone)-[:SKILL_TREE]->(stNew)
            }

            RETURN buildClone
            """)
    void createBuild(
            @Param("uid") String uid,
            @Param("avatarId") String avatarId,
            @Param("buildName") String buildName,
            @Param("updateDate") LocalDateTime updateDate
    );

    @Query("""
            MATCH (u:UIDNode {uid: $uid})

            MATCH (u)-[:HAS_BUILD]->(b:BuildNode {
                isStatic: false,
                avatarId: $avatarId,
                buildName: $buildNameOld
            })

            SET b.buildName = $buildNameNew
            SET b.updateDate = $updateDate
            RETURN b
            """)
    void editBuild(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildNameOld") String buildNameOld, @Param("buildNameNew") String buildNameNew, @Param("updateDate") LocalDateTime updateDate);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})

            MATCH (u)-[:HAS_BUILD]->(b:BuildNode {
                isStatic: false,
                avatarId: $avatarId,
                buildName: $buildName
            })-[:FIGHT_PROPS]->(f:FightPropNode)

            OPTIONAL MATCH (b)-[:SKILL_TREE]->(st:SkillTree)

            DETACH DELETE b, f, st
            """)
    void deleteBuild(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildName") String buildName);

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:HAS_BUILD]->(b:BuildNode {
                    avatarId: $avatarId,
                    buildName: $buildName,
                    isStatic: $isStatic
                })
        RETURN COUNT(b) > 0
    """)
    Boolean hasBuildNameWithStaticParam(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildName") String buildName, @Param("isStatic") Boolean isStatic);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})

            MATCH (u)-[:HAS_BUILD]->(b:BuildNode {
                isStatic: $isStatic,
                avatarId: $avatarId,
                buildName: $buildName
            })

            SET b.isHidden = $hide

            RETURN b
            """)
    void hideBuild(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildName") String buildName, @Param("isStatic") Boolean isStatic, @Param("hide") Boolean hide);

    @Query("""
        MATCH (:UIDNode {uid: $uid})
              -[:HAS_BUILD]->
              (b:BuildNode {
                    avatarId: $avatarId,
                    isStatic: true
              })
        RETURN coalesce(b.cv, 0.0)
    """)
    Double getStaticBuildCv(String uid, String avatarId);

    @Query("""
        MATCH (:UIDNode {uid: $uid})
              -[:HAS_BUILD]->
              (b:BuildNode {
                    avatarId: $avatarId,
                    isStatic: true
              })
        RETURN coalesce(b.rank, 0)
    """)
    Integer getStaticBuildRank(String uid, String avatarId);

    // Read separately from getStaticBuildRank/getStaticBuildCv above because those
    // two use plain scalar coalesce() returns (always exactly one row/value even
    // when the static build itself doesn't exist yet), whereas this one needs to
    // distinguish "no static build at all" (no row -> null projection, handled by
    // shouldICalulateAgain's other checks already) from "static build exists but
    // has no weapon equipped" (one row, every field null) — collapsing those two
    // states into a single coalesced default would make a real weapon un-equip
    // indistinguishable from "nothing to compare yet".
    @Query("""
        MATCH (:UIDNode {uid: $uid})-[:HAS_BUILD]->(b:BuildNode {avatarId: $avatarId, isStatic: true})
        OPTIONAL MATCH (b)-[ew:EQUIPS_WEAPON]->(w:WeaponNode)
        RETURN w.weaponId AS weaponId, ew.weaponLevel AS weaponLevel, ew.weaponRefinement AS weaponRefinement, ew.weaponAscension AS weaponAscension
    """)
    WeaponFingerprintProjection getStaticBuildWeaponFingerprint(@Param("uid") String uid, @Param("avatarId") String avatarId);

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode {isHidden: false})

        WITH b
        ORDER BY b.cv DESC
        SKIP $skip
        LIMIT $limit

        CALL (b) {
            OPTIONAL MATCH (b)-[er:EQUIPS_RELIC]->(r:RelicNode)
            OPTIONAL MATCH (r)-[sar:SUBAFFIX]->(sa:SubAffixNode)
            RETURN collect(DISTINCT er) AS ers, collect(DISTINCT r) AS relics,
                   collect(DISTINCT sar) AS sars, collect(DISTINCT sa) AS subAffixes
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[fpr:FIGHT_PROPS]->(f:FightPropNode)
            RETURN fpr, f
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[ew:EQUIPS_WEAPON]->(w:WeaponNode)
            RETURN ew, w
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[str:SKILL_TREE]->(st:SkillTree)
            RETURN str, st
        }

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w, str, st
    """)
    List<BuildNode> findBuildsByUidOrderByCvDesc(
            String uid,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode {isHidden: false})

        WITH b
        ORDER BY b.cv ASC, id(b)

        SKIP $skip
        LIMIT $limit

        CALL (b) {
            OPTIONAL MATCH (b)-[er:EQUIPS_RELIC]->(r:RelicNode)
            OPTIONAL MATCH (r)-[sar:SUBAFFIX]->(sa:SubAffixNode)
            RETURN collect(DISTINCT er) AS ers, collect(DISTINCT r) AS relics,
                   collect(DISTINCT sar) AS sars, collect(DISTINCT sa) AS subAffixes
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[fpr:FIGHT_PROPS]->(f:FightPropNode)
            RETURN fpr, f
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[ew:EQUIPS_WEAPON]->(w:WeaponNode)
            RETURN ew, w
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[str:SKILL_TREE]->(st:SkillTree)
            RETURN str, st
        }

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w, str, st
    """)
    List<BuildNode> findBuildsByUidOrderByCvAsc(
            String uid,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode {isHidden: false})
        WHERE b.avatarId IN $avatarIds

        WITH b
        ORDER BY b.cv DESC, id(b)

        SKIP $skip
        LIMIT $limit

        CALL (b) {
            OPTIONAL MATCH (b)-[er:EQUIPS_RELIC]->(r:RelicNode)
            OPTIONAL MATCH (r)-[sar:SUBAFFIX]->(sa:SubAffixNode)
            RETURN collect(DISTINCT er) AS ers, collect(DISTINCT r) AS relics,
                   collect(DISTINCT sar) AS sars, collect(DISTINCT sa) AS subAffixes
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[fpr:FIGHT_PROPS]->(f:FightPropNode)
            RETURN fpr, f
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[ew:EQUIPS_WEAPON]->(w:WeaponNode)
            RETURN ew, w
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[str:SKILL_TREE]->(st:SkillTree)
            RETURN str, st
        }

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w, str, st
    """)
    List<BuildNode> findBuildsByUidFilterByAvatarIdsOrderByCvDesc(
            String uid,
            Set<String> avatarIds,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode {isHidden: false})
        WHERE b.avatarId IN $avatarIds

        WITH b
        ORDER BY b.cv ASC, id(b)

        SKIP $skip
        LIMIT $limit

        CALL (b) {
            OPTIONAL MATCH (b)-[er:EQUIPS_RELIC]->(r:RelicNode)
            OPTIONAL MATCH (r)-[sar:SUBAFFIX]->(sa:SubAffixNode)
            RETURN collect(DISTINCT er) AS ers, collect(DISTINCT r) AS relics,
                   collect(DISTINCT sar) AS sars, collect(DISTINCT sa) AS subAffixes
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[fpr:FIGHT_PROPS]->(f:FightPropNode)
            RETURN fpr, f
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[ew:EQUIPS_WEAPON]->(w:WeaponNode)
            RETURN ew, w
        }

        CALL (b) {
            OPTIONAL MATCH (b)-[str:SKILL_TREE]->(st:SkillTree)
            RETURN str, st
        }

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w, str, st
    """)
    List<BuildNode> findBuildsByUidFilterByAvatarIdsOrderByCvAsc(
            String uid,
            Set<String> avatarIds,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode)

        WITH b
        ORDER BY b.cv ASC, id(b)

        return b
    """)
    List<BuildNode> getAllBuilds(String uid);

    // One-time backfill for EQUIPS_WEAPON edges created before rarity/path
    // existed there (see ...AlsoLinkTheWeaponNode above) — refreshing a
    // character does NOT fix this on its own: shouldICalulateAgain only
    // rebuilds a static build when the weapon itself actually changed
    // (id/level/refinement/ascension), so an unchanged equip's edge just
    // keeps missing rarity/path forever regardless of how many times it's
    // refreshed. This directly sets both from each weapon's current
    // CONTAINS_WEAPON edge — plain MATCH (not OPTIONAL): rows with no
    // CONTAINS_WEAPON match are simply not touched, never nulled out. Not
    // idempotent-sensitive — safe to call again, a second run just re-sets
    // the same values (or picks up freshly-corrected ones if CONTAINS_WEAPON
    // itself changes later).
    @Query("""
        MATCH (b:BuildNode)-[ew:EQUIPS_WEAPON]->(w:WeaponNode)
        MATCH (:Store {name: 'weapons'})-[cw:CONTAINS_WEAPON]->(w)
        SET ew.rarity = cw.rarity, ew.path = cw.path
        RETURN count(ew)
    """)
    Long backfillWeaponRarityAndPathOnEquipsWeaponEdges();

}
