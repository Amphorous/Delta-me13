package org.hoyo.celestia.builds.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Map;

@Data
@Node
public class SkillTree {

    // skill tree contains Map<SkillId, Map<propName, propVal>>.
    // propName => {IconPath, PointType, SpeedDelta(or other node stats)}
    // propVal => String (casted)
    // IconPath, PointType exists for all skills in metaFile.skills (honker_meta.json)
    // stat being added is from metaFile.tree.get("skillId").getFirst().get("props")

    @Id
    @GeneratedValue
    private Long id;

    @CompositeProperty(converter = SkillTreeConverter.class)
    private Map<String, Map<String, String>> skills;
}
