// API client — communicates with the Spring Boot backend
import type { Task, Memory, PendingAction, Briefing, ChatResponse, Message, Conversation } from '@/types';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('ai_token');
}

function generateUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...((options.headers as Record<string, string>) || {}),
  };

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('ai_token');
      localStorage.removeItem('ai_email');
      localStorage.removeItem('ai_userId');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.message || err.error || `HTTP ${res.status}`);
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}

// ── Auth ─────────────────────────────────────────────────────────
export const auth = {
  register: (email: string, password: string, timezone?: string) =>
    request<{ token: string; email: string; userId: string }>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        email,
        password,
        timezone: timezone || Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata',
      }),
    }),

  login: (email: string, password: string) =>
    request<{ token: string; email: string; userId: string }>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
};

// ── Chat ─────────────────────────────────────────────────────────
export const chat = {
  send: (message: string, conversationId?: string, idempotencyKey?: string) => {
    const key = idempotencyKey || generateUUID();
    return request<ChatResponse>('/api/v1/chat', {
      method: 'POST',
      headers: {
        'Idempotency-Key': key,
      },
      body: JSON.stringify({
        message,
        conversationId: conversationId || null,
      }),
    });
  },

  listConversations: () =>
    request<Conversation[]>('/api/v1/conversations'),

  getMessages: (conversationId: string) =>
    request<Message[]>(`/api/v1/conversations/${conversationId}/messages`),
};

// ── Tasks ────────────────────────────────────────────────────────
export const tasks = {
  create: (title: string, description?: string, priority?: string, dueAt?: string) =>
    request<Task>('/api/v1/tasks', {
      method: 'POST',
      body: JSON.stringify({
        title,
        description: description || null,
        priority: priority || 'MEDIUM',
        dueAt: dueAt || null,
      }),
    }),

  list: (openOnly = true) =>
    request<Task[]>(`/api/v1/tasks?openOnly=${openOnly}`),

  update: (id: string, patch: Partial<Task>) =>
    request<Task>(`/api/v1/tasks/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(patch),
    }),
};

// ── Memories ─────────────────────────────────────────────────────
export const memories = {
  create: (category: string, content: string, source = 'USER', importance = 3) =>
    request<Memory>('/api/v1/memories', {
      method: 'POST',
      body: JSON.stringify({ category, content, source, importance }),
    }),

  list: (category?: string) =>
    request<Memory[]>(
      category ? `/api/v1/memories?category=${encodeURIComponent(category)}` : '/api/v1/memories'
    ),

  delete: (id: string) =>
    request<void>(`/api/v1/memories/${id}`, { method: 'DELETE' }),
};

// ── Actions (Human-in-the-loop Approvals) ─────────────────────────
export const actions = {
  list: (pendingOnly = true) =>
    request<PendingAction[]>(`/api/v1/actions?pendingOnly=${pendingOnly}`),

  approve: (id: string) =>
    request<PendingAction>(`/api/v1/actions/${id}/approve`, { method: 'POST' }),

  reject: (id: string) =>
    request<PendingAction>(`/api/v1/actions/${id}/reject`, { method: 'POST' }),

  propose: (actionType: string, payload: Record<string, unknown>) =>
    request<PendingAction>('/api/v1/actions/propose', {
      method: 'POST',
      body: JSON.stringify({ actionType, payload }),
    }),
};

// ── Daily Briefing / Planner ──────────────────────────────────────
export const briefing = {
  today: () => request<Briefing>('/api/v1/briefing/today'),
};
