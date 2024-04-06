package fi.jannetahkola.palikka.game.api.status;

import fi.jannetahkola.palikka.game.api.status.model.GameStatusResponse;
import fi.jannetahkola.palikka.game.testutils.IntegrationTest;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test tests only access, see {@link GameStatusControllerTests}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GameStatusControllerIT extends IntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    GameStatusController gameStatusController;

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("palikka.integration.users-api.base-uri", () -> wireMockServer.baseUrl());
    }

    @SneakyThrows
    @Test
    void givenGetGameStatusRequest_thenOkResponse(@Autowired ApplicationContext context) {
        assertThat(context.getBean(WebSecurityConfiguration.class)).isNotNull(); // assert security is enabled

        stubForAdminUser(wireMockServer);

        GameStatusResponse gameStatus = new GameStatusResponse();
        gameStatus.setOnline(false);

        when(gameStatusController.getGameStatus()).thenReturn(gameStatus);

        mockMvc.perform(
                get("/game/status")
                        .header(HttpHeaders.AUTHORIZATION, testTokenGenerator.generateBearerToken(1))
        ).andExpect(status().isOk());
    }
}
