package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hoyo.celestia.loaders.model.metaModel.HonkerMetaObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Data
@Component
@Slf4j
public class GlobalMetaFileLoader {

    private volatile HonkerMetaObject metaFile;

    @PostConstruct
    public void init() {
        ObjectMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .build();

        try (InputStream is = new ClassPathResource("assets/honker_meta.json").getInputStream()) {
            metaFile = mapper.readValue(is, HonkerMetaObject.class);
            log.info("HonkerMetaFile loaded successfully.");
        } catch (IOException exception) {
            log.error("Failed to load HonkerMetaFile", exception);
        }
    }

    public void reload(HonkerMetaObject newMetaFile) {
        this.metaFile = newMetaFile;
        log.info("HonkerMetaFile hot-reloaded.");
    }
}
