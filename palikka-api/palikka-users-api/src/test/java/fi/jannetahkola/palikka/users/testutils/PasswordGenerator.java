package fi.jannetahkola.palikka.users.testutils;

import fi.jannetahkola.palikka.users.util.CryptoUtils;

class PasswordGenerator {
    public static void main(String[] args) {
        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hash("password".toCharArray(), salt);
        System.out.println("salt=" + salt + ", pass=" + hash);
    }
}
