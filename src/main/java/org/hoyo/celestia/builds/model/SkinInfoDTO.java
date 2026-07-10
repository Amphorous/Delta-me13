package org.hoyo.celestia.builds.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinInfoDTO {

    @JsonProperty("AvatarSideIconPath")
    private String avatarSideIconPath;

    @JsonProperty("AvatarCutinFrontImgPath")
    private String avatarCutinFrontImgPath;
}
