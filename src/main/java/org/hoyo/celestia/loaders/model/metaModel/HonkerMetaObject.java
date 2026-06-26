package org.hoyo.celestia.loaders.model.metaModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HonkerMetaObject {
        private Map<String,
                    Map<String,
                        Map<String, Double>
                    >
                > avatar;

        private Map<String,
                    Map<String,
                        Map<String, Double>
                    >
                > equipment;

        private Map<String,
                    Map<String,
                        Map<String,
                            Map<String, Double>
                        >
                    >
                > tree;

        private Map<String,
                    Map<String,
                        Map<String,
                            Map<String, Double>
                        >
                    >
                > equipmentSkill;

        private RelicMetaProperty relic; //this is only set skill

        private Map<String,
                    Map<String, Object>
                > skills;
}
