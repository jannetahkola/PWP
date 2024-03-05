package fi.jannetahkola.palikka.game.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class VarIntUtilTests {
    @SneakyThrows
    @Test
    void testDecode() {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        DataInputStream dataIn = new DataInputStream(in);

        byte[] responseLengthVarIntBytes = {-116, 1}; // result 140
        out.write(responseLengthVarIntBytes);

        byte[] packetIdVarIntBytes = {0}; // result 0
        out.write(packetIdVarIntBytes);

        byte[] payloadLengthVarIntBytes = {-119, 1}; // result 137
        out.write(payloadLengthVarIntBytes);

        int responseLength = VarIntUtil.read(dataIn);
        assertThat(responseLength).isEqualTo(140);

        int packetId = VarIntUtil.read(dataIn);
        assertThat(packetId).isZero();

        int payloadLength = VarIntUtil.read(dataIn);
        assertThat(payloadLength).isEqualTo(137);
    }

    @SneakyThrows
    @Test
    void testEncodeThenDecode() {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        DataOutputStream dataOut = new DataOutputStream(out);
        DataInputStream dataIn = new DataInputStream(in);

        VarIntUtil.write(dataOut, 140);
        int responseLength = VarIntUtil.read(dataIn);
        assertThat(responseLength).isEqualTo(140);

        VarIntUtil.write(dataOut, 0);
        int packetId = VarIntUtil.read(dataIn);
        assertThat(packetId).isZero();

        VarIntUtil.write(dataOut, 137);
        int payloadLength = VarIntUtil.read(dataIn);
        assertThat(payloadLength).isEqualTo(137);
    }
}
