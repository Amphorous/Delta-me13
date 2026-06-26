package org.hoyo.celestia.builds.model;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SkillTreeConverter implements Neo4jPersistentPropertyToMapConverter<String, Map<String, Map<String, String>>> {

    @Override
    public Map<String, Value> decompose(Map<String, Map<String, String>> property, Neo4jConversionService conversionService) {
        if (property == null) {
            return Collections.emptyMap();
        }
        Map<String, Value> result = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> skill : property.entrySet()) {
            String skillId = skill.getKey();
            for (Map.Entry<String, String> prop : skill.getValue().entrySet()) {
                result.put(skillId + "." + prop.getKey(), Values.value(prop.getValue()));
            }
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> compose(Map<String, Value> source, Neo4jConversionService conversionService) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Value> entry : source.entrySet()) {
            String compoundKey = entry.getKey();
            int dotIndex = compoundKey.indexOf('.');
            if (dotIndex < 0) continue;
            String skillId = compoundKey.substring(0, dotIndex);
            String propName = compoundKey.substring(dotIndex + 1);
            result.computeIfAbsent(skillId, k -> new HashMap<>())
                    .put(propName, entry.getValue().asString());
        }
        return result;
    }
}
