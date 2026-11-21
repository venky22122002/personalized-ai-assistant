'use client';

import { useState, useEffect } from 'react';
import { actions as actionApi } from '@/lib/api';
import type { PendingAction } from '@/types';
import styles from './Approvals.module.css';

export default function ApprovalsPage() {
  const [actionList, setActionList] = useState<PendingAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'pending' | 'all'>('pending');
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    load();
  }, [filter]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await actionApi.list(filter === 'pending');
      setActionList(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load actions');
    } finally {
      setLoading(false);
    }
  }

  async function handleApprove(id: string) {
    setProcessingId(id);
    try {
      const updated = await actionApi.approve(id);
      setActionList((prev) =>
        filter === 'pending'
          ? prev.filter((a) => a.id !== id)
          : prev.map((a) => (a.id === id ? updated : a))
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Approval failed');
    } finally {
      setProcessingId(null);
    }
  }

  async function handleReject(id: string) {
    setProcessingId(id);
    try {
      const updated = await actionApi.reject(id);
      setActionList((prev) =>
        filter === 'pending'
          ? prev.filter((a) => a.id !== id)
          : prev.map((a) => (a.id === id ? updated : a))
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Rejection failed');
    } finally {
      setProcessingId(null);
    }
  }

  function formatActionType(type: string) {
    switch (type) {
      case 'CREATE_TASK':
        return 'Create Task';
      case 'UPDATE_TASK':
        return 'Update Task';
      case 'COMPLETE_TASK':
        return 'Complete Task';
      default:
        return type;
    }
  }

  return (
    <div>
      <div className={styles.header}>
        <div>
          <h1 className="section-header">Action Approvals</h1>
          <p className="section-sub">
            Review and authorize actions requested by Neura AI
          </p>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>
          ↻ Refresh
        </button>
      </div>

      {/* Tabs */}
      <div className={styles.tabs}>
        <button
          className={`${styles.tabBtn} ${filter === 'pending' ? styles.active : ''}`}
          onClick={() => setFilter('pending')}
        >
          Pending Review (
          {filter === 'pending'
            ? actionList.length
            : actionList.filter((a) => a.status === 'PENDING').length}
          )
        </button>
        <button
          className={`${styles.tabBtn} ${filter === 'all' ? styles.active : ''}`}
          onClick={() => setFilter('all')}
        >
          All Activity
        </button>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {loading ? (
        <div className="empty-state">
          <div className="spinner" style={{ margin: 'auto' }} />
        </div>
      ) : actionList.length === 0 ? (
        <div className="empty-state card">
          <span className="empty-state-icon">🛡️</span>
          <p className="empty-state-title">
            {filter === 'pending' ? 'No pending approvals' : 'No action history yet'}
          </p>
          <p className="text-secondary text-sm">
            {filter === 'pending'
              ? 'When Neura proposes to create, update, or complete a task from chat, it will show up here for your verification.'
              : 'Actions proposed by the AI assistant will be logged here.'}
          </p>
        </div>
      ) : (
        actionList.map((action, i) => (
          <div
            key={action.id}
            className={`card ${styles.actionCard} animate-in`}
            style={{ animationDelay: `${i * 0.03}s` }}
          >
            <div className={styles.cardTop}>
              <div className={styles.typeBadgeWrap}>
                <span className="badge badge-todo">{formatActionType(action.actionType)}</span>
                <span className={`badge badge-${action.status.toLowerCase()}`}>
                  {action.status}
                </span>
              </div>
              <span className="text-xs text-muted">
                {new Date(action.createdAt).toLocaleTimeString([], {
                  hour: '2-digit',
                  minute: '2-digit',
                })}{' '}
                · {new Date(action.createdAt).toLocaleDateString()}
              </span>
            </div>

            {/* Payload info box */}
            <div className={styles.payloadBox}>
              {action.payload?.title && (
                <div className={styles.payloadRow}>
                  <span className={styles.payloadLabel}>Title:</span>
                  <span className={styles.payloadValue} style={{ fontWeight: 600 }}>
                    {String(action.payload.title)}
                  </span>
                </div>
              )}
              {action.payload?.description && (
                <div className={styles.payloadRow}>
                  <span className={styles.payloadLabel}>Details:</span>
                  <span className={styles.payloadValue}>
                    {String(action.payload.description)}
                  </span>
                </div>
              )}
              {action.payload?.priority && (
                <div className={styles.payloadRow}>
                  <span className={styles.payloadLabel}>Priority:</span>
                  <span
                    className={`badge badge-${String(action.payload.priority).toLowerCase()}`}
                  >
                    {String(action.payload.priority)}
                  </span>
                </div>
              )}
              {action.payload?.dueAt && (
                <div className={styles.payloadRow}>
                  <span className={styles.payloadLabel}>Deadline:</span>
                  <span className={styles.payloadValue}>
                    {new Date(String(action.payload.dueAt)).toLocaleString()}
                  </span>
                </div>
              )}
              {action.payload?.taskId && (
                <div className={styles.payloadRow}>
                  <span className={styles.payloadLabel}>Task ID:</span>
                  <span className="text-xs text-muted font-mono">
                    {String(action.payload.taskId)}
                  </span>
                </div>
              )}
            </div>

            {/* Execution Result */}
            {action.result && (
              <div className={styles.resultBox}>
                <span>✓ {action.result}</span>
              </div>
            )}

            {/* Bottom Actions */}
            <div className={styles.cardBottom}>
              <span className="text-xs text-muted">
                {action.status === 'PENDING' ? (
                  <span>
                    Expires: {new Date(action.expiresAt).toLocaleTimeString()}
                  </span>
                ) : (
                  <span>ID: {action.id.slice(0, 8)}…</span>
                )}
              </span>

              {action.status === 'PENDING' && (
                <div className={styles.btnGroup}>
                  <button
                    id={`reject-action-${action.id}`}
                    className="btn btn-reject btn-sm"
                    onClick={() => handleReject(action.id)}
                    disabled={processingId === action.id}
                  >
                    Reject
                  </button>
                  <button
                    id={`approve-action-${action.id}`}
                    className="btn btn-approve btn-sm"
                    onClick={() => handleApprove(action.id)}
                    disabled={processingId === action.id}
                  >
                    {processingId === action.id ? (
                      <span className="spinner" style={{ width: 14, height: 14 }} />
                    ) : (
                      'Approve ✓'
                    )}
                  </button>
                </div>
              )}
            </div>
          </div>
        ))
      )}
    </div>
  );
}
