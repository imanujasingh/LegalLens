package com.contractGuard.LegalLens.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient configuration.
 *
 * Three tiers of client are configured with different model/temperature
 * trade-offs to balance cost, latency, and reasoning quality:
 *
 *  ┌──────────────────┬───────────────────┬──────────────────────────────────────┐
 *  │ Bean             │ Model             │ Use case                             │
 *  ├──────────────────┼───────────────────┼──────────────────────────────────────┤
 *  │ fastChatClient   │ gpt-4o-mini       │ Clause extraction, status checks     │
 *  │ standardChatClient│ gpt-4o           │ Full contract risk assessment (main) │
 *  │ complexChatClient│ gpt-4o (temp 0.0) │ Deterministic clause diff & scoring  │
 *  └──────────────────┴───────────────────┴──────────────────────────────────────┘
 */
@Configuration
public class AiChatConfig {

    /**
     * Fast, low-cost client for lightweight tasks:
     * clause extraction, duplicate detection summaries, status label generation.
     * gpt-4o-mini gives ~10x cost reduction vs gpt-4o for short-context tasks.
     */
    @Bean
    public ChatClient fastChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model("gpt-4o-mini")
                                .temperature(0.1)
                                .build()
                )
                .build();
    }

    /**
     * Standard client for full contract analysis.
     * gpt-4o with temperature 0.1 — low randomness for consistent legal risk output.
     * Used by AIService.analyzeContract().
     */
    @Bean
    public ChatClient standardChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model("gpt-4o")
                                .temperature(0.1)
                                .build()
                )
                .build();
    }

    /**
     * Complex/deterministic client for structured output tasks:
     * clause diffing between versions, risk score computation,
     * and any prompt that requires JSON output without hallucinated keys.
     * Temperature 0.0 enforces maximum determinism.
     */
    @Bean
    public ChatClient complexChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model("gpt-4o")
                                .temperature(0.0)
                                .build()
                )
                .build();
    }
}
