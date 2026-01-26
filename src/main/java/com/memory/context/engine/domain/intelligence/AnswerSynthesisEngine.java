package com.memory.context.engine.domain.intelligence;

import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.domain.relationship.entity.MemoryRelationship;
import com.memory.context.engine.domain.relationship.repository.MemoryRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Answer Synthesis Engine - The core intelligence of Digital Memory Engine.
 * Generates intelligent answers by analyzing and synthesizing relevant
 * memories.
 * No LLM required - uses classical NLP and graph algorithms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerSynthesisEngine {

    private final MemoryRepository memoryRepository;
    private final MemoryRelationshipRepository relationshipRepository;
    private final TextSimilarityService similarityService;
    private final KeywordExtractionService keywordService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Main entry point: Ask a question and get an intelligent answer.
     */
    public AnswerResponse ask(String question) {
        String userId = getCurrentUser();
        log.info("Processing question for user {}: '{}'", userId, question);

        // 1. Parse the question to understand intent
        QuestionAnalysis analysis = analyzeQuestion(question);

        // 2. Find relevant memories using full-text search
        List<ScoredMemory> relevantMemories = findRelevantMemories(question, userId, 10);

        // 3. Expand context via relationships
        List<ScoredMemory> expandedContext = expandContextViaRelationships(relevantMemories, userId);

        // 4. Synthesize answer from memories
        String synthesizedAnswer = synthesizeAnswer(question, analysis, relevantMemories, expandedContext);

        // 5. Build response with sources
        return AnswerResponse.builder()
                .question(question)
                .answer(synthesizedAnswer)
                .sources(relevantMemories.stream()
                        .map(m -> new SourceReference(m.memory.getId(), m.memory.getTitle(), m.score))
                        .collect(Collectors.toList()))
                .relatedMemories(expandedContext.stream()
                        .map(m -> m.memory.getId())
                        .collect(Collectors.toList()))
                .confidence(calculateConfidence(relevantMemories))
                .build();
    }

    /**
     * Analyze the question to understand intent and extract key terms.
     */
    private QuestionAnalysis analyzeQuestion(String question) {
        QuestionAnalysis analysis = new QuestionAnalysis();

        String lowerQ = question.toLowerCase();

        // Detect question type
        if (lowerQ.startsWith("what")) {
            analysis.type = QuestionType.WHAT;
        } else if (lowerQ.startsWith("why")) {
            analysis.type = QuestionType.WHY;
        } else if (lowerQ.startsWith("how")) {
            analysis.type = QuestionType.HOW;
        } else if (lowerQ.startsWith("when")) {
            analysis.type = QuestionType.WHEN;
        } else if (lowerQ.startsWith("who")) {
            analysis.type = QuestionType.WHO;
        } else if (lowerQ.contains("list") || lowerQ.contains("show") || lowerQ.contains("all")) {
            analysis.type = QuestionType.LIST;
        } else {
            analysis.type = QuestionType.GENERAL;
        }

        // Detect temporal references
        if (lowerQ.contains("today") || lowerQ.contains("yesterday") ||
                lowerQ.contains("last week") || lowerQ.contains("recent")) {
            analysis.hasTemporal = true;
        }

        // Extract keywords
        analysis.keywords = keywordService.extractKeywords(question, 5);

        return analysis;
    }

    /**
     * Find memories relevant to the question using PostgreSQL full-text search.
     */
    private List<ScoredMemory> findRelevantMemories(String question, String userId, int limit) {
        // Extract search terms
        List<String> keywords = keywordService.extractKeywords(question, 5);
        String searchQuery = String.join(" | ", keywords); // OR search

        if (searchQuery.isBlank()) {
            searchQuery = question.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        }

        log.debug("Searching with query: {}", searchQuery);

        // Use PostgreSQL full-text search with ranking
        String sql = """
                SELECT m.id, m.title, m.content, m.importance_score, m.context, m.created_at,
                       ts_rank(
                           setweight(to_tsvector('english', COALESCE(m.title, '')), 'A') ||
                           setweight(to_tsvector('english', COALESCE(m.content, '')), 'B'),
                           plainto_tsquery('english', ?)
                       ) as rank
                FROM memories m
                WHERE m.user_id = ?
                  AND m.archived = false
                  AND (
                      to_tsvector('english', COALESCE(m.title, '') || ' ' || COALESCE(m.content, ''))
                      @@ plainto_tsquery('english', ?)
                      OR LOWER(m.title) LIKE ?
                      OR LOWER(m.content) LIKE ?
                  )
                ORDER BY rank DESC, m.importance_score DESC
                LIMIT ?
                """;

        String likePattern = "%" + question.toLowerCase().split("\\s+")[0] + "%";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Memory memory = new Memory();
                memory.setId(rs.getLong("id"));
                memory.setTitle(rs.getString("title"));
                memory.setContent(rs.getString("content"));
                memory.setImportanceScore(rs.getInt("importance_score"));
                memory.setCreatedAt(rs.getTimestamp("created_at").toInstant());

                double rank = rs.getDouble("rank");
                double similarity = similarityService.cosineSimilarity(question,
                        memory.getTitle() + " " + memory.getContent());

                return new ScoredMemory(memory, Math.max(rank, similarity));
            }, searchQuery, userId, searchQuery, likePattern, likePattern, limit);
        } catch (Exception e) {
            log.warn("Full-text search failed, falling back to LIKE: {}", e.getMessage());
            return fallbackSearch(question, userId, limit);
        }
    }

    /**
     * Fallback to LIKE search if full-text search fails.
     */
    private List<ScoredMemory> fallbackSearch(String question, String userId, int limit) {
        String pattern = "%" + question.toLowerCase() + "%";

        return jdbcTemplate.query("""
                SELECT id, title, content, importance_score, created_at
                FROM memories
                WHERE user_id = ? AND archived = false
                  AND (LOWER(title) LIKE ? OR LOWER(content) LIKE ?)
                ORDER BY importance_score DESC
                LIMIT ?
                """, (rs, rowNum) -> {
            Memory memory = new Memory();
            memory.setId(rs.getLong("id"));
            memory.setTitle(rs.getString("title"));
            memory.setContent(rs.getString("content"));
            memory.setImportanceScore(rs.getInt("importance_score"));

            double similarity = similarityService.cosineSimilarity(question,
                    memory.getTitle() + " " + memory.getContent());

            return new ScoredMemory(memory, similarity);
        }, userId, pattern, pattern, limit);
    }

    /**
     * Expand context by finding related memories via relationships.
     */
    private List<ScoredMemory> expandContextViaRelationships(List<ScoredMemory> baseMemories, String userId) {
        Set<Long> processedIds = baseMemories.stream()
                .map(m -> m.memory.getId())
                .collect(Collectors.toSet());

        List<ScoredMemory> expanded = new ArrayList<>();

        for (ScoredMemory scored : baseMemories) {
            List<MemoryRelationship> relationships = relationshipRepository.findAllConnected(scored.memory.getId());

            for (MemoryRelationship rel : relationships) {
                Long relatedId = rel.getSourceMemory().getId().equals(scored.memory.getId())
                        ? rel.getTargetMemory().getId()
                        : rel.getSourceMemory().getId();

                if (!processedIds.contains(relatedId)) {
                    processedIds.add(relatedId);
                    memoryRepository.findById(relatedId).ifPresent(related -> {
                        if (related.getUserId().equals(userId) && !related.isArchived()) {
                            // Score based on relationship strength and base memory score
                            double relScore = scored.score * rel.getStrength().doubleValue() * 0.5;
                            expanded.add(new ScoredMemory(related, relScore));
                        }
                    });
                }
            }
        }

        return expanded;
    }

    /**
     * Synthesize a coherent answer from the relevant memories.
     */
    private String synthesizeAnswer(String question, QuestionAnalysis analysis,
            List<ScoredMemory> relevantMemories,
            List<ScoredMemory> expandedContext) {

        if (relevantMemories.isEmpty()) {
            return "I don't have any memories related to this question. " +
                    "Try creating some memories first with relevant information.";
        }

        StringBuilder answer = new StringBuilder();

        // Opening based on question type
        switch (analysis.type) {
            case WHAT -> answer.append("Based on your memories, here's what I found:\n\n");
            case WHY -> answer.append("Looking at your memories for the reasons:\n\n");
            case HOW -> answer.append("Here's how, according to your memories:\n\n");
            case WHEN -> answer.append("From your memories, regarding timing:\n\n");
            case LIST -> answer.append("Here are the relevant items from your memories:\n\n");
            default -> answer.append("From your memories:\n\n");
        }

        // Extract and present key information from each memory
        int count = 0;
        for (ScoredMemory scored : relevantMemories) {
            if (count >= 5)
                break; // Limit to top 5

            Memory m = scored.memory;
            String relevantSentence = similarityService.findMostRelevantSentence(m.getContent(), question);

            answer.append("• ").append(relevantSentence).append("\n");
            answer.append("  ").append("(from: \"").append(m.getTitle()).append("\"");
            answer.append(", importance: ").append(m.getImportanceScore()).append("/10)\n\n");

            count++;
        }

        // Add related context if available
        if (!expandedContext.isEmpty()) {
            answer.append("---\n📎 Related memories:\n");
            for (ScoredMemory related : expandedContext.stream().limit(3).toList()) {
                answer.append("  → ").append(related.memory.getTitle()).append("\n");
            }
        }

        // Add confidence note
        double confidence = calculateConfidence(relevantMemories);
        if (confidence < 0.3) {
            answer.append("\n⚠️ Note: Low confidence match. Consider adding more relevant memories.");
        }

        return answer.toString();
    }

    /**
     * Calculate overall confidence in the answer.
     */
    private double calculateConfidence(List<ScoredMemory> memories) {
        if (memories.isEmpty())
            return 0.0;

        double avgScore = memories.stream()
                .mapToDouble(m -> m.score)
                .average()
                .orElse(0.0);

        // Boost confidence if we have multiple matching memories
        double countBonus = Math.min(0.2, memories.size() * 0.05);

        return Math.min(1.0, avgScore + countBonus);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Inner classes

    public record ScoredMemory(Memory memory, double score) {
    }

    public enum QuestionType {
        WHAT, WHY, HOW, WHEN, WHO, LIST, GENERAL
    }

    private static class QuestionAnalysis {
        QuestionType type = QuestionType.GENERAL;
        List<String> keywords = new ArrayList<>();
        boolean hasTemporal = false;
    }
}
