package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnableDefaultApiExceptionHandling
class DefaultApiExceptionHandlingEnabledTests {
    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        assertThat(context.getBean(DefaultApiExceptionHandler.class)).isNotNull();
    }
}
