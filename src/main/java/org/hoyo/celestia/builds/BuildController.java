package org.hoyo.celestia.builds;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.model.BuildEditResultDTO;
import org.hoyo.celestia.builds.service.BuildEditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/build")
@RequiredArgsConstructor
public class BuildController {

    private final BuildEditService buildEditService;

    @GetMapping("/create")
    public ResponseEntity<BuildEditResultDTO> createBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName) {
        return buildEditService.createBuild(uid, avatarId, buildName);
    }

    @GetMapping("/rename")
    public ResponseEntity<BuildEditResultDTO> renameBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
                                                          @RequestParam("buildNameOld") String buildNameOld, @RequestParam("buildNameNew") String buildNameNew) {
        return buildEditService.editBuildName(uid, avatarId, buildNameOld, buildNameNew);
    }

    @GetMapping("/delete")
    public ResponseEntity<BuildEditResultDTO> deleteBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId, @RequestParam("buildName") String buildName) {
        return buildEditService.deleteBuild(uid, avatarId, buildName);
    }

    @GetMapping("/hide")
    public ResponseEntity<BuildEditResultDTO> hideBuild(@RequestParam("uid") String uid, @RequestParam("avatarId") String avatarId,
                                                        @RequestParam("buildName") String buildName, @RequestParam("isStatic") Boolean isStatic,
                                                        @RequestParam("hide")  Boolean hide
    ) {
        return buildEditService.setHide(uid, avatarId, buildName, isStatic, hide);
    }
}
