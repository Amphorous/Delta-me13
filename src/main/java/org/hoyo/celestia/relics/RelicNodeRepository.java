package org.hoyo.celestia.relics;

import org.hoyo.celestia.relics.DTOs.BuildProjection;
import org.hoyo.celestia.relics.DTOs.DuplicateRelicGroupProjection;
import org.hoyo.celestia.relics.DTOs.RelicProjectionDTO;
import org.hoyo.celestia.relics.model.RelicNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
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
        mainValue: $mainValue,
        cv: $cv,
        creationDate: $creationDate
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
    RelicNode insertRelic(
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
            Double cv,
            java.time.LocalDateTime creationDate,
            List<Map<String, Object>> subAffixes
    );

    // Parameter order here previously didn't match the call site
    // (`existsRelic(uid, relicId)` in SubloaderService) — Spring Data Neo4j
    // binds $uid/$relicId in the query to these DECLARED parameter names, not
    // call-site position, so the values were silently swapped and this always
    // returned false, letting createRelicNode/insertRelic recreate the "same"
    // relic (by content-hash relicId) on every refresh that revisited it.
    // Explicit @Param added so this can never silently drift out of sync with
    // its caller again, regardless of declared order.
    @Query("""
            RETURN EXISTS( (:UIDNode {uid: $uid})-[:OWNS_RELIC]->(:RelicNode {relicId: $relicId}) )
            """)
    Boolean existsRelic(@Param("uid") String uid, @Param("relicId") String relicId);

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

    // ── sort-only queries (with optional typeFilter) ──────────────────────────

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE ($typeFilter IS NULL OR r.type = $typeFilter)
        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)

        WITH r,
             CASE WHEN r.mainType = $sortBy THEN toFloat(r.mainValue) ELSE 0.0 END +
             reduce(
                 statValue = 0.0,
                 x IN collect(sa) |
                 statValue +
                 CASE
                     WHEN x.type = $sortBy THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS sortValue

        ORDER BY sortValue ASC, r.cv DESC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByStatAsc(
            String uid,
            String sortBy,
            String typeFilter,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE ($typeFilter IS NULL OR r.type = $typeFilter)
        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)

        WITH r,
             CASE WHEN r.mainType = $sortBy THEN toFloat(r.mainValue) ELSE 0.0 END +
             reduce(
                 statValue = 0.0,
                 x IN collect(sa) |
                 statValue +
                 CASE
                     WHEN x.type = $sortBy THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS sortValue

        ORDER BY sortValue DESC, r.cv DESC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByStatDesc(
            String uid,
            String sortBy,
            String typeFilter,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE ($typeFilter IS NULL OR r.type = $typeFilter)

        ORDER BY r.cv ASC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByCVAsc(
            String uid,
            String typeFilter,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE ($typeFilter IS NULL OR r.type = $typeFilter)

        ORDER BY r.cv DESC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedSortedByCVDesc(
            String uid,
            String typeFilter,
            long skip,
            long limit
    );

    // ── filter + sort queries (with optional typeFilter) ──────────────────────

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE (($filterField = 'setName' AND r.setName = $filterValue)
           OR ($filterField = 'tid' AND r.tid = $filterValue)
           OR ($filterField = 'type' AND r.type = $filterValue)
           OR ($filterField = 'setId' AND r.setId = $filterValue))
          AND ($typeFilter IS NULL OR r.type = $typeFilter)

        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)

        WITH r,
             CASE WHEN r.mainType = $sortBy THEN toFloat(r.mainValue) ELSE 0.0 END +
             reduce(
                 statValue = 0.0,
                 x IN collect(sa) |
                 statValue +
                 CASE
                     WHEN x.type = $sortBy THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS sortValue

        ORDER BY sortValue ASC, r.cv DESC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedFilteredSortedByStatAsc(
            String uid,
            String filterField,
            String filterValue,
            String sortBy,
            String typeFilter,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE (($filterField = 'setName' AND r.setName = $filterValue)
           OR ($filterField = 'tid' AND r.tid = $filterValue)
           OR ($filterField = 'type' AND r.type = $filterValue)
           OR ($filterField = 'setId' AND r.setId = $filterValue))
          AND ($typeFilter IS NULL OR r.type = $typeFilter)

        OPTIONAL MATCH (r)-[:SUBAFFIX]->(sa:SubAffixNode)

        WITH r,
             CASE WHEN r.mainType = $sortBy THEN toFloat(r.mainValue) ELSE 0.0 END +
             reduce(
                 statValue = 0.0,
                 x IN collect(sa) |
                 statValue +
                 CASE
                     WHEN x.type = $sortBy THEN toFloat(x.value)
                     ELSE 0
                 END
             ) AS sortValue

        ORDER BY sortValue DESC, r.cv DESC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedFilteredSortedByStatDesc(
            String uid,
            String filterField,
            String filterValue,
            String sortBy,
            String typeFilter,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE (($filterField = 'setName' AND r.setName = $filterValue)
           OR ($filterField = 'tid' AND r.tid = $filterValue)
           OR ($filterField = 'type' AND r.type = $filterValue)
           OR ($filterField = 'setId' AND r.setId = $filterValue))
          AND ($typeFilter IS NULL OR r.type = $typeFilter)

        ORDER BY r.cv ASC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedFilteredSortedByCVAsc(
            String uid,
            String filterField,
            String filterValue,
            String typeFilter,
            long skip,
            long limit
    );

    @Query("""
        MATCH (u:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE (($filterField = 'setName' AND r.setName = $filterValue)
           OR ($filterField = 'tid' AND r.tid = $filterValue)
           OR ($filterField = 'type' AND r.type = $filterValue)
           OR ($filterField = 'setId' AND r.setId = $filterValue))
          AND ($typeFilter IS NULL OR r.type = $typeFilter)

        ORDER BY r.cv DESC, r.relicId
        SKIP $skip
        LIMIT $limit

        OPTIONAL MATCH (r)-[rel:SUBAFFIX]->(sa:SubAffixNode)

        RETURN r, collect(rel), collect(sa)
    """)
    List<RelicNode> findRelicsPagedFilteredSortedByCVDesc(
            String uid,
            String filterField,
            String filterValue,
            String typeFilter,
            long skip,
            long limit
    );

    // Aggregate (not a bare per-row RETURN r.cv) so this always yields exactly
    // one row regardless of how many RelicNodes currently match {uid, relicId}
    // — a plain per-row RETURN throws IncorrectResultSizeDataAccessException
    // the moment a uid ends up owning more than one RelicNode with the same
    // relicId. Root cause was the existsRelic parameter-order bug above (fixed
    // now); this aggregate stays regardless, so a stray duplicate can never
    // crash the whole upsert — see shouldICalulateAgain, which calls this once
    // per relic the character no longer has equipped.
    @Query("""
        MATCH (:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode {relicId: $relicId})
        RETURN coalesce(max(r.cv), 0.0)
    """)
    Double getRelicCv(String uid, String relicId);

    @Query("""
        MATCH (:UIDNode {uid: $uid})-[:OWNS_RELIC]->(r:RelicNode)
        WHERE r.relicId IN $relicIds
        RETURN coalesce(sum(r.cv), 0.0)
    """)
    Double getTotalRelicCv(String uid, Set<String> relicIds);

    // ── relic-dedupe maintenance (see RelicMaintenanceController) ─────────────
    // Read-only report: every (uid, relicId) pair currently owned more than once.
    // Grouping is scoped per-UID (u carried in the WITH below) — relicId is a
    // content hash of the relic's own stats (see CreateRelicService.calculateRelicId),
    // not a globally unique instance id, so this intentionally only flags
    // repeats WITHIN one account, never across different accounts.
    @Query("""
        MATCH (u:UIDNode)-[:OWNS_RELIC]->(r:RelicNode)
        WITH u.uid AS uid, r.relicId AS relicId, count(r) AS count
        WHERE count > 1
        RETURN uid, relicId, count
        ORDER BY count DESC
    """)
    List<DuplicateRelicGroupProjection> findDuplicateRelicGroups();

    // Collapses every duplicate group down to one surviving RelicNode (relics[0]),
    // redirecting any EQUIPS_RELIC edge a BuildNode has to a duplicate onto the
    // survivor first (MERGE so a build already pointing at the survivor doesn't
    // get a second parallel edge), then deletes the duplicates and their own
    // SubAffixNode children. `WITH DISTINCT dup` before the delete collapses back
    // to one row per duplicate node regardless of how many builds referenced it —
    // without it, a duplicate equipped by 2+ builds would be matched/deleted more
    // than once in the same query and Neo4j would error on the second attempt.
    @Query("""
        MATCH (u:UIDNode)-[:OWNS_RELIC]->(r:RelicNode)
        WITH u, r.relicId AS relicId, collect(r) AS relics
        WHERE size(relics) > 1
        WITH relics[0] AS keep, relics[1..] AS dupes
        UNWIND dupes AS dup
            OPTIONAL MATCH (b:BuildNode)-[:EQUIPS_RELIC]->(dup)
            FOREACH (build IN CASE WHEN b IS NOT NULL THEN [b] ELSE [] END |
                MERGE (build)-[:EQUIPS_RELIC]->(keep)
            )
        WITH DISTINCT dup
        OPTIONAL MATCH (dup)-[:SUBAFFIX]->(sa:SubAffixNode)
        DETACH DELETE dup, sa
        RETURN count(DISTINCT dup)
    """)
    Long dedupeAllRelics();
}
