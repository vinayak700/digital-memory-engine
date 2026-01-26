package com.memory.context.engine.domain.search.strategy;

import com.memory.context.engine.domain.search.api.dto.SearchRequest;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Search strategy using text-based search.
 * Falls back to ILIKE queries until vector embeddings are integrated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorSearchStrategy implements SearchStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "vector";
    }

    @Override
    public List<SearchResult> search(SearchRequest request, String userId) {
        log.info("Executing text search for user: {}, query: '{}'", userId, request.getQuery());

        String searchPattern = "%" + request.getQuery().toLowerCase() + "%";

        // Text-based search using ILIKE (case-insensitive)
        // Searches both title and content
        // TODO: Replace with vector similarity when embeddings are available
        return jdbcTemplate.query(
                """
                        SELECT id, title, LEFT(content, 200) as snippet,
                               CASE
                                 WHEN LOWER(title) LIKE ? THEN 0.9
                                 WHEN LOWER(content) LIKE ? THEN 0.7
                                 ELSE 0.5
                               END as similarity
                        FROM memories
                        WHERE user_id = ?
                          AND archived = false
                          AND (LOWER(title) LIKE ? OR LOWER(content) LIKE ?)
                        ORDER BY
                          CASE WHEN LOWER(title) LIKE ? THEN 0 ELSE 1 END,
                          importance_score DESC,
                          created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> SearchResult.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .contentSnippet(rs.getString("snippet"))
                        .similarityScore(rs.getDouble("similarity"))
                        .build(),
                searchPattern, searchPattern, userId, searchPattern, searchPattern, searchPattern, request.getLimit());
    }
}
