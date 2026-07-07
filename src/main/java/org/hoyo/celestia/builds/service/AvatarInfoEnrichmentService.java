package org.hoyo.celestia.builds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.builds.model.AvatarInfoDTO;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.model.SkinInfoDTO;
import org.hoyo.celestia.loaders.global.AvatarInfoRedisLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Attaches avatar display data (from the avatarInfo:{avatarId} Redis blobs written
 * by AvatarInfoRedisLoader) to a page of builds. Best-effort: any failure leaves
 * avatarInfo null on the affected builds and never breaks the response.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvatarInfoEnrichmentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void enrich(List<BuildNode> builds) {
        if (builds == null || builds.isEmpty()) return;
        try {
            List<String> avatarIds = builds.stream()
                    .map(BuildNode::getAvatarId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (avatarIds.isEmpty()) return;

            List<String> keys = avatarIds.stream()
                    .map(id -> AvatarInfoRedisLoader.AVATAR_INFO_KEY_PREFIX + id)
                    .toList();
            List<String> blobs = redisTemplate.opsForValue().multiGet(keys);

            Map<String, AvatarInfoDTO> byAvatarId = new HashMap<>();
            for (int i = 0; i < avatarIds.size(); i++) {
                String blob = blobs != null ? blobs.get(i) : null;
                if (blob == null) continue; // avatar not in Redis yet -> skip
                try {
                    byAvatarId.put(avatarIds.get(i), mapBlob(blob));
                } catch (Exception e) {
                    log.warn("Malformed avatarInfo blob for avatarId {}", avatarIds.get(i), e);
                }
            }

            builds.forEach(build -> build.setAvatarInfo(byAvatarId.get(build.getAvatarId())));
        } catch (Exception e) {
            log.warn("Avatar info enrichment skipped: {}", e.getMessage());
        }
    }

    private AvatarInfoDTO mapBlob(String json) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(json);
        AvatarInfoDTO dto = new AvatarInfoDTO();
        dto.setAvatarNameHash(node.path("AvatarName").path("Hash").asText(null));
        dto.setAvatarFullNameHash(node.path("AvatarFullName").path("Hash").asText(null));
        dto.setRarity(node.hasNonNull("Rarity") ? node.get("Rarity").asInt() : null);
        dto.setElement(node.path("Element").asText(null));
        dto.setAvatarBaseType(node.path("AvatarBaseType").asText(null));
        dto.setAvatarSideIconPath(node.path("AvatarSideIconPath").asText(null));
        dto.setAvatarCutinFrontImgPath(node.path("AvatarCutinFrontImgPath").asText(null));
        if (node.has("Skins")) {
            dto.setSkins(objectMapper.convertValue(node.get("Skins"),
                    new TypeReference<Map<String, SkinInfoDTO>>() {}));
        }
        if (node.has("Ranks")) {
            dto.setRanks(objectMapper.convertValue(node.get("Ranks"),
                    new TypeReference<Map<String, String>>() {}));
        }
        return dto;
    }
}
