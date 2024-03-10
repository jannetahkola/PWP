package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.LoggingFilter;
import fi.jannetahkola.palikka.core.config.meta.EnableRequestAndResponseLoggingSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "palikka.request-and-response-logging.enabled=true"
        }
)
@EnableRequestAndResponseLoggingSupport
class RequestAndResponseLoggingSupportEnabledTests {
    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        assertThat(context.getBean(LoggingFilter.class)).isNotNull();
    }
}
