package com.gold.safefam.infrastructure.messaging.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
public class OutboxPayloadCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public OutboxPayloadCipher(
            @Value("${safefam.messaging.outbox.encryption-key}")
            String encodedKey
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Outbox encryption key must be 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // 암호화: 평문(payload)을 eventId와 함께 AES-GCM으로 암호화
    public String encrypt(String plaintext, UUID eventId) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(
                    eventId.toString().getBytes(StandardCharsets.UTF_8)
            );

            byte[] encrypted = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(iv)
                    + "."
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to encrypt outbox payload",
                    exception
            );
        }
    }

    // 복호화: "IV.암호문" 구조를 분리한 뒤 동일한 eventId 검증을 거쳐 평문 복원
    public String decrypt(String encryptedPayload, UUID eventId) {
        try {
            String[] parts = encryptedPayload.split("\\.", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid encrypted outbox payload"
                );
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(
                    eventId.toString().getBytes(StandardCharsets.UTF_8)
            );

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to decrypt outbox payload",
                    exception
            );
        }
    }
}