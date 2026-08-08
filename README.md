# Distributed Log Analytics System for Large-Scale Microservices Monitoring

Real-time log analytics platform that ingests logs from microservices, detects failures, traces root cause through service dependency graphs, and explains incidents in plain English using AI.

## Problem

In microservice architectures, one service failing can cascade and break several dependent services at once. Engineers see multiple services failing simultaneously with no easy way to tell which one is the actual root cause versus which are just downstream effects — wasting critical time during incidents.

## What This Does

- Ingests logs from microservices in real-time via Kafka (not batch)
- Detects service failures and degradation using error-rate thresholds
- Traces failure propagation across dependent services using graph traversal (BFS/DFS) on Neo4j
- Distinguishes real cascading failures from unrelated, coincidental failures
- Generates human-readable root-cause explanations via LLM (LangChain)
- Displays live system health on a real-time dashboard (WebSocket)

## Tech Stack

**Backend (Java):** Spring Boot 3, Spring Kafka, Spring Security (JWT), Spring WebSocket
**Streaming:** Apache Kafka
**Storage:** Elasticsearch (log search), Neo4j (dependency graph), Redis (live counters), MySQL (persistent records)
**AI Layer:** Python, LangChain, LLM API
**Frontend:** React.js
**Infra:** Docker

## Architecture

```
Microservices (producers)
    → Kafka
    → Spring Boot Consumer
    → Elasticsearch / Redis / MySQL / Neo4j
    → Python AI/RCA Layer
    → React Dashboard (WebSocket)
```

## How It Works

1. Mock microservices (auth, user, product, order, payment, notif) generate logs continuously and push them to Kafka.
2. A Spring Boot consumer reads the stream in real time and routes each log to the correct store — Elasticsearch for search, Redis for live error counters, MySQL for permanent records.
3. A pre-built service dependency graph in Neo4j maps how services call each other.
4. When a service fails, error counters cross a threshold and a circuit breaker trips, triggering fast-fail behavior instead of hanging requests.
5. BFS/DFS traversal on the Neo4j graph identifies the true root cause and distinguishes it from services that failed only as a downstream effect — or failed independently, unrelated to the main incident.
6. An AI layer reads the graph output and generates a plain-English explanation of what broke and why.
7. All of this streams live to a React dashboard over WebSocket, so system health updates in real time without refreshing.

## Key Design Decisions

- **Kafka over direct REST calls** — decouples services, avoids blocking calls, and allows replay of the log stream.
- **Polyglot persistence** — each database is used only for what it's best at, instead of forcing one database to do everything.
- **Circuit Breaker (Resilience4j)** — prevents cascading slowdowns when a dependency is failing.
- **Graph traversal over timestamp correlation** — root cause is determined by actual dependency edges in the graph, not just "failed around the same time," which avoids false positives.

## Status

In active development — Mini Project #64, academic project.
