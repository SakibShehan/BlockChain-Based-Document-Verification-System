package com.docverify.dto;

public class UploadRequest {
    private byte[] fileBytes;
    private String userId;
    private String expiryDate;
    private Integer revocationStatus;

    public UploadRequest() {
    }

    public UploadRequest(byte[] fileBytes, String userId, String expiryDate, Integer revocationStatus) {
        this.fileBytes = fileBytes;
        this.userId = userId;
        this.expiryDate = expiryDate;
        this.revocationStatus = revocationStatus;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getRevocationStatus() {
        return revocationStatus;
    }

    public void setRevocationStatus(Integer revocationStatus) {
        this.revocationStatus = revocationStatus;
    }
}

