import axios from 'axios';

const API_BASE_URL = 'http://localhost:8090/api';

const api = axios.create({
    baseURL: API_BASE_URL,
});

export const documentAPI = {
    uploadDocument: async (file, userId, expiryDate, revocationStatus) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('userId', userId);
        formData.append('expiryDate', expiryDate);
        formData.append('revocationStatus', revocationStatus);
        return api.post('/documents/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    verifyDocument: async (file, transactionHash, userId, decryptionKey) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('transactionHash', transactionHash);
        formData.append('userId', userId);
        formData.append('decryptionKey', decryptionKey);
        return api.post('/documents/verify', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    updateRevocationStatus: async (file, userId, newRevocationStatus) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('userId', userId);
        formData.append('newRevocationStatus', newRevocationStatus);
        return api.post('/documents/revoke', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    getDocumentStatus: async (docHash) => {
        return api.get(`/documents/status/${docHash}`);
    },

    health: async () => {
        return api.get('/documents/health');
    },
};

export default api;