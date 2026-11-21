'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { auth } from '@/lib/api';
import { setAuth } from '@/store/auth';
import styles from './Login.module.css';

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res =
        mode === 'login'
          ? await auth.login(email.trim(), password)
          : await auth.register(email.trim(), password);

      setAuth(res.token, res.email, res.userId);
      router.push('/chat');
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : mode === 'login'
          ? 'Invalid email or password'
          : 'Registration failed. Password must be at least 8 characters.'
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.container}>
      {/* Ambient background glows */}
      <div className={styles.orb1} />
      <div className={styles.orb2} />

      <div className={styles.card}>
        <div className={styles.logo}>
          <span className={styles.logoIcon}>✦</span>
          <span className={styles.logoText}>Neura AI</span>
        </div>

        <h1 className={styles.title}>
          {mode === 'login' ? 'Welcome back' : 'Create account'}
        </h1>
        <p className={styles.sub}>
          {mode === 'login'
            ? 'Access your private, local personal assistant'
            : 'Own your AI assistant. 100% private & local.'}
        </p>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label}>Email Address</label>
            <input
              id="email"
              type="email"
              className="input-field"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              autoFocus
            />
          </div>

          <div className={styles.field}>
            <div className="flex justify-between items-center">
              <label className={styles.label}>Password</label>
              <button
                type="button"
                className="text-xs text-muted hover:text-white"
                onClick={() => setShowPassword((s) => !s)}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              className="input-field"
              placeholder={mode === 'register' ? 'Minimum 8 characters' : '••••••••'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={mode === 'register' ? 8 : 1}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </div>

          {error && <div className={styles.error}>{error}</div>}

          <button
            id="auth-submit"
            type="submit"
            className="btn btn-primary w-full mt-2"
            disabled={loading}
          >
            {loading ? (
              <span className="spinner" />
            ) : mode === 'login' ? (
              'Sign In'
            ) : (
              'Create Account'
            )}
          </button>
        </form>

        <p className={styles.toggle}>
          {mode === 'login' ? "Don't have an account? " : 'Already have an account? '}
          <button
            id="toggle-mode"
            type="button"
            onClick={() => {
              setMode((m) => (m === 'login' ? 'register' : 'login'));
              setError('');
            }}
            className={styles.toggleBtn}
          >
            {mode === 'login' ? 'Register now' : 'Sign in'}
          </button>
        </p>
      </div>
    </div>
  );
}
