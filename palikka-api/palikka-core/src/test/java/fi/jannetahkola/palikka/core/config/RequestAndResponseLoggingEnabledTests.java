package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.LoggingFilter;
import fi.jannetahkola.palikka.core.config.meta.EnableRequestAndResponseLogging;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnableRequestAndResponseLogging
class RequestAndResponseLoggingEnabledTests {
    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        assertThat(context.getBean(LoggingFilter.class)).isNotNull();
    }
}
