package org.hoyo.celestia.builds.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BuildEditResultDTO {
    Boolean status;
    String message;
}
