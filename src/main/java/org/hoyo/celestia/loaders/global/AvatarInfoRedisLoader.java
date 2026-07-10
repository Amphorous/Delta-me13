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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // last-known-good parsed sources, so a refresh where only some of the files
    // changed can still rebuild the combined avatarInfo blobs
    private volatile JsonNode avatarsJson;
    private volatile JsonNode ranksJson;
    private volatile JsonNode skillsJson;
    private volatile boolean loaded = false;

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
        if (newAvatars == null && newRanks == null && newSkills == null && loaded) {
            return;
        }
        if (newAvatars != null) avatarsJson = newAvatars;
        if (newRanks != null) ranksJson = newRanks;
        if (newSkills != null) skillsJson = newSkills;
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
        Iterator<String> avatarIds = avatars.fieldNames();
        while (avatarIds.hasNext()) {
            String avatarId = avatarIds.next();
            ObjectNode blob = avatars.get(avatarId).deepCopy();
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
            return null;
        });

        loaded = true;
        log.info("Wrote {} avatarInfo blobs, {} avatarRank icons and {} skillIcon paths to Redis.",
                avatarBlobs.size(), rankIcons.size(), skillIcons.size());
    }

    /** id -> IconPath for any json shaped Map<id, {IconPath: ..., ...}> (ranks.json, skills.json). */
    private Map<String, String> extractIconPaths(JsonNode source) {
        Map<String, String> icons = new LinkedHashMap<>();
        Iterator<String> ids = source.fieldNames();
        while (ids.hasNext()) {
            String id = ids.next();
            JsonNode icon = source.get(id).get("IconPath");
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
