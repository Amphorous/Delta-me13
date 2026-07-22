package org.hoyo.celestia.relics.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DuplicateRelicGroupProjection {
    private String uid;
    private String relicId;
    private Long count;
}
