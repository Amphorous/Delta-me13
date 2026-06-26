package org.hoyo.celestia.relics.service;

import org.hoyo.celestia.relics.DTOs.RelicPageDTO;
import org.hoyo.celestia.relics.DTOs.RelicProjectionDTO;
import org.hoyo.celestia.relics.RelicNodeRepository;
import org.hoyo.celestia.relics.model.RelicNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FetchRelicService {

    private static final int PAGE_LIMIT = 20;
    private static final Set<String> VALID_FILTER_FIELDS = Set.of("setName", "tid", "type", "setId");

    private final RelicNodeRepository relicNodeRepository;

    public FetchRelicService(RelicNodeRepository relicNodeRepository) {
        this.relicNodeRepository = relicNodeRepository;
    }

    public ResponseEntity<List<RelicNode>> getUserRelics(String uid){
        List<RelicNode> relics = relicNodeRepository.findRelicsPaged(uid, 0, 2);
        System.out.println("::::::::::::::::::::::::::::::::::::");
        System.out.println(relics.get(0).getSubAffixes().size());
        System.out.println("::::::::::::::::::::::::::::::::::::");
        return ResponseEntity.ok(relics);
    }

    public ResponseEntity<RelicPageDTO> getUserRelicsForDisplay(
            String uid, int pageNumber, String sortBy, String order,
            String filterField, String filterValue, String typeFilter
    ) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;
        long fetchLimit = PAGE_LIMIT + 1;
        boolean ascending = "ASC".equalsIgnoreCase(order);
        boolean hasFilter = filterField != null && filterValue != null
                && VALID_FILTER_FIELDS.contains(filterField);

        if (sortBy == null) {
            sortBy = "CV";
        }

        List<RelicNode> relics;

        if (hasFilter) {
            if ("CV".equalsIgnoreCase(sortBy)) {
                relics = ascending
                        ? relicNodeRepository.findRelicsPagedFilteredSortedByCVAsc(uid, filterField, filterValue, typeFilter, skip, fetchLimit)
                        : relicNodeRepository.findRelicsPagedFilteredSortedByCVDesc(uid, filterField, filterValue, typeFilter, skip, fetchLimit);
            } else {
                relics = ascending
                        ? relicNodeRepository.findRelicsPagedFilteredSortedByStatAsc(uid, filterField, filterValue, sortBy, typeFilter, skip, fetchLimit)
                        : relicNodeRepository.findRelicsPagedFilteredSortedByStatDesc(uid, filterField, filterValue, sortBy, typeFilter, skip, fetchLimit);
            }
        } else {
            if ("CV".equalsIgnoreCase(sortBy)) {
                relics = ascending
                        ? relicNodeRepository.findRelicsPagedSortedByCVAsc(uid, typeFilter, skip, fetchLimit)
                        : relicNodeRepository.findRelicsPagedSortedByCVDesc(uid, typeFilter, skip, fetchLimit);
            } else {
                relics = ascending
                        ? relicNodeRepository.findRelicsPagedSortedByStatAsc(uid, sortBy, typeFilter, skip, fetchLimit)
                        : relicNodeRepository.findRelicsPagedSortedByStatDesc(uid, sortBy, typeFilter, skip, fetchLimit);
            }
        }

        boolean hasMore = relics.size() > PAGE_LIMIT;
        List<RelicNode> pageRelics = hasMore ? relics.subList(0, PAGE_LIMIT) : relics;

        List<RelicProjectionDTO> relicDTOs = new ArrayList<>();
        for (RelicNode relic : pageRelics) {
            RelicProjectionDTO dto = new RelicProjectionDTO();
            dto.setRelic(relic);
            dto.setBuilds(relicNodeRepository.findBuildsForRelic(relic.getRelicId()));
            relicDTOs.add(dto);
        }

        RelicPageDTO page = new RelicPageDTO();
        page.setRelics(relicDTOs);
        page.setHasMore(hasMore);

        return ResponseEntity.ok(page);
    }
}
