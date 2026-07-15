package org.hoyo.celestia.user.service;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.subloaders.service.SubloaderService;
import org.hoyo.celestia.timeouts.service.TimeoutService;
import org.hoyo.celestia.user.UpdateStatus;
import org.hoyo.celestia.user.repository.UserRepository;
import org.hoyo.celestia.user.model.User;
import org.hoyo.celestia.user.validate.ValidateUid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CreateUserService {

    private final ValidateUid validateUid;
    private final UserRepository userRepository;
    private final TimeoutService timeoutService;
    private final SubloaderService subloaderService;
    private final ConcurrentHashMap<String, Object> uidLocks = new ConcurrentHashMap<>();

    //check mongo if user by uid exists
    //if no then enka call and create user
    //if yes then enka call and refresh data
    // [[ everytime a user refreshes a UID's data, this flow MUST occur, and the flow MUST go through here ]]
    public UpsertOutcome upsertUser(String uid){
        if(!validateUid.validate(uid)){
            return new UpsertOutcome(UpdateStatus.BAD_UID, List.of());
        }
        if(!timeoutService.canIEnkaCallYet(uid)){
            return new UpsertOutcome(UpdateStatus.ENKA_TIMEOUT, List.of());
        }

        // lock makes it so that concurrent user refreshes don't create dupes in race conditions
        Object lock = uidLocks.computeIfAbsent(uid, k -> new Object());
        synchronized (lock) {
            Optional<User> userInDb = userRepository.findByUid(uid);
            if(userInDb.isPresent()){
                User user = userInDb.get();
                User newUser = getUser(uid);
                if(newUser == null || newUser.getDetailInfo() == null || newUser.getUid() == null){
                    return new UpsertOutcome(UpdateStatus.ENKA_USER_NOT_FOUND, List.of());
                }
                newUser.setId(user.getId());
                userRepository.save(newUser);
                SubloaderService.SubloaderResult subloaderResult = subloaderService.userSubloader(newUser);
                if(!subloaderResult.success()){
                    return new UpsertOutcome(UpdateStatus.PRIVATE_BUILDS, List.of());
                }
                return new UpsertOutcome(UpdateStatus.UPDATED, subloaderResult.skippedAvatarIds());
            } else {
                User newUser = getUser(uid);
                if(newUser != null){
                    userRepository.save(newUser);
                    SubloaderService.SubloaderResult subloaderResult = subloaderService.userSubloader(newUser);
                    if(!subloaderResult.success()){
                        return new UpsertOutcome(UpdateStatus.PRIVATE_BUILDS, List.of());
                    }
                    return new UpsertOutcome(UpdateStatus.CREATED, subloaderResult.skippedAvatarIds());
                }
            }
            return new UpsertOutcome(UpdateStatus.UNKNOWN_ERROR, List.of());
        }
    }

    // UpdateStatus alone can't carry which characters were skipped over missing
    // meta assets (new game version), so upsert callers get this pair instead.
    public record UpsertOutcome(UpdateStatus status, List<String> skippedAvatarIds) {}

    // adds uid and timeout checks to fetchFromEnka()
    // use this when you want to get a full user object anywhere
    public User getUser(String uid){
        if (validateUid.validate(uid)){
            if(!timeoutService.canIEnkaCallYet(uid)){
                Optional<User> userInDb = userRepository.findByUid(uid);
                if(userInDb.isPresent()){
                    return userInDb.get();
                }
            }
            return fetchUserFromEnka(uid);
        }
        //System.err.println("Error in createUserService.fetchUser, could not get response from enka for uid " + uid);
        return null;
    }

    public String getUserBio(String uid){
        User user = getUser(uid);
        if(user != null){
            return user.getDetailInfo().getSignature();
        }
        return "Error getting user's signature.";
    }

    // this returns a user from enka (doesn't check)
    // THIS ONLY RETURNS A USER DOESN'T SAVE THEM
    public User fetchUserFromEnka(String uid){
        FeignEnkaApiService feignService = Feign.builder()
                .decoder(new JacksonDecoder())
                .target(FeignEnkaApiService.class, "https://enka.network");

        try{
            User response = feignService.getPlayerData(uid);
            if(response != null){
                //update timeout
                timeoutService.upsertTimeoutByUID(uid);
                return response;
            }
//            else {
//                System.err.println("Error in createUserService.fetchUser, could not get response from enka for uid " + uid);
//            }
        } catch (Exception e){
            e.printStackTrace();
            //TODO: make a better exception handling and reporting system
            // this exception must be detected and subsequent processes involving it must be stopped
            // frontend needs to know that enka didn't respond, so maybe try again or check if the UID is correct
        }

        return null;
    }


}
