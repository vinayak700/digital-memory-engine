package com.memory.context.engine.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return """
            <h1>Digital Memory Engine</h1>
            <p>Application is running successfully!</p>
            <p>Database connection: ✅ Working</p>
            <p>Spring Boot: ✅ Running on port 8082</p>
            <ul>
                <li><a href="/actuator/health">Health Check</a></li>
                <li><a href="/api/test">Test API</a></li>
            </ul>
            """;
    }

    @GetMapping("/api/test")
    public String test() {
        return "{\"status\": \"running\", \"message\": \"Digital Memory Engine API is working!\"}";
    }
}