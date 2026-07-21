package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads avatar display data (avatars.json minus Promotion), eidolon rank icon paths
 * (ranks.json) and skill icon paths (skills.json) into Redis, where they are read
 * frequently by the build enrichment path and potentially other services sharing
 * the instance.
 *
 * Keys:
 *   avatarInfo:{avatarId} -> JSON blob of the avatar minus "Promotion", plus a
 *                            precomputed "Ranks" map { "1": iconPath, ... }, and with
 *                            the SkillTree id lists (AvatarSkills/PropSkills/
 *                            SummonSkills) rewritten to { skillId: iconPath } maps —
 *                            so the read path needs a single MGET per page of builds
 *   avatarRank:{rankId}   -> plain IconPath string (rank number = last 2 digits of rankId)
 *   skillIcon:{skillId}   -> plain IconPath string (skills.json tree-node ids)
 *   pfpIcons              -> ONE JSON blob { headIconId: iconPath, ... } (pfps.json) —
 *                            stored whole rather than per-id since its only consumer
 *                            (GET /user/pfps, backing the frontend's profile-picture
 *                            lookup) always wants the entire map in a single GET
 *
 * Also builds an in-memory (not Redis) reverse index — Path/Element -> the set of
 * avatarIds with that Path/Element — used by the builds "filter by path/element"
 * feature. This never needs to be shared cross-instance the way the Redis-backed
 * data above does (each instance cheaply recomputes the same small, ~200-avatar
 * index from the same source data), so a plain volatile field is enough; no Redis
 * round-trip needed to resolve a filter at request time. See getAvatarIdsForPath/
 * getAvatarIdsForElement.
 *
 * Runs once on startup from the bundled classpath resources, and again with fresh
 * data whenever the asset refresh cycle detects upstream changes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvatarInfoRedisLoader {

    public static final String AVATAR_INFO_KEY_PREFIX = "avatarInfo:";
    public static final String AVATAR_RANK_KEY_PREFIX = "avatarRank:";
    public static final String SKILL_ICON_KEY_PREFIX = "skillIcon:";
    public static final String PFP_ICONS_KEY = "pfpIcons";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // last-known-good parsed sources, so a refresh where only some of the files
    // changed can still rebuild the combined avatarInfo blobs
    private volatile JsonNode avatarsJson;
    private volatile JsonNode ranksJson;
    private volatile JsonNode skillsJson;
    private volatile JsonNode pfpsJson;
    private volatile boolean loaded = false;

    // Reverse index for the builds path/element filter — rebuilt in the same pass
    // as avatarBlobs below, never independently. Immutable snapshots (built fully,
    // then assigned once) so a concurrent reader always sees a complete map, never
    // a partially-built one — same safe-publication idiom as the JsonNode fields above.
    private volatile Map<String, Set<String>> avatarIdsByPath = Map.of();
    private volatile Map<String, Set<String>> avatarIdsByElement = Map.of();

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadFromClasspath();
    }

    public void loadFromClasspath() {
        try {
            try (InputStream avatars = new ClassPathResource("assets/avatars.json").getInputStream();
                 InputStream ranks = new ClassPathResource("assets/ranks.json").getInputStream();
                 InputStream skills = new ClassPathResource("assets/skills.json").getInputStream()) {
                avatarsJson = objectMapper.readTree(avatars);
                ranksJson = objectMapper.readTree(ranks);
                skillsJson = objectMapper.readTree(skills);
            }
            // pfps is optional on purpose: a jar built before pfps.json was
            // bundled must still load the avatar/rank/skill data above.
            try (InputStream pfps = new ClassPathResource("assets/pfps.json").getInputStream()) {
                pfpsJson = objectMapper.readTree(pfps);
            } catch (Exception e) {
                log.warn("Bundled assets/pfps.json not available; pfp icons will load on the next asset refresh.", e);
            }
            writeToRedis();
        } catch (Exception e) {
            log.error("Failed to load avatar/rank/skill info into Redis; will retry on next asset refresh.", e);
        }
    }

    /**
     * Re-writes Redis from whatever sources are available, parsing the bundled
     * classpath files first if needed. Called on every refresh cycle even when no
     * upstream assets changed, so a flushed/restarted Redis self-heals on the next
     * cron tick or manual POST /admin/meta/refresh.
     */
    public void ensureLoaded() {
        if (avatarsJson == null || ranksJson == null || skillsJson == null) {
            loadFromClasspath();
            return;
        }
        try {
            writeToRedis();
        } catch (Exception e) {
            log.error("Failed to rewrite avatar/rank/skill info in Redis.", e);
        }
    }

    /** Called by the asset refresh cycle with the freshly synced files (may lack any of them). */
    public void refresh(Map<String, JsonNode> assets) {
        if (assets == null) return;
        JsonNode newAvatars = assets.get("avatars");
        JsonNode newRanks = assets.get("ranks");
        JsonNode newSkills = assets.get("skills");
        JsonNode newPfps = assets.get("pfps");
        if (newAvatars == null && newRanks == null && newSkills == null && newPfps == null && loaded) {
            return;
        }
        if (newAvatars != null) avatarsJson = newAvatars;
        if (newRanks != null) ranksJson = newRanks;
        if (newSkills != null) skillsJson = newSkills;
        if (newPfps != null) pfpsJson = newPfps;
        try {
            writeToRedis();
        } catch (Exception e) {
            log.error("Failed to rewrite avatar/rank/skill info in Redis during asset refresh.", e);
        }
    }

    private void writeToRedis() {
        JsonNode avatars = avatarsJson;
        JsonNode ranks = ranksJson;
        JsonNode skills = skillsJson;
        if (avatars == null || ranks == null || skills == null) {
            log.warn("Avatar/rank/skill source data not available yet, skipping Redis write.");
            return;
        }

        Map<String, String> rankIcons = extractIconPaths(ranks);
        Map<String, String> skillIcons = extractIconPaths(skills);

        Map<String, String> avatarBlobs = new LinkedHashMap<>();
        Map<String, Set<String>> pathIndex = new HashMap<>();
        Map<String, Set<String>> elementIndex = new HashMap<>();
        Iterator<String> avatarIds = avatars.fieldNames();
        while (avatarIds.hasNext()) {
            String avatarId = avatarIds.next();
            ObjectNode blob = avatars.get(avatarId).deepCopy();

            // AvatarBaseType (Path)/Element are top-level siblings of Promotion,
            // so reading them before it's removed below is just a matter of
            // ordering, not a dependency — indexed here since this loop already
            // visits every avatar once, at zero extra I/O cost.
            String path = blob.path("AvatarBaseType").asText(null);
            String element = blob.path("Element").asText(null);
            if (path != null) pathIndex.computeIfAbsent(path, k -> new HashSet<>()).add(avatarId);
            if (element != null) elementIndex.computeIfAbsent(element, k -> new HashSet<>()).add(avatarId);

            blob.remove("Promotion");
            ObjectNode ranksNode = blob.putObject("Ranks");
            for (JsonNode rankIdNode : blob.path("RankIDList")) {
                long rankId = rankIdNode.asLong();
                String icon = rankIcons.get(String.valueOf(rankId));
                if (icon != null) {
                    ranksNode.put(String.valueOf(rankId % 100), icon);
                }
            }
            resolveSkillTreeIcons(blob, skillIcons);
            avatarBlobs.put(avatarId, blob.toString());
        }

        // Build fully, then swap in one assignment (see the field comments) —
        // this class's existing safe-publication idiom for volatile fields.
        this.avatarIdsByPath = pathIndex.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));
        this.avatarIdsByElement = elementIndex.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));

        // pfps.json keys its icon path "Icon" (not "IconPath" like ranks/skills)
        Map<String, String> pfpIcons = pfpsJson == null
                ? Map.of()
                : extractIconPaths(pfpsJson, "Icon");
        String pfpIconsBlob;
        try {
            pfpIconsBlob = pfpIcons.isEmpty() ? null : objectMapper.writeValueAsString(pfpIcons);
        } catch (Exception e) {
            log.error("Failed to serialize pfp icon map, skipping its Redis write.", e);
            pfpIconsBlob = null;
        }
        String finalPfpIconsBlob = pfpIconsBlob;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            rankIcons.forEach((rankId, icon) -> connection.stringCommands().set(
                    (AVATAR_RANK_KEY_PREFIX + rankId).getBytes(StandardCharsets.UTF_8),
                    icon.getBytes(StandardCharsets.UTF_8)));
            skillIcons.forEach((skillId, icon) -> connection.stringCommands().set(
                    (SKILL_ICON_KEY_PREFIX + skillId).getBytes(StandardCharsets.UTF_8),
                    icon.getBytes(StandardCharsets.UTF_8)));
            avatarBlobs.forEach((avatarId, blob) -> connection.stringCommands().set(
                    (AVATAR_INFO_KEY_PREFIX + avatarId).getBytes(StandardCharsets.UTF_8),
                    blob.getBytes(StandardCharsets.UTF_8)));
            if (finalPfpIconsBlob != null) {
                connection.stringCommands().set(
                        PFP_ICONS_KEY.getBytes(StandardCharsets.UTF_8),
                        finalPfpIconsBlob.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        loaded = true;
        log.info("Wrote {} avatarInfo blobs, {} avatarRank icons, {} skillIcon paths and {} pfp icons to Redis.",
                avatarBlobs.size(), rankIcons.size(), skillIcons.size(), pfpIcons.size());
    }

    /**
     * Read side of the pfpIcons blob: headIconId -> iconPath, empty map when
     * the key is missing (Redis flushed and not yet re-seeded) or unparseable.
     */
    public Map<String, String> getPfpIcons() {
        try {
            String blob = redisTemplate.opsForValue().get(PFP_ICONS_KEY);
            if (blob == null) {
                return Map.of();
            }
            return objectMapper.readValue(blob, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to read pfp icon map from Redis.", e);
            return Map.of();
        }
    }

    /**
     * avatarIds whose Path (AvatarBaseType) matches, or an empty Set if the path
     * is unknown/null or the index hasn't loaded yet — never null, never throws,
     * so callers can feed the result straight into a Cypher `IN` list with no
     * special-casing (an empty Set there just yields zero matching builds).
     */
    public Set<String> getAvatarIdsForPath(String path) {
        return path == null ? Set.of() : avatarIdsByPath.getOrDefault(path, Set.of());
    }

    /** Same as {@link #getAvatarIdsForPath}, keyed by Element instead. */
    public Set<String> getAvatarIdsForElement(String element) {
        return element == null ? Set.of() : avatarIdsByElement.getOrDefault(element, Set.of());
    }

    /** id -> IconPath for any json shaped Map<id, {IconPath: ..., ...}> (ranks.json, skills.json). */
    private Map<String, String> extractIconPaths(JsonNode source) {
        return extractIconPaths(source, "IconPath");
    }

    /** Same, for sources whose icon field has a different name (pfps.json uses "Icon"). */
    private Map<String, String> extractIconPaths(JsonNode source, String iconField) {
        Map<String, String> icons = new LinkedHashMap<>();
        Iterator<String> ids = source.fieldNames();
        while (ids.hasNext()) {
            String id = ids.next();
            JsonNode icon = source.get(id).get(iconField);
            if (icon != null) {
                icons.put(id, icon.asText());
            }
        }
        return icons;
    }

    /**
     * Rewrites the blob's SkillTree id lists in place: AvatarSkills [id, ...] becomes
     * { id: iconPath, ... }; PropSkills/SummonSkills [[id, ...], ...] keep their
     * branch grouping but each branch becomes such a map.
     */
    private void resolveSkillTreeIcons(ObjectNode blob, Map<String, String> skillIcons) {
        JsonNode skillTree = blob.get("SkillTree");
        if (!(skillTree instanceof ObjectNode skillTreeNode)) return;

        Iterator<String> forms = skillTreeNode.fieldNames();
        while (forms.hasNext()) {
            JsonNode form = skillTreeNode.get(forms.next());
            if (!(form instanceof ObjectNode formNode)) continue;

            if (formNode.has("AvatarSkills")) {
                formNode.set("AvatarSkills", idsToIconMap(formNode.get("AvatarSkills"), skillIcons));
            }
            if (formNode.has("PropSkills")) {
                formNode.set("PropSkills", idGroupsToIconMaps(formNode.get("PropSkills"), skillIcons));
            }
            if (formNode.has("SummonSkills")) {
                formNode.set("SummonSkills", idGroupsToIconMaps(formNode.get("SummonSkills"), skillIcons));
            }
        }
    }

    private ObjectNode idsToIconMap(JsonNode idArray, Map<String, String> skillIcons) {
        ObjectNode iconMap = objectMapper.createObjectNode();
        for (JsonNode idNode : idArray) {
            String id = idNode.asText();
            iconMap.put(id, skillIcons.get(id));
        }
        return iconMap;
    }

    private ArrayNode idGroupsToIconMaps(JsonNode idGroups, Map<String, String> skillIcons) {
        ArrayNode groups = objectMapper.createArrayNode();
        for (JsonNode group : idGroups) {
            groups.add(idsToIconMap(group, skillIcons));
        }
        return groups;
    }
}
