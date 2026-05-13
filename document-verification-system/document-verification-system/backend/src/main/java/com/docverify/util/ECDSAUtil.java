package com.docverify.util;

import org.springframework.stereotype.Component;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Component
public class ECDSAUtil {

    public static class KeyPairResult {
        public String privateKey;
        public String publicKey;

        public KeyPairResult(String privateKey, String publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
    }

    public KeyPairResult generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        return new KeyPairResult(privateKeyBase64, publicKeyBase64);
    }

    public String signHash(String hash, String privateKeyBase64) throws Exception {
        byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyBase64);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        java.security.spec.PKCS8EncodedKeySpec keySpec = 
                new java.security.spec.PKCS8EncodedKeySpec(decodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature ecdsa = Signature.getInstance("SHA256withECDSA");
        ecdsa.initSign(privateKey);
        ecdsa.update(hash.getBytes());
        byte[] signatureBytes = ecdsa.sign();

        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public boolean verifySignature(String hash, String signatureBase64, String publicKeyBase64) throws Exception {
        byte[] decodedPublicKey = Base64.getDecoder().decode(publicKeyBase64);
        byte[] decodedSignature = Base64.getDecoder().decode(signatureBase64);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        java.security.spec.X509EncodedKeySpec keySpec = 
                new java.security.spec.X509EncodedKeySpec(decodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature ecdsa = Signature.getInstance("SHA256withECDSA");
        ecdsa.initVerify(publicKey);
        ecdsa.update(hash.getBytes());

        return ecdsa.verify(decodedSignature);
    }
}
