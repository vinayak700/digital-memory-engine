package com.memory.context.engine.domain.search.strategy;

import com.memory.context.engine.domain.search.api.dto.SearchRequest;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
import java.util.List;

/**
 * Strategy interface for different search implementations.
 */
public interface SearchStrategy {

    /**
     * Identifying name of the strategy (e.g., "vector", "keyword").
     */
    String getName();

    /**
     * Executes the search.
     * 
     * @param request The search request parameters.
     * @param userId  The ID of the user performing the search.
     * @return List of search results.
     */
    List<SearchResult> search(SearchRequest request, String userId);
}
