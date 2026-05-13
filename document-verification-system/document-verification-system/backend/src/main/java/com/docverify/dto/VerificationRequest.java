package com.docverify.dto;

public class VerificationRequest {
    private byte[] fileBytes;
    private String transactionHash;
    private String userId;
    private String decryptionKey;   // ← NEW: verifier provides this

    public VerificationRequest() {}

    public byte[] getFileBytes() { return fileBytes; }
    public void setFileBytes(byte[] fileBytes) { this.fileBytes = fileBytes; }

    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDecryptionKey() { return decryptionKey; }
    public void setDecryptionKey(String decryptionKey) { this.decryptionKey = decryptionKey; }
}