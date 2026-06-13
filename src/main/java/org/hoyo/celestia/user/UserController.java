package org.hoyo.celestia.user;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.relics.DTOs.RelicProjectionDTO;
import org.hoyo.celestia.relics.model.RelicNode;
import org.hoyo.celestia.relics.service.FetchRelicService;
import org.hoyo.celestia.timeouts.service.TimeoutService;
import org.hoyo.celestia.user.DTOs.NoRefreshUserDTO;
import org.hoyo.celestia.user.service.CreateUserService;
import org.hoyo.celestia.user.service.UserDetailsFetchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserService createUserService;
    private final UserDetailsFetchService userDetailsFetchService;
    private final TimeoutService timeoutService;
    private final FetchRelicService fetchRelicService;

    @GetMapping("/{uid}")
    public ResponseEntity<String> createUser(@PathVariable String uid){
        return createUserService.upsertUser(uid).toResponseEntity();
    }

    @GetMapping("/dashboard/noRefresh/{uid}")
    public ResponseEntity<NoRefreshUserDTO> refreshUser(@PathVariable String uid){
        return userDetailsFetchService.getUserCardDetailsNoRefresh(uid);
    }

    //unoptimised approach
    //after a hard refresh,
    // true is returned if an update/insert occurs => if true is returned, frontend calls timeout and noRefresh
    // false otherwise => if false is returned, frontend calls timeout and if timeout < 0 the button is greyed
    @GetMapping("/dashboard/refresh/{uid}")
    public ResponseEntity<Boolean> getUpsertStatus(@PathVariable String uid){
        return createUserService.upsertUser(uid).isSuccess();
    }

    @GetMapping("/timeout/{uid}")
    public ResponseEntity<Long> timeoutUser(@PathVariable String uid){
        return timeoutService.timeLeft(uid);
    }

    //TODO: add set wise and position wise filter options using request params
    @GetMapping("/relics/{uid}/{pageNumber}")
    public ResponseEntity<List<RelicProjectionDTO>> getUserRelics(
            @PathVariable String uid,
            @PathVariable int pageNumber,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String filterBy,
            @RequestParam(defaultValue = "DESC") String order
    ) {
        if (filterBy != null) {
            return fetchRelicService.getUserRelicsForDisplayFilteredBy(uid, pageNumber, filterBy);
        }

        if (sortBy != null) {
            return fetchRelicService.getUserRelicsForDisplaySortedBy(uid, pageNumber, sortBy, order);
        }

        return fetchRelicService.getUserRelicsForDisplay(uid, pageNumber);
    }

    @GetMapping("/bio/{uid}")
    public ResponseEntity<String> fetchUserBio(@PathVariable String uid){
        return ResponseEntity.ok(createUserService.getUserBio(uid));
    }

}
