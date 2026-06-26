package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.hoyo.celestia.loaders.model.metaModel.RelicMetaProperty;
import org.hoyo.celestia.loaders.model.metaModel.SetSkillData;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetaRegenService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HonkerMetaObject regenerate(Map<String, JsonNode> assets) {
        HonkerMetaObject meta = new HonkerMetaObject();

        JsonNode avatarsJson = assets.get("avatars");
        if (avatarsJson != null) {
            Map<String, Map<String, Map<String, Double>>> avatar = new HashMap<>();
            Iterator<String> avatarIds = avatarsJson.fieldNames();
            while (avatarIds.hasNext()) {
                String avatarId = avatarIds.next();
                JsonNode promotion = avatarsJson.get(avatarId).get("Promotion");
                avatar.put(avatarId, objectMapper.convertValue(promotion, Map.class));
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
                JsonNode weapon = weaponsJson.get(weaponId);
                equipment.put(weaponId, objectMapper.convertValue(weapon.get("Promotion"), Map.class));
                equipmentSkill.put(weaponId, objectMapper.convertValue(weapon.get("EquipmentSkill"), Map.class));
            }
            meta.setEquipment(equipment);
            meta.setEquipmentSkill(equipmentSkill);
        }

        JsonNode relicsJson = assets.get("relics");
        if (relicsJson != null) {
            JsonNode setsNode = relicsJson.get("Sets");
            Map<String, Map<String, SetSkillData>> setSkill = new HashMap<>();
            Iterator<String> setIds = setsNode.fieldNames();
            while (setIds.hasNext()) {
                String setId = setIds.next();
                JsonNode setSkillsNode = setsNode.get(setId).get("SetSkills");
                Map<String, SetSkillData> inner = new HashMap<>();
                Iterator<String> skillKeys = setSkillsNode.fieldNames();
                while (skillKeys.hasNext()) {
                    String key = skillKeys.next();
                    inner.put(key, objectMapper.convertValue(setSkillsNode.get(key), SetSkillData.class));
                }
                setSkill.put(setId, inner);
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

        log.info("HonkerMetaObject regenerated successfully.");
        return meta;
    }
}
