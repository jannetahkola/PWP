package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.LoggingFilter;
import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import fi.jannetahkola.palikka.core.api.exception.DelegatedAuthenticationEntryPoint;
import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.meta.EnableRequestAndResponseLoggingSupport;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnableRequestAndResponseLoggingSupport // Should not be enabled unless property is set
class NoConfigsEnabledTests {
    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        // authentication support & users integration
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(JwtService.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(UsersClient.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(PalikkaAuthenticationFilterConfigurer.class));

        // api exception handler
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(DefaultApiExceptionHandler.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(DelegatedAuthenticationEntryPoint.class));

        // logging
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(LoggingFilter.class));
    }
}
