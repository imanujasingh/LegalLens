package com.contractGuard.LegalLens.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {

    @Bean
    public ChatClient fastChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model("gpt-5.2")
                                .temperature(0.1)
                                .build()
                )
                .build();
    }

    @Bean
    public ChatClient standardChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model("gpt-5.2")
                                .temperature(0.1)
                                .build()
                )
                .build();
    }

    @Bean
    public ChatClient complexChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model("gpt-4o")
                                .temperature(0.1)
                                .build()
                )
                .build();
    }
}
