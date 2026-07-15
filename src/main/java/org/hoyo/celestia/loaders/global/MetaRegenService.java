package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.hoyo.celestia.loaders.model.metaModel.RelicMetaProperty;
import org.hoyo.celestia.loaders.model.metaModel.SetSkillData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetaRegenService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Rebuilds the in-memory meta from freshly synced assets. Individual
     * malformed/partial entries (a brand-new character published without its
     * Promotion stats yet, etc.) are SKIPPED with a warning instead of failing
     * the whole regen — one half-published entry must not keep the entire meta
     * pinned to a stale version. Skipped ids resurface through the request
     * path as MissingMetaAssetException (skip + user-facing warning), and get
     * picked up by a later sync once upstream publishes complete data.
     */
    public HonkerMetaObject regenerate(Map<String, JsonNode> assets) {
        HonkerMetaObject meta = new HonkerMetaObject();
        List<String> skipped = new ArrayList<>();

        JsonNode avatarsJson = assets.get("avatars");
        if (avatarsJson != null) {
            Map<String, Map<String, Map<String, Double>>> avatar = new HashMap<>();
            Iterator<String> avatarIds = avatarsJson.fieldNames();
            while (avatarIds.hasNext()) {
                String avatarId = avatarIds.next();
                try {
                    JsonNode promotion = avatarsJson.get(avatarId).get("Promotion");
                    if (promotion == null || promotion.isNull()) {
                        skipped.add("avatar " + avatarId + " (no Promotion data)");
                        continue;
                    }
                    avatar.put(avatarId, objectMapper.convertValue(promotion, Map.class));
                } catch (Exception e) {
                    skipped.add("avatar " + avatarId + " (" + e.getMessage() + ")");
                }
            }
            meta.setAvatar(avatar);
        }

        JsonNode weaponsJson = assets.get("weapons");
        if (weaponsJson != null) {
            Map<String, Map<String, Map<String, Double>>> equipment = new HashMap<>();
            Map<String, Map<String, Map<String, Map<String, Double>>>> equipmentSkill = new HashMap<>();
            Iterator<String> weaponIds = weaponsJson.fieldNames();
            while (weaponIds.hasNext()) {
                String weaponId = weaponIds.next();
                try {
                    JsonNode weapon = weaponsJson.get(weaponId);
                    JsonNode promotion = weapon.get("Promotion");
                    if (promotion == null || promotion.isNull()) {
                        skipped.add("weapon " + weaponId + " (no Promotion data)");
                        continue;
                    }
                    equipment.put(weaponId, objectMapper.convertValue(promotion, Map.class));
                    JsonNode equipSkill = weapon.get("EquipmentSkill");
                    if (equipSkill != null && !equipSkill.isNull()) {
                        equipmentSkill.put(weaponId, objectMapper.convertValue(equipSkill, Map.class));
                    }
                } catch (Exception e) {
                    skipped.add("weapon " + weaponId + " (" + e.getMessage() + ")");
                }
            }
            meta.setEquipment(equipment);
            meta.setEquipmentSkill(equipmentSkill);
        }

        JsonNode relicsJson = assets.get("relics");
        if (relicsJson != null && relicsJson.get("Sets") != null) {
            JsonNode setsNode = relicsJson.get("Sets");
            Map<String, Map<String, SetSkillData>> setSkill = new HashMap<>();
            Iterator<String> setIds = setsNode.fieldNames();
            while (setIds.hasNext()) {
                String setId = setIds.next();
                try {
                    JsonNode setSkillsNode = setsNode.get(setId).get("SetSkills");
                    if (setSkillsNode == null || setSkillsNode.isNull()) {
                        skipped.add("relic set " + setId + " (no SetSkills data)");
                        continue;
                    }
                    Map<String, SetSkillData> inner = new HashMap<>();
                    Iterator<String> skillKeys = setSkillsNode.fieldNames();
                    while (skillKeys.hasNext()) {
                        String key = skillKeys.next();
                        inner.put(key, objectMapper.convertValue(setSkillsNode.get(key), SetSkillData.class));
                    }
                    setSkill.put(setId, inner);
                } catch (Exception e) {
                    skipped.add("relic set " + setId + " (" + e.getMessage() + ")");
                }
            }
            RelicMetaProperty relicMeta = new RelicMetaProperty();
            relicMeta.setSetSkill(setSkill);
            meta.setRelic(relicMeta);
        }

        JsonNode treeJson = assets.get("tree");
        if (treeJson != null) {
            meta.setTree(objectMapper.convertValue(treeJson, Map.class));
        }

        JsonNode skillsJson = assets.get("skills");
        if (skillsJson != null) {
            meta.setSkills(objectMapper.convertValue(skillsJson, Map.class));
        }

        if (skipped.isEmpty()) {
            log.info("HonkerMetaObject regenerated successfully: {} avatars, {} weapons, {} relic sets.",
                    meta.getAvatar() == null ? 0 : meta.getAvatar().size(),
                    meta.getEquipment() == null ? 0 : meta.getEquipment().size(),
                    meta.getRelic() == null || meta.getRelic().getSetSkill() == null ? 0 : meta.getRelic().getSetSkill().size());
        } else {
            log.warn("HonkerMetaObject regenerated with {} skipped entr{} (incomplete upstream data — will heal on a later sync): {}",
                    skipped.size(), skipped.size() == 1 ? "y" : "ies", skipped);
        }
        return meta;
    }
}
