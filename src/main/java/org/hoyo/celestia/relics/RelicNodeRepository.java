package org.hoyo.celestia.relics;

import org.hoyo.celestia.relics.DTOs.BuildProjection;
import org.hoyo.celestia.relics.DTOs.RelicProjectionDTO;
import org.hoyo.celestia.relics.model.RelicNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public interface RelicNodeRepository extends Neo4jRepository<RelicNode, Long> {

    @Query("""
    CREATE (r:RelicNode {
        relicId: $relicId,
        mainAffixId: $mainAffixId,
        tid: $tid,
        type: $type,
        level: $level,
        setId: $setId,
        setName: $setName,
        mainType: $mainType,
        mainValue: $mainValue 
    })
    
    WITH r
    MATCH (u:UIDNode {uid: $uid})
    CREATE (u)-[:OWNS_RELIC]->(r)
    
    WITH r, $subAffixes AS subAffixes
    UNWIND subAffixes AS sa
    CREATE (s:SubAffixNode {
        type: sa.type,
        value: sa.value,
        cnt: sa.cnt,
        step: sa.step
    })
    CREATE (r)-[:SUBAFFIX]->(s)
    
    RETURN DISTINCT r
    
    """)
    RelicNode insertRelic( //TODO edit to add CV manually
            String relicId,
            String uid,
            String mainAffixId,
            String tid,
            String type,
            String level,
            String setId,
            String setName,
            String mainType,
            Double mainValue,
            List<Map<String, Object>> subAffixes
    );

    @Query("""
            RETURN EXISTS( (:UIDNode {uid: $uid})-[:OWNS_RELIC]->(:RelicNode {relicId: $relicId}) )
            """)
    Boolean existsRelic(String relicId, String uid);

    @Query("""
            MATCH (u:UIDNode {uid: $uid})-[:HAS_BUILD]->(b:BuildNode {avatarId: $avatarId, isStatic: $isStatic})
            MATCH (b)-[:EQUIPS_RELIC]->(r:RelicNode)
            RETURN COLLECT(DISTINCT r.relicId) AS relicIds
            """)
    Set<String> getAllRelicIdsFromStaticNode(String uid, String avatarId, Boolean isStatic);

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)
        RETURN r, collect(rel), collect(sa)
        ORDER BY r.relicId
        SKIP $skip
        LIMIT $limit
    """)
    List<RelicNode> findRelicsPaged(String uid, long skip, long limit);

    @Query("""
        MATCH (b:BuildNode)-[:EQUIPS_RELIC]->(r:RelicNode {relicId: $relicId})
        RETURN DISTINCT b.avatarId AS avatarId, b.buildName AS buildName
    """)
    List<BuildProjection> findBuildsForRelic(String relicId);

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)
    
        WITH r,
             reduce(
                 statValue = 0.0,
                 x IN collect(sa) |
                 statValue +
                 CASE
                     WHEN x.type = $sortBy THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS sortValue
    
        ORDER BY sortValue ASC, r.relicId
        SKIP $skip
        LIMIT $limit
    
        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)
    
        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByStatAsc(
            String uid,
            String sortBy,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)
    
        WITH r,
             reduce(
                 statValue = 0.0,
                 x IN collect(sa) |
                 statValue +
                 CASE
                     WHEN x.type = $sortBy THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS sortValue
    
        ORDER BY sortValue DESC, r.relicId
        SKIP $skip
        LIMIT $limit
    
        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)
    
        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByStatDesc(
            String uid,
            String sortBy,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)
    
        WITH r,
             reduce(
                 cv = 0.0,
                 x IN collect(sa) |
                 cv +
                 CASE
                     WHEN x.type = 'CriticalChance' THEN toFloat(x.value) * 2
                     WHEN x.type = 'CriticalDamage' THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS CV
    
        ORDER BY CV ASC, r.relicId
        SKIP $skip
        LIMIT $limit
    
        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)
    
        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByCVAsc(
            String uid,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)
    
        WITH r,
             reduce(
                 cv = 0.0,
                 x IN collect(sa) |
                 cv +
                 CASE
                     WHEN x.type = 'CriticalChance' THEN toFloat(x.value) * 2
                     WHEN x.type = 'CriticalDamage' THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS CV
    
        ORDER BY CV DESC, r.relicId
        SKIP $skip
        LIMIT $limit
    
        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)
    
        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByCVDesc(
            String uid,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)
    
        WHERE sa.type = $filterBy
    
        WITH DISTINCT r
    
        ORDER BY r.relicId
        SKIP $skip
        LIMIT $limit
    
        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)
    
        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedFiltered(
            String uid,
            String filterBy,
            long skip,
            long limit
    );
}
