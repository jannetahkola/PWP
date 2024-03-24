package fi.jannetahkola.palikka.game.api.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.game.api.status.model.GameStatusResponse;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.service.PacketService;
import fi.jannetahkola.palikka.game.util.VarIntUtil;
import fi.jannetahkola.palikka.game.service.factory.SocketFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

@Slf4j
@RestController
@RequestMapping("/game/status")
@RequiredArgsConstructor
public class GameStatusController {
    private final GameProperties properties;
    private final SocketFactory socketFactory;
    private final ObjectMapper objectMapper;

    @GetMapping
    public GameStatusResponse getGameStatus() {
        // See https://wiki.vg/Server_List_Ping
        GameProperties.StatusProperties statusProperties = properties.getStatus();
        log.debug("Fetching game status with config={}", statusProperties);

        String host = statusProperties.getHost();
        int port = statusProperties.getPort();
        long connectTimeoutInMillis = statusProperties.getConnectTimeout().toMillis();

        GameStatusResponse status = new GameStatusResponse();

        try (Socket socket = socketFactory.newSocket()) {
            // Game server runs on the same host by necessity so always use localhost
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), port), (int) connectTimeoutInMillis);
            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            final DataInputStream in = new DataInputStream(socket.getInputStream());
            PacketService.Packet handshakePacket = new PacketService().newHandshakePacket(host, port);
            out.write(handshakePacket.getBytes());
            status = readGameStatusFromStream(in, objectMapper);
        } catch (IOException e) {
            // It's not uncommon for this to fail because the game is down
            log.debug("Failed to get game status", e);
            status.setOnline(false);
        }

        status.setHost(host);
        status.setPort(port);

        log.debug("Returning game status={}", status);

        return status;
    }

    private static GameStatusResponse readGameStatusFromStream(DataInputStream in, ObjectMapper objectMapper) throws IOException {
        log.debug("Reading server status from stream");
        VarIntUtil.read(in); // Response length (not used)
        final int responsePacketId = VarIntUtil.read(in); // Packet id
        final int expectedPacketId = PacketService.PacketType.HANDSHAKE;
        if (responsePacketId != expectedPacketId) {
            throw new IllegalStateException(
                    "Response packet id did not match; expected "
                            + expectedPacketId + ", but was " + responsePacketId);
        }
        int responsePayloadLen = VarIntUtil.read(in);
        byte[] buffer = ByteBuffer.allocate(responsePayloadLen).array();
        in.readFully(buffer);
        String statusString = new String(buffer);
        log.debug("Received status from server={}", statusString);
        return objectMapper.readValue(statusString, GameStatusResponse.class);
    }
}
