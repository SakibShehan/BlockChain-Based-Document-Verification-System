package com.docverify.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String docHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String signature;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String privateKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ipfsLink;

    @Column(unique = true, length = 255)
    private String txHash;

    @Column(columnDefinition = "TEXT")
    private String ahiberCiphertext;

    @Column(columnDefinition = "TEXT")
    private String ahibeDecryptionKey;

    @Column(columnDefinition = "TEXT")
    private String cbfJson;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 20)
    private String expiryDate;

    @Column(nullable = false)
    private Integer revocationStatus;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Document() {
    }

    public Document(String docHash, String signature, String publicKey, String privateKey, String ipfsLink, String txHash, String ahiberCiphertext, String ahibeDecryptionKey, String cbfJson, String userId, String expiryDate, Integer revocationStatus, LocalDateTime uploadedAt) {
        this.docHash = docHash;
        this.signature = signature;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.ipfsLink = ipfsLink;
        this.txHash = txHash;
        this.ahiberCiphertext = ahiberCiphertext;
        this.ahibeDecryptionKey = ahibeDecryptionKey;
        this.cbfJson = cbfJson;
        this.userId = userId;
        this.expiryDate = expiryDate;
        this.revocationStatus = revocationStatus;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocHash() {
        return docHash;
    }

    public void setDocHash(String docHash) {
        this.docHash = docHash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setIpfsLink(String ipfsLink) {
        this.ipfsLink = ipfsLink;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getAhiberCiphertext() {
        return ahiberCiphertext;
    }

    public void setAhiberCiphertext(String ahiberCiphertext) {
        this.ahiberCiphertext = ahiberCiphertext;
    }

    public String getAhibeDecryptionKey() {
        return ahibeDecryptionKey;
    }

    public void setAhibeDecryptionKey(String ahibeDecryptionKey) {
        this.ahibeDecryptionKey = ahibeDecryptionKey;
    }

    public String getCbfJson() {
        return cbfJson;
    }

    public void setCbfJson(String cbfJson) {
        this.cbfJson = cbfJson;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
