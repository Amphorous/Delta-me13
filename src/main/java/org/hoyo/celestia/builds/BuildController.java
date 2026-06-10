package org.hoyo.celestia.builds;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.model.BuildEditResultDTO;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.service.BuildService;
import org.hoyo.celestia.builds.service.FetchBuildService;
import org.hoyo.celestia.fightprops.model.FightPropNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/build")
@RequiredArgsConstructor
public class BuildController {

    private final BuildService buildService;
    private final FetchBuildService fetchBuildService;

    @GetMapping("/create")
    public ResponseEntity<BuildEditResultDTO> createBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName) {
        return buildService.createBuild(uid, avatarId, buildName);
    }

    @GetMapping("/rename")
    public ResponseEntity<BuildEditResultDTO> renameBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
                                                          @RequestParam("buildNameOld") String buildNameOld, @RequestParam("buildNameNew") String buildNameNew) {
        return buildService.editBuildName(uid, avatarId, buildNameOld, buildNameNew);
    }

    @GetMapping("/delete")
    public ResponseEntity<BuildEditResultDTO> deleteBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName) {
        return buildService.deleteBuild(uid, avatarId, buildName);
    }

    @GetMapping("/hide")
    public ResponseEntity<BuildEditResultDTO> hideBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
                                                        @RequestParam("buildName") String buildName, @RequestParam("isStatic") Boolean isStatic,
                                                        @RequestParam("hide")  Boolean hide
    ) {
        return buildService.setHide(uid, avatarId, buildName, isStatic, hide);
    }

    @GetMapping("/get-list/test")
    public ResponseEntity<BuildNode> test(){
        return fetchBuildService.test();
    }

    @GetMapping("/get-list/{uid}/{pageNumber}")
    public ResponseEntity<List<BuildNode>> getBuilds(
            @PathVariable String uid,
            @PathVariable int pageNumber,
            @RequestParam(required = false) String filterByAvatarId,
            @RequestParam(defaultValue = "DESC") String order
    ) {
        return fetchBuildService.getAllBuilds(
                uid,
                pageNumber,
                order,
                filterByAvatarId
        );
    }

}
