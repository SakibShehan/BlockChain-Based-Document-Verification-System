import React, { useState } from 'react';
import { documentAPI } from '../services/api';
import './UpdateRevocation.css';

const UpdateRevocation = () => {
    const [file, setFile]                         = useState(null);
    const [userId, setUserId]                     = useState('');
    const [newRevocationStatus, setNewRevocationStatus] = useState(0);
    const [loading, setLoading]                   = useState(false);
    const [result, setResult]                     = useState(null);
    const [error, setError]                       = useState(null);

    const handleFileChange = (e) => setFile(e.target.files[0]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setResult(null);

        if (!file)          { setError('Please select a document file'); return; }
        if (!userId.trim()) { setError('Please enter User ID'); return; }

        setLoading(true);
        try {
            const response = await documentAPI.updateRevocationStatus(
                file, userId, newRevocationStatus
            );
            setResult(response.data);
        } catch (err) {
            const msg =
                err.response?.data?.error ||
                err.response?.data?.message ||
                err.message ||
                'Update failed. Check backend is running.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    const statusLabel = (s) => s === 1 ? '🔴 REVOKED (1)' : '🟢 NOT REVOKED (0)';

    return (
        <div className="revoke-container">
            <div className="revoke-header">
                <h2>🔄 Update Revocation Status</h2>
                <p className="revoke-subtitle">
                    Upload the document and set its new revocation status in the Counting Bloom Filter.
                </p>
                <div className="issuer-badge">🔐 Issuer Only</div>
            </div>

            <form onSubmit={handleSubmit} className="revoke-form">
                <div className="form-group">
                    <label>Document File *</label>
                    <input
                        type="file"
                        onChange={handleFileChange}
                        disabled={loading}
                        required
                    />
                    {file && <p className="file-name">✓ {file.name}</p>}
                </div>

                <div className="form-group">
                    <label>User ID (e.g., CE21040) *</label>
                    <input
                        type="text"
                        value={userId}
                        onChange={(e) => setUserId(e.target.value)}
                        placeholder="Issuer / Student ID"
                        disabled={loading}
                        required
                    />
                </div>

                <div className="form-group">
                    <label>New Revocation Status *</label>
                    <select
                        value={newRevocationStatus}
                        onChange={(e) => setNewRevocationStatus(parseInt(e.target.value))}
                        disabled={loading}
                    >
                        <option value={0}>🟢 Not Revoked (0)</option>
                        <option value={1}>🔴 Revoked (1)</option>
                    </select>
                    <p className="status-hint">
                        System will check if this status is already set and reject if no change needed.
                    </p>
                </div>

                <button type="submit" disabled={loading} className="revoke-btn">
                    {loading ? '⏳ Updating...' : '🔄 Update Revocation Status'}
                </button>
            </form>

            {error && (
                <div className="error-box">
                    <strong>❌ Error:</strong> {error}
                </div>
            )}

            {result && (
                <div className={`result-box ${result.success ? 'success' : 'failed'}`}>
                    <h3>{result.success ? '✅ Update Successful' : '⚠️ Update Not Applied'}</h3>
                    <p className="result-message">{result.message}</p>

                    {result.docHash && (
                        <div className="result-details">
                            <div className="detail-row">
                                <span className="detail-label">📄 Document Hash</span>
                                <code className="detail-value">{result.docHash}</code>
                            </div>

                            {result.oldStatus !== undefined && result.newStatus !== undefined && (
                                <div className="status-change-box">
                                    <div className="status-item">
                                        <span className="status-label">Previous Status</span>
                                        <span className={`status-badge ${result.oldStatus === 1 ? 'revoked' : 'active'}`}>
                                            {statusLabel(result.oldStatus)}
                                        </span>
                                    </div>
                                    <div className="status-arrow">→</div>
                                    <div className="status-item">
                                        <span className="status-label">New Status</span>
                                        <span className={`status-badge ${result.newStatus === 1 ? 'revoked' : 'active'}`}>
                                            {statusLabel(result.newStatus)}
                                        </span>
                                    </div>
                                </div>
                            )}

                            {result.success && result.cbfJson && (
                                <div className="cbf-box">
                                    <span className="detail-label">🌸 Updated CBF (stored on IPFS)</span>
                                    <pre className="cbf-preview">
                                        {(() => {
                                            try {
                                                return JSON.stringify(JSON.parse(result.cbfJson), null, 2);
                                            } catch { return result.cbfJson; }
                                        })()}
                                    </pre>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default UpdateRevocation;