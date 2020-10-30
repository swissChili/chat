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
