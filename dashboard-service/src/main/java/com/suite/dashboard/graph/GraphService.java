package com.suite.dashboard.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GraphService {

    private static final double DAMPING = 0.85;
    private static final int ITERATIONS = 50;

    private record Edge(String src, String dst, long calls) {}

    private final Driver driver;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    public Map<String, Object> loadGraph() {

        Map<String, Long> inCalls = new LinkedHashMap<>();
        Map<String, Long> outTotal = new HashMap<>();
        List<Edge> edges = new ArrayList<>();

        try (Session session = driver.session()) {

            Result result = session.run(
                    "MATCH (s:Service) " +
                            "OPTIONAL MATCH (s)-[r:CALLS]->(t:Service) " +
                            "RETURN s.name AS src, t.name AS dst, r.calls AS calls");

            while (result.hasNext()) {

                var rec = result.next();

                String src = rec.get("src").asString();
                inCalls.putIfAbsent(src, 0L);

                if (rec.get("dst").isNull()) {
                    continue;
                }

                String dst = rec.get("dst").asString();
                long calls = rec.get("calls").asLong(0L);

                edges.add(new Edge(src, dst, calls));
                inCalls.merge(dst, calls, Long::sum);
                outTotal.merge(src, calls, Long::sum);
            }
        }

        Map<String, Double> rank = pageRank(inCalls.keySet(), edges, outTotal);

        List<Map<String, Object>> nodes = new ArrayList<>();

        for (String name : inCalls.keySet()) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("name", name);
            n.put("inCalls", inCalls.get(name));
            n.put("pageRank", rank.get(name));
            nodes.add(n);
        }

        nodes.sort((a, b) -> Double.compare(
                (Double) b.get("pageRank"),
                (Double) a.get("pageRank")));

        List<Map<String, Object>> edgeList = new ArrayList<>();

        for (Edge e : edges) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("src", e.src());
            m.put("dst", e.dst());
            m.put("calls", e.calls());
            edgeList.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("algorithm", "Weighted PageRank (Java, d=0.85, 50 iterations) over Neo4j CALLS edges");
        out.put("bottleneck", nodes.isEmpty() ? null : nodes.get(0).get("name"));
        out.put("nodes", nodes);
        out.put("edges", edgeList);
        return out;
    }

    private Map<String, Double> pageRank(
            Set<String> names,
            List<Edge> edges,
            Map<String, Long> outTotal) {

        int n = names.size();
        Map<String, Double> pr = new HashMap<>();

        if (n == 0) {
            return pr;
        }

        for (String name : names) {
            pr.put(name, 1.0 / n);
        }

        for (int it = 0; it < ITERATIONS; it++) {

            double dangling = 0;

            for (String name : names) {
                if (outTotal.getOrDefault(name, 0L) == 0L) {
                    dangling += pr.get(name);
                }
            }

            Map<String, Double> next = new HashMap<>();

            for (String name : names) {
                next.put(name, (1 - DAMPING) / n + DAMPING * dangling / n);
            }

            for (Edge e : edges) {
                long total = outTotal.getOrDefault(e.src(), 0L);
                if (total == 0L) {
                    continue;
                }
                double share = pr.get(e.src()) * ((double) e.calls() / total);
                next.merge(e.dst(), DAMPING * share, Double::sum);
            }

            pr = next;
        }

        return pr;
    }
}