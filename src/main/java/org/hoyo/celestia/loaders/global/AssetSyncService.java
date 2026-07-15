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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
            "skills", "store/hsr/skills.json",
            "ranks", "store/hsr/ranks.json",
            "pfps", "store/hsr/pfps.json"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // SHAs of the last download that was fully PROCESSED downstream (meta
    // regenerated + reloaded) — advanced only via commitShas(), never during
    // the download itself. This is the fix for the stale-meta bug where any
    // caller of syncAssets (WeaponLoaderService's boot-time weapons load!)
    // advanced the SHAs as a download side effect without ever regenerating
    // the meta, so the real refresh cycle then saw "no change" forever and
    // the live meta stayed at whatever the jar was built with.
    private final Map<String, String> knownShas = new LinkedHashMap<>();
    // last successfully downloaded copy of each file — lets a partial change still
    // yield a FULL assets map, since MetaRegenService rebuilds the whole meta object
    private final Map<String, JsonNode> cachedAssets = new LinkedHashMap<>();

    /**
     * assets: full name->json map when anything was (re)downloaded, null when
     * everything matched the committed SHAs. shas: the remote SHAs seen during
     * this sync — hand them back to {@link #commitShas} ONLY once downstream
     * processing succeeded, so a failed regen retries on the next cycle.
     */
    public record SyncResult(Map<String, JsonNode> assets, Map<String, String> shas) {}

    public SyncResult syncAssets(int force) {
        Map<String, JsonNode> assets = new LinkedHashMap<>();
        Map<String, String> seenShas = new LinkedHashMap<>();
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
                String knownSha = knownShas.get(name);
                seenShas.put(name, remoteSha);

                if (remoteSha.equals(knownSha) && force != 1) {
                    log.info("{}: up to date (sha {})", name, shortSha(remoteSha));
                    assets.put(name, cachedAssets.get(name));
                    continue;
                }

                if (force == 1) {
                    log.info("{}: FORCED download regardless of change (sha {} -> {})",
                            name, shortSha(knownSha), shortSha(remoteSha));
                } else {
                    log.info("{}: update found on GitHub (sha {} -> {}), downloading...",
                            name, shortSha(knownSha), shortSha(remoteSha));
                }

                String content = fetchUrl(downloadUrl);
                JsonNode parsed = mapper.readTree(content);
                List<String> newIds = newTopLevelIds(cachedAssets.get(name), parsed);
                log.info("{}: downloaded {} top-level entries ({} KB){}",
                        name, parsed.size(), content.length() / 1024,
                        newIds.isEmpty() ? "" : ", new ids vs previous copy: " + newIds);

                assets.put(name, parsed);
                cachedAssets.put(name, parsed);
                anyChanged = true;

            } catch (Exception e) {
                log.error("{}: sync failed, falling back to last downloaded copy.", name, e);
                assets.put(name, cachedAssets.get(name));
            }
        }

        if (!anyChanged) {
            log.info("All assets match the last fully processed sync (nothing to download).");
            return new SyncResult(null, seenShas);
        }

        log.info("Asset download pass complete.");
        return new SyncResult(assets, seenShas);
    }

    /**
     * Mark this sync's SHAs as fully processed. Call ONLY after the downstream
     * pipeline (meta regen + reload) succeeded — anything that just wants the
     * asset data (e.g. the boot-time weapons load) must never call this.
     */
    public void commitShas(Map<String, String> shas) {
        if (shas != null) {
            knownShas.putAll(shas);
        }
    }

    /** First few top-level field names present in `updated` but not in `previous`. */
    private List<String> newTopLevelIds(JsonNode previous, JsonNode updated) {
        List<String> added = new ArrayList<>();
        if (updated == null) return added;
        updated.fieldNames().forEachRemaining(id -> {
            if ((previous == null || !previous.has(id)) && added.size() < 15) {
                added.add(id);
            }
        });
        return added;
    }

    private String shortSha(String sha) {
        if (sha == null) return "none";
        return sha.length() > 7 ? sha.substring(0, 7) : sha;
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
