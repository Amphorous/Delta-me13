package org.hoyo.celestia.relics.DTOs;

import lombok.Data;

import java.util.List;

@Data
public class RelicPageDTO {
    private List<RelicProjectionDTO> relics;
    private boolean hasMore;
}
