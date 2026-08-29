package org.hoyo.celestia.loaders.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

// Seeds the in-memory registry from Redis at Celestia's own startup. This
// used to be a Feign pull from Immercalc's /honker/get-list, best-effort
// since Immercalc (which always starts last) usually wasn't up yet - now it
// reads "leaderboard:avatarIds" directly from Redis instead, which is a
// depends_on: service_healthy dependency for every backend service, so it's
// guaranteed to already be up. Redis is the actual source of truth now:
// Immercalc writes this key (and publishes on "leaderboard-updates" for
// already-running instances - see LeaderboardUpdateListener) rather than
// calling Celestia directly, which is what makes this correct with more
// than one Celestia instance - the old Feign push only ever reached one
// instance behind the load balancer. Key name must match Immercalc's
// LeaderboardPushRunner exactly - no compiler link between the two repos.
@Slf4j
@Component
public class LeaderboardStartupSync implements ApplicationRunner {

    private static final String LEADERBOARD_AVATAR_IDS_KEY = "leaderboard:avatarIds";

    private final RedisTemplate<String, String> redisTemplate;
    private final LeaderboardAvatarRegistry leaderboardAvatarRegistry;

    public LeaderboardStartupSync(RedisTemplate<String, String> redisTemplate, LeaderboardAvatarRegistry leaderboardAvatarRegistry) {
        this.redisTemplate = redisTemplate;
        this.leaderboardAvatarRegistry = leaderboardAvatarRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Set<String> avatarIds = redisTemplate.opsForSet().members(LEADERBOARD_AVATAR_IDS_KEY);
            leaderboardAvatarRegistry.setAvatarIds(avatarIds == null ? Set.of() : avatarIds);
            log.info("Fetched leaderboard avatarId list from Redis at startup ({} avatarIds).", avatarIds == null ? 0 : avatarIds.size());
        } catch (Exception e) {
            log.warn("Could not fetch leaderboard avatarId list from Redis at startup: {}", e.getMessage());
        }
    }
}
