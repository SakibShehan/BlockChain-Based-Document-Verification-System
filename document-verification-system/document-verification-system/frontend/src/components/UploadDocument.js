import React, { useState } from 'react';
import { documentAPI } from '../services/api';
import './UploadDocument.css';

const FIELD_LABELS = {
    documentHash:       '🔐 Document Hash (SHA-256)',
    digitalSignature:   '✍️ Digital Signature (ECDSA)',
    publicKey:          '🔑 Public Key',
    privateKey:         '🗝️ Private Key',
    revocationStatus:   '🚫 Revocation Status',
    cbfJson:            '🌸 Counting Bloom Filter (CBF JSON)',
    ipfsCID:            '📦 IPFS CID',
    ahibeCiphertext:    '🔒 AHIBE Cipher Text',
    ahibeDecryptionKey: '🔓 AHIBE Decryption Key',
    transactionHash:    '⛓️ Blockchain Transaction Hash',
};

const FIELD_ORDER = [
    'documentHash',
    'digitalSignature',
    'publicKey',
    'privateKey',
    'revocationStatus',
    'cbfJson',
    'ipfsCID',
    'ahibeCiphertext',
    'ahibeDecryptionKey',
    'transactionHash',
];

const UploadDocument = () => {
    const [file, setFile]                     = useState(null);
    const [userId, setUserId]                 = useState('');
    const [expiryDate, setExpiryDate]         = useState('');
    const [revocationStatus, setRevocationStatus] = useState(0);
    const [loading, setLoading]               = useState(false);
    const [result, setResult]                 = useState(null);
    const [error, setError]                   = useState(null);

    const handleFileChange = (e) => setFile(e.target.files[0]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setResult(null);

        if (!file)          { setError('Please select a file'); return; }
        if (!userId.trim()) { setError('Please enter User ID'); return; }
        if (!expiryDate)    { setError('Please select expiry date'); return; }

        setLoading(true);
        try {
            const response = await documentAPI.uploadDocument(
                file, userId, expiryDate, revocationStatus
            );
            // response.data is the full UploadResponse JSON from backend
            setResult(response.data);
        } catch (err) {
            const msg =
                err.response?.data?.error ||
                err.response?.data?.message ||
                err.message ||
                'Upload failed. Check backend is running.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="upload-container">
            <h2>Upload Document</h2>

            <form onSubmit={handleSubmit} className="upload-form">
                <div className="form-group">
                    <label>Document File *</label>
                    <input type="file" onChange={handleFileChange} disabled={loading} required />
                    {file && <p className="file-name">✓ {file.name}</p>}
                </div>

                <div className="form-group">
                    <label>User ID (e.g., CE21040) *</label>
                    <input
                        type="text"
                        value={userId}
                        onChange={(e) => setUserId(e.target.value)}
                        placeholder="Student/User ID"
                        disabled={loading}
                        required
                    />
                </div>

                <div className="form-group">
                    <label>Expiry Date *</label>
                    <input
                        type="date"
                        value={expiryDate}
                        onChange={(e) => setExpiryDate(e.target.value)}
                        disabled={loading}
                        required
                    />
                </div>

                <div className="form-group">
                    <label>Revocation Status *</label>
                    <select
                        value={revocationStatus}
                        onChange={(e) => setRevocationStatus(parseInt(e.target.value))}
                        disabled={loading}
                    >
                        <option value={0}>Not Revoked (0)</option>
                        <option value={1}>Revoked (1)</option>
                    </select>
                </div>

                <button type="submit" disabled={loading} className="submit-btn">
                    {loading ? '⏳ Uploading...' : '📤 Upload Document'}
                </button>
            </form>

            {error && (
                <div className="error-box">
                    <strong>❌ Error:</strong> {error}
                </div>
            )}

            {result && (
                <div className="success-box">
                    <h3>✅ Upload Successful!</h3>
                    <p className="save-note">
                        📌 Save the <strong>Transaction Hash</strong> below — you will need it to verify this document.
                    </p>

                    <div className="generated-data">
                        <h4>📋 All Generated Data</h4>

                        {/* Top-level fields */}
                        <div className="data-item highlight">
                            <span className="data-label">⛓️ Blockchain Transaction Hash</span>
                            <code className="data-value">{result.transactionHash}</code>
                        </div>

                        <div className="data-item highlight">
                            <span className="data-label">🔐 Document Hash (SHA-256)</span>
                            <code className="data-value">{result.documentHash}</code>
                        </div>

                        {/* All 10 fields from generatedData map */}
                        {result.generatedData && FIELD_ORDER.filter(
                            k => k !== 'documentHash' && k !== 'transactionHash'
                        ).map((key) => {
                            const value = result.generatedData[key];
                            if (value === undefined || value === null) return null;
                            return (
                                <div key={key} className="data-item">
                                    <span className="data-label">{FIELD_LABELS[key] || key}</span>
                                    <code className="data-value">
                                        {key === 'cbfJson'
                                            ? <pre className="cbf-pre">{
                                                (() => { try { return JSON.stringify(JSON.parse(value), null, 2); } catch { return value; } })()
                                              }</pre>
                                            : value
                                        }
                                    </code>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
};

export default UploadDocument;