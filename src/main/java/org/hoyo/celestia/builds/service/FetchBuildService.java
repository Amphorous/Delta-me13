package org.hoyo.celestia.builds.service;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.BuildNodeRepository;
import org.hoyo.celestia.builds.model.BuildNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FetchBuildService {

    private static final int PAGE_LIMIT = 20;
    private final BuildNodeRepository buildNodeRepository;

    public ResponseEntity<List<BuildNode>> getBuilds(String uid, Integer pageNumber, String order, String filterByAvatarId) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;
        boolean ascending = "ASC".equalsIgnoreCase(order);
        if(ascending) {
            if(filterByAvatarId != null) {
                return ResponseEntity.ok(buildNodeRepository.findBuildsByUidFilterByAvatarIdOrderByCvAsc(uid, filterByAvatarId, skip, PAGE_LIMIT));
            }
            return ResponseEntity.ok(buildNodeRepository.findBuildsByUidOrderByCvAsc(uid, skip, PAGE_LIMIT));
        } else {
            if(filterByAvatarId != null) {
                return ResponseEntity.ok(buildNodeRepository.findBuildsByUidFilterByAvatarIdOrderByCvDesc(uid, filterByAvatarId, skip, PAGE_LIMIT));
            }
            return ResponseEntity.ok(buildNodeRepository.findBuildsByUidOrderByCvDesc(uid, skip, PAGE_LIMIT));
        }
    }

    public ResponseEntity<List<BuildNode>> getBuildList(String uid) {
        return ResponseEntity.ok(buildNodeRepository.getAllBuilds(uid));
    }

}
