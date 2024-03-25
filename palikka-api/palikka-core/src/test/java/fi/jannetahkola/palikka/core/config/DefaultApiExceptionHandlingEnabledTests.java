package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import fi.jannetahkola.palikka.core.api.exception.DelegatedAuthenticationEntryPoint;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(WebMvcConfigurationSupport.class) // Required for authentication entry point's HandlerExceptionResolver dep
@EnableDefaultApiExceptionHandling
class DefaultApiExceptionHandlingEnabledTests {
    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        // Handler is not enabled by just adding the annotation
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(DefaultApiExceptionHandler.class));
        assertThat(context.getBean(DelegatedAuthenticationEntryPoint.class)).isNotNull();
    }
}
