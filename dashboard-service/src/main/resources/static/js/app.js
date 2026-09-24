// ============================================================
// SERVICE CONFIGURATION
// ============================================================

const services = [
    { name: "auth-service", port: 8081 },
    { name: "user-service", port: 8082 },
    { name: "movies-service", port: 8083 },
    { name: "webseries-service", port: 8084 },
    { name: "payment-service", port: 8085 },
];


// ============================================================
// DEMO MODE
// ============================================================

const DEMO_MODE = true;


// ============================================================
// DOM REFERENCES
// ============================================================

const nodeContainer = document.getElementById("serviceNodes");
const logFeed = document.getElementById("logFeed");
const topEmitter = document.getElementById("topEmitter");
const upCount = document.getElementById("upCount");
const lastCheck = document.getElementById("lastCheck");


// ============================================================
// CREATE SERVICE NODES
// ============================================================

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


// ============================================================
// UPDATE SERVICE UI
// ============================================================

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


// ============================================================
// CHECK SINGLE SERVICE
// ============================================================

async function checkOne(service) {

    if (DEMO_MODE) {

        setServiceStatus(service, true);

        return true;
    }

    try {

        const response = await fetch(
            `http://localhost:${service.port}/actuator/health`,
            {
                cache: "no-store"
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
    }
}


// ============================================================
// CHECK ALL SERVICES
// ============================================================

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


// ============================================================
// FORMAT TIMESTAMP
// ============================================================

function formatTimestamp(timestamp) {

    if (!timestamp) {
        return "--";
    }

    return new Date(timestamp).toLocaleTimeString();
}


// ============================================================
// LEVEL CLASS
// ============================================================

function getLevelClass(level) {

    if (level === "ERROR") {
        return "log-error";
    }

    if (level === "WARN" || level === "WARNING") {
        return "log-warn";
    }

    return "log-info";
}


// ============================================================
// RENDER LOG EVENTS
// ============================================================

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

        topEmitter.textContent = "—";

        return;
    }

    const serviceCounts = {};

    logs.forEach(log => {

        const service = log.service || "unknown";

        serviceCounts[service] =
            (serviceCounts[service] || 0) + 1;
    });

    const topService =
        Object.entries(serviceCounts)
            .sort((a, b) => b[1] - a[1])[0];

    if (topService) {

        topEmitter.textContent =
            `${topService[0]} (${topService[1]})`;

    } else {

        topEmitter.textContent = "—";
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


// ============================================================
// BASIC HTML ESCAPE
// ============================================================

function escapeHtml(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


// ============================================================
// FETCH LOGS FROM SPRING BOOT
// ============================================================

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


// ============================================================
// AI ASSISTANT PLACEHOLDER
// ============================================================

document
    .getElementById("assistantToggle")
    .addEventListener("click", () => {

        alert(
            "AI Assistant will be connected after the log analytics and LLM layer are implemented."
        );

    });


// ============================================================
// START DASHBOARD
// ============================================================

async function refreshDashboard() {

    await checkAllServices();

    await loadLogs();
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