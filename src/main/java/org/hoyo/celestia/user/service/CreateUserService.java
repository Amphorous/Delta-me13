package org.hoyo.celestia.user.service;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import org.hoyo.celestia.subloaders.service.SubloaderService;
import org.hoyo.celestia.timeouts.service.TimeoutService;
import org.hoyo.celestia.user.UpdateStatus;
import org.hoyo.celestia.user.repository.UserRepository;
import org.hoyo.celestia.user.model.User;
import org.hoyo.celestia.user.validate.ValidateUid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CreateUserService {

    private final ValidateUid validateUid;
    private final UserRepository userRepository;
    private final TimeoutService timeoutService;
    private final SubloaderService subloaderService;

    public CreateUserService(ValidateUid validateUid, UserRepository userRepository, TimeoutService timeoutService, SubloaderService subloaderService) {
        this.validateUid = validateUid;
        this.userRepository = userRepository;
        this.timeoutService = timeoutService;
        this.subloaderService = subloaderService;
    }

    //check mongo if user by uid exists
    //if no then enka call and create user
    //if yes then enka call and refresh data
    // [[ everytime a user refreshes a UID's data, this flow MUST occur, and the flow MUST go through here ]]
    public UpdateStatus upsertUser(String uid){
        if(!validateUid.validate(uid)){
            return UpdateStatus.BAD_UID;
        }
        if(!timeoutService.canIEnkaCallYet(uid)){
            //timeout isnt ready
            return UpdateStatus.ENKA_TIMEOUT;
        }

        Optional<User> userInDb = userRepository.findByUid(uid);
        if(userInDb.isPresent()){
            User user = userInDb.get();
            User newUser = getUser(uid);
            if(newUser == null || newUser.getDetailInfo() == null || newUser.getUid() == null){
                return UpdateStatus.ENKA_USER_NOT_FOUND;
            }
            newUser.setId(user.getId());
            userRepository.save(newUser);
            //call subloader here---------
            Boolean subloaderStatus = subloaderService.userSubloader(newUser);
            if(!subloaderStatus){
                return UpdateStatus.PRIVATE_BUILDS;
            }
            //----------------------------
            return UpdateStatus.UPDATED;
        } else {
            User newUser = getUser(uid);
            if(newUser != null){
                userRepository.save(newUser);
                //call subloader here---------
                Boolean subloaderStatus = subloaderService.userSubloader(newUser);
                if(!subloaderStatus){
                    return UpdateStatus.PRIVATE_BUILDS;
                }
                //----------------------------
                return UpdateStatus.CREATED;
            }
        }
        return UpdateStatus.UNKNOWN_ERROR;
    }

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
        }

        return null;
    }


}
