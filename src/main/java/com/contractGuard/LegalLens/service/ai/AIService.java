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

        ChatResponse response = standardChatClient
                .prompt(prompt)
                .call()
                .chatResponse();

        return extractText(response);
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
