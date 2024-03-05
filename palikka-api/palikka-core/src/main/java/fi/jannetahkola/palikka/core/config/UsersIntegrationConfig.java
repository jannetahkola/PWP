package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.config.properties.RemoteUsersIntegrationProperties;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@EnableConfigurationProperties(RemoteUsersIntegrationProperties.class)
public class UsersIntegrationConfig {
    @Bean
    @ConditionalOnMissingBean(UsersClient.class)
    RemoteUsersClient usersClient(RemoteUsersIntegrationProperties properties) {
        log.info("------ Remote users client ENABLED ------");
        return new RemoteUsersClient(properties);
    }
}
