package org.hoyo.celestia.builds.service;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.BuildNodeRepository;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.model.BuildPageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FetchBuildService {

    private static final int PAGE_LIMIT = 20;
    private final BuildNodeRepository buildNodeRepository;
    private final AvatarInfoEnrichmentService avatarInfoEnrichmentService;

    public ResponseEntity<BuildPageDTO> getBuilds(String uid, Integer pageNumber, String order, String filterByAvatarId) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;
        long fetchLimit = PAGE_LIMIT + 1L;
        boolean ascending = "ASC".equalsIgnoreCase(order);
        List<BuildNode> builds;
        if(ascending) {
            builds = filterByAvatarId != null
                    ? buildNodeRepository.findBuildsByUidFilterByAvatarIdOrderByCvAsc(uid, filterByAvatarId, skip, fetchLimit)
                    : buildNodeRepository.findBuildsByUidOrderByCvAsc(uid, skip, fetchLimit);
        } else {
            builds = filterByAvatarId != null
                    ? buildNodeRepository.findBuildsByUidFilterByAvatarIdOrderByCvDesc(uid, filterByAvatarId, skip, fetchLimit)
                    : buildNodeRepository.findBuildsByUidOrderByCvDesc(uid, skip, fetchLimit);
        }

        boolean hasMore = builds.size() > PAGE_LIMIT;
        List<BuildNode> pageBuilds = hasMore ? builds.subList(0, PAGE_LIMIT) : builds;

        avatarInfoEnrichmentService.enrich(pageBuilds);

        BuildPageDTO page = new BuildPageDTO();
        page.setBuilds(pageBuilds);
        page.setHasMore(hasMore);

        return ResponseEntity.ok(page);
    }

    public ResponseEntity<List<BuildNode>> getBuildList(String uid) {
        List<BuildNode> builds = buildNodeRepository.getAllBuilds(uid);
        avatarInfoEnrichmentService.enrichMinimal(builds);
        return ResponseEntity.ok(builds);
    }

}
