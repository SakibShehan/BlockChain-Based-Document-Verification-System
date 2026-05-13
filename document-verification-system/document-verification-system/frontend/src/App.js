import React, { useState } from 'react';
import Home from './components/Home';
import UploadDocument from './components/UploadDocument';
import VerifyDocument from './components/VerifyDocument';
import UpdateRevocation from './components/UpdateRevocation';
import './App.css';

function App() {
    const [currentPage, setCurrentPage] = useState('home');

    const renderPage = () => {
        switch (currentPage) {
            case 'upload':
                return <UploadDocument />;
            case 'verify':
                return <VerifyDocument />;
            case 'revoke':
                return <UpdateRevocation />;
            case 'home':
            default:
                return <Home onNavigate={setCurrentPage} />;
        }
    };

    return (
        <div className="app">
            <nav className="navbar">
                <div className="navbar-content">
                    <button className="home-btn" onClick={() => setCurrentPage('home')}>
                        🔐 DocVerify
                    </button>
                    <div className="navbar-links">
                        <button
                            className={`nav-link ${currentPage === 'upload' ? 'active' : ''}`}
                            onClick={() => setCurrentPage('upload')}
                        >
                            Upload
                        </button>
                        <button
                            className={`nav-link ${currentPage === 'verify' ? 'active' : ''}`}
                            onClick={() => setCurrentPage('verify')}
                        >
                            Verify
                        </button>
                        <button
                            className={`nav-link revoke-link ${currentPage === 'revoke' ? 'active' : ''}`}
                            onClick={() => setCurrentPage('revoke')}
                        >
                            🔄 Revocation
                        </button>
                    </div>
                </div>
            </nav>

            <main className="main-content">
                {renderPage()}
            </main>

            <footer className="footer">
                <p>&copy; 2024 Document Verification System</p>
            </footer>
        </div>
    );
}

export default App;