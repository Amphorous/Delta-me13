package org.hoyo.celestia.subloaders.service;

import org.hoyo.celestia.builds.BuildNodeRepository;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.fightprops.model.FightPropNode;
import org.hoyo.celestia.fightprops.service.FightPropService;
import org.hoyo.celestia.uids.UIDNodeRepository;
import org.hoyo.celestia.relics.RelicNodeRepository;
import org.hoyo.celestia.relics.service.CreateRelicService;
import org.hoyo.celestia.user.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubloaderService {

    private final UIDNodeRepository uidNodeRepository;
    private final BuildNodeRepository buildNodeRepository;
    private final RelicNodeRepository relicNodeRepository;
    private final CreateRelicService createRelicService;
    private final FightPropService fightPropService;

    public SubloaderService(UIDNodeRepository uidNodeRepository, BuildNodeRepository buildNodeRepository, RelicNodeRepository relicNodeRepository, CreateRelicService createRelicService, FightPropService fightPropService) {
        this.uidNodeRepository = uidNodeRepository;
        this.buildNodeRepository = buildNodeRepository;
        this.relicNodeRepository = relicNodeRepository;
        this.createRelicService = createRelicService;
        this.fightPropService = fightPropService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean userSubloader(User user){
        if(user.getDetailInfo().getPrivacySettingInfo().getDisplayCollection() == null){
            //weird null
            return false;
        }
        if(!user.getDetailInfo().getPrivacySettingInfo().getDisplayCollection()){
            //users builds are private, return false
            return false;
        }

        //merge uid node into db
        uidNodeRepository.createUIDNodeIfNotExists(user.getUid());

        ArrayList<AvatarDetail> avatarDetailList = user.getDetailInfo().getAvatarDetailList();
        for(AvatarDetail character : avatarDetailList){
            /*
            * check if build exists for this character, else jump to calc
            *   if build exists, then check level, talent level string, if not same, jump to calc
            *       if all relics are same, skip iteration
            *       else, jump to calc
            *
            * calc (FightProps object with all the stats added)
            * read level and use metaFile to get base stats
            * read equipment, use meta to get stats
            * read skilllist, use meta to get stats
            * read artifacts, append stats
            * */
            //check if same build with different levels/talents exists, if so, replace it with the new build, in this case, this iteration will proceed
            //see if the shown build already exists
            // to do this, check talent levels, level, artifact
            //make a build object for each character
            //-->

            // FIXME potential upgrade by changing the skillliststring to only contain 1220.2.xxx class of stats concatenated
            // FIXME this does the exact same thing as what we already do, but with less redundancy
            // FIXME (make sure this is actually correct by cross checking the meta file before implementing)

            ArrayList<Skill> skillListTree = character.getSkillTreeList();
            String characterSkillListString = skillListTree.stream()
                    .map(skill -> String.valueOf(skill.getLevel()))
                    .collect(Collectors.joining());


            BuildCheckResult result = shouldICalulateAgain(character, user.getUid(), characterSkillListString);
            if(result.shouldI()){

                Double buildCv = result.cvToAdd();
                Set<String> currentRelicIdSet = result.currentRelicIdSet();

                // create a buildnode (whose isStatic == true) with the existing information
                BuildNode newStaticBuild = new BuildNode();
                newStaticBuild.setLevel(character.getLevel());
                newStaticBuild.setAvatarId(character.getAvatarId());
                newStaticBuild.setSkillListString(characterSkillListString);
                newStaticBuild.setIsStatic(true);

                FightPropNode fightPropNode = fightPropService.getFightPropNode(character);

                // look for build with avatarId == character.avatarId and isStatic == true and go to its fightpropnode and detach delete it
                // look for build with avatarId == character.avatarId and isStatic == true and detach delete it

                // create links from buildnode (whose isStatic == true) to corresponding relics

                // calculate fightpropnode and link to buildnode (whose isStatic == true)

                // link buildnode

                Map<String, Object> fightPropMapObject = fightPropNode.getStats().entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> "stats." + e.getKey(),
                                e -> (Object) e.getValue()
                        ));

                Equipment weapon = character.getEquipment();

                if (weapon != null) {
                    Integer weaponLevel = weapon.getLevel();
                    Integer refineWeapon = weapon.getRank();
                    Integer weaponAscension = weapon.getPromotion();
                    Double baseHP = 0.0;
                    Double baseDefense = 0.0;
                    Double baseAtk = 0.0;
                    String weaponId = weapon.getTid();
                    LocalDateTime creationDate = LocalDateTime.now();
                    for (Props prop : weapon.get_flat().getProps()){
                        if ((prop.getType()).equalsIgnoreCase("BaseHP")){
                            baseHP += prop.getValue();
                        } else if ((prop.getType()).equalsIgnoreCase("BaseDefence")){
                            baseDefense += prop.getValue();
                        } else if ((prop.getType()).equalsIgnoreCase("BaseAttack")){
                            baseAtk += prop.getValue();
                        }
                    }

                    buildNodeRepository.removeIsStaticBuildAndItsFightPropNodeThenInsertANewIsStaticBuildAndItsFightPropNodeAndAlsoLinkTheBuildNodeToItsRelicNodesAndAlsoLinkTheWeaponNode
                            (user.getUid(), character.getAvatarId(),
                                    character.getLevel(), characterSkillListString,
                                    true, false,
                                    newStaticBuild.getBuildName(),
                                    fightPropMapObject, currentRelicIdSet,
                                    weaponId, weaponLevel,
                                    refineWeapon, weaponAscension,
                                    baseHP, baseDefense,
                                    baseAtk,
                                    buildCv,
                                    creationDate
                            );
                } else {
                    LocalDateTime creationDate = LocalDateTime.now();
                    buildNodeRepository.removeIsStaticBuildAndItsFightPropNodeThenInsertANewIsStaticBuildAndItsFightPropNodeAndAlsoLinkTheBuildNodeToItsRelicNodes
                            (user.getUid(), character.getAvatarId(),
                                    character.getLevel(), characterSkillListString,
                                    true, false,
                                    newStaticBuild.getBuildName(),
                                    fightPropMapObject, currentRelicIdSet,
                                    buildCv,
                                    creationDate
                            );
                }
            }

        }
        //this marks the end of subloading, having read the user builds
        return true;
    }

    public record BuildCheckResult(Boolean shouldI, Set<String> currentRelicIdSet, Double cvToAdd) {}

    public BuildCheckResult shouldICalulateAgain(AvatarDetail character, String uid, String characterSkillListString) {
        Boolean flag = false;
        Integer level = character.getLevel();

        if (!buildNodeRepository.hasLevelsOnStaticBuild(uid, character.getAvatarId(), characterSkillListString, level)) {
            flag = true;
        }

        Double oldBuildCv = buildNodeRepository.getStaticBuildCv(uid, character.getAvatarId());

        if (oldBuildCv == null) {
            oldBuildCv = 0.0;
        }
        Double addedCv = 0.0;
        Double removedCv = 0.0;

        Set<String> staticNodeRelicIdSet =relicNodeRepository.getAllRelicIdsFromStaticNode(uid, character.getAvatarId(), true);

        Set<String> currentRelicIdSet =createRelicService.getRelicIdSetFromAvatarDetails(character);

        Set<String> currentRelicIdSetToInsert =new HashSet<>(currentRelicIdSet);

        Set<String> addedRelics =new HashSet<>(currentRelicIdSetToInsert);
        addedRelics.removeAll(staticNodeRelicIdSet);

        Set<String> removedRelics =new HashSet<>(staticNodeRelicIdSet);
        removedRelics.removeAll(currentRelicIdSetToInsert);

        if (!staticNodeRelicIdSet.equals(currentRelicIdSet)) {
            for (String relicId : addedRelics) {
                if (!relicNodeRepository.existsRelic(uid, relicId)) {
                    Integer type = Integer.parseInt(String.valueOf(relicId.charAt(0)));
                    Integer pos = getIndexByType(character.getRelicList(), type);
                    addedCv += createRelicService.createRelicNode(character.getRelicList().get(pos), uid, relicId);
                } else {
                    Double relicCv = relicNodeRepository.getRelicCv(uid, relicId);
                    if (relicCv != null) {
                        addedCv += relicCv;
                    }
                }
            }
            for (String relicId : removedRelics) {
                Double relicCv = relicNodeRepository.getRelicCv(uid, relicId);
                if (relicCv != null) {
                    removedCv += relicCv;
                }
            }
            flag = true;
        }
        Double newBuildCv = oldBuildCv - removedCv + addedCv;
        return new BuildCheckResult(
                flag,
                currentRelicIdSetToInsert,
                newBuildCv
        );
    }

    public int getIndexByType(ArrayList<Relic> relics, Integer type) {
        for (int i = 0; i < relics.size(); i++) {
            Integer relicType = relics.get(i).getType();
            if (Objects.equals(relicType, type)) {
                return i;
            }
        }
        return -1;
    }


}
