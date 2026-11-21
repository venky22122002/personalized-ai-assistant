'use client';

// Thin auth store using localStorage — replace with a proper
// state library (Zustand, Jotai) when complexity warrants it.

export interface AuthState {
  token: string | null;
  email: string | null;
  userId: string | null;
}

export function getAuth(): AuthState {
  if (typeof window === 'undefined') return { token: null, email: null, userId: null };
  return {
    token: localStorage.getItem('ai_token'),
    email: localStorage.getItem('ai_email'),
    userId: localStorage.getItem('ai_userId'),
  };
}

export function setAuth(token: string, email: string, userId: string) {
  localStorage.setItem('ai_token', token);
  localStorage.setItem('ai_email', email);
  localStorage.setItem('ai_userId', userId);
}

export function clearAuth() {
  localStorage.removeItem('ai_token');
  localStorage.removeItem('ai_email');
  localStorage.removeItem('ai_userId');
}

export function isAuthenticated(): boolean {
  return !!localStorage.getItem('ai_token');
}
