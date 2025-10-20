package com.memory.context.engine.domain.topic.api;

import com.memory.context.engine.domain.topic.entity.Topic;
import com.memory.context.engine.domain.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    public ResponseEntity<Topic> createTopic(@RequestParam String name,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(topicService.createTopic(name, description));
    }

    @GetMapping
    public ResponseEntity<List<Topic>> getTopics() {
        return ResponseEntity.ok(topicService.getTopics());
    }

    @PostMapping("/memory/{memoryId}/tag")
    public ResponseEntity<Void> tagMemory(@PathVariable Long memoryId, @RequestBody List<String> topicNames) {
        topicService.tagMemory(memoryId, topicNames);
        return ResponseEntity.ok().build();
    }
}
