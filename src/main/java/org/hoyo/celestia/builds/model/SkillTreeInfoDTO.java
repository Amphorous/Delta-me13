package org.hoyo.celestia.builds.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * One SkillTree entry from the avatarInfo Redis blob, keyed by form index ("0", ...).
 * Each entry maps skillId -> IconPath (resolved from skills.json at Redis load time
 * by AvatarInfoRedisLoader). PropSkills groups the stat-bonus trace nodes per branch;
 * SummonSkills is populated for remembrance characters (memosprite skills), empty
 * otherwise.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillTreeInfoDTO {

    @JsonProperty("AvatarSkills")
    private Map<String, String> avatarSkills;

    @JsonProperty("PropSkills")
    private List<Map<String, String>> propSkills;

    @JsonProperty("SummonSkills")
    private List<Map<String, String>> summonSkills;
}
