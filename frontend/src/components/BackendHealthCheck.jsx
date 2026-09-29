import { useEffect, useState } from 'react';

/**
 * Composant de test minimaliste pour vérifier la connexion Frontend → Backend.
 * Appelle directement /actuator/health (hors préfixe /api) via fetch natif.
 * À supprimer une fois la connexion validée.
 */
export function BackendHealthCheck() {
  const [status, setStatus] = useState('En cours...');
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch('/actuator/health')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((json) => {
        setData(json);
        setStatus('Connexion réussie !');
      })
      .catch((err) => {
        setError(err.message || 'Erreur inconnue');
        setStatus('Connexion échouée');
      });
  }, []);

  return (
    <div style={{
      margin: '2rem auto',
      maxWidth: '500px',
      padding: '1.5rem',
      border: '1px solid #e2e8f0',
      borderRadius: '8px',
      fontFamily: 'monospace',
      backgroundColor: '#f8fafc',
    }}>
      <h2 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 'bold' }}>
        Test Connexion Backend
      </h2>

      <p style={{ marginBottom: '0.5rem' }}>
        <strong>Statut :</strong>{' '}
        <span style={{ color: data ? '#16a34a' : error ? '#dc2626' : '#2563eb', fontWeight: 'bold' }}>
          {status}
        </span>
      </p>
      
      {error && (
        <pre style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: '0.75rem',
          borderRadius: '6px',
          fontSize: '0.85rem',
          overflowX: 'auto',
          border: '1px solid #fecaca',
          marginTop: '0.5rem',
        }}>
          {error}
        </pre>
      )}
    </div>
  );
}
