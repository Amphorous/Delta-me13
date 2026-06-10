package org.hoyo.celestia.builds.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.BuildNodeRepository;
import org.hoyo.celestia.builds.model.BuildEditResultDTO;
import org.hoyo.celestia.builds.model.BuildProjectionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BuildService {

    private final BuildNodeRepository buildNodeRepository;

    public ResponseEntity<BuildEditResultDTO> createBuild(String uid, String avatarId, String buildName) {
        // find buildNode with isStatic == true where uid == uid and avatarId == avatarId
        // copy buildNode, fightPropNode, and make a connection and set isStatic = false and buildName = buildName
        // NOTE: if user enters buildName == perhaps_feixiao, replace it with perchance_feixiao (doesn't really matter since the
        // isStatic flag tells us if the build is a default one or not, just a silly easter egg ig)
        // NOTE: if a build with the same buildName already exists, return reason (ask user to delete the build with same buildName) in String with status = false
        // copy relic relations from fightPropNode to relicNode
        // once op is complete, return status = true
        BuildEditResultDTO buildEditResultDTO = new BuildEditResultDTO();
        if(buildName == null || buildName.isEmpty()){
            buildEditResultDTO.setMessage("Build Name is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        } else if(avatarId == null || avatarId.isEmpty()){
            buildEditResultDTO.setMessage("Avatar Id is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        } else if(uid.isEmpty()){
            buildEditResultDTO.setMessage("Uid is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        } else if(buildName.equals("perhaps_feixiao")) {
            buildName = "feixiao_perchance";
        }

        if(!buildNodeRepository.hasBuildName(uid, avatarId, buildName)){
            buildNodeRepository.createBuild(uid, avatarId, buildName); //FIXME this might fail and we won't have any way to know that it did
            buildEditResultDTO.setStatus(true);
            buildEditResultDTO.setMessage("New Build has been created");
            return ResponseEntity.ok(buildEditResultDTO);
        }
        buildEditResultDTO.setStatus(false);
        buildEditResultDTO.setMessage("A build with the same name on the same character already exists.");
        return ResponseEntity.badRequest().body(buildEditResultDTO);
    }

    // make it so that frontend cant call this on a default build, instead make that call redirect to the createBuild method (on the frontend)
    public ResponseEntity<BuildEditResultDTO> editBuildName(String uid, String avatarId, String buildNameOld, String buildNameNew) {
        // find buildNode with isStatic == false where uid == uid, avatarId == avatarId, buildName == buildName
        // edit buildName
        // return response DTO

        BuildEditResultDTO buildEditResultDTO = new BuildEditResultDTO();
        if(buildNameOld == null || buildNameOld.isEmpty()){
            buildEditResultDTO.setMessage("Build Name is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }  else if(avatarId == null || avatarId.isEmpty()){
            buildEditResultDTO.setMessage("Avatar Id is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }  else if(uid.isEmpty()){
            buildEditResultDTO.setMessage("Uid is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }

        if(buildNodeRepository.hasBuildName(uid, avatarId, buildNameOld)){
            buildNodeRepository.editBuild(uid, avatarId, buildNameOld, buildNameNew);
            buildEditResultDTO.setStatus(true);
            buildEditResultDTO.setMessage("Build has been edited");
            return ResponseEntity.ok(buildEditResultDTO);
        }
        buildEditResultDTO.setStatus(false);
        buildEditResultDTO.setMessage("A build with the given buildName on the same character does not exist.");
        return ResponseEntity.badRequest().body(buildEditResultDTO);
    }

    public ResponseEntity<BuildEditResultDTO> deleteBuild(String uid, String avatarId, String buildName) {
        // find buildNode with uid == uid and avatarId == avatarId and buildName == buildName
        // detach delete fightPropNode
        // detach delete buildNode

        BuildEditResultDTO buildEditResultDTO = new BuildEditResultDTO();
        if(buildName == null || buildName.isEmpty()){
            buildEditResultDTO.setMessage("Build Name is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        } else if(avatarId == null || avatarId.isEmpty()){
            buildEditResultDTO.setMessage("Avatar Id is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }  else if(uid.isEmpty()){
            buildEditResultDTO.setMessage("Uid is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }

        if(buildNodeRepository.hasBuildName(uid, avatarId, buildName)){
            buildNodeRepository.deleteBuild(uid, avatarId, buildName);
            buildEditResultDTO.setStatus(true);
            buildEditResultDTO.setMessage("Build has been deleted");
            return ResponseEntity.ok(buildEditResultDTO);
        }

        buildEditResultDTO.setStatus(false);
        buildEditResultDTO.setMessage("A custom build with the given buildName does not exist.");
        return ResponseEntity.badRequest().body(buildEditResultDTO);
    }

    public ResponseEntity<BuildEditResultDTO> setHide(String uid, String avatarId, String buildName, Boolean isStatic, Boolean hide) {
        BuildEditResultDTO buildEditResultDTO = new BuildEditResultDTO();
        if(buildName == null || buildName.isEmpty()){
            buildEditResultDTO.setMessage("Build Name is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }  else if(avatarId == null || avatarId.isEmpty()){
            buildEditResultDTO.setMessage("Avatar Id is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }   else if(uid.isEmpty()){
            buildEditResultDTO.setMessage("Uid is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        } else if(isStatic == null){
            buildEditResultDTO.setMessage("Static is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        } else if(hide == null){
            buildEditResultDTO.setMessage("Hide is null or empty");
            buildEditResultDTO.setStatus(false);
            return ResponseEntity.badRequest().body(buildEditResultDTO);
        }

        if(buildNodeRepository.hasBuildNameWithStaticParam(uid, avatarId, buildName, isStatic)){
            buildNodeRepository.hideBuild(uid, avatarId, buildName, isStatic, hide);
            buildEditResultDTO.setStatus(true);
            buildEditResultDTO.setMessage("Build has been "+ ((hide)?"":"un-") +"hidden");
            return ResponseEntity.ok(buildEditResultDTO);
        }

        buildEditResultDTO.setStatus(false);
        buildEditResultDTO.setMessage("A custom build with the given buildName does not exist.");
        return ResponseEntity.badRequest().body(buildEditResultDTO);
    }

    public ResponseEntity<List<BuildProjectionDTO>> getAllBuilds(String uid){
        // return all builds which are visible
        return null;
    }
}
