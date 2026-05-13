package com.docverify.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Component
public class CountingBloomFilterUtil {

    private static final int CBF_SIZE   = 1000;
    private static final int HASH_COUNT = 3;
    private static int[] counters = new int[CBF_SIZE];

    public CountingBloomFilterUtil() {}

    private int getIndex(String input, int seed) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((input + seed).getBytes());
        byte[] hash = digest.digest();
        int value = 0;
        for (int i = 0; i < 4; i++) value = (value << 8) | (hash[i] & 0xff);
        return Math.abs(value) % CBF_SIZE;
    }

    public void addToBloomFilter(String docHash, int revocationStatus) throws Exception {
        if (revocationStatus == 1) {
            for (int i = 0; i < HASH_COUNT; i++) {
                int index = getIndex(docHash, i);
                counters[index]++;
            }
        }
    }

    /**
     * Updates revocation status of an existing document in CBF.
     * - If changing 0 → 1: increments counters (marks as revoked)
     * - If changing 1 → 0: decrements counters (removes revocation)
     */
    public void updateRevocationStatus(String docHash, int oldStatus, int newStatus) throws Exception {
        if (oldStatus == newStatus) return; // nothing to do

        if (oldStatus == 0 && newStatus == 1) {
            // Not revoked → Revoked: increment counters
            for (int i = 0; i < HASH_COUNT; i++) {
                int index = getIndex(docHash, i);
                counters[index]++;
            }
        } else if (oldStatus == 1 && newStatus == 0) {
            // Revoked → Not revoked: decrement counters (counting BF supports deletion)
            for (int i = 0; i < HASH_COUNT; i++) {
                int index = getIndex(docHash, i);
                if (counters[index] > 0) counters[index]--;
            }
        }
    }

    public boolean isRevoked(String docHash) throws Exception {
        for (int i = 0; i < HASH_COUNT; i++) {
            int index = getIndex(docHash, i);
            if (counters[index] == 0) return false;
        }
        return true;
    }

    public String exportToJsonString() throws Exception {
        StringBuilder cbfArray = new StringBuilder("[");
        for (int i = 0; i < counters.length; i++) {
            cbfArray.append(counters[i]);
            if (i < counters.length - 1) cbfArray.append(",");
        }
        cbfArray.append("]");

        Map<String, Object> cbfData = new HashMap<>();
        cbfData.put("cbf", cbfArray.toString());
        cbfData.put("size", CBF_SIZE);
        cbfData.put("hashCount", HASH_COUNT);

        return new ObjectMapper().writeValueAsString(cbfData);
    }

    public int[] getCounters() { return counters; }
    // ── ADD THIS METHOD TO CountingBloomFilterUtil.java ──────────────

    /**
     * Loads CBF state from a stored JSON string into the in-memory array.
     * Called on application startup to restore previous CBF state from DB.
     */
    public void loadFromJson(String cbfJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(cbfJson);

            String cbfArrayStr = root.get("cbf").asText();
            cbfArrayStr = cbfArrayStr.replace("[", "").replace("]", "").trim();
            String[] parts = cbfArrayStr.split(",");

            // Only load if size matches
            if (parts.length == CBF_SIZE) {
                for (int i = 0; i < parts.length; i++) {
                    counters[i] = Integer.parseInt(parts[i].trim());
                }
                System.out.println("[CBF] Restored " + CBF_SIZE + " counters from database.");
            } else {
                System.out.println("[CBF] Size mismatch — starting fresh.");
            }
        } catch (Exception e) {
            System.out.println("[CBF] Failed to load from JSON: " + e.getMessage());
        }
    }
}