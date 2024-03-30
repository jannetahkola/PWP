package fi.jannetahkola.palikka.users.util;

import lombok.experimental.UtilityClass;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;

@UtilityClass
public class CryptoUtils {
    private static final Random RANDOM = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    public String hash(char[] passwordUtf8, String saltBase64) {
        PBEKeySpec spec = new PBEKeySpec(
                passwordUtf8, Base64.decodeBase64(saltBase64), 1000, 256);
        Arrays.fill(passwordUtf8, '*'); // Cloned by key spec so we can mask it here
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return Base64.encodeBase64String(skf.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Hashing error", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean validatePassword(char[] password, String salt, String expectedHash) {
        byte[] hashBytes = Base64.decodeBase64(hash(password, salt));
        Arrays.fill(password, '*');
        byte[] expectedHashBytes = Base64.decodeBase64(expectedHash);
        if (hashBytes.length != expectedHashBytes.length) {
            return false;
        }
        for (int i = 0; i < hashBytes.length; i++) {
            if (hashBytes[i] != expectedHashBytes[i]) {
                return false;
            }
        }
        return true;
    }
}
