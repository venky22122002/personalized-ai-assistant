'use client';

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { chat as chatApi } from '@/lib/api';
import type { Conversation } from '@/types';
import styles from './Chat.module.css';

interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  proposedActions?: string[];
  createdAt: string;
}

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [showHistory, setShowHistory] = useState(false);
  const [error, setError] = useState('');

  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  useEffect(() => {
    loadConversations();
  }, []);

  async function loadConversations() {
    try {
      const list = await chatApi.listConversations();
      setConversations(list);
    } catch {
      // ignore
    }
  }

  async function selectConversation(id: string) {
    setConversationId(id);
    setShowHistory(false);
    setLoading(true);
    setError('');
    try {
      const history = await chatApi.getMessages(id);
      setMessages(
        history.map((m) => ({
          id: m.id,
          role: m.role as 'USER' | 'ASSISTANT',
          content: m.content,
          createdAt: m.createdAt,
        }))
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load conversation');
    } finally {
      setLoading(false);
    }
  }

  function startNewChat() {
    setMessages([]);
    setConversationId(undefined);
    setError('');
    setShowHistory(false);
    inputRef.current?.focus();
  }

  async function send() {
    const text = input.trim();
    if (!text || loading) return;

    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'USER',
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);
    setError('');

    try {
      const res = await chatApi.send(text, conversationId);
      if (!conversationId && res.conversationId) {
        setConversationId(res.conversationId);
        loadConversations();
      }

      const aiMsg: Message = {
        id: Date.now().toString() + '-ai',
        role: 'ASSISTANT',
        content: res.reply,
        proposedActions: res.proposedActions,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to get response');
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  return (
    <div className={styles.page}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => setShowHistory((s) => !s)}
            title="Chat History"
          >
            ☰ History
          </button>
          <div>
            <h1 className={styles.headerTitle}>Neura AI</h1>
            <p className={styles.headerSub}>Personal · Private · Local</p>
          </div>
        </div>
        <div className={styles.headerRight}>
          <button id="new-chat" className="btn btn-ghost btn-sm" onClick={startNewChat}>
            + New Chat
          </button>
        </div>
      </header>

      {/* History Drawer */}
      {showHistory && (
        <>
          <div className={styles.drawerBackdrop} onClick={() => setShowHistory(false)} />
          <div className={styles.historyDrawer}>
            <div className={styles.drawerHeader}>
              <h2 className={styles.drawerTitle}>Conversations</h2>
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => setShowHistory(false)}
              >
                ✕
              </button>
            </div>
            <button
              className="btn btn-primary btn-sm mb-3"
              onClick={startNewChat}
            >
              + Start Fresh Chat
            </button>
            <div className={styles.historyList}>
              {conversations.length === 0 ? (
                <p className="text-xs text-muted text-center py-4">
                  No previous conversations
                </p>
              ) : (
                conversations.map((c) => (
                  <button
                    key={c.id}
                    className={`${styles.historyItem} ${
                      conversationId === c.id ? styles.active : ''
                    }`}
                    onClick={() => selectConversation(c.id)}
                  >
                    <span className={styles.historyItemTitle}>
                      {c.title || 'Untitled Conversation'}
                    </span>
                    <span className={styles.historyItemDate}>
                      {new Date(c.createdAt).toLocaleDateString()}
                    </span>
                  </button>
                ))
              )}
            </div>
          </div>
        </>
      )}

      {/* Messages */}
      <div className={styles.messages}>
        {messages.length === 0 && (
          <div className={styles.welcome}>
            <div className={styles.welcomeOrb} />
            <span className={styles.welcomeIcon}>✦</span>
            <h2 className={styles.welcomeTitle}>Good day!</h2>
            <p className={styles.welcomeSub}>
              Ask me anything — I am running locally with access to your personalized memory.
            </p>
            <div className={styles.suggestions}>
              {[
                'Help me plan my highest priority tasks for today',
                'Create a task to finish the backend API integration',
                'What are my current goals and preferences?',
              ].map((s) => (
                <button
                  key={s}
                  className={styles.suggestion}
                  onClick={() => {
                    setInput(s);
                    inputRef.current?.focus();
                  }}
                >
                  <span>{s}</span>
                  <span style={{ color: 'var(--accent)' }}>→</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => (
          <div
            key={m.id}
            className={`${m.role === 'USER' ? 'bubble-user' : 'bubble-assistant'} animate-in`}
            style={{ animationDelay: `${i * 0.02}s` }}
          >
            {m.role === 'ASSISTANT' && (
              <span className={styles.aiBadge}>✦ Neura</span>
            )}
            <div className={styles.msgContent}>{m.content}</div>

            {/* Action proposal alert if returned */}
            {m.proposedActions && m.proposedActions.length > 0 && (
              <div className={styles.proposalCard}>
                <div>
                  <p className={styles.proposalText}>🛡️ Action Proposal Created</p>
                  <p className="text-xs text-muted">
                    This action requires your confirmation before execution.
                  </p>
                </div>
                <Link href="/approvals" className="btn btn-primary btn-sm">
                  Review in Approvals →
                </Link>
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="bubble-assistant animate-in">
            <span className={styles.aiBadge}>✦ Neura is thinking</span>
            <div className="typing-indicator">
              <div className="typing-dot" />
              <div className="typing-dot" />
              <div className="typing-dot" />
            </div>
          </div>
        )}

        {error && <div className={styles.errorBanner}>{error}</div>}

        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className={styles.inputBar}>
        <div className={styles.inputWrap}>
          <textarea
            id="chat-input"
            ref={inputRef}
            className={styles.textarea}
            placeholder="Message Neura…"
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              e.target.style.height = 'auto';
              e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px';
            }}
            onKeyDown={handleKeyDown}
            rows={1}
          />
          <button
            id="chat-send"
            className={styles.sendBtn}
            onClick={send}
            disabled={!input.trim() || loading}
          >
            {loading ? (
              <span className="spinner" style={{ width: 16, height: 16 }} />
            ) : (
              '▲'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
