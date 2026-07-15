package org.hoyo.celestia.user;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.loaders.global.AvatarInfoRedisLoader;
import org.hoyo.celestia.relics.DTOs.RelicPageDTO;
import org.hoyo.celestia.relics.model.RelicNode;
import org.hoyo.celestia.relics.service.FetchRelicService;
import org.hoyo.celestia.timeouts.service.TimeoutService;
import org.hoyo.celestia.user.DTOs.NoRefreshUserDTO;
import org.hoyo.celestia.user.DTOs.UpsertResultDTO;
import org.hoyo.celestia.user.service.CreateUserService;
import org.hoyo.celestia.user.service.UserDetailsFetchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



//@CrossOrigin
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserService createUserService;
    private final UserDetailsFetchService userDetailsFetchService;
    private final TimeoutService timeoutService;
    private final FetchRelicService fetchRelicService;
    private final AvatarInfoRedisLoader avatarInfoRedisLoader;

    @GetMapping("/{uid}")
    public ResponseEntity<String> createUser(@PathVariable String uid){
        return createUserService.upsertUser(uid).status().toResponseEntity();
    }

    @GetMapping("/dashboard/noRefresh/{uid}")
    public ResponseEntity<NoRefreshUserDTO> refreshUser(@PathVariable String uid){
        return userDetailsFetchService.getUserCardDetailsNoRefresh(uid);
    }

    //unoptimised approach
    //after a hard refresh,
    // body.success true if an update/insert occurs => frontend calls timeout and noRefresh
    // false otherwise => frontend calls timeout and if timeout < 0 the button is greyed
    // body.partial/warnings flag characters skipped over missing meta assets
    // (new game version) so the frontend can tell the user instead of silently
    // showing incomplete data. Was ResponseEntity<Boolean>; an object body is
    // still truthy to an older frontend and hard failures stay non-2xx.
    @GetMapping("/dashboard/refresh/{uid}")
    public ResponseEntity<UpsertResultDTO> getUpsertStatus(@PathVariable String uid){
        CreateUserService.UpsertOutcome outcome = createUserService.upsertUser(uid);

        UpsertResultDTO body = new UpsertResultDTO();
        body.setSuccess(Boolean.TRUE.equals(outcome.status().isSuccess().getBody()));
        body.setMessage(outcome.status().getMessage());
        body.setPartial(!outcome.skippedAvatarIds().isEmpty());
        body.setSkippedAvatarIds(outcome.skippedAvatarIds());

        List<String> warnings = new ArrayList<>();
        if(body.isPartial()){
            warnings.add("New game assets for character(s) " + String.join(", ", outcome.skippedAvatarIds())
                    + " are not yet supported; they were skipped and will appear after the next asset update.");
        }
        body.setWarnings(warnings);

        return ResponseEntity.status(outcome.status().getStatus()).body(body);
    }

    @GetMapping("/timeout/{uid}")
    public ResponseEntity<Long> timeoutUser(@PathVariable String uid){
        return timeoutService.timeLeft(uid);
    }

    @GetMapping("/relics/{uid}/{pageNumber}")
    public ResponseEntity<RelicPageDTO> getUserRelics(
            @PathVariable String uid,
            @PathVariable int pageNumber,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String filterField,
            @RequestParam(required = false) String filterValue,
            @RequestParam(required = false) String typeFilter,
            @RequestParam(defaultValue = "DESC") String order
    ) {
        return fetchRelicService.getUserRelicsForDisplay(uid, pageNumber, sortBy, order, filterField, filterValue, typeFilter);
    }

    @GetMapping("/bio/{uid}")
    public ResponseEntity<String> fetchUserBio(@PathVariable String uid){
        return ResponseEntity.ok(createUserService.getUserBio(uid));
    }

    // Whole headIconId -> iconPath map (Enka's pfps.json, kept fresh in Redis
    // by the asset refresh cycle) — replaces the frontend's bundled pfps.json,
    // which went stale whenever a game version added new profile pictures.
    // The frontend fetches this once per session and caches it module-scope.
    @GetMapping("/pfps")
    public ResponseEntity<Map<String, String>> getPfpIcons(){
        return ResponseEntity.ok(avatarInfoRedisLoader.getPfpIcons());
    }

}
