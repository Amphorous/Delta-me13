package org.hoyo.celestia.loaders.global;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

// Subscribed (see RedisConfig's RedisMessageListenerContainer bean) to the
// "leaderboard-updates" channel Immercalc publishes to on its own startup
// (LeaderboardPushRunner, in Immercalc). This is what makes the avatarId set
// fan out to every running Celestia instance at once, instead of the old
// Feign push which only ever reached one instance behind the load balancer -
// Redis pub/sub genuinely broadcasts to every subscriber. The channel name
// is a plain string with no compile-time link to Immercalc's copy of it -
// it must stay "leaderboard-updates" on both sides.
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardUpdateListener implements MessageListener {

    private final LeaderboardAvatarRegistry leaderboardAvatarRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            List<String> avatarIds = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            leaderboardAvatarRegistry.setAvatarIds(avatarIds);
            log.info("Leaderboard avatarId list updated via Redis pub/sub ({} avatarIds).", avatarIds.size());
        } catch (Exception e) {
            log.warn("Failed to process leaderboard-updates pub/sub message: {}", e.getMessage());
        }
    }
}
