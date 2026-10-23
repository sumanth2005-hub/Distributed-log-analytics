const services = [
    { name: "auth-service", port: 8081 },
    { name: "user-service", port: 8082 },
    { name: "movies-service", port: 8083 },
    { name: "webseries-service", port: 8084 },
    { name: "payment-service", port: 8085 },
];

const DEMO_MODE = false;

const HEALTH_TIMEOUT_MS = 1500;

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


async function checkOne(service) {

    if (DEMO_MODE) {
        setServiceStatus(service, true);
        return true;
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), HEALTH_TIMEOUT_MS);

    try {

        const response = await fetch(
            `http://localhost:${service.port}/actuator/health`,
            {
                cache: "no-store",
                signal: controller.signal
            }
        );

        if (!response.ok) {
            throw new Error("Health endpoint unavailable");
        }

        const data = await response.json();

        const isUp = data.status === "UP";

        setServiceStatus(service, isUp);

        return isUp;

    } catch (error) {

        setServiceStatus(service, false);

        return false;

    } finally {

        clearTimeout(timer);
    }
}


async function checkAllServices() {

    lastCheck.textContent = "Checking...";

    const results = await Promise.all(
        services.map(checkOne)
    );

    const runningServices =
        results.filter(Boolean).length;

    upCount.textContent =
        `${runningServices} / ${services.length}`;

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