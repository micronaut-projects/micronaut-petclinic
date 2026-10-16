package io.micronaut.samples.petclinic.system;

import io.micronaut.core.annotation.ReflectiveAccess;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * H2 implementation of the 128-bit ETag function contract used by the Vet mapping.
 * PostgreSQL and MySQL provide profile-local implementations with the same signature.
 */
public final class VetEtagFunctions {
    private VetEtagFunctions() {
    }

    /**
     * Produces a stable 128-bit tag for the Vet scalar ETag inputs.
     *
     * @param id vet identifier
     * @param firstName first name
     * @param lastName last name
     * @return uppercase 32-character hexadecimal hash
     */
    @ReflectiveAccess // H2 resolves the SQL alias by class and method name in native images.
    public static String sysRowEtag(Integer id, String firstName, String lastName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            addValue(digest, id == null ? null : id.toString());
            addValue(digest, firstName);
            addValue(digest, lastName);
            byte[] hash = digest.digest();
            StringBuilder result = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                result.append(Character.forDigit((hash[i] >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(hash[i] & 0x0f, 16));
            }
            return result.toString().toUpperCase(java.util.Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private static void addValue(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
