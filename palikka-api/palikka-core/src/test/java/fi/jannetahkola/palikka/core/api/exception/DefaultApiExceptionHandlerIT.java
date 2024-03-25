package fi.jannetahkola.palikka.core.api.exception;

import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @SpyBean
    DefaultApiExceptionHandler exceptionHandlerMock;

    @SneakyThrows
    @Test
    void testBadRequest(CapturedOutput capturedOutput) {
        mockMvc.perform(get("/400"))
                .andDo(print())
                .andExpect(status().is(400))
                .andExpect(jsonPath("status", equalTo(400)))
                .andExpect(jsonPath("title", equalTo("Bad Request")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
        verify(exceptionHandlerMock, times(1)).badRequestException(any(), any());
        assertThat(capturedOutput).contains("Bad request exception occurred");
    }

    @SneakyThrows
    @Test
    void testAccessDenied(CapturedOutput capturedOutput) {
        mockMvc.perform(get("/403"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("status", equalTo(403)))
                .andExpect(jsonPath("title", equalTo("Forbidden")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
        verify(exceptionHandlerMock, times(1)).accessDeniedException(any(), any());
        assertThat(capturedOutput).contains("Access denied exception occurred");
    }

    @SneakyThrows
    @Test
    void testNotFound(CapturedOutput capturedOutput) {
        mockMvc.perform(get("/404"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("status", equalTo(404)))
                .andExpect(jsonPath("title", equalTo("Not Found")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
        verify(exceptionHandlerMock, times(1)).notFoundException(any(), any());
        assertThat(capturedOutput).contains("Not found exception occurred");
    }

    @SneakyThrows
    @Test
    void testNoResourceFound() {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("status", equalTo(404)))
                .andExpect(jsonPath("title", equalTo("Not Found")))
                .andExpect(jsonPath("detail", equalTo("No static resource unknown.")))
                .andExpect(jsonPath("instance", equalTo("/unknown")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @SneakyThrows
    @Test
    void testConflict(CapturedOutput capturedOutput) {
        mockMvc.perform(get("/409"))
                .andExpect(status().is(409))
                .andExpect(jsonPath("title", equalTo("Conflict")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
        verify(exceptionHandlerMock, times(1)).conflictException(any(), any());
        assertThat(capturedOutput).contains("Conflict exception occurred");
    }

    @SneakyThrows
    @Test
    void testInternalServerError(CapturedOutput capturedOutput) {
        mockMvc.perform(get("/500"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("status", equalTo(500)))
                .andExpect(jsonPath("title", equalTo("Internal Server Error")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
        verify(exceptionHandlerMock, times(1)).unhandledException(any(), any());
        assertThat(capturedOutput).contains("Unhandled exception occurred");
    }

    @SneakyThrows
    @Test
    void testHttpMessageNotReadableException() {
        // e.g. no body provided
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(400))
                .andDo(print())
                .andExpect(jsonPath("status", equalTo(400)))
                .andExpect(jsonPath("title", equalTo("Bad Request")))
                .andExpect(jsonPath("detail", containsString("HTTP message not readable")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("unsupportedMediaTypeArgs")
    void testHttpMediaTypeNotSupportedException(String contentType,
                                                String expectedErrorMessageSubstring) {
        String json = new JSONObject().put("value", "valid-value").toString();
        mockMvc.perform(post("/").contentType(contentType).content(json))
                .andExpect(status().is(415))
                .andExpect(jsonPath("status", equalTo(415)))
                .andExpect(jsonPath("title", equalTo("Unsupported Media Type")))
                .andExpect(jsonPath("detail", containsString(expectedErrorMessageSubstring)))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @SneakyThrows
    @Test
    void testHttpMediaTypeNotSupportedException_whenMethodHandlerConfiguresContentType() {
        String json = new JSONObject().put("value", "valid-value").toString();
        mockMvc.perform(post("/json").content(json))
                .andDo(print())
                .andExpect(status().is(415))
                .andExpect(jsonPath("status", equalTo(415)))
                .andExpect(jsonPath("title", equalTo("Unsupported Media Type")))
                .andExpect(jsonPath("detail", equalTo("Content-Type 'null' is not supported.")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    static Stream<Arguments> unsupportedMediaTypeArgs() {
        return Stream.of(
                Arguments.of(MediaType.TEXT_PLAIN_VALUE, "Content-Type 'text/plain;charset=UTF-8' is not supported"),
                Arguments.of("", "Content-Type 'application/octet-stream' is not supported"),
                Arguments.of("test", "Could not parse Content-Type."),
                Arguments.of("test/test", "Content-Type 'test/test;charset=UTF-8' is not supported"),
                Arguments.of("höpö/höpö", "Could not parse Content-Type.")
        );
    }

    @SneakyThrows
    @Test
    void testMethodArgumentInvalidException() {
        String json = new JSONObject().put("value", " ").toString();
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON_VALUE).content(json))
                .andExpect(status().is(400))
                .andExpect(jsonPath("status", equalTo(400)))
                .andExpect(jsonPath("title", equalTo("Bad Request")))
                .andExpect(jsonPath("detail", equalTo("value: must not be blank")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @SneakyThrows
    @Test
    void testMethodArgumentTypeMismatchException() {
        mockMvc.perform(get("/resource/{id}", "nan"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("status", equalTo(400)))
                .andExpect(jsonPath("title", equalTo("Bad Request")))
                .andExpect(jsonPath("detail", equalTo("Failed to convert 'id' with value: 'nan'")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @SneakyThrows
    @Test
    void testMethodNotAllowedException() {
        mockMvc.perform(get("/json"))
                .andExpect(status().is(405))
                .andExpect(jsonPath("status", equalTo(405)))
                .andExpect(jsonPath("title", equalTo("Method Not Allowed")))
                .andExpect(jsonPath("detail", equalTo("Method 'GET' is not supported.")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @RestController
    @Validated
    @SpringBootApplication
    static class DefaultApiExceptionHandlerITTestApplication {
        public static void main(String[] args) {
            SpringApplication.run(DefaultApiExceptionHandlerITTestApplication.class);
        }

        @GetMapping("/400")
        public void testBadRequest() {
            throw new BadRequestException("");
        }

        @GetMapping(value = "/400/hal", produces = MediaTypes.HAL_JSON_VALUE)
        public void testBadRequestWithHandlerDefinedContentType() {
            throw new BadRequestException("");
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
        public void testInvalidPostRequest(@Valid @RequestBody ValidatedRequestObject request) {
        }

        @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        public void testPostRequestWithHandlerDefinedContentType(@Valid @RequestBody ValidatedRequestObject request) {
        }

        @GetMapping("/resource/{id}")
        public String testInvalidParam(@PathVariable("id") Integer id) {
            return String.valueOf(id);
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
            // Default exception handler is registered automatically in this module
        }

        @Data
        static class ValidatedRequestObject {
            @NotBlank
            String value;
        }
    }
}
