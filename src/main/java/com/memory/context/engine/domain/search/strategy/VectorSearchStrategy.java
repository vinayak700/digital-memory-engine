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

    String query = request.getQuery().trim();

    // Try PostgreSQL full-text search first (handles multi-word queries properly)
    try {
      return executeFullTextSearch(query, userId, request.getLimit());
    } catch (Exception e) {
      log.warn("Full-text search failed, falling back to ILIKE: {}", e.getMessage());
      return executeFallbackSearch(query, userId, request.getLimit());
    }
  }

  /**
   * PostgreSQL full-text search using plainto_tsquery.
   * Tokenizes words and applies stemming for better multi-word matching.
   */
  private List<SearchResult> executeFullTextSearch(String query, String userId, int limit) {
    return jdbcTemplate.query(
        """
            SELECT id, title, content,
                   ts_rank(
                       setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
                       setweight(to_tsvector('english', COALESCE(content, '')), 'B'),
                       plainto_tsquery('english', ?)
                   ) as similarity
            FROM memories
            WHERE user_id = ?
              AND archived = false
              AND (
                  to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(content, ''))
                  @@ plainto_tsquery('english', ?)
              )
            ORDER BY similarity DESC, importance_score DESC, created_at DESC
            LIMIT ?
            """,
        (rs, rowNum) -> SearchResult.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .similarityScore(Math.min(1.0, rs.getDouble("similarity") + 0.5))
            .build(),
        query, userId, query, limit);
  }

  /**
   * Fallback search using AND-based ILIKE for each term.
   * Used when full-text search is unavailable.
   */
  private List<SearchResult> executeFallbackSearch(String query, String userId, int limit) {
    // Split query into individual terms
    String[] terms = query.toLowerCase().split("\\s+");

    // Build dynamic WHERE clause: each term must match title OR content
    StringBuilder whereClause = new StringBuilder();
    List<Object> params = new java.util.ArrayList<>();
    params.add(userId);

    for (int i = 0; i < terms.length; i++) {
      if (i > 0)
        whereClause.append(" AND ");
      whereClause.append("(LOWER(title) LIKE ? OR LOWER(content) LIKE ?)");
      String pattern = "%" + terms[i] + "%";
      params.add(pattern);
      params.add(pattern);
    }
    params.add(limit);

    String sql = String.format("""
        SELECT id, title, content, 0.7 as similarity
        FROM memories
        WHERE user_id = ?
          AND archived = false
          AND (%s)
        ORDER BY importance_score DESC, created_at DESC
        LIMIT ?
        """, whereClause);

    return jdbcTemplate.query(sql,
        (rs, rowNum) -> SearchResult.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .similarityScore(rs.getDouble("similarity"))
            .build(),
        params.toArray());
  }
}
