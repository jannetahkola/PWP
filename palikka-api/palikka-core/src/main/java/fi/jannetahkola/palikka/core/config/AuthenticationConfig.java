package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.authenticator.JwtAuthenticationProvider;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@EnableRedisRepositories(basePackageClasses = RevokedTokenRepository.class)
@EnableConfigurationProperties(RedisProperties.class)
public class AuthenticationConfig {
    @Bean
    PalikkaAuthenticationFilterConfigurer authenticationFilterConfigurer(JwtAuthenticationProvider jwtAuthenticationProvider) {
        return new PalikkaAuthenticationFilterConfigurer(jwtAuthenticationProvider);
    }

    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(JwtService jwtService,
                                                        UsersClient usersClient,
                                                        RevokedTokenRepository revokedTokenRepository) {
        return new JwtAuthenticationProvider(jwtService, usersClient, revokedTokenRepository);
    }

    @Bean
    LettuceConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();
        lettuceConnectionFactory.setHostName(redisProperties.getHost());
        lettuceConnectionFactory.setPort(redisProperties.getPort());
        return lettuceConnectionFactory;
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
