/*
Decentralized chat software
Copyright (C) 2020  swissChili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package sh.swisschili.chat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.ChatProtos;
import sh.swisschili.chat.util.SignedAuth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.prefs.Preferences;

public class UserCredentials extends SignedAuth {
    private static final Preferences preferences = Preferences.userNodeForPackage(UserCredentials.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCredentials.class);

    private static final String[] PREF_KEYS = new String[] {
            "user.credentials.privateKey",
            "user.credentials.publicKey",
            "user.name",
            "user.host",
            "user.password"
    };

    public static class CredentialsNotFound extends Exception {
        public CredentialsNotFound(String reason) {
            super(reason);
        }
    }

    public static boolean userLoggedIn() {
        for (String key : PREF_KEYS) {
            if (preferences.get(key, "").isEmpty()) {
                LOGGER.warn("User preference undefined: " + key);
                return false;
            }
        }
        return true;
    }

    public static void clearUser() {
        for (String key : PREF_KEYS) {
            preferences.remove(key);
        }
    }

    public static void setUser(ChatProtos.User user) {
        preferences.put("user.name", user.getName());
        preferences.put("user.host", user.getHost());
    }

    public static ChatProtos.User getUser() throws CredentialsNotFound {
        if (!userLoggedIn())
            throw new CredentialsNotFound("User not logged in");

        return ChatProtos.User.newBuilder()
                .setName(preferences.get("user.name", "Unnamed"))
                .setHost(preferences.get("user.host", "localhost"))
                .build();
    }

    public static void setPassword(char[] password) {
        preferences.put("user.password", String.valueOf(password));
    }

    public static char[] getPassword() throws CredentialsNotFound {
        if (!userLoggedIn())
            throw new CredentialsNotFound("User not logged in");

        return preferences.get("user.password", "").toCharArray();
    }

    public static KeyPair getUserKeys() throws CredentialsNotFound {
        byte[] privBytes = preferences.getByteArray("user.credentials.privateKey", new byte[]{});
        byte[] pubBytes = preferences.getByteArray("user.credentials.publicKey", new byte[]{});

        LOGGER.info(String.format("Got keys: %s, %s", new String(privBytes, 0, 10),
                new String(pubBytes, 0, 10)));

        try {
            PrivateKey privKey = privateKeyFromBytes(privBytes);
            PublicKey pubKey = pubKeyFromBytes(pubBytes);

            return new KeyPair(pubKey, privKey);
        } catch (InvalidKeySpecException e) {
            throw new CredentialsNotFound(e.getMessage());
        }
    }

    public static KeyPair createUserKeys() {
        KeyPair keys = generateKeyPair();
        preferences.putByteArray("user.credentials.privateKey", privateKeyToBytes(keys.getPrivate()));
        preferences.putByteArray("user.credentials.publicKey", pubKeyToBytes(keys.getPublic()));

        return keys;
    }
}
