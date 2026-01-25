package com.memory.context.engine.domain.search.service;

import com.memory.context.engine.domain.search.api.dto.SearchRequest;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
import com.memory.context.engine.infrastructure.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for semantic similarity search using pgvector.
 * Results are cached in Redis for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    private static final String CACHE_SEARCH = "search-results";

    /**
     * Performs semantic search using vector similarity.
     * 
     * NOTE: Requires embedding generation for the query.
     * Currently a placeholder - actual implementation would call embedding API.
     */
    @Cacheable(value = CACHE_SEARCH, key = "#request.query + '-' + #request.limit")
    public List<SearchResult> search(SearchRequest request) {
        String userId = getCurrentUser();
        log.info("Searching memories for user: {}, query: {}", userId, request.getQuery());

        // TODO: Generate embedding for query
        // float[] queryEmbedding =
        // embeddingService.generateQueryEmbedding(request.getQuery());

        // For now, return empty results with a log message
        log.debug("Search requires embedding API integration. Returning empty results.");

        // Actual implementation would use:
        // return jdbcTemplate.query(
        // """
        // SELECT id, title, LEFT(content, 200) as snippet,
        // 1 - (embedding <=> ?::vector) as similarity
        // FROM memories
        // WHERE user_id = ? AND archived = false
        // AND embedding IS NOT NULL
        // AND 1 - (embedding <=> ?::vector) > ?
        // ORDER BY similarity DESC
        // LIMIT ?
        // """,
        // (rs, rowNum) -> SearchResult.builder()
        // .id(rs.getLong("id"))
        // .title(rs.getString("title"))
        // .contentSnippet(rs.getString("snippet"))
        // .similarityScore(rs.getDouble("similarity"))
        // .build(),
        // vectorString, userId, vectorString, request.getSimilarityThreshold(),
        // request.getLimit()
        // );

        return List.of();
    }

    /**
     * Finds memories similar to a given memory.
     */
    public List<SearchResult> findSimilar(Long memoryId, int limit) {
        String userId = getCurrentUser();
        log.info("Finding similar memories to: {} for user: {}", memoryId, userId);

        return jdbcTemplate.query(
                """
                        SELECT m2.id, m2.title, LEFT(m2.content, 200) as snippet,
                               1 - (m1.embedding <=> m2.embedding) as similarity
                        FROM memories m1
                        JOIN memories m2 ON m1.id != m2.id
                            AND m2.user_id = ?
                            AND m2.archived = false
                            AND m2.embedding IS NOT NULL
                        WHERE m1.id = ? AND m1.embedding IS NOT NULL
                        ORDER BY similarity DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> SearchResult.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .contentSnippet(rs.getString("snippet"))
                        .similarityScore(rs.getDouble("similarity"))
                        .build(),
                userId, memoryId, limit);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
