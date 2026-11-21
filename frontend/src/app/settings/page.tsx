'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getAuth, clearAuth } from '@/store/auth';
import styles from './Settings.module.css';

export default function SettingsPage() {
  const router = useRouter();
  const [authData, setAuthData] = useState<{ email: string | null; userId: string | null }>({
    email: null,
    userId: null,
  });

  useEffect(() => {
    const auth = getAuth();
    setAuthData({ email: auth.email, userId: auth.userId });
  }, []);

  function handleLogout() {
    if (confirm('Are you sure you want to sign out?')) {
      clearAuth();
      router.replace('/login');
    }
  }

  return (
    <div>
      <div className={styles.header}>
        <h1 className="section-header">Settings</h1>
        <p className="section-sub">Manage your preferences, backend connection, and profile</p>
      </div>

      {/* User Profile */}
      <div className={`card ${styles.section}`}>
        <h2 className={styles.sectionTitle}>
          <span>👤</span> User Account
        </h2>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Email Address</span>
          <span className={styles.itemValue}>{authData.email || 'Not logged in'}</span>
        </div>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>User ID</span>
          <span className="text-xs font-mono text-muted">{authData.userId || '—'}</span>
        </div>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Default Timezone</span>
          <span className={styles.itemValue}>
            {Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata'}
          </span>
        </div>
      </div>

      {/* Backend & AI Infrastructure */}
      <div className={`card ${styles.section}`}>
        <h2 className={styles.sectionTitle}>
          <span>⚡</span> System Architecture
        </h2>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Backend API</span>
          <span className="badge badge-done">Online (Port 8080)</span>
        </div>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>API Endpoint</span>
          <span className="text-xs font-mono text-muted">
            {process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}
          </span>
        </div>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Local LLM Engine</span>
          <span className="badge badge-todo">Ollama (qwen2.5:7b)</span>
        </div>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Vector Embeddings</span>
          <span className="badge badge-low">pgvector (768-dim)</span>
        </div>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Idempotency Engine</span>
          <span className="badge badge-executed">Active (UUID v4)</span>
        </div>
      </div>

      {/* Privacy & Security */}
      <div className={`card ${styles.section}`}>
        <h2 className={styles.sectionTitle}>
          <span>🛡️</span> Privacy & Local First
        </h2>
        <p className="text-sm text-secondary mb-3">
          Your personal assistant runs 100% locally. Chat messages, memories, and actions are
          stored in your own PostgreSQL instance and processed by your local Ollama model.
        </p>
        <div className={styles.itemRow}>
          <span className={styles.itemLabel}>Data Transmission</span>
          <span className="badge badge-low">Localhost Only</span>
        </div>
      </div>

      {/* Danger Zone / Logout */}
      <div className={`card ${styles.dangerSection}`}>
        <h2 className={styles.sectionTitle} style={{ color: 'var(--high)' }}>
          <span>🚪</span> Session
        </h2>
        <p className="text-sm text-secondary mb-4">
          Sign out of your local session. Your data remains safely stored in PostgreSQL.
        </p>
        <button id="logout-btn" className="btn btn-reject w-full" onClick={handleLogout}>
          Sign Out of Neura
        </button>
      </div>
    </div>
  );
}
