package com.contractGuard.LegalLens.service.ai;

import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import com.contractGuard.LegalLens.exception.AiUnavailableException;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final ChatClient standardChatClient;
    private final PromptBuilder promptBuilder;

    @Value("classpath:prompts/risk-assessment.txt")
    private Resource riskAssessmentPrompt;

    public String analyzeContract(String contractText, PartyProfileEntity partyProfile) {
        log.info("Inside AIService.analyzeContract for party: {}", partyProfile.getPartyName());
        PromptTemplate template = new PromptTemplate(riskAssessmentPrompt);

        Map<String, Object> params =
                promptBuilder.buildContractAnalysisParameters(
                        contractText, partyProfile
                );

        Prompt prompt = template.create(params);

        try {
            ChatResponse response = standardChatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            return extractText(response);
        } catch (ResourceAccessException rae) {
            log.error("AI service network error: {}", rae.getMessage(), rae);
            throw new AiUnavailableException("AI service unavailable due to network error: " + rae.getMessage());
        } catch (Exception ex) {
            log.error("AI service failed: {}", ex.getMessage(), ex);
            throw new AiUnavailableException("AI service failed: " + ex.getMessage());
        }
    }

    private String extractText(ChatResponse response) {
        if (response == null ||
                response.getResult() == null ||
                response.getResult().getOutput() == null) {

            throw new IllegalStateException("Empty AI response");
        }
        return response.getResult().getOutput().getText();
    }
}
