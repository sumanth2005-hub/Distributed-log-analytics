package com.suite.dashboard.graph;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/graph")
    public ResponseEntity<?> graph() {
        try {
            return ResponseEntity.ok(graphService.loadGraph());
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Neo4j unavailable",
                    "detail", String.valueOf(e.getMessage())));
        }
    }
}