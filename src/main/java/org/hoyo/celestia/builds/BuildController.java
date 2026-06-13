package org.hoyo.celestia.builds;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.model.BuildEditResultDTO;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.service.BindingJsonHandler;
import org.hoyo.celestia.builds.service.BuildService;
import org.hoyo.celestia.builds.service.FetchBuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/build")
@RequiredArgsConstructor
public class BuildController {

    private final BuildService buildService;
    private final FetchBuildService fetchBuildService;
    private final BindingJsonHandler bindingJsonHandler;
    private static final String GAME = "hsr";
    
    @GetMapping("/create/protected")
    public ResponseEntity<BuildEditResultDTO> createBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.createBuild(uid, avatarId, buildName);  
        }
        return ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/rename/protected")
    public ResponseEntity<BuildEditResultDTO> renameBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
            @RequestParam("buildNameOld") String buildNameOld, @RequestParam("buildNameNew") String buildNameNew
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.editBuildName(uid, avatarId, buildNameOld, buildNameNew);
        }
        return ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/delete/protected")
    public ResponseEntity<BuildEditResultDTO> deleteBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.deleteBuild(uid, avatarId, buildName);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/hide")
    public ResponseEntity<BuildEditResultDTO> hideBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
            @RequestParam("buildName") String buildName, @RequestParam("isStatic") Boolean isStatic,
            @RequestParam("hide")  Boolean hide
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.setHide(uid, avatarId, buildName, isStatic, hide);
        }
        return ResponseEntity.badRequest().build();
    }


    @GetMapping("/get-list/all/{uid}/protected")
    public ResponseEntity<List<BuildNode>> getAllBuilds(@RequestHeader("Aquila-User-Key") String userKey, @PathVariable String uid) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return fetchBuildService.getBuildList(uid);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/get-list/{uid}/{pageNumber}")
    public ResponseEntity<List<BuildNode>> getBuilds(
            @PathVariable String uid,
            @PathVariable int pageNumber,
            @RequestParam(required = false) String filterByAvatarId,
            @RequestParam(defaultValue = "DESC") String order
    ) {
        return fetchBuildService.getBuilds(
                uid,
                pageNumber,
                order,
                filterByAvatarId
        );
    }

}
