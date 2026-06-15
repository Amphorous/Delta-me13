package org.hoyo.celestia.builds;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.model.BuildEditResultDTO;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.service.BindingJsonHandler;
import org.hoyo.celestia.builds.service.BuildService;
import org.hoyo.celestia.builds.service.FetchBuildService;
import org.springframework.http.HttpStatus;
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

    // adding @RequestHeader("Aquila-User-Key") to an endpoint now protects the route automatically

    // protected
    @GetMapping("/create")
    public ResponseEntity<BuildEditResultDTO> createBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.createBuild(uid, avatarId, buildName);
        }
        return unauthorizedUid();
    }

    // protected
    @GetMapping("/rename")
    public ResponseEntity<BuildEditResultDTO> renameBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
            @RequestParam("buildNameOld") String buildNameOld, @RequestParam("buildNameNew") String buildNameNew
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.editBuildName(uid, avatarId, buildNameOld, buildNameNew);
        }
        return unauthorizedUid();
    }

    // protected
    @GetMapping("/delete")
    public ResponseEntity<BuildEditResultDTO> deleteBuild(
            @RequestHeader("Aquila-User-Key") String userKey,
            @RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName
    ) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return buildService.deleteBuild(uid, avatarId, buildName);
        }
        return unauthorizedUid();
    }

    // protected
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
        return unauthorizedUid();
    }

    // protected
    @GetMapping("/get-list/all/{uid}")
    public ResponseEntity<List<BuildNode>> getAllBuilds(@RequestHeader("Aquila-User-Key") String userKey, @PathVariable String uid) {
        if(bindingJsonHandler.checkAquilaKeyToUidBinding(userKey, GAME, uid)) {
            return fetchBuildService.getBuildList(uid);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
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

    private ResponseEntity<BuildEditResultDTO> unauthorizedUid() {
        BuildEditResultDTO result = new BuildEditResultDTO();
        result.setStatus(false);
        result.setMessage("UID is not linked to your account");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

}
