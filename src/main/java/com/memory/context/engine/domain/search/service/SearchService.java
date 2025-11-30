package com.memory.context.engine.domain.search.service;

import com.memory.context.engine.domain.search.api.dto.SearchRequest;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
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

    private final com.memory.context.engine.domain.search.strategy.SearchStrategyFactory searchStrategyFactory;
    private final JdbcTemplate jdbcTemplate; // Kept for findSimilar method

    private static final String CACHE_SEARCH = "search-results";

    /**
     * Performs semantic search using vector similarity.
     * 
     * NOTE: Requires embedding generation for the query.
     * Currently a placeholder - actual implementation would call embedding API.
     */
    @Cacheable(value = CACHE_SEARCH, key = "'user:' + #root.target.getCurrentUser() + ':q:' + #request.query + ':l:' + #request.limit")
    public List<SearchResult> search(SearchRequest request) {
        String userId = getCurrentUser();
        log.info("Searching memories for user: {}, query: {}", userId, request.getQuery());

        // Use factory to get the appropriate strategy (defaulting to vector if not
        // specified)
        // In the future, request.getType() could specify 'keyword' vs 'vector'
        return searchStrategyFactory.getDefaultStrategy().search(request, userId);
    }

    /**
     * Finds memories similar to a given memory.
     */
    public List<SearchResult> findSimilar(Long memoryId, int limit) {
        return findSimilarForUser(memoryId, getCurrentUser(), limit);
    }

    /**
     * Finds memories similar to a given memory for a specific user.
     */
    public List<SearchResult> findSimilarForUser(Long memoryId, String userId, int limit) {
        log.info("Finding similar memories to: {} for user: {}", memoryId, userId);

        return jdbcTemplate.query(
                """
                        SELECT m2.id, m2.title, m2.content,
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
                        .content(rs.getString("content"))
                        .similarityScore(rs.getDouble("similarity"))
                        .build(),
                userId, memoryId, limit);
    }

    public String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
