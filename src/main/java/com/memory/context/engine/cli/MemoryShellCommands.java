package com.memory.context.engine.cli;

import com.memory.context.engine.domain.intelligence.AnswerResponse;
import com.memory.context.engine.domain.intelligence.AnswerSynthesisEngine;
import com.memory.context.engine.domain.memory.api.dto.CreateMemoryRequest;
import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.api.dto.UpdateMemoryRequest;
import com.memory.context.engine.domain.memory.service.MemoryService;
import com.memory.context.engine.domain.relationship.api.dto.CreateRelationshipRequest;
import com.memory.context.engine.domain.relationship.api.dto.RelatedMemoryDto;
import com.memory.context.engine.domain.relationship.entity.RelationshipType;
import com.memory.context.engine.domain.relationship.service.GraphService;
import com.memory.context.engine.domain.search.api.dto.SearchRequest;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
import com.memory.context.engine.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Collections;
import java.util.List;

/**
 * CLI commands for Digital Memory Engine.
 * Provides terminal-based access to all memory operations.
 */
@ShellComponent
@RequiredArgsConstructor
public class MemoryShellCommands {

    private final MemoryService memoryService;
    private final SearchService searchService;
    private final GraphService graphService;
    private final AnswerSynthesisEngine answerEngine;

    private String currentUser = "cli-user";

    // ================================
    // User Management
    // ================================

    @ShellMethod(key = "set-user", value = "Set the current user for operations")
    public String setUser(@ShellOption(help = "User ID") String userId) {
        this.currentUser = userId;
        setSecurityContext(userId);
        return "✓ Current user set to: " + userId;
    }

    @ShellMethod(key = "whoami", value = "Show current user")
    public String whoami() {
        return "Current user: " + currentUser;
    }

    // ================================
    // Memory Commands
    // ================================

    @ShellMethod(key = "memory-create", value = "Create a new memory")
    public String createMemory(
            @ShellOption(help = "Title of the memory") String title,
            @ShellOption(help = "Content of the memory") String content,
            @ShellOption(help = "Importance (1-10)", defaultValue = "5") int importance) {

        setSecurityContext(currentUser);

        CreateMemoryRequest request = CreateMemoryRequest.builder()
                .title(title)
                .content(content)
                .importanceScore(importance)
                .build();

        MemoryResponse response = memoryService.createMemory(request);
        return String.format("✓ Memory created (ID: %d)\n  Title: %s\n  Importance: %d",
                response.getId(), response.getTitle(), response.getImportanceScore());
    }

    @ShellMethod(key = "memory-list", value = "List all memories")
    public String listMemories(
            @ShellOption(help = "Page number", defaultValue = "0") int page,
            @ShellOption(help = "Page size", defaultValue = "10") int size) {

        setSecurityContext(currentUser);

        List<MemoryResponse> memories = memoryService.getActiveMemories(PageRequest.of(page, size));

        if (memories.isEmpty()) {
            return "No memories found.";
        }

        StringBuilder sb = new StringBuilder("📋 Memories (Page " + page + "):\n");
        sb.append("─".repeat(50)).append("\n");

        for (MemoryResponse m : memories) {
            sb.append(String.format("  [%d] %s (★ %d)\n", m.getId(), m.getTitle(), m.getImportanceScore()));
        }

        return sb.toString();
    }

    @ShellMethod(key = "memory-get", value = "Get memory details")
    public String getMemory(@ShellOption(help = "Memory ID") Long id) {
        setSecurityContext(currentUser);

        MemoryResponse m = memoryService.getMemory(id);
        return String.format("""
                🧠 Memory #%d
                ─────────────────────────────
                Title: %s
                Content: %s
                Importance: %d
                Archived: %s
                Created: %s
                """,
                m.getId(), m.getTitle(), m.getContent(),
                m.getImportanceScore(), m.isArchived(), m.getCreatedAt());
    }

    @ShellMethod(key = "memory-update", value = "Update a memory")
    public String updateMemory(
            @ShellOption(help = "Memory ID") Long id,
            @ShellOption(help = "New title", defaultValue = "") String title,
            @ShellOption(help = "New importance (1-10)", defaultValue = "-1") int importance) {

        setSecurityContext(currentUser);

        UpdateMemoryRequest.UpdateMemoryRequestBuilder builder = UpdateMemoryRequest.builder();
        if (!title.isEmpty())
            builder.title(title);
        if (importance > 0)
            builder.importanceScore(importance);

        MemoryResponse updated = memoryService.updateMemory(id, builder.build());
        return "✓ Memory #" + id + " updated. New title: " + updated.getTitle();
    }

    @ShellMethod(key = "memory-archive", value = "Archive a memory")
    public String archiveMemory(@ShellOption(help = "Memory ID") Long id) {
        setSecurityContext(currentUser);
        memoryService.archiveMemory(id);
        return "✓ Memory #" + id + " archived.";
    }

    // ================================
    // Search Commands
    // ================================

    @ShellMethod(key = "search", value = "Search memories")
    public String search(
            @ShellOption(help = "Search query") String query,
            @ShellOption(help = "Max results", defaultValue = "5") int limit) {

        setSecurityContext(currentUser);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .limit(limit)
                .build();

        List<SearchResult> results = searchService.search(request);

        if (results.isEmpty()) {
            return "No results found for: " + query;
        }

        StringBuilder sb = new StringBuilder("🔍 Search Results:\n");
        for (SearchResult r : results) {
            sb.append(String.format("  [%d] %s (%.2f similarity)\n",
                    r.getId(), r.getTitle(), r.getSimilarityScore()));
        }

        return sb.toString();
    }

    // ================================
    // Relationship Commands
    // ================================

    @ShellMethod(key = "relationship-create", value = "Create relationship between memories")
    public String createRelationship(
            @ShellOption(help = "Source memory ID") Long source,
            @ShellOption(help = "Target memory ID") Long target,
            @ShellOption(help = "Relationship type", defaultValue = "RELATED_TO") String type,
            @ShellOption(help = "Strength (0.0-1.0)", defaultValue = "0.5") double strength) {

        setSecurityContext(currentUser);

        CreateRelationshipRequest request = CreateRelationshipRequest.builder()
                .sourceMemoryId(source)
                .targetMemoryId(target)
                .type(RelationshipType.valueOf(type.toUpperCase()))
                .strength(java.math.BigDecimal.valueOf(strength))
                .build();

        graphService.createRelationship(request);
        return String.format("✓ Relationship created: %d → %d (%s)", source, target, type);
    }

    @ShellMethod(key = "relationship-list", value = "List related memories")
    public String listRelationships(@ShellOption(help = "Memory ID") Long memoryId) {
        setSecurityContext(currentUser);

        List<RelatedMemoryDto> related = graphService.getRelatedMemories(memoryId);

        if (related.isEmpty()) {
            return "No related memories found for #" + memoryId;
        }

        StringBuilder sb = new StringBuilder("🔗 Related to Memory #" + memoryId + ":\n");
        for (RelatedMemoryDto r : related) {
            sb.append(String.format("  → [%d] %s (%.2f strength)\n",
                    r.getMemoryId(), r.getTitle(), r.getStrength()));
        }

        return sb.toString();
    }

    // ================================
    // Intelligent Q&A
    // ================================

    @ShellMethod(key = "ask", value = "Ask a question and get an intelligent answer from your memories")
    public String ask(@ShellOption(help = "Your question") String question) {
        setSecurityContext(currentUser);

        AnswerResponse response = answerEngine.ask(question);

        StringBuilder sb = new StringBuilder();
        sb.append("\n🧠 Answer (confidence: ").append(String.format("%.0f%%", response.getConfidence() * 100))
                .append(")\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(response.getAnswer());
        sb.append("\n\n📚 Sources:\n");
        for (var source : response.getSources()) {
            sb.append(String.format("  • [%d] %s (%.0f%% match)\n",
                    source.getMemoryId(), source.getTitle(), source.getRelevanceScore() * 100));
        }

        return sb.toString();
    }

    // ================================
    // Help
    // ================================

    @ShellMethod(key = "commands", value = "Show available commands")
    public String showCommands() {
        return """
                📚 Digital Memory Engine CLI Commands
                ═══════════════════════════════════════

                User:
                  set-user <userId>          Set current user
                  whoami                     Show current user

                Memory:
                  memory-create              Create a new memory
                  memory-list                List all memories
                  memory-get <id>            Get memory details
                  memory-update <id>         Update a memory
                  memory-archive <id>        Archive a memory

                Search:
                  search <query>             Search memories

                Relationships:
                  relationship-create        Link two memories
                  relationship-list <id>     Show related memories

                System:
                  commands                   Show this help
                  exit                       Exit the shell
                """;
    }

    // ================================
    // Security Helper
    // ================================

    private void setSecurityContext(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
    }
}
