package com.memory.context.engine.domain.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Service to interact with Google's Gemini API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateAnswer(String question, List<String> memoryContexts) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is missing. Returning fallback response.");
            return "I am unable to generate an intelligent answer because the AI service is not configured.";
        }

        String prompt = buildPrompt(question, memoryContexts);

        int maxRetries = 3;
        long retryDelay = 1000; // Start with 1 second

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                GeminiRequest request = new GeminiRequest();
                request.setContents(Collections.singletonList(
                        new Content(Collections.singletonList(
                                new Part(prompt)))));

                HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

                String url = apiUrl + "?key=" + apiKey;
                GeminiResponse response = restTemplate.postForObject(url, entity, GeminiResponse.class);

                if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                    Candidate candidate = response.getCandidates().get(0);
                    if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
                        return candidate.getContent().getParts().get(0).getText();
                    }
                }

                return "I couldn't generate an answer from the AI model.";
            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable
                    | org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.warn("Gemini API overloaded or rate limited (Attempt {}/{}). Retrying in {} ms...", attempt,
                        maxRetries, retryDelay);
                if (attempt == maxRetries) {
                    log.error("Gemini API failed after {} attempts.", maxRetries, e);
                    return "Sorry, the AI service is currently overloaded. Please try again later.";
                }
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "Operation interrupted.";
                }
                retryDelay *= 2; // Exponential backoff
            } catch (Exception e) {
                log.error("Error calling Gemini API: ", e);
                return "Sorry, I encountered an error while communicating with the AI service.";
            }
        }
        return "Sorry, I was unable to get a response from the AI service.";
    }

    private String buildPrompt(String question, List<String> memoryContexts) {
        StringBuilder contextBuilder = new StringBuilder();
        for (String memory : memoryContexts) {
            contextBuilder.append("- ").append(memory).append("\n");
        }

        return """
                You are a helpful personal memory assistant. You have access to the user's digital memories.
                Answer the user's question based ONLY on the provided memories.
                If the answer is not in the memories, say "I don't have enough information in your memories to answer that."
                Do not make up information.

                USER MEMORIES:
                %s

                USER QUESTION:
                %s

                ANSWER:
                """
                .formatted(contextBuilder.toString(), question);
    }

    // --- DTOs for Gemini API ---

    @Data
    private static class GeminiRequest {
        private List<Content> contents;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Candidate {
        private Content content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class Content {
        private List<Part> parts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class Part {
        private String text;
    }
}
