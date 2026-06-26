package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class AssetSyncService {

    private static final String REPO = "EnkaNetwork/API-docs";
    private static final String BRANCH = "master";
    private static final Map<String, String> FILES = Map.of(
            "avatars", "store/hsr/avatars.json",
            "weapons", "store/hsr/weapons.json",
            "relics", "store/hsr/relics.json",
            "tree", "store/hsr/tree.json",
            "skills", "store/hsr/skills.json"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, String> knownShas = new LinkedHashMap<>();

    public Map<String, JsonNode> syncAssets() {
        Map<String, JsonNode> assets = new LinkedHashMap<>();
        boolean anyChanged = false;

        for (Map.Entry<String, String> entry : FILES.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue();

            try {
                String apiUrl = "https://api.github.com/repos/" + REPO + "/contents/" + path + "?ref=" + BRANCH;
                String metaJson = fetchUrl(apiUrl);
                JsonNode meta = mapper.readTree(metaJson);
                String remoteSha = meta.get("sha").asText();
                String downloadUrl = meta.get("download_url").asText();

                if (remoteSha.equals(knownShas.get(name))) {
                    log.info("No change in {}", name);
                    continue;
                }

                log.info("Change detected in {}, downloading...", name);
                String content = fetchUrl(downloadUrl);
                JsonNode parsed = mapper.readTree(content);
                assets.put(name, parsed);
                knownShas.put(name, remoteSha);
                anyChanged = true;

            } catch (Exception e) {
                log.error("Failed to sync asset: {}", name, e);
            }
        }

        if (!anyChanged) {
            log.info("No asset updates detected.");
            return null;
        }

        log.info("Asset sync complete. {} files updated.", assets.size());
        return assets;
    }

    private String fetchUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }
}
