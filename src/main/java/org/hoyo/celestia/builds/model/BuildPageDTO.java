package org.hoyo.celestia.builds.model;

import lombok.Data;

import java.util.List;

@Data
public class BuildPageDTO {
    List<BuildNode> builds;
    boolean hasMore;
}
