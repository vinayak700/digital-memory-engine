package com.memory.context.engine.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root controller for application info and health status.
 */
@RestController
public class HomeController {

    @Value("${spring.application.name:Digital Memory Engine}")
    private String appName;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>%s</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                               max-width: 600px; margin: 50px auto; padding: 20px;
                               background: #f5f5f5; }
                        .container { background: white; padding: 30px; border-radius: 8px;
                                     box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        h1 { color: #333; margin-bottom: 20px; }
                        .status { background: #d4edda; padding: 10px; border-radius: 4px;
                                  margin: 10px 0; color: #155724; }
                        a { color: #007bff; text-decoration: none; }
                        a:hover { text-decoration: underline; }
                        ul { list-style: none; padding: 0; }
                        li { margin: 8px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🧠 %s</h1>
                        <div class="status">✅ Application running on port 8082</div>
                        <h3>Quick Links</h3>
                        <ul>
                            <li>📊 <a href="/actuator/health">Health Check</a></li>
                            <li>📈 <a href="/actuator/metrics">Metrics</a></li>
                            <li>🔍 <a href="/api/test">API Test</a></li>
                        </ul>
                        <h3>API Endpoints</h3>
                        <ul>
                            <li><code>POST /api/v1/memories</code> - Create memory</li>
                            <li><code>GET /api/v1/memories</code> - List memories</li>
                            <li><code>GET /api/v1/memories/{id}</code> - Get memory</li>
                            <li><code>PATCH /api/v1/memories/{id}</code> - Update memory</li>
                            <li><code>DELETE /api/v1/memories/{id}</code> - Archive memory</li>
                            <li><code>POST /api/v1/search</code> - Semantic search</li>
                            <li><code>POST /api/v1/relationships</code> - Create relationship</li>
                        </ul>
                    </div>
                </body>
                </html>
                """.formatted(appName, appName);
    }

    @GetMapping(value = "/api/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> test() {
        return Map.of(
                "status", "running",
                "application", appName,
                "message", "Digital Memory Engine API is working!");
    }
}