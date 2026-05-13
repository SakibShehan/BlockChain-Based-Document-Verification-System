#  Blockchain-Based Document Verification System

A full-stack document authentication platform implementing a novel cryptographic pipeline for **revocation-aware** and **expiry-aware** certificate verification using blockchain, decentralized storage, and modern cryptographic primitives.

---

##  Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Cryptographic Pipeline](#cryptographic-pipeline)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Security Analysis](#security-analysis)
- [Project Structure](#project-structure)
- [Screenshots](#screenshots)
- [Attack Demonstrations](#running-attack-demonstrations)
- [Known Limitations](#known-limitations-prototype)
- [License](#license)

---

## Overview

This system provides end-to-end document verification for issuers and verifiers. An issuer uploads a document and the system automatically generates a cryptographic proof — including a digital signature, blockchain transaction, and encrypted IPFS link. A verifier can later authenticate the document using the transaction hash and a decryption key, with automatic expiry enforcement and revocation checking.

The core contribution is the **combination of Counting Bloom Filter (CBF) for revocation** and **AES-256-GCM attribute-based encryption for expiry-aware access control** — two mechanisms that together provide a complete certificate lifecycle management system.

---

## System Architecture

```
┌─────────────────────────────────────────────────────┐
│                React Frontend (Port 3000)            │
│     Upload · Verify · Update Revocation Status      │
└────────────────────────┬────────────────────────────┘
                         │ HTTP REST API
┌────────────────────────▼────────────────────────────┐
│            Spring Boot Backend (Port 8090)           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │  Controller │→ │   Service   │→ │ Repository  │ │
│  │   Layer     │  │   Layer     │  │   Layer     │ │
│  └─────────────┘  └──────┬──────┘  └──────┬──────┘ │
│                          │                │         │
│              ┌───────────▼───────────┐    │         │
│              │    Utility Layer      │    │         │
│              │ HashUtil · ECDSAUtil  │    │         │
│              │ AHIBEUtil · CBFUtil   │    │         │
│              │ IPFSUtil              │    │         │
│              └───────────────────────┘    │         │
└───────────────────────────────────────────┼─────────┘
                                            │
          ┌─────────────────────────────────┼──────┐
          │             │                   │      │
    ┌─────▼─────┐ ┌─────▼─────┐ ┌──────────▼────┐ │
    │PostgreSQL │ │   IPFS    │ │   Ethereum    │ │
    │(Metadata) │ │(CBF JSON) │ │  (Ganache)    │ │
    └───────────┘ └───────────┘ └───────────────┘ │
```

---

## Cryptographic Pipeline

### Upload Flow
```
Document File
     ↓
SHA-256 Hash Generation
     ↓
ECDSA Key Pair Generation + Digital Signature (secp256r1)
     ↓
Counting Bloom Filter Update (revocation tracking)
     ↓
CBF JSON → IPFS Upload → CID
     ↓
AES-256-GCM Encryption of CID (userId + expiryDate attributes)
     ↓
Blockchain Transaction (hash + signature + publicKey + encryptedCID)
     ↓
All data saved to PostgreSQL
     ↓
Returns: txHash · docHash · decryptionKey · all generated data
```

### Verification Flow
```
Document + TX Hash + User ID + Decryption Key
     ↓
SHA-256 Hash from uploaded file
     ↓
Database lookup by hash → Not found: "Document not uploaded"
     ↓
TX Hash match with DB record
     ↓
Live Blockchain TX validation (exists + correct contract + succeeded)
     ↓
AES-256-GCM Decryption (checks userId fingerprint + expiry timestamp)
     → EXPIRED if past expiry date
     ↓
CBF JSON fetched from IPFS → Revocation status check
     ↓
ECDSA Signature verification using stored public key
     ↓
Returns: hash match · signature valid · revocation status · expiry date
```

---

## Features

### Issuer
- Upload any document and generate a complete cryptographic proof
- All 12 generated values displayed: hash, signature, public key, private key, CBF JSON, IPFS CID, AHIBE ciphertext, decryption key, transaction hash
- Update revocation status of any uploaded document — supports both revocation and reinstatement using Counting Bloom Filter deletion

### Verifier
- Verify document authenticity using document file + TX hash + user ID + decryption key
- Blockchain info box showing stored hash, signature, public key, encrypted IPFS link fetched from database
- Verification results: hash match, signature validity, revocation status, expiry date, decrypted IPFS CID

### Security
- SHA-256 document integrity with avalanche effect verification
- ECDSA digital signatures with SecureRandom nonce — nonce reuse attack prevented
- AES-256-GCM authenticated encryption — IND-CCA2 secure, bit-flipping impossible
- Counting Bloom Filter with deletion support for dynamic revocation management
- Automatic expiry enforcement embedded in ciphertext — no manual intervention needed
- userId fingerprint embedded in AES-GCM plaintext — cross-user replay blocked
- Three-step live blockchain TX validation via Web3j
- CBF state persistence across server restarts via database restore on startup

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React.js, Axios, CSS3 |
| Backend | Java 17, Spring Boot 3.2, Spring Data JPA |
| Database | PostgreSQL |
| Blockchain | Ethereum (Ganache), Web3j, Solidity |
| Decentralized Storage | IPFS (Kubo local daemon) |
| Cryptography | SHA-256, ECDSA secp256r1, AES-256-GCM, Counting Bloom Filter |
| Build Tool | Maven |
| API Style | REST — stateless, JSON responses |

---

## Getting Started

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 17+ | Backend runtime |
| Node.js | 18+ | Frontend runtime |
| PostgreSQL | 14+ | Database |
| Ganache | Latest | Local Ethereum blockchain |
| IPFS Kubo | Latest | Local IPFS node |
| Maven | 3.8+ | Backend build |

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/blockchain-document-verifier.git
cd blockchain-document-verifier
```

### 2. Setup PostgreSQL

```sql
CREATE DATABASE doc_verifier;
```

### 3. Configure Backend

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/doc_verifier
    username: your_username
    password: your_password

blockchain:
  ganache-url: http://127.0.0.1:7545
  contract-address: "YOUR_DEPLOYED_CONTRACT_ADDRESS"
  private-key: "YOUR_GANACHE_ACCOUNT_PRIVATE_KEY"

server:
  port: 8090
```

### 4. Deploy Smart Contract

Open `DocumentStore.sol` in Remix IDE → compile → deploy to Ganache → copy the contract address into `application.yml`.

### 5. Start All Services

```bash
# Terminal 1 — IPFS daemon
ipfs daemon

# Terminal 2 — Ganache blockchain
ganache -p 7545

# Terminal 3 — Spring Boot backend
cd backend
./mvnw spring-boot:run

# Terminal 4 — React frontend
cd frontend
npm install
npm start
```

### 6. Open the App

```
http://localhost:3000
```

---

## API Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| `GET` | `/api/documents/health` | Health check | — |
| `GET` | `/api/documents` | API info and available endpoints | — |
| `POST` | `/api/documents/upload` | Upload and register a document | `file`, `userId`, `expiryDate`, `revocationStatus` |
| `POST` | `/api/documents/verify` | Verify a document | `file`, `transactionHash`, `userId`, `decryptionKey` |
| `POST` | `/api/documents/revoke` | Update revocation status | `file`, `userId`, `newRevocationStatus` |
| `GET` | `/api/documents/status/{docHash}` | Get document status by hash | `docHash` |

---

## Security Analysis

| Attack | Component | Defense |
|--------|-----------|---------|
| Document Tampering | SHA-256 | Hash mismatch → rejected |
| Preimage / Collision Attack | SHA-256 | 2²⁵⁶ resistance |
| Signature Forgery | ECDSA | ECDLP — 2¹²⁸ operations |
| Nonce Reuse Attack | ECDSA | SecureRandom unique nonce per signature |
| Cross-Document Replay | ECDSA | Signature bound to exact document hash |
| Cross-User Replay | AES-GCM AHIBE | userId fingerprint embedded in ciphertext |
| Expired Document Access | AES-GCM AHIBE | Expiry timestamp embedded — automatic enforcement |
| Wrong Key Access | AES-GCM | AEADBadTagException — 100% block rate |
| Bit-Flipping Attack | AES-GCM | 128-bit auth tag invalidates any modification |
| CBF Tampering | IPFS | Content addressing — CID changes on any modification |
| Fake TX Submission | Blockchain | Live 3-step TX validation |
| Data Immutability | Blockchain | Immutable on-chain records |

---

## Project Structure

```
blockchain-document-verifier/
├── backend/
│   ├── src/main/java/com/docverify/
│   │   ├── attack/
│   │   │   └── AttackDemoRunner.java       # 8-attack cryptographic demo
│   │   ├── config/
│   │   │   └── CbfInitializer.java         # CBF restore on startup
│   │   ├── contract/
│   │   │   └── DocumentStoreContract.java  # Web3j smart contract wrapper
│   │   ├── controller/
│   │   │   └── DocumentController.java     # REST endpoints
│   │   ├── dto/
│   │   │   ├── UploadRequest.java
│   │   │   ├── UploadResponse.java
│   │   │   ├── VerificationRequest.java
│   │   │   ├── VerificationResponse.java
│   │   │   ├── RevocationRequest.java
│   │   │   └── RevocationResponse.java
│   │   ├── entity/
│   │   │   └── Document.java               # JPA entity — primary key: docHash
│   │   ├── repository/
│   │   │   └── DocumentRepository.java     # Spring Data JPA interface
│   │   ├── service/
│   │   │   ├── DocumentService.java        # Business logic orchestration
│   │   │   └── BlockchainService.java      # Web3j + smart contract calls
│   │   └── util/
│   │       ├── HashUtil.java               # SHA-256
│   │       ├── ECDSAUtil.java              # ECDSA secp256r1
│   │       ├── AHIBEUtil.java              # AES-256-GCM encryption
│   │       ├── CountingBloomFilterUtil.java # CBF revocation
│   │       └── IPFSUtil.java               # IPFS Kubo client
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Home.js / Home.css
│   │   │   ├── UploadDocument.js / UploadDocument.css
│   │   │   ├── VerifyDocument.js / VerifyDocument.css
│   │   │   └── UpdateRevocation.js / UpdateRevocation.css
│   │   ├── services/
│   │   │   └── api.js                      # Axios API calls
│   │   ├── App.js
│   │   └── App.css
│   └── package.json
│
└── DocumentStore.sol                       # Ethereum smart contract
```

---

## Running Attack Demonstrations

Run the cryptographic attack demo with real file input via Swing GUI:

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=attack-demo"
```

A file browser opens — select your original and tampered documents. All 8 attacks run sequentially with console output showing defense results and quantitative metrics.

**Attacks demonstrated:**
1. Document Tampering — SHA-256 avalanche effect
2. ECDSA Nonce Reuse — r-value uniqueness verification
3. Context-Based Replay — cross-document, cross-user, TX replay
4. Expired Document Access — AES-GCM automatic expiry
5. Wrong Decryption Key — AEADBadTagException defense
6. CBF False Positive Rate — theoretical vs actual measurement
7. CBF Scalability — O(k) constant query time at scale
8. Bit-Flipping on AES-GCM — all 5 tampering attempts blocked

---

## Known Limitations (Prototype)

- CBF is in-memory — restored from DB on startup but resets if DB is cleared
- Ganache is a local test network — not production Ethereum
- IPFS runs on a single local node — no pinning service for high availability
- ECDSA and AES-GCM are not post-quantum secure — future upgrade: CRYSTALS-Dilithium + Kyber
- No user authentication — designed as a single-issuer thesis demonstration

---

## License

This project was developed as a thesis implementation and is available for academic reference.

---

<p align="center">
  Built with Spring Boot · React · Ethereum · IPFS · AES-256-GCM · ECDSA · Counting Bloom Filter
</p>
