package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {
    @Bean
    @ConditionalOnMissingBean(LettuceConnectionFactory.class)
    LettuceConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();
        lettuceConnectionFactory.setHostName(redisProperties.getHost());
        lettuceConnectionFactory.setPort(redisProperties.getPort());
        return lettuceConnectionFactory;
    }

    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
