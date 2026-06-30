package org.hoyo.celestia.loaders.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import org.hoyo.celestia.loaders.WeaponNodeRepository;
import org.hoyo.celestia.loaders.global.AssetSyncService;
import org.hoyo.celestia.loaders.global.GlobalMetaFileLoader;
import org.hoyo.celestia.loaders.model.*;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Service
public class WeaponLoaderService {
    private final WeaponNodeRepository weaponNodeRepository;
    private final GlobalMetaFileLoader globalMetaFileLoader;
    private final AssetSyncService assetSyncService;

    public WeaponLoaderService(WeaponNodeRepository weaponNodeRepository, GlobalMetaFileLoader globalMetaFileLoader, AssetSyncService assetSyncService) {
        this.weaponNodeRepository = weaponNodeRepository;
        this.globalMetaFileLoader = globalMetaFileLoader;
        this.assetSyncService = assetSyncService;
    }

    @PostConstruct
    public void init(){
        System.out.println(loadWeaponsFromFile());
    }

    //read honker_weps.json
    //-if Weapons node doesn't exist, make it
    //iterate every wep id and relate it to weapon node using the parameters:
    //1. weapon_id
    //2. path
    //3. rarity
    // also add lvl 80 calcd stats using honker_meta.json

    public ResponseEntity<String> execute() {
        return ResponseEntity.status(HttpStatus.OK).body(loadWeaponsFromFile());
    }

    public String loadWeaponsFromFile(){
        ObjectMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .build();

        Map<String, JsonNode> assets = assetSyncService.syncAssets();
        JsonNode honkerWepsRootNode = assets != null ? assets.get("weapons") : null;
        if (honkerWepsRootNode == null) {
            return "No weapons data available to load (asset sync returned nothing).";
        }

        Set<Map.Entry<String, JsonNode>> entries = honkerWepsRootNode.properties();
        //System.out.println(metaFile.getEquipment().get("20000").get("0").get("AttackAdd"));
        HonkerMetaObject metaFile = globalMetaFileLoader.getMetaFile();

        Iterator<Map.Entry<String, JsonNode>> entryIterator = entries.iterator();
        int count = 0;

        while(entryIterator.hasNext()){
            Map.Entry<String, JsonNode> entry = entryIterator.next();
            String weaponId = entry.getKey();
            JsonNode weaponNode = entry.getValue();

            HonkerWeaponObject weapon = mapper.convertValue(weaponNode, HonkerWeaponObject.class);
            //call a function which takes the meta obj and the weapon onj and makes a node for it

            count += weaponNodeCreator(weapon, metaFile, weaponId);
        }

        return "Added " + count + " weapons";
    }

    private int weaponNodeCreator(HonkerWeaponObject weapon, HonkerMetaObject metaFile, String weaponId) {
        //now create a pojo for the node and relationship which is supposed to go into the graph db
        //create repo methods for inserting the synthesized object
        //return

        // new addition is to check if a weapon exists and add only if it doesnt

        if(weaponNodeRepository.checkIfWeaponExists(weaponId)){
            return 0;
        }

        Double weaponBaseAtk = 0.0;
        Double weaponBaseDef = 0.0;
        Double weaponBaseHP = 0.0;
        try {
            Map<String, Double> weaponAsc6Stats = metaFile.getEquipment().get(weaponId).get("6");
            weaponBaseAtk = (79*weaponAsc6Stats.get("BaseAttackAdd"))
                            + weaponAsc6Stats.get("BaseAttack");
            weaponBaseDef = (79*weaponAsc6Stats.get("BaseDefenceAdd"))
                            + weaponAsc6Stats.get("BaseDefence");;
            weaponBaseHP = (79*weaponAsc6Stats.get("BaseHPAdd"))
                            + weaponAsc6Stats.get("BaseHP");;
        } catch (Exception e) {
            System.err.println("Version mismatch between files: weapons.json (formerly honker_weps.json) and honker_meta.json");
            e.printStackTrace();
        }

        WeaponMaxStat weapon80stats = new WeaponMaxStat();
        weapon80stats.setBaseAttack(weaponBaseAtk);
        weapon80stats.setBaseDefense(weaponBaseDef);
        weapon80stats.setBaseHP(weaponBaseHP);

        WeaponNode weaponNode = new WeaponNode();
        weaponNode.setNameHash(weapon.getEquipmentName().getHash());
        weaponNode.setImagePath(weapon.getImagePath());
        weaponNode.setBaseAttack(weapon80stats.getBaseAttack());
        weaponNode.setBaseDefense(weapon80stats.getBaseDefense());
        weaponNode.setBaseHP(weapon80stats.getBaseHP());

        weaponNode.setWeaponId(weaponId);
        //weaponNodeRepository.createWeaponNodeAndLinkToStore(weaponNode, weaponId, weapon.getImagePath(), String.valueOf(weapon.getRarity()));


        // old/original approach where creation and linking was separated
//        weaponNodeRepository.save(weaponNode);
//        weaponNodeRepository.linkWeaponNodeToStore(weaponId, weapon.getAvatarBaseType(), String.valueOf(weapon.getRarity()));

        // new approach where creation and linking isn't separated
        weaponNodeRepository.createAndLinkWeaponNodeToStore(weaponId, weapon.getAvatarBaseType(), String.valueOf(weapon.getRarity()), weaponNode.getNameHash(),
                weaponNode.getImagePath(), weaponNode.getBaseAttack(), weaponNode.getBaseDefense(), weaponNode.getBaseHP()
                );

        return 1;

//        Store store = storeRepository.findByName("weapons").orElseGet(() -> {
//            Store s = new Store();
//            s.setName("weapons");
//            return storeRepository.save(s);
//        });
//
//        ContainsWeapon relation = new ContainsWeapon();
//        relation.setWeaponId(weaponId);
//        relation.setPath(weapon.getImagePath());
//        relation.setRarity(String.valueOf(weapon.getRarity()));
//        relation.setWeaponNode(weaponNode);
//
//        store.getContainsWeaponList().add(relation);
//        storeRepository.save(store);
    }
}
