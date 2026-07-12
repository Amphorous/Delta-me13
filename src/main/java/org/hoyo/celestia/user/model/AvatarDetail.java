package org.hoyo.celestia.user.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class AvatarDetail {
    private Integer pos;
    private Integer rank = 0; // eidolon rank 0-6; Enka omits the field entirely at rank 0, so the initializer is the real default
    private ArrayList<Relic> relicList;
    private Integer level;
    private Integer promotion;
    private ArrayList<Skill> skillTreeList;
    private Equipment equipment;
    private String avatarId;
    private Boolean _assist;
}
