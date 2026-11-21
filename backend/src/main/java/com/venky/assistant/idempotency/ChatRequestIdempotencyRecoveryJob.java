
package com.venky.assistant.idempotency;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChatRequestIdempotencyRecoveryJob {

	private static final Logger log = LoggerFactory.getLogger(ChatRequestIdempotencyRecoveryJob.class);

	private final ChatRequestIdempotencyRepository repository;
	private final ChatRequestIdempotencyService idempotencyService;

	public ChatRequestIdempotencyRecoveryJob(ChatRequestIdempotencyRepository repository,
			ChatRequestIdempotencyService idempotencyService) {
		this.repository = repository;
		this.idempotencyService = idempotencyService;
	}

	@Scheduled(fixedDelay = 60_000)
	public void recoverStaleRequests() {
		Instant cutoff = Instant.now().minusSeconds(600);

		List<ChatRequestIdempotency> staleRequests = repository.findStaleProcessingRequests(cutoff);

		for (ChatRequestIdempotency request : staleRequests) {
			try {
				boolean recovered = idempotencyService.markStaleRequestFailed(request.getUserId(),
						request.getRequestId(), cutoff);

				if (recovered) {
					log.warn("Marked stale chat request FAILED: userId={}, requestId={}", request.getUserId(),
							request.getRequestId());
				}
			} catch (Exception ex) {
				log.error("Could not recover stale chat request: requestId={}", request.getRequestId(), ex);
			}
		}
	}
}
