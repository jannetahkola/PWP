package fi.jannetahkola.palikka.core.integration;

import fi.jannetahkola.palikka.core.api.exception.ConflictException;
import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import fi.jannetahkola.palikka.core.api.exception.NotFoundException;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "logging.level.fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler=debug"
        })
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureMockMvc
class DefaultApiExceptionHandlerIT {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    DefaultApiExceptionHandler exceptionHandlerMock;

    @SneakyThrows
    @Test
    void testForbidden(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.accessDeniedException(any())).thenCallRealMethod();
        mockMvc.perform(get("/403"))
                .andExpect(status().is(403));
        verify(exceptionHandlerMock, times(1)).accessDeniedException(any());
        assertThat(capturedOutput).contains("Access denied exception occurred");
    }

    @SneakyThrows
    @Test
    void testNotFound(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.notFoundException(any())).thenCallRealMethod();
        mockMvc.perform(get("/404"))
                .andExpect(status().is(404));
        verify(exceptionHandlerMock, times(1)).notFoundException(any());
        assertThat(capturedOutput).contains("Not found exception occurred");
    }

    @SneakyThrows
    @Test
    void testNoResourceFound(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.noResourceFoundException(any())).thenCallRealMethod();
        mockMvc.perform(get("/unknown"))
                .andExpect(status().is(404));
        verify(exceptionHandlerMock, times(1)).noResourceFoundException(any());
        assertThat(capturedOutput).contains("No resource found exception occurred");
    }

    @SneakyThrows
    @Test
    void testConflict(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.conflictException(any())).thenCallRealMethod();
        mockMvc.perform(get("/409"))
                .andExpect(status().is(409));
        verify(exceptionHandlerMock, times(1)).conflictException(any());
        assertThat(capturedOutput).contains("Conflict exception occurred");
    }

    @SneakyThrows
    @Test
    void testInternalServerError(CapturedOutput capturedOutput) {
        when(exceptionHandlerMock.unhandledException(any())).thenCallRealMethod();
        mockMvc.perform(get("/500"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("message", equalTo("Something went wrong")));
        verify(exceptionHandlerMock, times(1)).unhandledException(any());
        assertThat(capturedOutput).contains("Unhandled exception occurred");
    }

    @SneakyThrows
    @Test
    void testHttpMessageNotReadableException(CapturedOutput capturedOutput) {
        // e.g. no body provided
        when(exceptionHandlerMock.httpMessageNotReadableException(any())).thenCallRealMethod();
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(400))
                .andExpect(jsonPath("message", equalTo("Invalid request")));
        assertThat(capturedOutput).contains("HTTP message not readable exception occurred");
    }

    @SneakyThrows
    @Test
    void testMethodArgumentInvalid(CapturedOutput capturedOutput) {
        // TODO test missing & invalid content type
        // TODO assert body format
        when(exceptionHandlerMock.methodArgumentNotValidException(any())).thenCallRealMethod();
        String json = new JSONObject().put("value", " ").toString();
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON_VALUE).content(json))
                .andExpect(status().is(400));
        verify(exceptionHandlerMock, times(1)).methodArgumentNotValidException(any());
        assertThat(capturedOutput).contains("Method argument not valid exception occurred");
    }

    @RestController
    @Validated
    @SpringBootApplication
    static class DefaultApiExceptionHandlerITTestApplication {
        public static void main(String[] args) {
            SpringApplication.run(DefaultApiExceptionHandlerITTestApplication.class);
        }

        @GetMapping("/403")
        public void testForbidden() {
            throw new AccessDeniedException("");
        }

        @GetMapping("/404")
        public void testNotFound() {
            throw new NotFoundException("");
        }

        @GetMapping("/409")
        public void testConflict() {
            throw new ConflictException("");
        }

        @GetMapping("/500")
        public void testInternalServerError() {
            throw new RuntimeException("");
        }

        @PostMapping
        public void testValidatedBody(@Valid @RequestBody ValidatedRequestObject request) {
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
        }

        @Data
        static class ValidatedRequestObject {
            @NotBlank
            String value;
        }
    }
}
