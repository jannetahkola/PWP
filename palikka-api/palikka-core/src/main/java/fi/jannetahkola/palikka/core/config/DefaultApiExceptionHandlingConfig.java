package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.api.exception.DelegatedAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class DefaultApiExceptionHandlingConfig {
    @Bean
    DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new DelegatedAuthenticationEntryPoint(resolver);
    }
}
