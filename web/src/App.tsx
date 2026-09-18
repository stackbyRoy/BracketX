import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { HomePage } from './pages/HomePage';
import { CreateTournamentPage } from './pages/CreateTournamentPage';
import { TournamentDetailPage } from './pages/TournamentDetailPage';
import { ProfilePage } from './pages/ProfilePage';
import { AuthPage } from './pages/AuthPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
          <Navbar />
          <main style={{ flex: 1 }}>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/create" element={<CreateTournamentPage />} />
              <Route path="/t/:id" element={<TournamentDetailPage />} />
              <Route path="/tournament/:id" element={<TournamentDetailPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/auth" element={<AuthPage />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
