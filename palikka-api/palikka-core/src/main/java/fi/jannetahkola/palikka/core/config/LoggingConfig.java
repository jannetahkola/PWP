package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.LoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@Slf4j
public class LoggingConfig {
    @Bean
    @ConditionalOnProperty(
            value = "palikka.request-and-response-logging.enabled",
            havingValue = "true"
    )
    LoggingFilter loggingFilter() {
        log.warn("----- Request and response logging ENABLED -----");
        return new LoggingFilter();
    }
}
