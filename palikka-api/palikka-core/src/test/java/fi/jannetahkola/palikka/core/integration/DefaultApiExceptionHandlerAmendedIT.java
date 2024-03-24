package fi.jannetahkola.palikka.core.integration;

import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import fi.jannetahkola.palikka.core.api.exception.model.BadRequestErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.ErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.ServerErrorModel;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static fi.jannetahkola.palikka.core.api.exception.ExceptionUtil.errorResponseOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that the default exception handler can be amended to with another controller advice.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureMockMvc
class DefaultApiExceptionHandlerAmendedIT {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    DefaultApiExceptionHandler exceptionHandlerMock;

    @SneakyThrows
    @Test
    void testUnhandledExceptionHandledWithOverwrittenHandler(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.unhandledException(any())).thenCallRealMethod();
        mockMvc.perform(get("/500"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("message", equalTo("Something went wrong")));
        verify(exceptionHandlerMock, times(0)).unhandledException(any());
        assertThat(capturedOutput).contains("Unhandled exception occurred and handler overwritten");
    }

    @SneakyThrows
    @Test
    void testBadRequestExceptionHandledWithAdditionalHandler(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.unhandledException(any())).thenCallRealMethod();
        mockMvc.perform(get("/400"))
                .andExpect(status().is(400));
        assertThat(capturedOutput).contains("Illegal argument exception occurred");
    }

    @RestController
    @Validated
    @SpringBootApplication
    static class DefaultApiExceptionHandlerOverrideITTestApplication {
        public static void main(String[] args) {
            SpringApplication.run(DefaultApiExceptionHandlerAmendedIT.class);
        }

        @GetMapping("/500")
        public void testInternalServerError() {
            throw new RuntimeException("");
        }

        @GetMapping("/400")
        public void testBadRequest() {
            throw new IllegalArgumentException("");
        }

        @EnableAutoConfiguration(exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
        @Configuration
        static class TestApplicationConfiguration {
        }

        @Slf4j
        @ControllerAdvice
        @EnableDefaultApiExceptionHandling
        static class TestApplicationExceptionHandler {
            @ExceptionHandler
            public ResponseEntity<ErrorModel> handleSomethingElse(IllegalArgumentException e) {
                log.info("Illegal argument exception occurred", e);
                return errorResponseOf(new BadRequestErrorModel(e));
            }

            // Should override the method from default handler
            @ExceptionHandler
            public ResponseEntity<ErrorModel> unhandledException(Exception e) {
                log.info("Unhandled exception occurred and handler overwritten", e);
                return errorResponseOf(new ServerErrorModel("Something went wrong"));
            }
        }
    }
}
