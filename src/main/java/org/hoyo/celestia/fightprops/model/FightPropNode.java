package org.hoyo.celestia.fightprops.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Map;

@Data
@Node
public class FightPropNode {

    @Id
    @GeneratedValue
    private Long id;

    @CompositeProperty
    private Map<String, Double> stats;
}
