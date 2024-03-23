package fi.jannetahkola.palikka.game.websocket;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.level.fi.jannetahkola.palikka.game=debug",
                "palikka.game.session.auto-clean.fixed-delay=1000",
                "palikka.game.session.auto-clean.enabled=true",
                "palikka.jwt.keystore.signing.path=keystore-dev.p12",
                "palikka.jwt.keystore.signing.pass=password",
                "palikka.jwt.keystore.signing.type=pkcs12",
                "palikka.jwt.token.user.issuer=palikka-dev-usr",
                "palikka.jwt.token.user.signing.key-alias=jwt-usr",
                "palikka.jwt.token.user.signing.key-pass=password",
                "palikka.jwt.token.user.signing.validity-time=10s",
        })
@ExtendWith(OutputCaptureExtension.class)
class SessionCleanUpSchedulerIT {
    @Autowired
    SessionCleanUpScheduler sessionCleanUpScheduler;

    @MockBean
    SessionStore sessionStoreMock;

    @BeforeEach
    void beforeEach() {
        doNothing().when(sessionStoreMock).evictExpiredSessions();
    }


    @SneakyThrows
    @Test
    void testScheduledJobIsRan() {
        verify(sessionStoreMock, atLeastOnce()).evictExpiredSessions();
        assertThat(sessionCleanUpScheduler.getInvocationCount()).isPositive();
    }
}
