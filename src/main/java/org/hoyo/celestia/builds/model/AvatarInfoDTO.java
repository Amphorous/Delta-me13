package org.hoyo.celestia.builds.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Avatar display data attached to each build, assembled from the avatarInfo:{avatarId}
 * Redis blob. Name hashes stay Strings — textmap hashes can exceed Long range.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvatarInfoDTO {

    @JsonProperty("AvatarNameHash")
    private String avatarNameHash;

    @JsonProperty("AvatarFullNameHash")
    private String avatarFullNameHash;

    @JsonProperty("Rarity")
    private Integer rarity;

    @JsonProperty("Element")
    private String element;

    @JsonProperty("AvatarBaseType")
    private String avatarBaseType;

    @JsonProperty("AvatarSideIconPath")
    private String avatarSideIconPath;

    @JsonProperty("AvatarCutinFrontImgPath")
    private String avatarCutinFrontImgPath;

    // skinId -> icon paths
    @JsonProperty("Skins")
    private Map<String, SkinInfoDTO> skins;

    // rank number ("1".."6") -> icon path
    @JsonProperty("Ranks")
    private Map<String, String> ranks;
}
