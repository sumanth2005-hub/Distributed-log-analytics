const services = [
    { name: "auth-service", port: 8081 },
    { name: "user-service", port: 8082 },
    { name: "movies-service", port: 8083 },
    { name: "webseries-service", port: 8084 },
    { name: "payment-service", port: 8085 },
];

const DEMO_MODE = false;

const nodeContainer = document.getElementById("serviceNodes");
const logFeed = document.getElementById("logFeed");
const topEmitter = document.getElementById("topEmitter");
const upCount = document.getElementById("upCount");
const lastCheck = document.getElementById("lastCheck");


services.forEach(service => {

    const node = document.createElement("div");

    node.className = "flex flex-col items-center gap-2";
    node.id = "node-" + service.port;

    node.innerHTML = `
        <div class="node-circle w-16 h-16 rounded-full border-2 border-[#24242c]
                    flex items-center justify-center bg-[#111116] transition-all duration-300">

            <span class="node-dot w-3 h-3 rounded-full bg-[#5a5a63]"></span>

        </div>

        <p class="text-xs font-medium">${service.name}</p>

        <p class="text-[10px] text-[#86868f]">
            :${service.port}
        </p>

        <p
            class="text-[11px] font-semibold"
            id="status-${service.port}">
            CHECKING
        </p>
    `;

    nodeContainer.appendChild(node);
});


function setServiceStatus(service, isUp) {

    const wrapper = document.getElementById("node-" + service.port);

    if (!wrapper) {
        return;
    }

    const circle = wrapper.querySelector(".node-circle");
    const dot = wrapper.querySelector(".node-dot");
    const statusEl = document.getElementById("status-" + service.port);

    if (isUp) {

        circle.classList.remove("node-down");
        circle.classList.add("node-up");

        dot.className =
            "node-dot w-3 h-3 rounded-full node-dot-up";

        statusEl.textContent = "UP";

        statusEl.className =
            "text-[11px] font-semibold text-[#22c55e]";

    } else {

        circle.classList.remove("node-up");
        circle.classList.add("node-down");

        dot.className =
            "node-dot w-3 h-3 rounded-full node-dot-down";

        statusEl.textContent = "DOWN";

        statusEl.className =
            "text-[11px] font-semibold text-[#ef4444]";
    }
}


async function checkAllServices() {

    lastCheck.textContent = "Checking...";

    let results = [];

    try {

        const response =
            await fetch("/api/health", {
                cache: "no-store"
            });

        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }

        results = await response.json();

    } catch (error) {

        console.error(
            "Health check failed:",
            error
        );
    }

    let running = 0;

    services.forEach(service => {

        const hit = results.find(r => r.port === service.port);

        const isUp = DEMO_MODE ? true : Boolean(hit && hit.up);

        if (isUp) {
            running++;
        }

        setServiceStatus(service, isUp);
    });

    upCount.textContent =
        `${running} / ${services.length}`;

    lastCheck.textContent =
        "Last checked: " +
        new Date().toLocaleTimeString();
}


function formatTimestamp(timestamp) {

    if (!timestamp) {
        return "--";
    }

    return new Date(timestamp).toLocaleTimeString();
}


function getLevelClass(level) {

    if (level === "ERROR") {
        return "log-error";
    }

    if (level === "WARN" || level === "WARNING") {
        return "log-warn";
    }

    return "log-info";
}


function renderLogs(logs) {

    const MAX_VISIBLE_LOGS = 30;

    logs = (logs || []).slice(0, MAX_VISIBLE_LOGS);

    if (!logs || logs.length === 0) {

        logFeed.innerHTML = `
            <div class="empty-log">
                <div class="empty-icon">◌</div>

                <p>No log events yet.</p>

                <span>
                    Trigger an API request to generate a Kafka event.
                </span>
            </div>
        `;

        return;
    }

    logFeed.innerHTML = logs.map(log => {

        const service =
            escapeHtml(log.service || "unknown");

        const level =
            escapeHtml(log.level || "INFO");

        const message =
            escapeHtml(log.message || "");

        const latency =
            Number(log.latencyMs || 0);

        const time =
            formatTimestamp(log.timestamp);

        return `
            <div class="log-row">

                <div class="log-time">
                    ${time}
                </div>

                <div class="log-service">
                    ${service}
                </div>

                <div class="log-level ${getLevelClass(level)}">
                    ${level}
                </div>

                <div class="log-message">
                    ${message}
                </div>

                <div class="log-latency">
                    ${latency} ms
                </div>

            </div>
        `;

    }).join("");
}


function escapeHtml(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


async function loadLogs() {

    try {

        const response =
            await fetch("/api/logs/recent", {
                cache: "no-store"
            });

        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }

        const logs = await response.json();

        renderLogs(logs);

    } catch (error) {

        console.error(
            "Failed to load logs:",
            error
        );

        logFeed.innerHTML = `
            <div class="empty-log error-state">

                <div class="empty-icon">!</div>

                <p>Unable to load log events.</p>

                <span>
                    Dashboard API is currently unavailable.
                </span>

            </div>
        `;
    }
}


// ---------- Redis-backed stats (/api/stats) ----------

function ensureStatsPanel() {

    let panel = document.getElementById("statsPanel");

    if (panel) {
        return panel;
    }

    panel = document.createElement("div");
    panel.id = "statsPanel";
    panel.style.cssText = "margin:0 0 14px 0;";

    logFeed.parentElement.insertBefore(
        panel,
        logFeed.parentElement.firstChild
    );

    return panel;
}


function buildChart(buckets) {

    const W = 300;
    const H = 56;
    const n = buckets.length || 1;

    const max = Math.max(
        1,
        ...buckets.map(b => b.info + b.error)
    );

    const bw = W / n;

    const bars = buckets.map((b, i) => {

        const infoH = (b.info / max) * H;
        const errH = (b.error / max) * H;
        const x = i * bw + 1;
        const w = Math.max(1, bw - 2);

        return `<g>` +
            `<title>${b.minute} UTC: ${b.info} info, ${b.error} error</title>` +
            `<rect x="${x}" y="${H - infoH - errH}" width="${w}" height="${infoH}" fill="rgba(129,140,248,0.55)"/>` +
            `<rect x="${x}" y="${H - errH}" width="${w}" height="${errH}" fill="#ef4444"/>` +
            `</g>`;

    }).join("");

    return `<svg viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" ` +
        `style="width:100%;height:56px;display:block">${bars}</svg>`;
}


function renderStats(stats) {

    const totals = stats.totals || {};
    const names = Object.keys(totals);

    const sumOf = name =>
        Object.values(totals[name]).reduce((a, b) => a + b, 0);

    if (stats.highestVolume && totals[stats.highestVolume]) {

        topEmitter.textContent =
            `${stats.highestVolume} (${sumOf(stats.highestVolume)})`;

    } else {

        topEmitter.textContent = "—";
    }

    const chips = names.map(name => {

        const info = totals[name].INFO || 0;
        const err = totals[name].ERROR || 0;

        return `<div style="padding:4px 10px;border-radius:999px;` +
            `background:rgba(255,255,255,0.06);font-size:11px">` +
            `${escapeHtml(name)}: ${info} info · ` +
            `<span style="color:#ef4444">${err} err</span></div>`;

    }).join("");

    const panel = ensureStatsPanel();

    panel.innerHTML = `
        <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:10px">
            ${chips}
        </div>

        <div style="font-size:11px;opacity:0.7;margin-bottom:4px">
            Events per minute (last 15 min) · red = errors
        </div>

        ${buildChart(stats.perMinute || [])}
    `;
}


async function loadStats() {

    try {

        const response =
            await fetch("/api/stats?minutes=15", {
                cache: "no-store"
            });

        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }

        renderStats(await response.json());

    } catch (error) {

        console.error(
            "Failed to load stats:",
            error
        );
    }
}


// ---------- Neo4j dependency graph (/api/graph) ----------

const graphBox = document.getElementById("graphBox");
const rankBox = document.getElementById("rankBox");
const graphMeta = document.getElementById("graphMeta");


function shortName(name) {

    return String(name).replace("-service", "");
}


function renderGraph(data) {

    const nodes = data.nodes || [];
    const edges = data.edges || [];

    if (nodes.length === 0) {

        graphBox.innerHTML = `
            <div class="empty-log">
                <p>No graph data in Neo4j.</p>
            </div>
        `;

        rankBox.innerHTML = "";

        return;
    }

    const W = 560;
    const H = 320;
    const cx = W / 2;
    const cy = H / 2;
    const RING = 115;

    const layout = [...nodes].sort(
        (a, b) => a.name.localeCompare(b.name)
    );

    const pos = {};

    layout.forEach((n, i) => {

        const angle =
            (2 * Math.PI * i) / layout.length - Math.PI / 2;

        pos[n.name] = {
            x: cx + RING * Math.cos(angle),
            y: cy + RING * Math.sin(angle)
        };
    });

    const maxPr = Math.max(
        ...nodes.map(n => n.pageRank),
        0.0001
    );

    const maxCalls = Math.max(
        ...edges.map(e => e.calls),
        1
    );

    const radius = {};

    nodes.forEach(n => {
        radius[n.name] = 16 + 20 * (n.pageRank / maxPr);
    });

    const topName = nodes[0].name;

    const edgeSvg = edges.map(e => {

        const p1 = pos[e.src];
        const p2 = pos[e.dst];

        if (!p1 || !p2) {
            return "";
        }

        const dx = p2.x - p1.x;
        const dy = p2.y - p1.y;
        const len = Math.hypot(dx, dy) || 1;
        const ux = dx / len;
        const uy = dy / len;

        const sx = p1.x + ux * radius[e.src];
        const sy = p1.y + uy * radius[e.src];
        const ex = p2.x - ux * (radius[e.dst] + 5);
        const ey = p2.y - uy * (radius[e.dst] + 5);

        const qx = (p1.x + p2.x) / 2 - uy * 30;
        const qy = (p1.y + p2.y) / 2 + ux * 30;

        const lx = 0.25 * sx + 0.5 * qx + 0.25 * ex;
        const ly = 0.25 * sy + 0.5 * qy + 0.25 * ey;

        const width = 1 + 4 * Math.sqrt(e.calls / maxCalls);

        return `<path d="M${sx} ${sy} Q${qx} ${qy} ${ex} ${ey}" fill="none" ` +
            `stroke="rgba(129,140,248,0.7)" stroke-width="${width}" marker-end="url(#arr)"/>` +
            `<text x="${lx}" y="${ly}" fill="#c7c9d9" font-size="10" ` +
            `text-anchor="middle">${e.calls}</text>`;

    }).join("");

    const nodeSvg = nodes.map(n => {

        const p = pos[n.name];
        const r = radius[n.name];
        const isTop = n.name === topName;

        const fill = isTop
            ? "rgba(239,68,68,0.35)"
            : "rgba(129,140,248,0.25)";

        const stroke = isTop ? "#ef4444" : "#818cf8";

        return `<g>` +
            `<title>${escapeHtml(n.name)}: ${n.inCalls} in-calls, PageRank ${n.pageRank.toFixed(3)}</title>` +
            `<circle cx="${p.x}" cy="${p.y}" r="${r}" fill="${fill}" stroke="${stroke}" stroke-width="2"/>` +
            `<text x="${p.x}" y="${p.y + r + 14}" fill="#e5e7f0" font-size="11" ` +
            `text-anchor="middle">${escapeHtml(shortName(n.name))}</text>` +
            `</g>`;

    }).join("");

    graphBox.innerHTML = `
        <svg viewBox="0 0 ${W} ${H}" style="width:100%;height:auto;display:block">
            <defs>
                <marker id="arr" viewBox="0 0 10 10" refX="9" refY="5"
                        markerUnits="userSpaceOnUse" markerWidth="9" markerHeight="9"
                        orient="auto">
                    <path d="M0 0 L10 5 L0 10 z" fill="#818cf8"/>
                </marker>
            </defs>
            ${edgeSvg}
            ${nodeSvg}
        </svg>
    `;

    const rows = nodes.map((n, i) => {

        const tag = i === 0
            ? `<span style="color:#ef4444;font-weight:600">bottleneck</span>`
            : "";

        return `<div style="display:grid;grid-template-columns:20px 1fr 52px 60px;` +
            `gap:6px;padding:6px 0;border-bottom:1px solid rgba(255,255,255,0.06);` +
            `font-size:12px;align-items:center">` +
            `<span style="opacity:0.6">${i + 1}</span>` +
            `<span>${escapeHtml(shortName(n.name))} ${tag}</span>` +
            `<span style="text-align:right">${n.inCalls}</span>` +
            `<span style="text-align:right">${n.pageRank.toFixed(3)}</span>` +
            `</div>`;

    }).join("");

    rankBox.innerHTML = `
        <div style="display:grid;grid-template-columns:20px 1fr 52px 60px;gap:6px;
                    font-size:10px;opacity:0.6;padding-bottom:4px">
            <span>#</span><span>Service</span>
            <span style="text-align:right">In-calls</span>
            <span style="text-align:right">PageRank</span>
        </div>
        ${rows}
        <p style="font-size:10px;opacity:0.55;margin-top:8px">
            ${escapeHtml(data.algorithm || "")}
        </p>
    `;

    graphMeta.textContent =
        "Updated " + new Date().toLocaleTimeString();
}


async function loadGraph() {

    try {

        const response =
            await fetch("/api/graph", {
                cache: "no-store"
            });

        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }

        renderGraph(await response.json());

    } catch (error) {

        console.error(
            "Failed to load graph:",
            error
        );

        graphMeta.textContent = "Neo4j unavailable";
    }
}


document
    .getElementById("assistantToggle")
    .addEventListener("click", () => {

        alert(
            "AI Assistant will be connected after the log analytics and LLM layer are implemented."
        );

    });


async function refreshDashboard() {

    await checkAllServices();

    await loadLogs();

    await loadStats();

    await loadGraph();
}


refreshDashboard();


setInterval(
    checkAllServices,
    5000
);


setInterval(
    loadLogs,
    2000
);


setInterval(
    loadStats,
    2000
);


setInterval(
    loadGraph,
    5000
);