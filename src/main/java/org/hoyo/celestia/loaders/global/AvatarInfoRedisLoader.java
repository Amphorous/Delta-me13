package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Loads avatar display data (avatars.json minus Promotion) and eidolon rank icon
 * paths (ranks.json) into Redis, where they are read frequently by the build
 * enrichment path and potentially other services sharing the instance.
 *
 * Keys:
 *   avatarInfo:{avatarId} -> JSON blob of the avatar minus "Promotion", plus a
 *                            precomputed "Ranks" map { "1": iconPath, ... } so the
 *                            read path needs a single MGET per page of builds
 *   avatarRank:{rankId}   -> plain IconPath string (rank number = last 2 digits of rankId)
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

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // last-known-good parsed sources, so a refresh where only one of the two files
    // changed can still rebuild the combined avatarInfo blobs
    private volatile JsonNode avatarsJson;
    private volatile JsonNode ranksJson;
    private volatile boolean loaded = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadFromClasspath();
    }

    public void loadFromClasspath() {
        try {
            try (InputStream avatars = new ClassPathResource("assets/avatars.json").getInputStream();
                 InputStream ranks = new ClassPathResource("assets/ranks.json").getInputStream()) {
                avatarsJson = objectMapper.readTree(avatars);
                ranksJson = objectMapper.readTree(ranks);
            }
            writeToRedis();
        } catch (Exception e) {
            log.error("Failed to load avatar/rank info into Redis; will retry on next asset refresh.", e);
        }
    }

    /**
     * Re-writes Redis from whatever sources are available, parsing the bundled
     * classpath files first if needed. Called on every refresh cycle even when no
     * upstream assets changed, so a flushed/restarted Redis self-heals on the next
     * cron tick or manual POST /admin/meta/refresh.
     */
    public void ensureLoaded() {
        if (avatarsJson == null || ranksJson == null) {
            loadFromClasspath();
            return;
        }
        try {
            writeToRedis();
        } catch (Exception e) {
            log.error("Failed to rewrite avatar/rank info in Redis.", e);
        }
    }

    /** Called by the asset refresh cycle with the freshly synced files (may lack either). */
    public void refresh(Map<String, JsonNode> assets) {
        if (assets == null) return;
        JsonNode newAvatars = assets.get("avatars");
        JsonNode newRanks = assets.get("ranks");
        if (newAvatars == null && newRanks == null && loaded) {
            return;
        }
        if (newAvatars != null) avatarsJson = newAvatars;
        if (newRanks != null) ranksJson = newRanks;
        try {
            writeToRedis();
        } catch (Exception e) {
            log.error("Failed to rewrite avatar/rank info in Redis during asset refresh.", e);
        }
    }

    private void writeToRedis() {
        JsonNode avatars = avatarsJson;
        JsonNode ranks = ranksJson;
        if (avatars == null || ranks == null) {
            log.warn("Avatar/rank source data not available yet, skipping Redis write.");
            return;
        }

        Map<String, String> rankIcons = new LinkedHashMap<>();
        Iterator<String> rankIds = ranks.fieldNames();
        while (rankIds.hasNext()) {
            String rankId = rankIds.next();
            JsonNode icon = ranks.get(rankId).get("IconPath");
            if (icon != null) {
                rankIcons.put(rankId, icon.asText());
            }
        }

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
            avatarBlobs.put(avatarId, blob.toString());
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            rankIcons.forEach((rankId, icon) -> connection.stringCommands().set(
                    (AVATAR_RANK_KEY_PREFIX + rankId).getBytes(StandardCharsets.UTF_8),
                    icon.getBytes(StandardCharsets.UTF_8)));
            avatarBlobs.forEach((avatarId, blob) -> connection.stringCommands().set(
                    (AVATAR_INFO_KEY_PREFIX + avatarId).getBytes(StandardCharsets.UTF_8),
                    blob.getBytes(StandardCharsets.UTF_8)));
            return null;
        });

        loaded = true;
        log.info("Wrote {} avatarInfo blobs and {} avatarRank icons to Redis.",
                avatarBlobs.size(), rankIcons.size());
    }
}
