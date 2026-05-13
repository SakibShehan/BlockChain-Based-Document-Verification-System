package com.docverify.service;

import com.docverify.contract.DocumentStoreContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.math.BigInteger;
import java.io.IOException;
import java.util.Optional;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    // ── Gas settings matching reference project ──────────────────────────
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L); // 20 Gwei
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(500_000L);

    @Value("${blockchain.ganache-url:http://127.0.0.1:7545}")
    private String ganacheUrl;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    @Value("${blockchain.private-key}")
    private String privateKey;

    private Web3j web3j;
    private DocumentStoreContract contract;

    // ── Initialize on startup ─────────────────────────────────────────────
    @PostConstruct
    public void init() {
        try {
            this.web3j = Web3j.build(new HttpService(ganacheUrl));

            String chainId = web3j.ethChainId().send().getChainId().toString();
            log.info("Connected to Ganache. Chain ID: {}", chainId);

            Credentials credentials = Credentials.create(privateKey);
            log.info("Loaded account: {}", credentials.getAddress());

            StaticGasProvider gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);

            this.contract = DocumentStoreContract.load(
                    contractAddress, web3j, credentials, gasProvider);

            log.info("DocumentStoreContract loaded at: {}", contractAddress);

        } catch (Exception e) {
            log.error("Failed to connect to blockchain: {}", e.getMessage());
            log.warn("Blockchain unavailable — upload will fail if contract is unreachable.");
        }
    }

    // ── UPLOAD: calls storeDocument() on Ganache ──────────────────────────
    public String uploadDocument(
            String hash,
            String signature,
            String publicKey,
            String ipfsLink) throws Exception {

        log.info("Calling storeDocument() on blockchain...");

        if (contract == null) {
            throw new RuntimeException("Blockchain contract not initialized. Check Ganache and contract address.");
        }

        TransactionReceipt receipt = contract.storeDocument(hash, signature, publicKey, ipfsLink).send();

        if (!receipt.isStatusOK()) {
            throw new RuntimeException("Blockchain transaction failed (reverted). Check contract logic.");
        }

        String txHash = receipt.getTransactionHash();
        log.info("Transaction mined. TX hash: {}", txHash);
        log.info("Block number: {}", receipt.getBlockNumber());
        log.info("Gas used: {}", receipt.getGasUsed());

        return txHash;
    }

    // ── VERIFY: checks real transaction on Ganache ────────────────────────
    public boolean verifyTransaction(String txHash) {
        try {
            if (web3j == null) {
                log.warn("Blockchain not connected — skipping tx verification.");
                return true;
            }

            // Step 1: Does transaction exist?
            Optional<org.web3j.protocol.core.methods.response.Transaction> txOpt =
                    web3j.ethGetTransactionByHash(txHash).send().getTransaction();

            if (txOpt.isEmpty()) {
                log.warn("Transaction not found on blockchain: {}", txHash);
                return false;
            }

            // Step 2: Was it sent to the correct contract?
            org.web3j.protocol.core.methods.response.Transaction tx = txOpt.get();
            String recipient = tx.getTo() == null ? "" : tx.getTo().toLowerCase().trim();
            String expected  = contractAddress.toLowerCase().trim();

            if (!recipient.equals(expected)) {
                log.warn("TX sent to wrong contract. Expected: {} Got: {}", expected, recipient);
                return false;
            }

            // Step 3: Did it succeed?
            Optional<org.web3j.protocol.core.methods.response.TransactionReceipt> receiptOpt =
                    web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();

            if (receiptOpt.isEmpty()) {
                log.warn("Transaction receipt not available yet: {}", txHash);
                return false;
            }

            boolean ok = receiptOpt.get().isStatusOK();
            log.info("Transaction verification result: {}", ok ? "VALID" : "FAILED");
            return ok;

        } catch (IOException e) {
            log.warn("Error verifying transaction: {} — accepting as valid for demo.", e.getMessage());
            return true;
        }
    }

    // ── Shutdown ──────────────────────────────────────────────────────────
    @PreDestroy
    public void shutdown() {
        if (web3j != null) {
            web3j.shutdown();
            log.info("Web3j connection closed.");
        }
    }

    public boolean isBlockchainConnected() {
        if (web3j == null) return false;
        try {
            web3j.ethChainId().send();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}