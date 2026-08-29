package org.hoyo.celestia.config;

import org.hoyo.celestia.loaders.global.LeaderboardUpdateListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    // Must match Immercalc's LeaderboardPushRunner channel name exactly -
    // no compiler link between the two repos, just this string.
    @Bean
    public ChannelTopic leaderboardUpdatesTopic() {
        return new ChannelTopic("leaderboard-updates");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            LeaderboardUpdateListener leaderboardUpdateListener,
            ChannelTopic leaderboardUpdatesTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(leaderboardUpdateListener, leaderboardUpdatesTopic);
        return container;
    }
}

