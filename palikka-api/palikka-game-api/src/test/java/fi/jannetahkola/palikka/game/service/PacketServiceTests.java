package fi.jannetahkola.palikka.game.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PacketServiceTests {
    private static final String expectedHandshakePacketBytes = "16, 0, -14, 5, 9, 108, 111, 99, 97, 108, 104, 111, 115, 116, 99, -35, 1, 1, 0";
    private static final String expectedLoginPacketBytes = "17, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 112, 97, 115, 115, 119, 111, 114, 100, 0";
    private static final String expectedCommandPacketBytes = "19, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 116, 105, 109, 101, 32, 115, 101, 116, 32, 48, 0";

    @Test
    void testCreatesValidPackets() {
        PacketService packetService = new PacketService();
        PacketService.Packet handshakePacket = packetService.newHandshakePacket("localhost", 25565);
        PacketService.Packet loginPacket = packetService.newLoginPacket("password");
        PacketService.Packet commandPacket = packetService.newCommandPacket("time set 0");

        assertThat(handshakePacket.getBytesAsString()).isEqualTo(expectedHandshakePacketBytes);
        assertThat(loginPacket.getBytesAsString()).isEqualTo(expectedLoginPacketBytes);
        assertThat(commandPacket.getBytesAsString()).isEqualTo(expectedCommandPacketBytes);
    }
}
