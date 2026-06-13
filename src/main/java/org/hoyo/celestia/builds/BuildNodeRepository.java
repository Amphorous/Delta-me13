package org.hoyo.celestia.builds;

import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.fightprops.model.FightPropNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

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
        DETACH DELETE f
        DETACH DELETE old
    
        CREATE (b1:BuildNode {
            level: $level,
            skillListString: $skillListString,
            isStatic: $isStatic,
            avatarId: $avatarId,
            buildName: $buildName,
            isHidden: $isHidden,
            cv: $cv
        })
        CREATE (u)-[:HAS_BUILD]->(b1)
    
        WITH u, b1, $fightPropMap AS fightPropMap, $relicIds AS relicIds

        CREATE (f1:FightPropNode)
        SET f1 = fightPropMap
        CREATE (b1)-[:FIGHT_PROPS]->(f1)

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
        CREATE (b1)-[:EQUIPS_WEAPON {
               weaponLevel: weaponLevel,
               weaponRefinement: refineWeapon,
               weaponAscension: weaponAscension,
               baseHP: baseHP,
               baseDefence: baseDefence,
               baseAtk: baseAtk
        }]->(w)
    
        RETURN DISTINCT b1
    """)
    BuildNode removeIsStaticBuildAndItsFightPropNodeThenInsertANewIsStaticBuildAndItsFightPropNodeAndAlsoLinkTheBuildNodeToItsRelicNodesAndAlsoLinkTheWeaponNode(
            @Param("uid") String uid,
            @Param("avatarId") String avatarId,
            @Param("level") Integer level,
            @Param("skillListString") String skillListString,
            @Param("isStatic") Boolean isStatic,
            @Param("isHidden") Boolean isHidden,
            @Param("buildName") String buildName,
            @Param("fightPropMap") Map<String, Object> fightPropMap,
            @Param("relicIds") Set<String> relicIds,
            @Param("weaponId") String weaponId,
            @Param("weaponLevel") Integer weaponLevel,
            @Param("refineWeapon") Integer refineWeapon,
            @Param("weaponAscension") Integer weaponAscension,
            @Param("baseHP") Double baseHP,
            @Param("baseDefence") Double baseDefence,
            @Param("baseAtk") Double baseAtk,
            @Param("cv") Double cv

    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
    
        OPTIONAL MATCH (u)-[:HAS_BUILD]->(old:BuildNode {avatarId: $avatarId, isStatic: $isStatic})-[:FIGHT_PROPS]->(f:FightPropNode)
        DETACH DELETE f
        DETACH DELETE old
    
        CREATE (b1:BuildNode {
            level: $level,
            skillListString: $skillListString,
            isStatic: $isStatic,
            avatarId: $avatarId,
            buildName: $buildName,
            isHidden: $isHidden,
            cv: $cv
        })
        CREATE (u)-[:HAS_BUILD]->(b1)
    
        WITH u, b1, $fightPropMap AS fightPropMap, $relicIds AS relicIds

        CREATE (f1:FightPropNode)
        SET f1 = fightPropMap
        CREATE (b1)-[:FIGHT_PROPS]->(f1)

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
            @Param("skillListString") String skillListString,
            @Param("isStatic") Boolean isStatic,
            @Param("isHidden") Boolean isHidden,
            @Param("buildName") String buildName,
            @Param("fightPropMap") Map<String, Object> fightPropMap,
            @Param("relicIds") Set<String> relicIds,
            @Param("cv") Double cv
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
            
            CALL apoc.refactor.cloneNodes([b], true)
            YIELD output AS buildClone
            
            SET buildClone.isStatic = false
            SET buildClone.buildName = $buildName
            
            WITH buildClone, f
            MATCH (buildClone)-[r:FIGHT_PROPS]->()
            DELETE r
            
            WITH buildClone, f
            CALL apoc.refactor.cloneNodes([f])
            YIELD output AS fNew
            
            WITH buildClone, fNew
            CREATE (buildClone)-[:FIGHT_PROPS]->(fNew)
            
            RETURN buildClone
            """)
    void createBuild(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildName") String buildName);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})
            
            MATCH (u)-[:HAS_BUILD]->(b:BuildNode {
                isStatic: false,
                avatarId: $avatarId,
                buildName: $buildNameOld
            })
            
            SET b.buildName = $buildNameNew
            RETURN b
            """)
    void editBuild(@Param("uid") String uid, @Param("avatarId") String avatarId, @Param("buildNameOld") String buildNameOld, @Param("buildNameNew") String buildNameNew);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})
            
            MATCH (u)-[:HAS_BUILD]->(b:BuildNode {
                isStatic: false,
                avatarId: $avatarId,
                buildName: $buildName
            })-[:FIGHT_PROPS]->(f:FightPropNode)
            
            DETACH DELETE b, f
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

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w
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

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w
    """)
    List<BuildNode> findBuildsByUidOrderByCvAsc(
            String uid,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode {avatarId: $avatarId, isHidden: false})

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

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w
    """)
    List<BuildNode> findBuildsByUidFilterByAvatarIdOrderByCvDesc(
            String uid,
            String avatarId,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})
        MATCH (u)-[:HAS_BUILD]->(b:BuildNode {avatarId: $avatarId, isHidden: false})

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

        RETURN b, ers, relics, sars, subAffixes, fpr, f, ew, w
    """)
    List<BuildNode> findBuildsByUidFilterByAvatarIdOrderByCvAsc(
            String uid,
            String avatarId,
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

}
