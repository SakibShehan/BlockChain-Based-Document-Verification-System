package com.docverify.contract;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tuples.generated.Tuple4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Web3j Java wrapper for the DocumentStore Solidity contract.
 *
 * KEY BUG FIX:
 *   Web3j 4.x has a known bug where executeCallMultipleValueReturn() silently
 *   fails to decode multiple dynamic ABI string return values.
 *   It throws ArrayIndexOutOfBoundsException("Index: 0, Size: 0").
 *
 *   The fix in getDocumentDirect():
 *     - Manually call web3j.ethCall() with the raw ABI-encoded function
 *     - Decode the hex response using FunctionReturnDecoder.decode()
 *   This bypasses the broken internal Web3j decoder path entirely.
 */
public class DocumentStoreContract extends Contract {

    // Only needed for .deploy() — not for .load() which is what we use
    public static final String BINARY = "0x0";

    // Stored explicitly so getDocumentDirect() can make its own eth_call
    private final Web3j       web3j;
    private final Credentials credentials;
    private final String      contractAddress;

    // ── Constructor ────────────────────────────────────────────────────────
    protected DocumentStoreContract(
            String contractAddress,
            Web3j web3j,
            Credentials credentials,
            ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, credentials, gasProvider);
        this.web3j           = web3j;
        this.credentials     = credentials;
        this.contractAddress = contractAddress;
    }

    // ── Factory method ─────────────────────────────────────────────────────
    public static DocumentStoreContract load(
            String contractAddress,
            Web3j web3j,
            Credentials credentials,
            ContractGasProvider gasProvider) {
        return new DocumentStoreContract(contractAddress, web3j, credentials, gasProvider);
    }

    // ── WRITE: storeDocument ───────────────────────────────────────────────
    /**
     * Store document metadata on-chain.
     * Sends a signed transaction and waits until it is mined.
     * The returned TransactionReceipt contains the txHash.
     */
    @SuppressWarnings("rawtypes")
    public RemoteFunctionCall<TransactionReceipt> storeDocument(
            String hash, String signature, String publicKey, String ipfsLink) {

        final Function function = new Function(
                "storeDocument",
                Arrays.<Type>asList(
                        new Utf8String(hash),
                        new Utf8String(signature),
                        new Utf8String(publicKey),
                        new Utf8String(ipfsLink)
                ),
                Collections.emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ── READ: getDocument  (BUG-FIXED version) ─────────────────────────────
    /**
     * Retrieve stored document metadata by hash.
     *
     * Root cause of the "Index: 0" bug:
     *   Web3j's executeCallMultipleValueReturn() uses an internal ABI decoder
     *   that fails on tuple responses with 4+ dynamic string fields.
     *
     * Fix applied here:
     *   1. Build the ABI-encoded function call manually.
     *   2. Send it as a raw ethCall() — bypassing the broken decoder path.
     *   3. Decode the hex response with FunctionReturnDecoder.decode() directly.
     *
     * @param  hash  The document hash (the mapping key in the contract)
     * @return Tuple4 of (hash, signature, publicKey, ipfsLink)
     * @throws Exception if the document is not found or the call fails
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Tuple4<String, String, String, String> getDocumentDirect(String hash)
            throws Exception {

        // Step 1: Define the Solidity function signature with typed outputs
        Function function = new Function(
                "getDocument",
                Collections.singletonList(new Utf8String(hash)),
                Arrays.asList(
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {}
                )
        );

        // Step 2: ABI-encode the function call to hex
        String encodedCall = FunctionEncoder.encode(function);

        // Step 3: Send raw eth_call to the contract (no transaction, no gas cost)
        EthCall ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(),  // caller address
                        contractAddress,           // contract address
                        encodedCall                // ABI-encoded call data
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        // Step 4: Detect Solidity-level revert ("Document not found" etc.)
        if (ethCall.isReverted()) {
            throw new RuntimeException("Contract reverted: " + ethCall.getRevertReason());
        }

        String rawHex = ethCall.getValue();

        if (rawHex == null || rawHex.equals("0x") || rawHex.isEmpty()) {
            throw new RuntimeException("Empty response — document hash not found.");
        }

        // Step 5: Decode the ABI hex directly — this is the correct approach
        List<Type> decoded = FunctionReturnDecoder.decode(
                rawHex, function.getOutputParameters());

        if (decoded == null || decoded.size() < 4) {
            throw new RuntimeException(
                    "ABI decode returned " + (decoded == null ? "null" : decoded.size()) +
                            " values (expected 4). Raw response: " + rawHex);
        }

        return new Tuple4<>(
                (String) decoded.get(0).getValue(),
                (String) decoded.get(1).getValue(),
                (String) decoded.get(2).getValue(),
                (String) decoded.get(3).getValue()
        );
    }

    // ── READ: documentExists ───────────────────────────────────────────────
    /**
     * Returns true if the hash is stored in the contract.
     * Single bool return — not affected by the Web3j string bug.
     */
    @SuppressWarnings("rawtypes")
    public RemoteFunctionCall<Boolean> documentExists(String hash) {
        final Function function = new Function(
                "documentExists",
                Collections.singletonList(new Utf8String(hash)),
                Collections.singletonList(new TypeReference<Bool>() {})
        );
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }
}