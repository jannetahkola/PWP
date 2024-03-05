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

    public KeyPair loadKeyPairFromPropertiesOrError(JwtProperties properties) {
        KeyPair keyPair = null;
        try {
            keyPair = KeyUtil.loadKeyPair(
                    properties.getKeystorePath(),
                    properties.getKeystorePass(),
                    properties.getKeystoreType(),
                    properties.getToken().getKeyAlias(),
                    properties.getToken().getKeyPass());
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException |
                 UnrecoverableKeyException e) {
            log.error("Failed to load key from properties {}", properties);
        }
        return Objects.requireNonNull(keyPair);
    }
}
