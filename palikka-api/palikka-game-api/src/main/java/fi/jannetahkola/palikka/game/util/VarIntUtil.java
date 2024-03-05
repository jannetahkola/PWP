package fi.jannetahkola.palikka.game.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Utility methods for performing conversions between integer and VarInt. The VarInt type matches the definition
 * in the <a href="https://wiki.vg/Protocol#VarInt_and_VarLong">Minecraft protocol</a>.
 */
public class VarIntUtil {
    private VarIntUtil() {
        // util
    }

    /**
     * Writes an integer into the provided {@link DataOutputStream} as a VarInt.
     * Encoding logic from <a href="https://wiki.vg/Protocol#VarInt_and_VarLong">Minecraft protocol</a>.
     * @param out {@link DataOutputStream} to write to.
     * @param value Integer to encode and write.
     * @throws IOException If writing to the stream fails.
     */
    public static void write(DataOutputStream out, int value) throws IOException {
        do {
            byte temp = (byte)(value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= (byte)0b10000000;
            }
            out.writeByte(temp);
        } while (value != 0);
    }

    /**
     * Reads a VarInt into an integer from the provided {@link DataInputStream}.
     * Encoding logic from <a href="https://wiki.vg/Protocol#VarInt_and_VarLong">Minecraft protocol</a>.
     * @param in {@link DataInputStream} to read from.
     * @return Decoded integer value.
     * @throws IOException If reading the stream fails, or if an invalid VarInt was read from the stream (> 5 bytes).
     */
    public static int read(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);
        return result;
    }
}

