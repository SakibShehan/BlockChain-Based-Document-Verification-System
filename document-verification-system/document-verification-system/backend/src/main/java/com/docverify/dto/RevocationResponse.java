package com.docverify.dto;

public class RevocationResponse {
    private boolean success;
    private String message;
    private String docHash;
    private Integer oldStatus;
    private Integer newStatus;
    private String cbfJson;

    public RevocationResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDocHash() { return docHash; }
    public void setDocHash(String docHash) { this.docHash = docHash; }

    public Integer getOldStatus() { return oldStatus; }
    public void setOldStatus(Integer oldStatus) { this.oldStatus = oldStatus; }

    public Integer getNewStatus() { return newStatus; }
    public void setNewStatus(Integer newStatus) { this.newStatus = newStatus; }

    public String getCbfJson() { return cbfJson; }
    public void setCbfJson(String cbfJson) { this.cbfJson = cbfJson; }
}