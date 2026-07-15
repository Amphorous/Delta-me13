package org.hoyo.celestia.fightprops.service;

import org.hoyo.celestia.buffEffects.model.TeamMemberDTO;
import org.hoyo.celestia.buffEffects.service.StaticEffects;
import org.hoyo.celestia.fightprops.model.FightPropNode;
import org.hoyo.celestia.loaders.global.GlobalMetaFileLoader;
import org.hoyo.celestia.loaders.global.MissingMetaAssetException;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.hoyo.celestia.user.model.AvatarDetail;
import org.hoyo.celestia.user.model.Props;
import org.hoyo.celestia.user.model.Relic;
import org.hoyo.celestia.user.model.Skill;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class FightPropService {

    private final GlobalMetaFileLoader globalMetaFileLoader;
    private final StaticEffects staticEffects;

    public FightPropService(GlobalMetaFileLoader globalMetaFileLoader, StaticEffects staticEffects) {
        this.globalMetaFileLoader = globalMetaFileLoader;
        this.staticEffects = staticEffects;
    }

    public FightPropNode getFightPropNode(AvatarDetail character){
        FightPropNode fightPropNode = new FightPropNode();

        //get character base stats from metaFile
        //get weapon base from metaFile
        HonkerMetaObject localMetaFile = globalMetaFileLoader.getMetaFile();
        //localMetaFile.getAvatar().get(character.getAvatarId()).get(String.valueOf(character.getPromotion()))
        String avatarId = character.getAvatarId();
//        String promotion = String.valueOf(character.getPromotion());
        Integer intPromotion =  character.getPromotion();
        String promotion = "0";
        if(intPromotion != null){
            promotion = String.valueOf(character.getPromotion());
        }

        if(promotion == null || promotion.isEmpty()){
            promotion = "0";
        }

        Integer level = character.getLevel();

        Map<String, Double> fightPropMap = new HashMap<>();
        // Brand-new characters (fresh game version) won't be in the meta file
        // until the synced assets catch up — surface that as a typed miss the
        // subloader can skip, instead of an NPE that fails the whole upsert.
        Map<String, Map<String, Double>> avatarPromotions = localMetaFile.getAvatar().get(avatarId);
        if (avatarPromotions == null || avatarPromotions.get(promotion) == null) {
            throw new MissingMetaAssetException("avatar", avatarId);
        }
        Map<String, Double> currentAscensionStats = avatarPromotions.get(promotion);


//        Double addValue = null;
//        for(Map.Entry<String, Double> entry : currentAscensionStats.entrySet()) {
//            String key = entry.getKey();
//            boolean check = (key.substring(key.length()-3)).equalsIgnoreCase("add");
//            if(check) {
//                addValue = entry.getValue();
//                continue;
//            }
//            if (addValue != null) {
//                Double finalStatValue = (level - 1)*addValue + entry.getValue();
//                String finalKey = "Base" + key.substring(0, key.length()-4);
//                fightPropMap.put(finalKey, finalStatValue);
//                addValue = null;
//                continue;
//            }
//            fightPropMap.put(key, entry.getValue());
//        }

        //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
        //not tested, there might be stat errors when the application runs
        //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

        //following maps keep track of the "Add" and "Base" values encountered when reading through a character's
        //promotion, they are then used to calculate the final base stat after finding the required pair
        Map<String, Double> foundBaseValues = new HashMap<>();
        Map<String, Double> foundAddValues = new HashMap<>();
        for(Map.Entry<String, Double> entry : currentAscensionStats.entrySet()) {
            String key = entry.getKey();
            boolean checkForBase = (key.substring(key.length()-4)).equalsIgnoreCase("base");
            boolean checkForAdd = (key.substring(key.length()-3)).equalsIgnoreCase("add");
            if(checkForBase) {
                String substringKey = key.substring(0, key.length()-4);
                // see if foundAddValues has the required substring key (ex: "Attack")
                if(foundAddValues.containsKey(substringKey)) {
                    Double requiredBase = entry.getValue();
                    Double requiredAdd = foundAddValues.get(substringKey);
                    Double finalStatValue = (level - 1)*requiredAdd + requiredBase;
                    String finalKey = "Base" + substringKey;
                    fightPropMap.put(finalKey, finalStatValue);
                    foundAddValues.remove(substringKey);
                }
                else {
                    foundBaseValues.put(substringKey, entry.getValue());
                }
                continue;
            }
            if(checkForAdd) {
                String substringKey = key.substring(0, key.length()-3);
                if(foundBaseValues.containsKey(substringKey)) {
                    Double requiredBase = foundBaseValues.get(substringKey);
                    Double requiredAdd = entry.getValue();
                    Double finalStatValue = (level - 1)*requiredAdd + requiredBase;
                    String finalKey = "Base" + substringKey;
                    fightPropMap.put(finalKey, finalStatValue);
                    foundBaseValues.remove(substringKey);
                }
                else {
                    foundAddValues.put(substringKey, entry.getValue());
                }
                continue;
            }

            fightPropMap.put(key, entry.getValue());
        }

        //foundBaseValues might have leftover base stats which had no corresponding adds
        //insert all of them as is into the fightpropmap
        for(Map.Entry<String, Double> entry : foundBaseValues.entrySet()) {
            String key = entry.getKey();
            Double finalStatValue = entry.getValue();
            String finalKey = "Base" + key;
            fightPropMap.put(finalKey, finalStatValue);
        }

        //wep stats calc
        try {
            for(Props prop : character.getEquipment().get_flat().getProps()){
                fightPropMap.put(prop.getType(),prop.getValue()+fightPropMap.getOrDefault(prop.getType(),0.0));
            }
        } catch (NullPointerException e) {
            //no weapon
        }

        Map<Integer, Integer> relicSets = new HashMap<>();
        //iterate relics to get all stats from there
        if(character.getRelicList() != null){
            for(Relic relic:character.getRelicList()){
                relicSets.put(relic.get_flat().getSetID(), relicSets.getOrDefault(relic.get_flat().getSetID(), 0) + 1);
                for(Props prop : relic.get_flat().getProps()){
                    String key = prop.getType();
                    if(key.substring(key.length()-4).equalsIgnoreCase("base")){
                        key = key.substring(0,key.length()-4);
                    }
                    fightPropMap.put(key,prop.getValue()+fightPropMap.getOrDefault(key,0.0));
                }
            }
        }


        //finally get trace stats using AAAA2XX as skillId from metaFile.tree
        if(character.getSkillTreeList() != null){
            for(Skill skill : character.getSkillTreeList()){
                String pointId = skill.getPointId().toString();
                if(pointId.charAt(pointId.length()-3)=='2'){
                    if(skill.getLevel()==1){
                        //now we need to add the stat value to fightpropmap
                        // Same story as the avatar lookup above: a new trace
                        // point from a fresh game version won't be in the meta
                        // tree yet. Treat it as a typed miss (skipping the whole
                        // character) rather than storing a build with silently
                        // wrong stats.
                        Map<String, Map<String, Map<String, Double>>> treePoint = localMetaFile.getTree().get(pointId);
                        Map<String, Double> stat = null;
                        if (treePoint != null && treePoint.get("1") != null) {
                            stat = treePoint.get("1").get("props");
                        }
                        if (stat == null) {
                            throw new MissingMetaAssetException("tree", pointId);
                        }
                        for(Map.Entry<String, Double> entry : stat.entrySet()) {

                            StaticEffects.effectRoutingStatAdder(fightPropMap, entry);
                        }
                    }
                }
            }
        }

        //metafile.getTree().get("skillid").get("1").get("props") <- this is a "map" of props, just iterate and add to set/add value to the record in fightprop

        //do set effects now
        TeamMemberDTO currentCharacter = new TeamMemberDTO();
        currentCharacter.setStats(fightPropMap);
        try {
            currentCharacter.setWeaponId(character.getEquipment().getTid());
            currentCharacter.setWeaponRank(character.getEquipment().getRank());
        } catch (NullPointerException e) {
            currentCharacter.setWeaponId("unknown");
            currentCharacter.setWeaponRank(-1);
        }
        currentCharacter.setRelicSets(relicSets);

        staticEffects.effectSwitch(currentCharacter);


        //this is after all static stat calcs are over
        fightPropMap.put("SPRatio",fightPropMap.getOrDefault("SPRatio",0.0)+1);
        fightPropNode.setStats(fightPropMap);

//        System.out.println("::::::::::::::::::::::::" + character.getAvatarId() + "::::::::::::::::::::::::");
//        for(Map.Entry<String, Double> entry : currentCharacter.getStats().entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());
//        }
//        System.out.println("::::::::::::::::::::::::" + character.getAvatarId() + "::::::::::::::::::::::::");

        return fightPropNode;
    }
}
