package fi.jannetahkola.palikka.game.api.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.game.api.status.model.GameStatusResponse;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.service.factory.SocketFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests the controller without Spring context because of difficulties mocking the connection with it.
 */
class GameStatusControllerTests {
    static final String RESPONSE_PAYLOAD = "{\"version\":{\"name\":\"1.20.4\",\"protocol\":765},\"enforcesSecureChat\":true,\"description\":\"A Minecraft Server\",\"players\":{\"max\":20,\"online\":0}}";

    @SneakyThrows
    @Test
    void givenGetStatusRequest_whenUp_thenResponseParsedCorrectly() {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        PipedInputStream inOut = new PipedInputStream();
        PipedOutputStream outOut = new PipedOutputStream(inOut);

        Socket socketMock = mock(Socket.class);
        when(socketMock.getInputStream()).thenReturn(in);
        when(socketMock.getOutputStream()).thenReturn(outOut);
        doNothing().when(socketMock).connect(any(), anyInt());

        SocketFactory socketFactoryMock = mock(SocketFactory.class);
        when(socketFactoryMock.newSocket()).thenReturn(socketMock);

        GameProperties.StatusProperties statusProperties = new GameProperties.StatusProperties();
        statusProperties.setHost("127.0.0.1");
        statusProperties.setPort(25565);
        GameProperties gameProperties = new GameProperties();
        gameProperties.setStatus(statusProperties);
        GameStatusController controller = new GameStatusController(gameProperties, socketFactoryMock, new ObjectMapper());

        byte[] responseLengthVarIntBytes = {-116, 1}; // result 140
        out.write(responseLengthVarIntBytes);

        byte[] packetIdVarIntBytes = {0}; // result 0
        out.write(packetIdVarIntBytes);

        byte[] payloadLengthVarIntBytes = {-119, 1}; // result 137
        out.write(payloadLengthVarIntBytes);

        out.write(RESPONSE_PAYLOAD.getBytes(StandardCharsets.UTF_8));

        GameStatusResponse gameStatus = CompletableFuture.supplyAsync(controller::getGameStatus).get(1000, TimeUnit.MILLISECONDS);
        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.isOnline()).isTrue();
        assertThat(gameStatus.getVersion()).isEqualTo("1.20.4");
        assertThat(gameStatus.getPlayers().getMax()).isEqualTo(20);
        assertThat(gameStatus.getDescription()).isEqualTo("A Minecraft Server");
    }

    @SneakyThrows
    @Test
    void givenGetStatusRequest_whenDown_thenOkResponse() {
        Socket socketMock = mock(Socket.class);
        doThrow(new IOException()).when(socketMock).connect(any(), anyInt());

        SocketFactory socketFactoryMock = mock(SocketFactory.class);
        when(socketFactoryMock.newSocket()).thenReturn(socketMock);

        GameProperties.StatusProperties statusProperties = new GameProperties.StatusProperties();
        statusProperties.setHost("127.0.0.1");
        statusProperties.setPort(25565);
        GameProperties gameProperties = new GameProperties();
        gameProperties.setStatus(statusProperties);

        GameStatusController controller = new GameStatusController(gameProperties, socketFactoryMock, new ObjectMapper());
        GameStatusResponse gameStatus = controller.getGameStatus();

        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.isOnline()).isFalse();
        assertThat(gameStatus.getHost()).isEqualTo("127.0.0.1");
        assertThat(gameStatus.getPort()).isEqualTo(25565);
    }
}
