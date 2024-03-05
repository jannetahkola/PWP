package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.LoggingFilter;
import org.springframework.context.annotation.Bean;

public class LoggingConfig {
    @Bean
    LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }
}
