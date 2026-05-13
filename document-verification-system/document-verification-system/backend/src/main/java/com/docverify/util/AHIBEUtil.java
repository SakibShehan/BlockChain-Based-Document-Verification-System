package com.docverify.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * AHIBE Utility — AES-256-GCM Implementation
 *
 * Replaces XOR stream cipher with AES-256-GCM (Authenticated Encryption).
 *
 * AES-GCM provides:
 * Confidentiality  — AES-256 encryption
 * Integrity        — GCM authentication tag detects ANY bit modification
 * Authenticity     — ciphertext tied to the key used to produce it
 * IND-CCA2 security — chosen ciphertext attacks defeated by auth tag
 * Nonce uniqueness  — random 96-bit IV per encryption, never reused
 */
@Component
public class AHIBEUtil {

    private final HashUtil hashUtil;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int    IV_SIZE   = 12;   // 96-bit IV
    private static final int    TAG_SIZE  = 128;  // 128-bit auth tag
    private static final String SEP       = "|";

    public AHIBEUtil(HashUtil hashUtil) {
        this.hashUtil = hashUtil;
    }

    public static class AHIBEResult {
        public String ciphertext;
        public String decryptionKey;

        public AHIBEResult(String ciphertext, String decryptionKey) {
            this.ciphertext    = ciphertext;
            this.decryptionKey = decryptionKey;
        }
    }

    // ════════════════════════════════════════════════════════
    // ENCRYPT — AES-256-GCM
    // ════════════════════════════════════════════════════════
    public AHIBEResult encryptCID(String ipfsCID, String userId, String expiryDate) throws Exception {

        // 1. Fresh 256-bit master secret per document
        SecureRandom random = new SecureRandom();
        byte[] masterSecret = new byte[32];
        random.nextBytes(masterSecret);

        // 2. Derive 256-bit AES key = SHA-256(userId || masterSecret)
        byte[] aesKeyBytes = deriveUserKey(masterSecret, userId);

        // 3. Parse expiry date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long expiryTs = sdf.parse(expiryDate).getTime();

        // 4. userId fingerprint = first 16 chars of SHA-256(userId)
        String userIdFingerprint = hashUtil.bytesToHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(userId.getBytes("UTF-8"))
        ).substring(0, 16);

        // 5. Plaintext = CID|expiryTs|userIdFingerprint
        String plaintext = ipfsCID + SEP + expiryTs + SEP + userIdFingerprint;

        // 6. Random 96-bit IV — never reused
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);

        // 7. AES-256-GCM encrypt
        SecretKey secretKey = new SecretKeySpec(aesKeyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_SIZE, iv));
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // 8. Format: Base64(iv):Base64(ciphertext+tag)
        String fullCiphertext = Base64.getEncoder().encodeToString(iv)
                + ":" + Base64.getEncoder().encodeToString(encryptedBytes);

        // 9. Decryption key = hex(aesKeyBytes) — given to verifier
        String decryptionKey = hashUtil.bytesToHex(aesKeyBytes);

        return new AHIBEResult(fullCiphertext, decryptionKey);
    }

    // ════════════════════════════════════════════════════════
    // DECRYPT — AES-256-GCM
    // ════════════════════════════════════════════════════════
    public String decryptCID(String ciphertextFull, String decryptionKeyHex, String userId) {
        try {
            // 1. Split IV and ciphertext
            String[] parts = ciphertextFull.split(":");
            if (parts.length != 2) return null;

            byte[] iv             = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

            // 2. Reconstruct AES key
            byte[] aesKeyBytes = hashUtil.hexToBytes(decryptionKeyHex);
            if (aesKeyBytes.length != 32) return null;

            // 3. AES-256-GCM decrypt
            // GCM verifies auth tag automatically here
            // Wrong key OR tampered ciphertext → AEADBadTagException
            SecretKey secretKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_SIZE, iv));
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // 4. Parse plaintext = CID|expiryTs|userIdFingerprint
            String plaintext = new String(decryptedBytes, "UTF-8");
            String[] fields  = plaintext.split("\\" + SEP);
            if (fields.length != 3) return null;

            String cid               = fields[0];
            String expiryPart        = fields[1];
            String storedFingerprint = fields[2];

            // 5. Validate userId fingerprint
            String expectedFingerprint = hashUtil.bytesToHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(userId.getBytes("UTF-8"))
            ).substring(0, 16);

            if (!storedFingerprint.equals(expectedFingerprint)) return null;

            // 6. Check expiry
            try {
                long expiryTs = Long.parseLong(expiryPart.trim());
                if (new Date().getTime() > expiryTs) return "EXPIRED";
            } catch (NumberFormatException e) {
                return null;
            }

            return cid;

        } catch (javax.crypto.AEADBadTagException e) {
            // Auth tag failed — wrong key OR bit-flip attack blocked
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════
    // Key Derivation — SHA-256(userId || masterSecret)
    // ════════════════════════════════════════════════════════
    private byte[] deriveUserKey(byte[] masterSecret, String userId) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(userId.getBytes("UTF-8"));
        md.update(masterSecret);
        return md.digest(); // 256-bit — perfect for AES-256
    }
}