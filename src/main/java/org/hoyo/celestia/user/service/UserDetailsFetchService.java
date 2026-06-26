package org.hoyo.celestia.user.service;

import org.hoyo.celestia.user.DTOs.NoRefreshUserDTO;
import org.hoyo.celestia.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsFetchService {

    private final UserRepository userRepository;

    public UserDetailsFetchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //this refreshes if the user isnt in the DB, also region check function isnt here yet
    //this does not call subloader, so make sure to call upsert user on going to dashboard page
    /**
     * 1) on going to dashboard, request for builds
     * 1.5) if no builds are found, show that and show the full refresh option (at all times show the "show_builds boolean")
     * 2) after full refresh
     * */


    public String getRegionFromUid(String uid) {
        return switch (uid.charAt(0)) {
            case '0' -> "MHY";
            case '1', '2', '5' -> "CN";
            case '6' -> "NA";
            case '7' -> "EU";
            case '8' -> "ASIA";
            case '9' -> "THM";
            default -> "NONE";
        };
    }

    public ResponseEntity<NoRefreshUserDTO> getUserCardDetailsNoRefresh(String uid){
        if(!userRepository.existsByUid(uid)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        NoRefreshUserDTO noRefreshUserDTO = userRepository.findUserCardByUid(uid);
        noRefreshUserDTO.setRegion(getRegionFromUid(uid));
        return ResponseEntity.ok(noRefreshUserDTO);
    }

}
