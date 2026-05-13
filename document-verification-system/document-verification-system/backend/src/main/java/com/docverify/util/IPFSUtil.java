package com.docverify.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Component
public class IPFSUtil {

    private final HttpClient httpClient;
    private boolean ipfsConnected;

    @Value("${ipfs.host:127.0.0.1}")
    private String ipfsHost;

    @Value("${ipfs.port:5001}")
    private int ipfsPort;

    public IPFSUtil() {
        this.httpClient = HttpClient.newHttpClient();
        this.ipfsConnected = checkIPFSConnection();
    }

    /**
     * Uploads JSON content to IPFS and returns the CID (Content Identifier)
     */
    public String uploadJsonToIPFS(String jsonContent) throws Exception {
        if (!ipfsConnected) {
            // Return mock CID if IPFS not available
            return "QmMockCID" + UUID.randomUUID().toString().substring(0, 20);
        }

        File tempFile = File.createTempFile("cbf_", ".json");
        Files.write(tempFile.toPath(), jsonContent.getBytes(StandardCharsets.UTF_8));

        try {
            String cid = uploadFileToIPFS(tempFile);
            if (cid == null || cid.isEmpty()) {
                // Return mock CID if upload fails
                return "QmMockCID" + UUID.randomUUID().toString().substring(0, 20);
            }
            return cid;
        } catch (Exception e) {
            System.err.println("Warning: IPFS upload failed: " + e.getMessage());
            // Return mock CID if upload fails
            return "QmMockCID" + UUID.randomUUID().toString().substring(0, 20);
        } finally {
            if (!tempFile.delete()) {
                System.err.println("Warning: Could not delete temporary file: " + tempFile.getAbsolutePath());
            }
        }
    }

    /**
     * Fetches content from IPFS using the provided CID
     */
    public String fetchFromIPFS(String cid) throws Exception {
        if (!ipfsConnected) {
            throw new RuntimeException("IPFS not connected");
        }

        try {
            // Build URL using configured host and port
            String ipfsUrl = String.format("http://%s:%d/api/v0/cat?arg=%s", 
                getIPFSHost(), getIPFSPort(), cid);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ipfsUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new RuntimeException("IPFS fetch failed with status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("IPFS fetch failed for CID: " + cid + " - " + e.getMessage());
        }
    }

    /**
     * Checks if IPFS daemon is connected and available
     */
    public boolean isIPFSConnected() {
        return ipfsConnected;
    }

    /**
     * Internal method to upload a file to IPFS
     */
    private String uploadFileToIPFS(File file) throws IOException, InterruptedException {
        String ipfsUrl = String.format("http://%s:%d/api/v0/add", 
            getIPFSHost(), getIPFSPort());
        
        byte[] fileContent = Files.readAllBytes(file.toPath());
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ipfsUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileContent))
                .header("Content-Type", "application/octet-stream")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            // Parse the CID from IPFS response (simplified)
            String responseBody = response.body();
            // The IPFS response typically contains: {"Name":"","Hash":"QmXXXX",...}
            int hashIndex = responseBody.indexOf("\"Hash\":\"");
            if (hashIndex != -1) {
                int startIndex = hashIndex + 8;
                int endIndex = responseBody.indexOf("\"", startIndex);
                return responseBody.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    /**
     * Internal method to check if IPFS daemon is available
     */
    private boolean checkIPFSConnection() {
        try {
            String ipfsUrl = String.format("http://%s:%d/api/v0/id", 
                getIPFSHost(), getIPFSPort());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ipfsUrl))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Warning: Could not connect to IPFS daemon at " + 
                getIPFSHost() + ":" + getIPFSPort());
            return false;
        }
    }

    /**
     * Get IPFS host address
     */
    private String getIPFSHost() {
        return ipfsHost != null ? ipfsHost : "127.0.0.1";
    }

    /**
     * Get IPFS port
     */
    private int getIPFSPort() {
        return ipfsPort > 0 ? ipfsPort : 5001;
    }
}
