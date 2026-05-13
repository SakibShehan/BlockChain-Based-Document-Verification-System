package com.docverify.service;

import com.docverify.dto.*;
import com.docverify.entity.Document;
import com.docverify.repository.DocumentRepository;
import com.docverify.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentRepository documentRepository;
    private final HashUtil hashUtil;
    private final ECDSAUtil ecdsaUtil;
    private final CountingBloomFilterUtil cbfUtil;
    private final AHIBEUtil ahibeUtil;
    private final IPFSUtil ipfsUtil;
    private final BlockchainService blockchainService;

    public DocumentService(
            DocumentRepository documentRepository,
            HashUtil hashUtil,
            ECDSAUtil ecdsaUtil,
            CountingBloomFilterUtil cbfUtil,
            AHIBEUtil ahibeUtil,
            IPFSUtil ipfsUtil,
            BlockchainService blockchainService) {
        this.documentRepository = documentRepository;
        this.hashUtil = hashUtil;
        this.ecdsaUtil = ecdsaUtil;
        this.cbfUtil = cbfUtil;
        this.ahibeUtil = ahibeUtil;
        this.ipfsUtil = ipfsUtil;
        this.blockchainService = blockchainService;
    }

    public UploadResponse uploadDocument(UploadRequest request) throws Exception {
        log.info("Starting document upload for user: {}", request.getUserId());

        String documentHash = hashUtil.generateSHA256(request.getFileBytes());
        log.info("Generated hash: {}", documentHash);

        ECDSAUtil.KeyPairResult keyPairResult = ecdsaUtil.generateKeyPair();
        String digitalSignature = ecdsaUtil.signHash(documentHash, keyPairResult.privateKey);
        log.info("Generated digital signature");

        cbfUtil.addToBloomFilter(documentHash, request.getRevocationStatus());
        String cbfJson = cbfUtil.exportToJsonString();
        log.info("CBF updated");

        String ipfsCID;
        try {
            ipfsCID = ipfsUtil.uploadJsonToIPFS(cbfJson);
            log.info("Uploaded to IPFS, CID: {}", ipfsCID);
        } catch (Exception e) {
            log.warn("IPFS upload failed: {}", e.getMessage());
            ipfsCID = "QmPlaceholder" + System.nanoTime();
        }

        AHIBEUtil.AHIBEResult ahibeResult = ahibeUtil.encryptCID(
                ipfsCID,
                request.getUserId(),
                request.getExpiryDate()
        );
        log.info("AHIBE encryption completed");

        String txHash = blockchainService.uploadDocument(
                documentHash,
                digitalSignature,
                keyPairResult.publicKey,
                ahibeResult.ciphertext
        );
        log.info("Document stored on blockchain, TX: {}", txHash);

        Document doc = new Document();
        doc.setDocHash(documentHash);
        doc.setSignature(digitalSignature);
        doc.setPublicKey(keyPairResult.publicKey);
        doc.setPrivateKey(keyPairResult.privateKey);
        doc.setIpfsLink(ipfsCID);
        doc.setTxHash(txHash);
        doc.setAhiberCiphertext(ahibeResult.ciphertext);
        doc.setAhibeDecryptionKey(ahibeResult.decryptionKey);
        doc.setCbfJson(cbfJson);
        doc.setUserId(request.getUserId());
        doc.setExpiryDate(request.getExpiryDate());
        doc.setRevocationStatus(request.getRevocationStatus());

        documentRepository.save(doc);
        log.info("Document saved to database");

        UploadResponse response = new UploadResponse();
        response.setSuccess(true);
        response.setMessage("Document uploaded successfully");
        response.setDocumentHash(documentHash);
        response.setTransactionHash(txHash);

        Map<String, String> generatedData = new HashMap<>();
        generatedData.put("documentHash", documentHash);
        generatedData.put("digitalSignature", digitalSignature);
        generatedData.put("publicKey", keyPairResult.publicKey);
        generatedData.put("privateKey", keyPairResult.privateKey);
        generatedData.put("revocationStatus", String.valueOf(request.getRevocationStatus()));
        generatedData.put("cbfJson", cbfJson);
        generatedData.put("ipfsCID", ipfsCID);
        generatedData.put("ahibeCiphertext", ahibeResult.ciphertext);
        generatedData.put("ahibeDecryptionKey", ahibeResult.decryptionKey);
        generatedData.put("transactionHash", txHash);

        response.setGeneratedData(generatedData);

        return response;
    }

    public VerificationResponse verifyDocument(VerificationRequest request) throws Exception {
        log.info("Starting verification for TX: {}", request.getTransactionHash());

        VerificationResponse response = new VerificationResponse();

        String uploadedHash = hashUtil.generateSHA256(request.getFileBytes());
        log.info("Generated hash from file: {}", uploadedHash);

        Optional<Document> docOpt = documentRepository.findByDocHash(uploadedHash);

        if (docOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Document was not uploaded previously");
            return response;
        }

        Document doc = docOpt.get();

        if (!doc.getTxHash().equalsIgnoreCase(request.getTransactionHash())) {
            response.setSuccess(false);
            response.setMessage("Transaction hash mismatch");
            return response;
        }

        boolean blockchainValid = blockchainService.verifyTransaction(request.getTransactionHash());
        if (!blockchainValid) {
            response.setSuccess(false);
            response.setMessage("Transaction not valid");
            return response;
        }

        String decryptedCID = ahibeUtil.decryptCID(
                doc.getAhiberCiphertext(),
                request.getDecryptionKey(),
                request.getUserId()
        );

        if ("EXPIRED".equals(decryptedCID)) {
            response.setSuccess(false);
            response.setMessage("Document has expired");
            response.setDocHash(uploadedHash);
            response.setDocSignature(doc.getSignature());
            response.setPublicKey(doc.getPublicKey());
            response.setAhibeEncryptedLink(doc.getAhiberCiphertext());
            return response;
        }

        if (decryptedCID == null) {
            response.setSuccess(false);
            response.setMessage("AHIBE decryption failed");
            return response;
        }

        String cbfData;
        try {
            cbfData = ipfsUtil.fetchFromIPFS(decryptedCID);
        } catch (Exception e) {
            cbfData = doc.getCbfJson();
        }

        boolean isRevoked = cbfUtil.isRevoked(uploadedHash);

        boolean signatureValid = ecdsaUtil.verifySignature(
                uploadedHash,
                doc.getSignature(),
                doc.getPublicKey()
        );

        response.setSuccess(true);
        response.setMessage("Document verified successfully");
        response.setDocHash(uploadedHash);
        response.setDocSignature(doc.getSignature());
        response.setPublicKey(doc.getPublicKey());
        response.setAhibeEncryptedLink(doc.getAhiberCiphertext());
        response.setHashMatch(uploadedHash.equals(doc.getDocHash()));
        response.setSignatureValid(signatureValid);
        response.setRevocationStatus(isRevoked ? "REVOKED" : "NOT REVOKED");
        response.setIpfsCID(decryptedCID);
        response.setExpiryDate(doc.getExpiryDate());

        return response;
    }

    public String getDocumentStatus(String docHash) {
        Optional<Document> doc = documentRepository.findByDocHash(docHash);
        return doc.isPresent() ? "FOUND" : "NOT FOUND";
    }
    // ── ADD THIS METHOD TO DocumentService.java ──────────────────────────

    public RevocationResponse updateRevocationStatus(RevocationRequest request) throws Exception {
        log.info("Revocation update request from user: {}", request.getUserId());

        RevocationResponse response = new RevocationResponse();

        // Step 1: Generate hash from uploaded document
        String docHash = hashUtil.generateSHA256(request.getFileBytes());

        // Step 2: Check if document exists in database
        Optional<Document> docOpt = documentRepository.findByDocHash(docHash);
        if (docOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Document not found in database. Please upload it first.");
            return response;
        }

        Document doc = docOpt.get();

        if (!doc.getUserId().equalsIgnoreCase(request.getUserId().trim())) {
            response.setSuccess(false);
            response.setMessage("User ID does not match the document owner. Access denied.");
            response.setDocHash(docHash);
            return response;
        }
        int oldStatus = doc.getRevocationStatus();
        int newStatus = request.getNewRevocationStatus();

        // Step 3: Check if new status is same as current status
        if (oldStatus == newStatus) {
            response.setSuccess(false);
            response.setMessage("Revocation status is already " +
                    (oldStatus == 1 ? "REVOKED (1)" : "NOT REVOKED (0)") +
                    ". No update needed.");
            response.setDocHash(docHash);
            response.setOldStatus(oldStatus);
            response.setNewStatus(newStatus);
            return response;
        }

        // Step 4: Update CBF — counting BF supports deletion (decrement for 1→0)
        cbfUtil.updateRevocationStatus(docHash, oldStatus, newStatus);
        log.info("CBF updated: {} → {}", oldStatus, newStatus);

        // Step 5: Export updated CBF and upload to IPFS
        String updatedCbfJson = cbfUtil.exportToJsonString();
        String newIpfsCID;
        try {
            newIpfsCID = ipfsUtil.uploadJsonToIPFS(updatedCbfJson);
            log.info("Updated CBF uploaded to IPFS, CID: {}", newIpfsCID);
        } catch (Exception e) {
            log.warn("IPFS upload failed during revocation update: {}", e.getMessage());
            newIpfsCID = doc.getIpfsLink(); // keep old CID if IPFS fails
        }

        // Step 6: Update database
        doc.setRevocationStatus(newStatus);
        doc.setCbfJson(updatedCbfJson);
        doc.setIpfsLink(newIpfsCID);
        documentRepository.save(doc);
        log.info("Database updated with new revocation status: {}", newStatus);

        // Step 7: Build response
        response.setSuccess(true);
        response.setMessage("Revocation status updated successfully from " +
                (oldStatus == 1 ? "REVOKED" : "NOT REVOKED") +
                " to " +
                (newStatus == 1 ? "REVOKED" : "NOT REVOKED") + ".");
        response.setDocHash(docHash);
        response.setOldStatus(oldStatus);
        response.setNewStatus(newStatus);
        response.setCbfJson(updatedCbfJson);

        return response;
    }
}
