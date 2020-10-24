package sh.swisschili.chat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class SignedAuth {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignedAuth.class);
    private static KeyFactory keyFactory;

    public SignedAuth() {
        try {
            keyFactory = KeyFactory.getInstance("DSA", "SUN");
        } catch (Exception ignored) {
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

            keyGen.initialize(1024, random);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            LOGGER.error("Could not generate key pair (this should never happen)");
            return null;
        }
    }

    /**
     * @param keyPair The keys to sign with
     * @param data The data to sign
     * @return The signature
     * @throws InvalidKeyException If the key is invalid
     * @throws SignatureException If the signature failed for some other reason
     * @apiNote In the case of verifying a multi-part protobuf message, the member bytes should be passed in numerical
     * order by protobuf implementation
     */
    public static byte[] sign(KeyPair keyPair, byte[]... data) throws InvalidKeyException, SignatureException {
        try {
            Signature signature = Signature.getInstance("SHA1withDSA", "SUN");
            signature.initSign(keyPair.getPrivate());
            for (byte[] dataChunk : data) {
                signature.update(dataChunk);
            }
            return signature.sign();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            LOGGER.error("Could not generate signature (this should never happen)");
            return null;
        }
    }

    /**
     * @param publicKey The public key to verify with
     * @param signature The signature to verify
     * @param data      The data to verify
     * @return Is the signature valid?
     * @apiNote In the case of verifying a multi-part protobuf message, the member bytes should be passed in numerical
     * order by protobuf implementation
     */
    public static boolean verify(PublicKey publicKey, byte[] signature, byte[]... data) {
        try {
            Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
            sig.initVerify(publicKey);

            for (byte[] dataChunk : data) {
                sig.update(dataChunk);
            }
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] pubKeyToBytes(PublicKey publicKey) {
        return publicKey.getEncoded();
    }

    public static PublicKey pubKeyFromBytes(byte[] bytes) throws InvalidKeySpecException {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(bytes);
        return keyFactory.generatePublic(pubKeySpec);
    }

    public static byte[] privKeyToBytes(PrivateKey key) {
        return key.getEncoded();
    }

    public static PrivateKey privKeyFromBytes(byte[] bytes) throws InvalidKeySpecException {
        X509EncodedKeySpec privKeySpec = new X509EncodedKeySpec(bytes);
        return keyFactory.generatePrivate(privKeySpec);
    }
}
