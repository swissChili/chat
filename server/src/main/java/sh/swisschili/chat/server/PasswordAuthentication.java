/*
Decentralized chat software
Copyright (C) 2021  swissChili

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

package sh.swisschili.chat.server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// copy and pasted from stack overflow ;)
public final class PasswordAuthentication {

    /**
     * Each token produced by this class uses this identifier as a prefix.
     */
    public static final String ID = "$31$";

    /**
     * The minimum recommended cost, used by default
     */
    public static final int DEFAULT_COST = 16;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";

    private static final int SIZE = 128;

    private static final Pattern layout = Pattern.compile("\\$31\\$(\\d\\d?)\\$(.{43})");

    private final SecureRandom random;

    private final int cost;

    public PasswordAuthentication() {
        this(DEFAULT_COST);
    }

    /**
     * Create a password manager with a specified cost
     *
     * @param cost the exponential computational cost of hashing a password, 0 to 30
     */
    public PasswordAuthentication(int cost) {
        iterations(cost); /* Validate cost */
        this.cost = cost;
        this.random = new SecureRandom();
    }

    private static int iterations(int cost) {
        if ((cost < 0) || (cost > 30))
            throw new IllegalArgumentException("cost: " + cost);
        return 1 << cost;
    }

    /**
     * Hash a password for storage.
     *
     * @return a secure authentication token to be stored for later authentication
     */
    public String hash(char[] password) {
        byte[] salt = new byte[SIZE / 8];
        random.nextBytes(salt);
        byte[] dk = pbkdf2(password, salt, 1 << cost);
        byte[] hash = new byte[salt.length + dk.length];
        System.arraycopy(salt, 0, hash, 0, salt.length);
        System.arraycopy(dk, 0, hash, salt.length, dk.length);
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return ID + cost + '$' + enc.encodeToString(hash);
    }

    /**
     * Authenticate with a password and a stored password token.
     *
     * @return true if the password and token match
     */
    public boolean authenticate(char[] password, String token) {
        Matcher m = layout.matcher(token);
        if (!m.matches())
            throw new IllegalArgumentException("Invalid token format");
        int iterations = iterations(Integer.parseInt(m.group(1)));
        byte[] hash = Base64.getUrlDecoder().decode(m.group(2));
        byte[] salt = Arrays.copyOfRange(hash, 0, SIZE / 8);
        byte[] check = pbkdf2(password, salt, iterations);
        int zero = 0;
        for (int idx = 0; idx < check.length; ++idx)
            zero |= hash[salt.length + idx] ^ check[idx];
        return zero == 0;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, SIZE);
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance(ALGORITHM);
            return f.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing algorithm: " + ALGORITHM, ex);
        } catch (InvalidKeySpecException ex) {
            throw new IllegalStateException("Invalid SecretKeyFactory", ex);
        }
    }

    /**
     * Hash a password in an immutable {@code String}.
     *
     * <p>Passwords should be stored in a {@code char[]} so that it can be filled
     * with zeros after use instead of lingering on the heap and elsewhere.
     *
     * @deprecated Use {@link #hash(char[])} instead
     */
    @Deprecated
    public String hash(String password) {
        return hash(password.toCharArray());
    }

    /**
     * Authenticate with a password in an immutable {@code String} and a stored
     * password token.
     *
     * @see #hash(String)
     * @deprecated Use {@link #authenticate(char[], String)} instead.
     */
    @Deprecated
    public boolean authenticate(String password, String token) {
        return authenticate(password.toCharArray(), token);
    }
}

