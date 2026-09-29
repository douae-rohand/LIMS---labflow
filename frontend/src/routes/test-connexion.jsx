import { createFileRoute } from '@tanstack/react-router';
import { BackendHealthCheck } from '@/components/BackendHealthCheck';

export const Route = createFileRoute('/test-connexion')({
  component: TestConnexionPage,
});

function TestConnexionPage() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#f1f5f9' }}>
      <h1 style={{ fontFamily: 'sans-serif', fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem', color: '#1e293b' }}>
        Page de test - Connexion Frontend / Backend
      </h1>
      <BackendHealthCheck />
      <p style={{ marginTop: '1rem', fontSize: '0.75rem', color: '#94a3b8', fontFamily: 'sans-serif' }}>
        Cette page est temporaire
      </p>
    </div>
  );
}
