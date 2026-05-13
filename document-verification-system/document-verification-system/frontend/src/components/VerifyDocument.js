import React, { useState } from 'react';
import { documentAPI } from '../services/api';
import './VerifyDocument.css';

const VerifyDocument = () => {
    const [file, setFile]                         = useState(null);
    const [transactionHash, setTransactionHash]   = useState('');
    const [userId, setUserId]                     = useState('');
    const [decryptionKey, setDecryptionKey]       = useState('');
    const [loading, setLoading]                   = useState(false);
    const [result, setResult]                     = useState(null);
    const [error, setError]                       = useState(null);

    const handleFileChange = (e) => setFile(e.target.files[0]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setResult(null);

        if (!file)                    { setError('Please select a file'); return; }
        if (!transactionHash.trim())  { setError('Please enter transaction hash'); return; }
        if (!userId.trim())           { setError('Please enter User ID'); return; }
        if (!decryptionKey.trim())    { setError('Please enter decryption key'); return; }

        setLoading(true);
        try {
            const response = await documentAPI.verifyDocument(file, transactionHash, userId, decryptionKey);
            setResult(response.data);
        } catch (err) {
            const msg =
                err.response?.data?.error ||
                err.response?.data?.message ||
                err.message ||
                'Verification failed. Check backend is running.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    const safe = (val, len = 60) =>
        val ? (val.length > len ? val.substring(0, len) + '...' : val) : 'N/A';

    return (
        <div className="verify-container">
            <h2>Verify Document</h2>

            <form onSubmit={handleSubmit} className="verify-form">
                <div className="form-group">
                    <label>Document File *</label>
                    <input type="file" onChange={handleFileChange} disabled={loading} required />
                    {file && <p className="file-name">✓ {file.name}</p>}
                </div>

                <div className="form-group">
                    <label>Transaction Hash *</label>
                    <input
                        type="text"
                        value={transactionHash}
                        onChange={(e) => setTransactionHash(e.target.value)}
                        placeholder="0x..."
                        disabled={loading}
                        required
                    />
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
                    <label>AHIBE Decryption Key *</label>
                    <input
                        type="text"
                        value={decryptionKey}
                        onChange={(e) => setDecryptionKey(e.target.value)}
                        placeholder="Paste AHIBE decryption key from upload result"
                        disabled={loading}
                        required
                    />
                </div>

                <button type="submit" disabled={loading} className="submit-btn">
                    {loading ? '⏳ Verifying...' : '🔍 Verify Document'}
                </button>
            </form>

            {error && (
                <div className="error-box">
                    <strong>❌ Error:</strong> {error}
                </div>
            )}

            {result && (
                <div className={`result-box ${result.success ? 'success' : 'failed'}`}>
                    <h3>{result.success ? '✅ Verification Successful' : '❌ Verification Failed'}</h3>
                    <p className="result-message"><strong>Message:</strong> {result.message}</p>

                    {(result.docHash || result.docSignature || result.publicKey || result.ahibeEncryptedLink) && (
                        <div className="blockchain-info-box">
                            <h4>⛓️ Blockchain Stored Information</h4>
                            <div className="info-row">
                                <span className="info-label">🔐 Document Hash</span>
                                <code className="info-value">{safe(result.docHash)}</code>
                            </div>
                            <div className="info-row">
                                <span className="info-label">✍️ Digital Signature</span>
                                <code className="info-value">{safe(result.docSignature)}</code>
                            </div>
                            <div className="info-row">
                                <span className="info-label">🔑 Public Key</span>
                                <code className="info-value">{safe(result.publicKey)}</code>
                            </div>
                            <div className="info-row">
                                <span className="info-label">🔒 AHIBE Encrypted IPFS Link</span>
                                <code className="info-value">{safe(result.ahibeEncryptedLink)}</code>
                            </div>
                        </div>
                    )}

                    {result.success && (
                        <div className="verification-details">
                            <h4>🧪 Verification Results</h4>
                            <div className="detail-item">
                                <strong>Hash Match:</strong>
                                <span className={result.hashMatch ? 'valid' : 'invalid'}>
                                    {result.hashMatch ? '✓ YES' : '✗ NO'}
                                </span>
                            </div>
                            <div className="detail-item">
                                <strong>Signature Valid:</strong>
                                <span className={result.signatureValid ? 'valid' : 'invalid'}>
                                    {result.signatureValid ? '✓ YES' : '✗ NO'}
                                </span>
                            </div>
                            <div className="detail-item">
                                <strong>Revocation Status:</strong>
                                <span className={result.revocationStatus === 'NOT REVOKED' ? 'valid' : 'invalid'}>
                                    {result.revocationStatus || 'N/A'}
                                </span>
                            </div>
                            <div className="detail-item">
                                <strong>Expiry Date:</strong>
                                <span>{result.expiryDate || 'N/A'}</span>
                            </div>
                            <div className="detail-item">
                                <strong>IPFS CID (Decrypted):</strong>
                                <code>{safe(result.ipfsCID, 50)}</code>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default VerifyDocument;