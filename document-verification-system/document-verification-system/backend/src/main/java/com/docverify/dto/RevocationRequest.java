package com.docverify.dto;

public class RevocationRequest {
    private byte[] fileBytes;
    private String userId;
    private Integer newRevocationStatus;

    public RevocationRequest() {}

    public byte[] getFileBytes() { return fileBytes; }
    public void setFileBytes(byte[] fileBytes) { this.fileBytes = fileBytes; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getNewRevocationStatus() { return newRevocationStatus; }
    public void setNewRevocationStatus(Integer newRevocationStatus) { this.newRevocationStatus = newRevocationStatus; }
}