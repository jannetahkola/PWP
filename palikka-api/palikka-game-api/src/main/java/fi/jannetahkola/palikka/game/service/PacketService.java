package fi.jannetahkola.palikka.game.service;

import fi.jannetahkola.palikka.game.util.VarIntUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PacketService {
    public static class Packet {
        private final byte[] bytes;

        /**
         * Creates a new packet by wrapping the provided payload bytes with a length and padding.
         * @param payload Packet payload.
         */
        public Packet(byte[] payload, int packetType) {
            if (PacketType.HANDSHAKE == packetType) {
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                final DataOutputStream stream = new DataOutputStream(buffer);
                try {
                    stream.write(payload.length);
                    stream.write(payload);
                    stream.write(1);
                    stream.write(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.bytes = buffer.toByteArray();
            } else {
                this.bytes = ByteBuffer.allocate(4 + payload.length + 1).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(payload.length + 1)
                        .put(payload)
                        .put((byte) 0)
                        .array();
            }

        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getBytesAsString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(bytes[i]);
                if (i < bytes.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    public static class PacketType {
        private PacketType() {
            // Util
        }

        public static final int HANDSHAKE = 0;
        public static final int COMMAND = 2;
        public static final int LOGIN = 3;
    }

    private int packetId = 0;

    private int getPacketIdAndIncrement() {
        return packetId++;
    }

    private ByteBuffer newAllocatedBuffer(final int allocSize) {
        return ByteBuffer.allocate(allocSize).order(ByteOrder.LITTLE_ENDIAN);
    }

    public Packet newHandshakePacket(final String host, final Integer port) {
        final int packetType = PacketType.HANDSHAKE;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final DataOutputStream stream = new DataOutputStream(buffer);
        try {
            stream.writeByte(packetType);
            VarIntUtil.write(stream, 754); // Protocol version // TODO update
            VarIntUtil.write(stream, host.getBytes(StandardCharsets.UTF_8).length);
            stream.writeBytes(host);
            stream.writeShort(port);
            VarIntUtil.write(stream, 1); // State (1 = server status)
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Packet(buffer.toByteArray(), packetType);
    }

    public Packet newLoginPacket(final String password) {
        final int packetType = PacketType.LOGIN;
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] payload = newAllocatedBuffer(4 + 4 + passwordBytes.length)
                .putInt(getPacketIdAndIncrement())
                .putInt(packetType)
                .put(passwordBytes)
                .array();
        return new Packet(payload, packetType);
    }

    public Packet newCommandPacket(final String command) {
        final int packetType = PacketType.COMMAND;
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        byte[] payload = newAllocatedBuffer(4 + 4 + commandBytes.length)
                .putInt(getPacketIdAndIncrement())
                .putInt(packetType)
                .put(commandBytes)
                .array();
        return new Packet(payload, packetType);
    }
}

