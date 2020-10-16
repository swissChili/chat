package sh.swisschili.chat.client;

import sh.swisschili.chat.util.SignedAuth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.prefs.Preferences;

public class UserCredentials extends SignedAuth {
    private static final Preferences preferences = Preferences.userNodeForPackage(UserCredentials.class);

    public static class CredentialsNotFound extends Exception {
    }

    public static KeyPair getUserKeys() throws CredentialsNotFound {
        byte[] privBytes = preferences.getByteArray("user.credentials.privateKey", new byte[]{});
        byte[] pubBytes = preferences.getByteArray("user.credentials.publicKey", new byte[]{});

        try {
            PrivateKey privKey = privKeyFromBytes(privBytes);
            PublicKey pubKey = pubKeyFromBytes(pubBytes);

            return new KeyPair(pubKey, privKey);
        } catch (InvalidKeySpecException e) {
            throw new CredentialsNotFound();
        }
    }

    public static KeyPair createUserKeys() {
        KeyPair keys = generateKeyPair();
        preferences.putByteArray("user.credentials.privateKey", keys.getPrivate().getEncoded());
        preferences.putByteArray("user.credentials.publicKey", keys.getPublic().getEncoded());

        return keys;
    }
}
