package fi.jannetahkola.palikka.game.api.status;

import fi.jannetahkola.palikka.game.api.status.model.GameStatusResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class GameStatusControllerIT {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    GameStatusController gameStatusController;

    @SneakyThrows
    @Test
    void givenGetGameStatusRequest_thenOkResponse(@Autowired ApplicationContext context) {
        assertThat(context.getBean(WebSecurityConfiguration.class)).isNotNull(); // assert security is enabled

        GameStatusResponse gameStatus = new GameStatusResponse();
        gameStatus.setOnline(false);

        when(gameStatusController.getGameStatus()).thenReturn(gameStatus);

        mockMvc.perform(get("/game/status")).andExpect(status().isOk());
    }
}
