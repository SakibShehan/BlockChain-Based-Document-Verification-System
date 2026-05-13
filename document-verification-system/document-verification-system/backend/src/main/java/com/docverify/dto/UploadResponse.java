package com.docverify.dto;

import java.util.Map;

public class UploadResponse {
    private Boolean success;
    private String message;
    private String documentHash;
    private String transactionHash;
    private Map<String, String> generatedData;

    public UploadResponse() {
    }

    public UploadResponse(Boolean success, String message, String documentHash, String transactionHash, Map<String, String> generatedData) {
        this.success = success;
        this.message = message;
        this.documentHash = documentHash;
        this.transactionHash = transactionHash;
        this.generatedData = generatedData;
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

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public Map<String, String> getGeneratedData() {
        return generatedData;
    }

    public void setGeneratedData(Map<String, String> generatedData) {
        this.generatedData = generatedData;
    }
}

