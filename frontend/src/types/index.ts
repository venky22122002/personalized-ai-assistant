// Types shared across the frontend matching backend API DTOs

export interface User {
  userId: string;
  email: string;
  token: string;
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
}

export interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

export interface ChatResponse {
  conversationId: string;
  reply: string;
  proposedActions: string[];
}

export interface Memory {
  id: string;
  category: 'PREFERENCE' | 'GOAL' | 'FACT' | 'SKILL' | 'PROJECT' | 'HABIT' | 'NOTE' | 'OTHER' | string;
  content: string;
  source: string;
  importance: number;
  createdAt: string;
}

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PendingAction {
  id: string;
  actionType: 'CREATE_TASK' | 'COMPLETE_TASK' | 'UPDATE_TASK' | string;
  payload: Record<string, unknown>;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'FAILED' | 'EXPIRED';
  expiresAt: string;
  result: string | null;
  createdAt: string;
}

export interface Briefing {
  date: string;
  greeting: string;
  openTasks: Task[];
  totalOpen: number;
  highPriority: number;
}
