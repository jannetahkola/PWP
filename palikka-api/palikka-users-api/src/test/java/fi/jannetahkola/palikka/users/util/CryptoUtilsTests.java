package fi.jannetahkola.palikka.users.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoUtilsTests {
    @Test
    void test() {
        char[] password = new char[]{ 'a', 'b', 'c' };
        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hash(password, salt);
        assertThat(salt).isNotBlank().isBase64();
        assertThat(hash).isNotBlank().isBase64();
        assertThat(password).containsOnly('*');

        char[] passwordAgain = new char[]{ 'a', 'b', 'c' };
        assertThat(CryptoUtils.validatePassword("abd".toCharArray(), salt, hash)).isFalse();
        assertThat(CryptoUtils.validatePassword(passwordAgain, salt, hash)).isTrue();
        assertThat(passwordAgain).containsOnly('*');
    }
}
