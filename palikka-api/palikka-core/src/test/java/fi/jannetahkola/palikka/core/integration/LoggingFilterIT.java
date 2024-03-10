package fi.jannetahkola.palikka.core.integration;

import fi.jannetahkola.palikka.core.config.meta.EnableRequestAndResponseLoggingSupport;
import lombok.Data;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "palikka.request-and-response-logging.enabled=true"
        }
)
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureMockMvc
class LoggingFilterIT {
    @Autowired
    MockMvc mockMvc;

    @SneakyThrows
    @Test
    void testRequestAndResponseAreLogged(CapturedOutput capturedOutput) {
        String json = new JSONObject().put("value", "test").toString();
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON_VALUE).content(json))
                .andExpect(status().is2xxSuccessful());
        assertThat(capturedOutput.getAll()).contains(
                "Request: Content-Type=application/json;charset=UTF-8\n" +
                "Content-Length=16\n" +
                "{\"value\":\"test\"}");
        assertThat(capturedOutput.getAll()).contains(
                "Response: Content-Type=text/plain;charset=UTF-8\n" +
                        "Content-Length=11\n" +
                        "Hello, test");
    }

    @RestController
    @SpringBootApplication
    static class LoggingFilterITTestApplication {
        public static void main(String[] args) {
            SpringApplication.run(LoggingFilterITTestApplication.class);
        }

        @PostMapping
        public String test(@RequestBody TestRequestObject requestObject) {
            return "Hello, " + requestObject.getValue();
        }

        @EnableAutoConfiguration(exclude = {
                // Disable security
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
        @Configuration
        @EnableRequestAndResponseLoggingSupport
        static class TestApplicationConfiguration {
        }

        @Data
        static class TestRequestObject {
            String value;
        }
    }
}
