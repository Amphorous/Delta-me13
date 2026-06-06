package org.hoyo.celestia.relics.service;

import org.hoyo.celestia.relics.DTOs.RelicProjectionDTO;
import org.hoyo.celestia.relics.RelicNodeRepository;
import org.hoyo.celestia.relics.model.RelicNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FetchRelicService {

    private static final int PAGE_LIMIT = 20;

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

    public ResponseEntity<List<RelicProjectionDTO>> getUserRelicsForDisplay(String uid, int pageNumber) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;
        List<RelicNode> relics = relicNodeRepository.findRelicsPaged(uid, skip, PAGE_LIMIT);

        List<RelicProjectionDTO> relicDTOs = new ArrayList<>();
        for (RelicNode relic : relics) {
            RelicProjectionDTO relicDTO = new RelicProjectionDTO();
            relicDTO.setRelic(relic);
            relicDTO.setBuilds(relicNodeRepository.findBuildsForRelic(relic.getRelicId()));
            relicDTOs.add(relicDTO);
        }

        return ResponseEntity.ok(relicDTOs);
    }

    public ResponseEntity<List<RelicProjectionDTO>> getUserRelicsForDisplaySortedBy(String uid, int pageNumber, String sortBy, String order) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;

        List<RelicNode> relics;

        boolean ascending = "ASC".equalsIgnoreCase(order);

        if ("CV".equalsIgnoreCase(sortBy)) {
            relics = ascending
                    ? relicNodeRepository.findRelicsPagedSortedByCVAsc(uid, skip, PAGE_LIMIT)
                    : relicNodeRepository.findRelicsPagedSortedByCVDesc(uid, skip, PAGE_LIMIT);
        } else {
            relics = ascending
                    ? relicNodeRepository.findRelicsPagedSortedByStatAsc(uid, sortBy, skip, PAGE_LIMIT)
                    : relicNodeRepository.findRelicsPagedSortedByStatDesc(uid, sortBy, skip, PAGE_LIMIT);
        }

        List<RelicProjectionDTO> relicDTOs = new ArrayList<>();

        for (RelicNode relic : relics) {
            RelicProjectionDTO dto = new RelicProjectionDTO();
            dto.setRelic(relic);
            dto.setBuilds(relicNodeRepository.findBuildsForRelic(relic.getRelicId()));
            relicDTOs.add(dto);
        }

        return ResponseEntity.ok(relicDTOs);
    }

    public ResponseEntity<List<RelicProjectionDTO>> getUserRelicsForDisplayFilteredBy(String uid, int pageNumber, String filterBy) {
        long skip = (long) (pageNumber - 1) * PAGE_LIMIT;

        List<RelicNode> relics = relicNodeRepository.findRelicsPagedFiltered(uid, filterBy, skip, PAGE_LIMIT);

        List<RelicProjectionDTO> relicDTOs = relics.stream()
                .map(relic -> {
                    RelicProjectionDTO dto = new RelicProjectionDTO();
                    dto.setRelic(relic);
                    dto.setBuilds(relicNodeRepository.findBuildsForRelic(relic.getRelicId()));
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(relicDTOs);
    }
}
