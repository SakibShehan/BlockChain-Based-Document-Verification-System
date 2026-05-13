package com.docverify.controller;

import com.docverify.dto.*;
import com.docverify.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("expiryDate") String expiryDate,
            @RequestParam("revocationStatus") Integer revocationStatus) {

        try {
            log.info("Upload request from user: {}", userId);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File cannot be empty"));
            }

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("User ID required"));
            }

            if (revocationStatus == null || (revocationStatus != 0 && revocationStatus != 1)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Revocation status must be 0 or 1"));
            }

            UploadRequest request = new UploadRequest();
            request.setFileBytes(file.getBytes());
            request.setUserId(userId);
            request.setExpiryDate(expiryDate);
            request.setRevocationStatus(revocationStatus);

            var response = documentService.uploadDocument(request);

            log.info("Upload successful: {}", response.getDocumentHash());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Upload error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("Upload failed: " + e.getMessage())
            );
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("transactionHash") String txHash,
            @RequestParam("userId") String userId,
            @RequestParam("decryptionKey") String decryptionKey) {

        try {
            log.info("Verification request for TX: {}", txHash);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File cannot be empty"));
            }
            if (txHash == null || txHash.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Transaction hash required"));
            }
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("User ID required"));
            }
            if (decryptionKey == null || decryptionKey.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Decryption key required"));
            }

            VerificationRequest request = new VerificationRequest();
            request.setFileBytes(file.getBytes());
            request.setTransactionHash(txHash);
            request.setUserId(userId);
            request.setDecryptionKey(decryptionKey);

            var response = documentService.verifyDocument(request);

            log.info("Verification result: {}", response.getSuccess());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Verification error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("Verification failed: " + e.getMessage())
            );
        }

    }

    @GetMapping("/status/{docHash}")
    public ResponseEntity<?> getDocumentStatus(@PathVariable String docHash) {
        try {
            String status = documentService.getDocumentStatus(docHash);
            Map<String, String> response = new HashMap<>();
            response.put("docHash", docHash);
            response.put("status", status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse(e.getMessage())
            );
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Document Verification System");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("")
    public ResponseEntity<?> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Document Verification System");
        response.put("version", "1.0.0");
        response.put("message", "Welcome to Document Verification System");
        response.put("endpoints", new HashMap<String, String>() {{
            put("health", "GET /documents/health");
            put("upload", "POST /documents/upload (file, userId, expiryDate, revocationStatus)");
            put("verify", "POST /documents/verify (file, transactionHash, userId)");
            put("status", "GET /documents/status/{docHash}");
        }});
        return ResponseEntity.ok(response);
    }

    static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> updateRevocationStatus(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("newRevocationStatus") Integer newRevocationStatus) {

        try {
            log.info("Revocation update request from user: {}", userId);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File cannot be empty"));
            }
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("User ID required"));
            }
            if (newRevocationStatus == null || (newRevocationStatus != 0 && newRevocationStatus != 1)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Revocation status must be 0 or 1"));
            }

            RevocationRequest request = new RevocationRequest();
            request.setFileBytes(file.getBytes());
            request.setUserId(userId);
            request.setNewRevocationStatus(newRevocationStatus);

            var response = documentService.updateRevocationStatus(request);

            log.info("Revocation update result: {}", response.isSuccess());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Revocation update error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("Revocation update failed: " + e.getMessage())
            );
        }
    }
}
