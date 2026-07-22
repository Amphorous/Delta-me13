package org.hoyo.celestia.builds.service;

import lombok.RequiredArgsConstructor;
import org.hoyo.celestia.builds.BuildNodeRepository;
import org.hoyo.celestia.builds.model.BuildNode;
import org.hoyo.celestia.builds.model.BuildPageDTO;
import org.hoyo.celestia.loaders.global.AvatarInfoRedisLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FetchBuildService {

    private static final int PAGE_LIMIT = 20;
    private final BuildNodeRepository buildNodeRepository;
    private final AvatarInfoEnrichmentService avatarInfoEnrichmentService;
    private final AvatarInfoRedisLoader avatarInfoRedisLoader;

    public ResponseEntity<BuildPageDTO> getBuilds(String uid, Integer pageNumber, String order, String filterByAvatarId, String filterByPath, String filterByElement) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;
        long fetchLimit = PAGE_LIMIT + 1L;
        boolean ascending = "ASC".equalsIgnoreCase(order);

        Set<String> avatarIds = resolveAvatarIdFilter(filterByAvatarId, filterByPath, filterByElement);
        boolean hasFilter = avatarIds != null;

        List<BuildNode> builds;
        if(ascending) {
            builds = hasFilter
                    ? buildNodeRepository.findBuildsByUidFilterByAvatarIdsOrderByCvAsc(uid, avatarIds, skip, fetchLimit)
                    : buildNodeRepository.findBuildsByUidOrderByCvAsc(uid, skip, fetchLimit);
        } else {
            builds = hasFilter
                    ? buildNodeRepository.findBuildsByUidFilterByAvatarIdsOrderByCvDesc(uid, avatarIds, skip, fetchLimit)
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

    // Path/Element aren't stored on BuildNode (see AvatarInfoRedisLoader's reverse
    // index) — a filter by either resolves to the set of avatarIds that match,
    // which is what the widened findBuilds...FilterByAvatarIds... queries actually
    // filter on. filterByAvatarId is mutually exclusive with path/element (a
    // character already has exactly one fixed path/element, so combining with it
    // would be redundant) and wins outright if somehow more than one is sent.
    // Path and Element ARE combinable with each other (AND, via retainAll) — the
    // frontend's single active-character-filter / combinable-path-element UX
    // maps directly onto this. A resolved empty Set (unknown path/element, or no
    // overlap between the two) is intentionally distinct from null — it still
    // takes the filtered-query path, which naturally returns zero rows via `IN []`.
    private Set<String> resolveAvatarIdFilter(String filterByAvatarId, String filterByPath, String filterByElement) {
        if (filterByAvatarId != null && !filterByAvatarId.isBlank()) {
            return Set.of(filterByAvatarId);
        }
        boolean hasPath = filterByPath != null && !filterByPath.isBlank();
        boolean hasElement = filterByElement != null && !filterByElement.isBlank();
        if (!hasPath && !hasElement) {
            return null;
        }

        Set<String> result = hasPath
                ? new HashSet<>(avatarInfoRedisLoader.getAvatarIdsForPath(filterByPath))
                : new HashSet<>(avatarInfoRedisLoader.getAvatarIdsForElement(filterByElement));
        if (hasPath && hasElement) {
            result.retainAll(avatarInfoRedisLoader.getAvatarIdsForElement(filterByElement));
        }
        return result;
    }

}
