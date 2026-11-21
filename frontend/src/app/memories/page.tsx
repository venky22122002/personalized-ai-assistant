'use client';

import { useState, useEffect } from 'react';
import { memories as memoryApi } from '@/lib/api';
import type { Memory } from '@/types';
import styles from './Memories.module.css';

const CATEGORIES = [
  'ALL',
  'PREFERENCE',
  'GOAL',
  'FACT',
  'SKILL',
  'PROJECT',
  'HABIT',
  'NOTE',
  'OTHER',
] as const;

export default function MemoriesPage() {
  const [memoryList, setMemoryList] = useState<Memory[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // Form State
  const [category, setCategory] = useState<string>('PREFERENCE');
  const [content, setContent] = useState('');
  const [importance, setImportance] = useState<number>(3);

  useEffect(() => {
    load();
  }, [selectedCategory]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await memoryApi.list(
        selectedCategory === 'ALL' ? undefined : selectedCategory
      );
      setMemoryList(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load memories');
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;

    setSaving(true);
    setError('');
    try {
      const newMemory = await memoryApi.create(category, content.trim(), 'USER', importance);
      setMemoryList((prev) => [newMemory, ...prev]);
      setContent('');
      setImportance(3);
      setShowForm(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save memory');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Are you sure you want to remove this memory?')) return;
    try {
      await memoryApi.delete(id);
      setMemoryList((prev) => prev.filter((m) => m.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete memory');
    }
  }

  const filteredMemories = memoryList.filter((m) =>
    searchQuery ? m.content.toLowerCase().includes(searchQuery.toLowerCase()) : true
  );

  return (
    <div>
      <div className={styles.header}>
        <div>
          <h1 className="section-header">Personal Memory</h1>
          <p className="section-sub">
            Knowledge Neura remembers to personalize your context
          </p>
        </div>
        <button
          className="btn btn-primary btn-sm"
          onClick={() => setShowForm((s) => !s)}
        >
          {showForm ? '✕ Close' : '+ Add Memory'}
        </button>
      </div>

      {/* Info Banner */}
      <div className={styles.infoBanner}>
        <span style={{ fontSize: '1.4rem' }}>🧠</span>
        <div>
          <strong style={{ color: 'var(--text-primary)' }}>Semantic Retrieval</strong>
          <p style={{ marginTop: 2 }}>
            Memories are automatically embedded and used by the backend to personalize
            responses when you chat with Neura.
          </p>
        </div>
      </div>

      {/* Add Memory Form */}
      {showForm && (
        <form onSubmit={handleCreate} className={`card ${styles.formCard} animate-in`}>
          <h3 className="text-sm font-semibold text-primary">Add New Memory</h3>
          <textarea
            className="input-field"
            placeholder="What should Neura remember? (e.g. 'I prefer writing backend APIs in Java 21 with Spring Boot')."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={3}
            required
            autoFocus
          />

          <div className={styles.formGrid}>
            <div>
              <label className="text-xs text-secondary font-semibold mb-1 block">
                Category
              </label>
              <select
                className="input-field"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                {CATEGORIES.filter((c) => c !== 'ALL').map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-xs text-secondary font-semibold mb-1 block">
                Importance ({importance}/5)
              </label>
              <div className={styles.importanceSelector}>
                {[1, 2, 3, 4, 5].map((lvl) => (
                  <button
                    key={lvl}
                    type="button"
                    className={`${styles.starBtn} ${lvl <= importance ? styles.active : ''}`}
                    onClick={() => setImportance(lvl)}
                  >
                    ★
                  </button>
                ))}
              </div>
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
              type="submit"
              className="btn btn-primary btn-sm"
              disabled={saving || !content.trim()}
            >
              {saving ? <span className="spinner" style={{ width: 14, height: 14 }} /> : 'Save Memory'}
            </button>
          </div>
        </form>
      )}

      {/* Search Input */}
      <div className={styles.searchBar}>
        <input
          type="text"
          className="input-field"
          placeholder="🔍 Search memories…"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {/* Category Chips */}
      <div className={styles.categoryChips}>
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            className={`${styles.chip} ${selectedCategory === cat ? styles.active : ''}`}
            onClick={() => setSelectedCategory(cat)}
          >
            {cat}
          </button>
        ))}
      </div>

      {error && <div className="text-xs text-red-400 mb-3">{error}</div>}

      {/* Memory List */}
      {loading ? (
        <div className="empty-state">
          <div className="spinner" style={{ margin: 'auto' }} />
        </div>
      ) : filteredMemories.length === 0 ? (
        <div className="empty-state card">
          <span className="empty-state-icon">💡</span>
          <p className="empty-state-title">No memories found</p>
          <p className="text-secondary text-sm">
            {searchQuery
              ? 'No memories match your search filter.'
              : 'Add your first memory or chat with Neura — memories are also extracted automatically!'}
          </p>
        </div>
      ) : (
        filteredMemories.map((mem, i) => (
          <div
            key={mem.id}
            className={`card ${styles.memoryCard} animate-in`}
            style={{ animationDelay: `${i * 0.03}s` }}
          >
            <div className={styles.cardHeader}>
              <div className={styles.headerLeft}>
                <span className="badge badge-todo">{mem.category}</span>
                <span className={styles.stars}>
                  {'★'.repeat(mem.importance)}
                  {'☆'.repeat(5 - mem.importance)}
                </span>
              </div>
              <span className="text-xs text-muted">
                {new Date(mem.createdAt).toLocaleDateString()}
              </span>
            </div>

            <div className={styles.content}>{mem.content}</div>

            <div className={styles.footer}>
              <span className="badge badge-low text-xs">
                Source: {mem.source}
              </span>
              <button
                className={styles.deleteBtn}
                onClick={() => handleDelete(mem.id)}
                title="Delete memory"
              >
                Delete 🗑️
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
