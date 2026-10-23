package com.suite.dashboard.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class HealthController {

    private record Target(String name, int port) {}

    private static final List<Target> TARGETS = List.of(
            new Target("auth-service", 8081),
            new Target("user-service", 8082),
            new Target("movies-service", 8083),
            new Target("webseries-service", 8084),
            new Target("payment-service", 8085));

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1000))
            .build();

    private final boolean useServiceNames;

    public HealthController(
            @Value("${health.use-service-names:true}") boolean useServiceNames) {
        this.useServiceNames = useServiceNames;
    }

    @GetMapping("/health")
    public List<Map<String, Object>> health() {

        List<CompletableFuture<Map<String, Object>>> futures =
                TARGETS.stream().map(this::check).toList();

        return futures.stream().map(CompletableFuture::join).toList();
    }

    private CompletableFuture<Map<String, Object>> check(Target t) {

        String host = useServiceNames ? t.name() : "localhost";

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://" + host + ":" + t.port() + "/actuator/health"))
                .timeout(Duration.ofMillis(1500))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> res.statusCode() == 200 && res.body().contains("UP"))
                .exceptionally(e -> false)
                .thenApply(up -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", t.name());
                    m.put("port", t.port());
                    m.put("up", up);
                    return m;
                });
    }
}