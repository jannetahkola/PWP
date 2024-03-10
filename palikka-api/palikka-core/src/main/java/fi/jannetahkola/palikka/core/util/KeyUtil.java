package fi.jannetahkola.palikka.core.util;

import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;

@Slf4j
@UtilityClass
public class KeyUtil {
    public KeyPair loadKeyPair(String keystorePath,
                               String keystorePass,
                               String keystoreType,
                               String keyAlias,
                               String keyPass) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(keystorePath)) {
            KeyStore ks = KeyStore.getInstance(keystoreType);
            ks.load(is, keystorePass.toCharArray());

            Key key = ks.getKey(keyAlias, keyPass.toCharArray());
            PublicKey publicKey = ks.getCertificate(keyAlias).getPublicKey();

            return new KeyPair(publicKey, (PrivateKey) key);
        }
    }

    public KeyPair loadKeyPair(JwtProperties.KeyStoreProperties keystoreProperties,
                               JwtProperties.TokenProperties tokenProperties) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(keystoreProperties.getPath())) {
            KeyStore ks = KeyStore.getInstance(keystoreProperties.getType());
            ks.load(is, keystoreProperties.getPass().toCharArray());

            Key key = ks.getKey(tokenProperties.getSigning().getKeyAlias(), tokenProperties.getSigning().getKeyPass().toCharArray());
            PublicKey publicKey = ks.getCertificate(tokenProperties.getSigning().getKeyAlias()).getPublicKey();

            return new KeyPair(publicKey, (PrivateKey) key);
        }
    }

    public Key loadPublicKey(String keystorePath,
                             String keystorePass,
                             String keystoreType,
                             String keyAlias) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(keystorePath)) {
            KeyStore ks = KeyStore.getInstance(keystoreType);
            ks.load(is, keystorePass.toCharArray());
            return ks.getCertificate(keyAlias).getPublicKey();
        }
    }

    public KeyPair loadKeyPairFromPropertiesOrError(JwtProperties.KeyStoreProperties keystoreProperties,
                                                    JwtProperties.TokenProperties tokenProperties) {
        KeyPair keyPair = null;
        try {
            keyPair = loadKeyPair(keystoreProperties, tokenProperties);
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException |
                 UnrecoverableKeyException e) {
            log.error("Failed to private load key pair from keystore properties={}, token properties={}", keystoreProperties, tokenProperties);
        }
        return Objects.requireNonNull(keyPair);
    }

    public PublicKey loadPublicKeyFromPropertiesOrError(JwtProperties.KeyStoreProperties keystoreProperties,
                                                        JwtProperties.TokenProperties tokenProperties) {
        PublicKey key = null;
        try {
            key = (PublicKey) KeyUtil.loadPublicKey(
                    keystoreProperties.getPath(),
                    keystoreProperties.getPass(),
                    keystoreProperties.getType(),
                    tokenProperties.getVerification().getKeyAlias());
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            log.error("Failed to public load key from keystore properties={}, token properties={}", keystoreProperties, tokenProperties);
        }
        return Objects.requireNonNull(key);
    }
}
