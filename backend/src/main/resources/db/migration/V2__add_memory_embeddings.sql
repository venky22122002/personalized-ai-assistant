-- Store one semantic embedding for each memory.
-- embeddinggemma produces 768-dimensional vectors.

CREATE TABLE memory_embeddings (
    memory_id UUID PRIMARY KEY
        REFERENCES memories(id) ON DELETE CASCADE,

    embedding vector(768) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Accelerate cosine-similarity searches over stored embeddings.
CREATE INDEX idx_memory_embeddings_hnsw
    ON memory_embeddings
    USING hnsw (embedding vector_cosine_ops);
