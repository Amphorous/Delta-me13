package org.hoyo.celestia.relics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.relics.DTOs.DuplicateRelicGroupProjection;
import org.hoyo.celestia.relics.RelicNodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelicDedupeService {

    private final RelicNodeRepository relicNodeRepository;

    public List<DuplicateRelicGroupProjection> findDuplicates() {
        return relicNodeRepository.findDuplicateRelicGroups();
    }

    public long dedupeAll() {
        Long deleted = relicNodeRepository.dedupeAllRelics();
        long count = deleted == null ? 0 : deleted;
        log.warn("Relic dedupe: removed {} duplicate RelicNode(s) (redirected any EQUIPS_RELIC edges to the surviving node first).", count);
        return count;
    }
}
