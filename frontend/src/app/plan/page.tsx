'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { briefing as briefingApi, tasks as taskApi } from '@/lib/api';
import type { Briefing, Task } from '@/types';
import styles from './Plan.module.css';

export default function PlanPage() {
  const [data, setData] = useState<Briefing | null>(null);
  const [loading, setLoading] = useState(true);
  const [completedCount, setCompletedCount] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const briefing = await briefingApi.today();
      setData(briefing);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not fetch today’s briefing');
    } finally {
      setLoading(false);
    }
  }

  async function markDone(id: string) {
    try {
      await taskApi.update(id, { status: 'DONE' } as Partial<Task>);
      setData((prev) =>
        prev
          ? {
              ...prev,
              openTasks: prev.openTasks.filter((t) => t.id !== id),
              totalOpen: Math.max(0, prev.totalOpen - 1),
              highPriority:
                prev.openTasks.find((t) => t.id === id)?.priority === 'HIGH'
                  ? Math.max(0, prev.highPriority - 1)
                  : prev.highPriority,
            }
          : prev
      );
      setCompletedCount((c) => c + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update task');
    }
  }

  if (loading) {
    return (
      <div className="empty-state" style={{ paddingTop: 90 }}>
        <div className="spinner" style={{ margin: '0 auto 16px' }} />
        <p className="text-secondary">Loading your daily briefing…</p>
      </div>
    );
  }

  if (error && !data) {
    return (
      <div className="empty-state" style={{ paddingTop: 80 }}>
        <span className="empty-state-icon">⚠️</span>
        <p className="empty-state-title">Unable to connect</p>
        <p className="text-muted mb-4">{error}</p>
        <button className="btn btn-primary btn-sm" onClick={load}>
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div>
      {/* Hero Header */}
      <div className={styles.hero}>
        <div className={styles.heroOrb} />
        <p className={styles.heroDate}>{data?.date || 'Today'}</p>
        <h1 className={styles.heroGreeting}>{data?.greeting || 'Welcome back'} 👋</h1>
        <p className={styles.heroSub}>Here is your personalized focus plan for today.</p>

        <div className={styles.stats}>
          <div className={styles.stat}>
            <span className={styles.statNum}>{data?.totalOpen ?? 0}</span>
            <span className={styles.statLabel}>Open Tasks</span>
          </div>
          <div className={styles.statDiv} />
          <div className={styles.stat}>
            <span className={`${styles.statNum} ${(data?.highPriority ?? 0) > 0 ? styles.red : ''}`}>
              {data?.highPriority ?? 0}
            </span>
            <span className={styles.statLabel}>High Priority</span>
          </div>
          <div className={styles.statDiv} />
          <div className={styles.stat}>
            <span className={styles.statNum} style={{ color: 'var(--done)' }}>
              {completedCount}
            </span>
            <span className={styles.statLabel}>Completed</span>
          </div>
        </div>
      </div>

      {/* Task list */}
      <div className={styles.section}>
        <div className="flex items-center justify-between mb-3">
          <h2 className={styles.sectionTitle} style={{ marginBottom: 0 }}>
            Today&apos;s Focus
          </h2>
          <Link href="/tasks" className="text-xs text-secondary hover:text-white">
            View All Tasks →
          </Link>
        </div>

        {data?.openTasks.length === 0 ? (
          <div className="empty-state card">
            <span className="empty-state-icon">🎉</span>
            <p className="empty-state-title">All tasks cleared!</p>
            <p className="text-secondary text-sm">No pending tasks for today. You&apos;re completely up to date.</p>
          </div>
        ) : (
          data?.openTasks.map((task, i) => (
            <div
              key={task.id}
              className={`card ${styles.taskRow} animate-in`}
              style={{ animationDelay: `${i * 0.04}s` }}
            >
              <div className={styles.taskLeft}>
                <span className={`badge badge-${task.priority.toLowerCase()}`}>
                  {task.priority}
                </span>
                <div>
                  <p className={styles.taskTitle}>{task.title}</p>
                  {task.description && (
                    <p className="text-xs text-muted mt-1">{task.description}</p>
                  )}
                </div>
              </div>
              <button
                id={`plan-done-${task.id}`}
                className={`btn btn-ghost btn-sm ${styles.doneBtn}`}
                onClick={() => markDone(task.id)}
              >
                Done ✓
              </button>
            </div>
          ))
        )}
      </div>

      <div className={styles.quickActions}>
        <button id="refresh-plan" className="btn btn-ghost flex-1" onClick={load}>
          ↻ Refresh Plan
        </button>
        <Link href="/chat" className="btn btn-primary flex-1">
          ✦ Ask Neura to Plan
        </Link>
      </div>
    </div>
  );
}
