package org.hoyo.celestia.buffEffects.service;

import org.hoyo.celestia.buffEffects.model.TeamMemberDTO;
import org.hoyo.celestia.loaders.global.GlobalMetaFileLoader;
import org.hoyo.celestia.loaders.model.metaModel.RelicMetaProperty;
import org.hoyo.celestia.loaders.model.metaModel.SetSkillData;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StaticEffects {
    private final GlobalMetaFileLoader globalMetaFileLoader;

    public StaticEffects(GlobalMetaFileLoader globalMetaFileLoader) {
        this.globalMetaFileLoader = globalMetaFileLoader;
    }

    public void effectSwitch(TeamMemberDTO teamMember) {
        Map<String,Double> stats = teamMember.getStats();
        Map<Integer,Integer> relicSets = teamMember.getRelicSets();
        String weaponId = teamMember.getWeaponId();
        Integer weaponRank = teamMember.getWeaponRank();

        //accesses metafile to fetch the relevant static buffs
        for(Map.Entry<Integer,Integer> entry : relicSets.entrySet()) {
            Integer relicSet = entry.getKey();
            Integer relicCount = entry.getValue();
            relicEffectRouting(stats, relicSet, relicCount);
        }
        weaponEffectRouting(stats, weaponId, weaponRank);
    }

    private void weaponEffectRouting(Map<String, Double> stats, String weaponId, Integer rank) {
//        System.out.println("routing called");
        Map<String, Double> context = null;
        try{
            context = globalMetaFileLoader.getMetaFile().getEquipmentSkill().get(weaponId).get(String.valueOf(rank)).get("props");
        } catch (NullPointerException e) {
            //weapon prolly doesnt have a static effect
            return;
        }
        if(context == null) {return;}

        for(Map.Entry<String, Double> entry : context.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue() +" are being added in weapons");
            effectRoutingStatAdder(stats, entry);
        }
    }

    public void relicEffectRouting(Map<String,Double> stats, Integer relicSet, Integer relicCount) {
        // A brand-new relic set (fresh game version) may be absent from the
        // meta entirely, or present with missing piece-count entries — a set
        // bonus is a minor additive effect, so skip silently rather than fail
        // the character (same treatment as the context == null case below).
        RelicMetaProperty relicMeta = globalMetaFileLoader.getMetaFile().getRelic();
        if(relicMeta == null || relicMeta.getSetSkill() == null) {return;}
        Map<String, SetSkillData> context = relicMeta.getSetSkill().get(String.valueOf(relicSet));
        if(context == null) {return;}

        if(relicCount == 4 && context.get("4") != null && context.get("4").getProps() != null) {
            for(Map.Entry<String,Double> entry : context.get("4").getProps().entrySet()) {
                effectRoutingStatAdder(stats, entry);
            }
        }
        if(relicCount >= 2 && context.get("2") != null && context.get("2").getProps() != null) {
            for(Map.Entry<String,Double> entry : context.get("2").getProps().entrySet()) {
                effectRoutingStatAdder(stats, entry);
            }
        }
    }

    public static void effectRoutingStatAdder(Map<String, Double> stats, Map.Entry<String, Double> entry) {
        String key = entry.getKey();
        Double value = entry.getValue();
        if(key.substring(key.length()-4).equalsIgnoreCase("base")){
            key = key.substring(0,key.length()-4);
        }
        stats.put(key, stats.getOrDefault(key, 0.0) + value);
    }

    public void effectAdd(Map<String,Double> stats, String targetStat, Double value) {
        stats.put(targetStat, value + stats.getOrDefault(targetStat, 0.0));
    }
}
