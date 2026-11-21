
package com.venky.assistant.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

	@Bean
	public ChatModel chatLanguageModel(@Value("${assistant.ai.ollama-base-url}") String baseUrl,
			@Value("${assistant.ai.chat-model}") String model,
			@Value("${assistant.ai.chat-timeout-seconds}") long timeoutSeconds) {

		return OllamaChatModel.builder().baseUrl(baseUrl).modelName(model).temperature(0.2)
				.timeout(Duration.ofSeconds(timeoutSeconds)).build();
	}

	@Bean
	public EmbeddingModel embeddingModel(@Value("${assistant.ai.ollama-base-url}") String baseUrl,
			@Value("${assistant.ai.embedding-model}") String model,
			@Value("${assistant.ai.embedding-timeout-seconds}") long timeoutSeconds) {

		return OllamaEmbeddingModel.builder().baseUrl(baseUrl).modelName(model)
				.timeout(Duration.ofSeconds(timeoutSeconds)).build();
	}
}
