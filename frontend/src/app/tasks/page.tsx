'use client';

import { useState, useEffect } from 'react';
import { tasks as taskApi } from '@/lib/api';
import type { Task } from '@/types';
import styles from './Tasks.module.css';

const PRIORITY_ORDER = { HIGH: 0, MEDIUM: 1, LOW: 2 };

export default function TasksPage() {
  const [taskList, setTaskList] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [filter, setFilter] = useState<'open' | 'in_progress' | 'completed' | 'all'>('open');
  const [error, setError] = useState('');

  // Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [dueDate, setDueDate] = useState('');

  useEffect(() => {
    load();
  }, [filter]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await taskApi.list(filter !== 'all' && filter !== 'completed');
      setTaskList(
        data.sort(
          (a, b) =>
            (PRIORITY_ORDER[a.priority as keyof typeof PRIORITY_ORDER] ?? 1) -
            (PRIORITY_ORDER[b.priority as keyof typeof PRIORITY_ORDER] ?? 1)
        )
      );
    } catch {
      setError('Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }

  async function create(e?: React.FormEvent) {
    if (e) e.preventDefault();
    if (!title.trim()) return;

    setCreating(true);
    setError('');
    try {
      const dueInstant = dueDate ? new Date(dueDate).toISOString() : undefined;
      const t = await taskApi.create(
        title.trim(),
        description || undefined,
        priority,
        dueInstant
      );
      setTaskList((prev) => [t, ...prev]);
      setTitle('');
      setDescription('');
      setPriority('MEDIUM');
      setDueDate('');
      setShowForm(false);
    } catch {
      setError('Failed to create task');
    } finally {
      setCreating(false);
    }
  }

  async function updateStatus(id: string, status: string) {
    try {
      const updated = await taskApi.update(id, { status } as Partial<Task>);
      setTaskList((prev) => prev.map((t) => (t.id === id ? updated : t)));
    } catch {
      setError('Failed to update task status');
    }
  }

  // Filter tasks locally based on active tab
  const displayedTasks = taskList.filter((t) => {
    if (filter === 'open') return t.status === 'TODO' || t.status === 'IN_PROGRESS';
    if (filter === 'in_progress') return t.status === 'IN_PROGRESS';
    if (filter === 'completed') return t.status === 'DONE';
    return true; // 'all'
  });

  const openCount = taskList.filter((t) => t.status === 'TODO' || t.status === 'IN_PROGRESS').length;

  return (
    <div>
      <div className={styles.header}>
        <div>
          <h1 className="section-header">Tasks</h1>
          <p className="section-sub">{openCount} active tasks to accomplish</p>
        </div>
        <button
          id="new-task"
          className="btn btn-primary btn-sm"
          onClick={() => setShowForm((s) => !s)}
        >
          {showForm ? '✕ Cancel' : '+ Add Task'}
        </button>
      </div>

      {/* Create Task Form */}
      {showForm && (
        <form onSubmit={create} className={`card p-4 mb-4 animate-in ${styles.form}`}>
          <h3 className="text-sm font-semibold">New Task</h3>
          <input
            id="task-title"
            className="input-field"
            placeholder="What needs to be done? *"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            autoFocus
          />
          <textarea
            id="task-desc"
            className="input-field"
            placeholder="Add additional notes or details (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
          />

          <div className={styles.formGrid}>
            <div>
              <label className="text-xs text-secondary font-semibold mb-1 block">
                Priority
              </label>
              <select
                id="task-priority"
                className="input-field"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
              >
                <option value="HIGH">🔴 High Priority</option>
                <option value="MEDIUM">🟡 Medium Priority</option>
                <option value="LOW">🟢 Low Priority</option>
              </select>
            </div>

            <div>
              <label className="text-xs text-secondary font-semibold mb-1 block">
                Due Date & Time
              </label>
              <input
                type="datetime-local"
                className="input-field"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
              />
            </div>
          </div>

          <div className="flex justify-between items-center mt-2">
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => setShowForm(false)}
            >
              Cancel
            </button>
            <button
              id="create-task-submit"
              type="submit"
              className="btn btn-primary btn-sm"
              disabled={creating || !title.trim()}
            >
              {creating ? <span className="spinner" style={{ width: 14, height: 14 }} /> : 'Create Task'}
            </button>
          </div>
        </form>
      )}

      {/* Filter Tabs */}
      <div className={styles.filters}>
        {(
          [
            { id: 'open', label: 'Open' },
            { id: 'in_progress', label: 'In Progress' },
            { id: 'completed', label: 'Completed' },
            { id: 'all', label: 'All' },
          ] as const
        ).map((f) => (
          <button
            key={f.id}
            id={`filter-${f.id}`}
            className={`${styles.filterBtn} ${filter === f.id ? styles.active : ''}`}
            onClick={() => setFilter(f.id)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {loading ? (
        <div className="empty-state">
          <div className="spinner" style={{ margin: 'auto' }} />
        </div>
      ) : displayedTasks.length === 0 ? (
        <div className="empty-state card">
          <span className="empty-state-icon">✅</span>
          <p className="empty-state-title">No tasks in this view</p>
          <p className="text-secondary text-sm">
            {filter === 'completed'
              ? 'Completed tasks will appear here.'
              : 'Add a new task or ask Neura to organize your goals!'}
          </p>
        </div>
      ) : (
        displayedTasks.map((task, i) => (
          <div
            key={task.id}
            className={`card ${styles.taskCard} ${
              task.status === 'DONE' ? styles.done : ''
            } animate-in`}
            style={{ animationDelay: `${i * 0.03}s` }}
          >
            <div className={styles.taskMain}>
              <button
                id={`toggle-task-${task.id}`}
                className={styles.checkbox}
                onClick={() =>
                  updateStatus(task.id, task.status === 'DONE' ? 'TODO' : 'DONE')
                }
                aria-label={task.status === 'DONE' ? 'Mark incomplete' : 'Mark complete'}
              >
                {task.status === 'DONE' ? '✓' : ''}
              </button>
              <div className={styles.taskText}>
                <p
                  className={`${styles.taskTitle} ${
                    task.status === 'DONE' ? styles.strikethrough : ''
                  }`}
                >
                  {task.title}
                </p>
                {task.description && (
                  <p className="text-xs text-muted mt-1">{task.description}</p>
                )}
              </div>
            </div>

            <div className={styles.taskMeta}>
              <div className={styles.metaLeft}>
                <span className={`badge badge-${task.priority.toLowerCase()}`}>
                  {task.priority}
                </span>
                <span
                  className={`badge badge-${task.status.toLowerCase().replace('_', '-')}`}
                >
                  {task.status.replace('_', ' ')}
                </span>
                {task.dueAt && (
                  <span className="text-xs text-muted">
                    Due {new Date(task.dueAt).toLocaleDateString()}{' '}
                    {new Date(task.dueAt).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                )}
              </div>

              <div className={styles.statusActions}>
                {task.status === 'TODO' && (
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ padding: '2px 8px', fontSize: '0.72rem' }}
                    onClick={() => updateStatus(task.id, 'IN_PROGRESS')}
                  >
                    Start →
                  </button>
                )}
                {task.status === 'IN_PROGRESS' && (
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{
                      padding: '2px 8px',
                      fontSize: '0.72rem',
                      color: 'var(--done)',
                      borderColor: 'rgba(107,203,119,0.3)',
                    }}
                    onClick={() => updateStatus(task.id, 'DONE')}
                  >
                    Done ✓
                  </button>
                )}
                {task.status !== 'CANCELLED' && task.status !== 'DONE' && (
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ padding: '2px 8px', fontSize: '0.72rem', color: 'var(--text-muted)' }}
                    onClick={() => updateStatus(task.id, 'CANCELLED')}
                    title="Cancel task"
                  >
                    ✕
                  </button>
                )}
              </div>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
