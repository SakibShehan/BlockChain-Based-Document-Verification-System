package com.docverify.dto;

public class VerificationResponse {
    private Boolean success;
    private String message;
    private String docHash;
    private String docSignature;
    private String publicKey;
    private String ahibeEncryptedLink;
    private Boolean hashMatch;
    private Boolean signatureValid;
    private String revocationStatus;
    private String ipfsCID;
    private String expiryDate;

    public VerificationResponse() {
    }

    public VerificationResponse(Boolean success, String message, String docHash, String docSignature, String publicKey, String ahibeEncryptedLink, Boolean hashMatch, Boolean signatureValid, String revocationStatus, String ipfsCID, String expiryDate) {
        this.success = success;
        this.message = message;
        this.docHash = docHash;
        this.docSignature = docSignature;
        this.publicKey = publicKey;
        this.ahibeEncryptedLink = ahibeEncryptedLink;
        this.hashMatch = hashMatch;
        this.signatureValid = signatureValid;
        this.revocationStatus = revocationStatus;
        this.ipfsCID = ipfsCID;
        this.expiryDate = expiryDate;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocHash() {
        return docHash;
    }

    public void setDocHash(String docHash) {
        this.docHash = docHash;
    }

    public String getDocSignature() {
        return docSignature;
    }

    public void setDocSignature(String docSignature) {
        this.docSignature = docSignature;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAhibeEncryptedLink() {
        return ahibeEncryptedLink;
    }

    public void setAhibeEncryptedLink(String ahibeEncryptedLink) {
        this.ahibeEncryptedLink = ahibeEncryptedLink;
    }

    public Boolean getHashMatch() {
        return hashMatch;
    }

    public void setHashMatch(Boolean hashMatch) {
        this.hashMatch = hashMatch;
    }

    public Boolean getSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(Boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public String getRevocationStatus() {
        return revocationStatus;
    }

    public void setRevocationStatus(String revocationStatus) {
        this.revocationStatus = revocationStatus;
    }

    public String getIpfsCID() {
        return ipfsCID;
    }

    public void setIpfsCID(String ipfsCID) {
        this.ipfsCID = ipfsCID;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}

