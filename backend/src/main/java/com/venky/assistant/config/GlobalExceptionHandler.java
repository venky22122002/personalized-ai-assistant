
package com.venky.assistant.config;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	public record ErrorResponse(int status, String error, Instant timestamp) {
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {

		return error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {

		return error(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(Collectors.joining("; "));

		return error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {

		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

		return error(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

		if (hasCause(ex, HttpTimeoutException.class, SocketTimeoutException.class, TimeoutException.class)) {

			return error(HttpStatus.GATEWAY_TIMEOUT, "AI service timed out. Please try again.");
		}

		if (hasCause(ex, ConnectException.class)) {

			return error(HttpStatus.SERVICE_UNAVAILABLE, "AI service is temporarily unavailable.");
		}

		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
	}

	@SafeVarargs
	private static boolean hasCause(Throwable exception, Class<? extends Throwable>... types) {

		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

		Throwable current = exception;

		while (current != null && visited.add(current)) {

			for (Class<? extends Throwable> type : types) {
				if (type.isInstance(current)) {
					return true;
				}
			}

			current = current.getCause();
		}

		return false;
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {

		return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, Instant.now()));
	}
}
