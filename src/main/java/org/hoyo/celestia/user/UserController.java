package org.hoyo.celestia.user;

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
public class UserController {

    private final CreateUserService createUserService;
    private final UserDetailsFetchService userDetailsFetchService;
    private final TimeoutService timeoutService;
    private final FetchRelicService fetchRelicService;

    public UserController(CreateUserService createUserService, UserDetailsFetchService userDetailsFetchService, TimeoutService timeoutService, FetchRelicService fetchRelicService) {
        this.createUserService = createUserService;
        this.userDetailsFetchService = userDetailsFetchService;
        this.timeoutService = timeoutService;
        this.fetchRelicService = fetchRelicService;
    }

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

    @GetMapping("/relics/{uid}/{pageNumber}")
    public ResponseEntity<List<RelicProjectionDTO>> getUserRelics(@PathVariable String uid, @PathVariable int pageNumber) {
        return fetchRelicService.getUserRelicsForDisplay(uid, pageNumber);
    }

}
