import React from 'react';
import './Home.css';

const Home = ({ onNavigate }) => {
    return (
        <div className="home-container">
            <div className="hero-section">
                <h1>📄 Document Verification System</h1>
                <p className="subtitle">
                    Secure document verification using blockchain, AHIBE encryption, and decentralized storage
                </p>
                <div className="button-group">
                    <button className="btn btn-primary" onClick={() => onNavigate('upload')}>
                        📤 Upload Document
                    </button>
                    <button className="btn btn-secondary" onClick={() => onNavigate('verify')}>
                        ✓ Verify Document
                    </button>
                    <button className="btn btn-revoke" onClick={() => onNavigate('revoke')}>
                        🔄 Update Revocation
                    </button>
                </div>
                <p className="revoke-note">
                    🔐 Revocation update is for <strong>issuers only</strong>
                </p>
            </div>

            <div className="features-section">
                <h2>Key Features</h2>
                <div className="features-grid">
                    <div className="feature-card">
                        <h3>🔐 SHA-256 Hashing</h3>
                        <p>Document integrity verification</p>
                    </div>
                    <div className="feature-card">
                        <h3>✍️ ECDSA Signatures</h3>
                        <p>Digital authentication with secp256r1</p>
                    </div>
                    <div className="feature-card">
                        <h3>🌐 Blockchain Storage</h3>
                        <p>Immutable Ethereum records</p>
                    </div>
                    <div className="feature-card">
                        <h3>⏱️ AHIBE Encryption</h3>
                        <p>Time-bounded automatic expiry</p>
                    </div>
                    <div className="feature-card">
                        <h3>🌸 Counting Bloom Filter</h3>
                        <p>Efficient revocation management with deletion support</p>
                    </div>
                    <div className="feature-card">
                        <h3>📌 IPFS Integration</h3>
                        <p>Decentralized metadata storage</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Home;