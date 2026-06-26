package org.hoyo.celestia.builds.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BindingJsonHandler {

    private static final String BINDINGS_KEY_PREFIX = "userBindings:";
    private static final String GAMES_INDEX_SUFFIX = ":games";

    private final RedisTemplate<String, String> redisTemplate;

    public Boolean checkAquilaKeyToUidBinding(String encryptedKey, String game, String uid) {
        Set<String> uids = getUserGameMappings(encryptedKey).get(game);
        return uids != null && uids.contains(uid);
    }

    /**
     * Retrieves all game UIDs associated with a user.
     *<p>
     * Structure:<p>
     * bindings = {
     *      "hsr": ["uid1", ...],
     *      "gi": ["uid1", ...]
     * }
     * @param encryptedKey The unique identifier from Discord.
     * @return A map of game names to sets of UIDs.
     */
    private Map<String, Set<String>> getUserGameMappings(String encryptedKey) {
        Set<String> games = redisTemplate.opsForSet().members(gamesIndexKey(encryptedKey));
        if (games == null || games.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Set<String>> mappings = new HashMap<>();
        for (String game : games) {
            Set<String> uids = redisTemplate.opsForSet().members(bindingsKey(encryptedKey, game));
            mappings.put(game, uids == null ? Set.of() : uids);
        }
        return mappings;
    }

    private String gamesIndexKey(String encryptedKey) {
        return BINDINGS_KEY_PREFIX + encryptedKey + GAMES_INDEX_SUFFIX;
    }

    private String bindingsKey(String encryptedKey, String game) {
        return BINDINGS_KEY_PREFIX + encryptedKey + ":" + game;
    }
}
