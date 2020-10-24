import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.SignedAuth;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class TestSignedAuth {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSignedAuth.class);

    @Test
    public void testSignature() throws SignatureException, InvalidKeyException {
        LOGGER.info("Testing signing string");

        KeyPair keyPair = SignedAuth.generateKeyPair();
        String testString = "This is a test string to sign";

        assert keyPair != null;
        byte[] signed = SignedAuth.sign(keyPair, testString.getBytes());

        assert SignedAuth.verify(keyPair.getPublic(), signed, testString.getBytes());
    }

    @Test
    public void testDeserializeKeys() throws InvalidKeySpecException {
        LOGGER.info("Testing serializing/deserializing keys to byte[]");

        KeyPair keyPair = SignedAuth.generateKeyPair();
        assert keyPair != null;

        byte[] pubBytes = SignedAuth.pubKeyToBytes(keyPair.getPublic());
        byte[] privateBytes = SignedAuth.privateKeyToBytes(keyPair.getPrivate());

        assert pubBytes.length > 0;
        assert privateBytes.length > 0;

        PublicKey publicKey = SignedAuth.pubKeyFromBytes(pubBytes);
        PrivateKey privateKey = SignedAuth.privateKeyFromBytes(privateBytes);

        assert publicKey.equals(keyPair.getPublic());
        assert privateKey.equals(keyPair.getPrivate());
    }
}
