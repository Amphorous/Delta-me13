package org.hoyo.celestia.buffEffects.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamMemberDTO implements Serializable {

    private Map<String, Double> stats; // fight prop map
    private Map<Integer, Integer> relicSets; // sets map
    private String weaponId;
    private Integer weaponRank;

    // @JsonIgnore matters here, not just style - without it Jackson treats
    // this as a "clone" property and calls it during serialization, which
    // would recursively embed another full (also-cloneable) TeamMemberDTO
    // inside the JSON forever.
    // Handwritten instead of SerializationUtils.clone() - stats/relicSets
    // are the only mutable state (weaponId/weaponRank are immutable types),
    // and their keys/values are all immutable too, so a fresh HashMap per
    // field is a complete, decoupled copy without going through
    // serialize-then-deserialize at all.
    @JsonIgnore
    public TeamMemberDTO getClone(){
        TeamMemberDTO clone = new TeamMemberDTO();
        clone.setStats(stats != null ? new HashMap<>(stats) : null);
        clone.setRelicSets(relicSets != null ? new HashMap<>(relicSets) : null);
        clone.setWeaponId(weaponId);
        clone.setWeaponRank(weaponRank);
        return clone;
    }
}
