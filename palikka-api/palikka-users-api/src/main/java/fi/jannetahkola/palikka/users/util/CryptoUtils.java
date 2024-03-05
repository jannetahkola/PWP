package fi.jannetahkola.palikka.users.util;

import lombok.experimental.UtilityClass;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

@UtilityClass
public class CryptoUtils {
    private static final Random RANDOM = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    public String hash(String passwordUtf8, String saltBase64) {
        char[] passwordChars = passwordUtf8.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChars, Base64.decodeBase64(saltBase64), 1000, 256);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return Base64.encodeBase64String(skf.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Hashing error", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean validatePassword(String password, String salt, String expectedHash) {
        byte[] hashBytes = Base64.decodeBase64(hash(password, salt));
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
