package org.hoyo.celestia.relics.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Data
@Node
public class RelicNode {
    @Id
    @GeneratedValue
    private Long id;
    private String relicId;
    private String mainAffixId;
    private String tid;
    private String type;
    private String level;
    private String setId;
    private String setName;
    private String mainType;
    private Double mainValue;
    private Double cv;

    @Relationship(type = "SUBAFFIX", direction = Relationship.Direction.OUTGOING)
    private List<SubAffixNode> subAffixes;
}
