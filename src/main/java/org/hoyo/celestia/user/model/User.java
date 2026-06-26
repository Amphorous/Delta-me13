package org.hoyo.celestia.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @Id
    private String id;
    private DetailInfo detailInfo;
    private Integer ttl;
    @Indexed(unique = true)
    private String uid;

}
