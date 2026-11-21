
package com.venky.assistant.idempotency;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ChatRequestIdempotencyId implements Serializable {

    private UUID userId;
    private UUID requestId;

    public ChatRequestIdempotencyId() {
    }

    public ChatRequestIdempotencyId(UUID userId, UUID requestId) {
        this.userId = userId;
        this.requestId = requestId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ChatRequestIdempotencyId that)) {
            return false;
        }

        return Objects.equals(userId, that.userId)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, requestId);
    }
}
