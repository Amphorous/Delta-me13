package org.hoyo.celestia.builds.service;

import org.hoyo.celestia.builds.model.SkillTree;
import org.hoyo.celestia.loaders.global.GlobalMetaFileLoader;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.hoyo.celestia.user.model.Skill;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class SkillTreeService {

    private final GlobalMetaFileLoader globalMetaFileLoader;

    public SkillTreeService(GlobalMetaFileLoader globalMetaFileLoader) {
        this.globalMetaFileLoader = globalMetaFileLoader;
    }

    public SkillTree getSkillTree(String avatarId, ArrayList<Skill> skillTreeList) {
        HonkerMetaObject metaFile = globalMetaFileLoader.getMetaFile();
        Map<String, Map<String, String>> skillsData = new HashMap<>();

        Map<String, Map<String, Object>> allSkills = metaFile.getSkills();
        if (allSkills == null) {
            SkillTree skillTree = new SkillTree();
            skillTree.setSkills(skillsData);
            return skillTree;
        }

        Map<String, Integer> levelByPointId = new HashMap<>();
        if (skillTreeList != null) {
            for (Skill skill : skillTreeList) {
                levelByPointId.put(String.valueOf(skill.getPointId()), skill.getLevel());
            }
        }

        for (Map.Entry<String, Map<String, Object>> entry : allSkills.entrySet()) {
            String skillId = entry.getKey();
            if (!skillId.startsWith(avatarId)) continue;

            Map<String, String> skillInfo = new HashMap<>();
            for (Map.Entry<String, Object> prop : entry.getValue().entrySet()) {
                skillInfo.put(prop.getKey(), String.valueOf(prop.getValue()));
            }

            skillInfo.put("Level", String.valueOf(levelByPointId.getOrDefault(skillId, 0)));

            if ("1".equals(skillInfo.get("PointType"))) {
                var tree = metaFile.getTree();
                if (tree != null && tree.containsKey(skillId)) {
                    var levelMap = tree.get(skillId);
                    if (levelMap != null && levelMap.containsKey("1")) {
                        Map<String, Double> props = levelMap.get("1").get("props");
                        if (props != null) {
                            for (Map.Entry<String, Double> stat : props.entrySet()) {
                                skillInfo.put(stat.getKey(), String.valueOf(stat.getValue()));
                            }
                        }
                    }
                }
            }

            skillsData.put(skillId, skillInfo);
        }

        SkillTree skillTree = new SkillTree();
        skillTree.setSkills(skillsData);
        return skillTree;
    }
}
