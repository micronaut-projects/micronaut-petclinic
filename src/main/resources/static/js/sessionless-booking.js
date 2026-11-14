const root = document.getElementById("sessionless-booking");
if (root) {
    const element = id => root.querySelector("#sessionless-" + id);
    const labels = Object.fromEntries([...root.querySelectorAll("[data-sessionless-key]")]
        .map(node => [node.dataset.sessionlessKey, node.textContent]));
    const text = key => labels[key] ?? labels.error;
    const base = "/oracle/transaction-priority/sessionless";
    const storageKey = "petclinic.sessionless-booking";
    let pending;
    let storageUnavailable = false;
    let busy = false;
    let appointments = [];
    let timer;

    function message(key, error = false) {
        element("message").textContent = text(key) + (pending && storageUnavailable ? " " + text("storageError") : "");
        element("message").classList.toggle("text-danger", error);
    }

    function remember(hold) {
        pending = hold;
        try {
            if (hold) sessionStorage.setItem(storageKey, JSON.stringify(hold));
            else sessionStorage.removeItem(storageKey);
        } catch {
            // Keep the in-memory handle usable if browser storage is unavailable.
            storageUnavailable = true;
        }
    }

    try {
        const stored = JSON.parse(sessionStorage.getItem(storageKey) || "null");
        if (stored && Number.isInteger(stored.appointmentId) && typeof stored.transactionId === "string"
                && Number.isFinite(stored.deadline) && typeof stored.label === "string") pending = stored;
    } catch {
        storageUnavailable = true;
    }

    function render() {
        const select = element("select");
        const selected = select.value;
        select.replaceChildren();
        for (const appointment of appointments) {
            if (appointment.status === "AVAILABLE" && appointment.id !== pending?.appointmentId) {
                select.add(new Option(appointment.displayOrder + ". " + appointment.label, appointment.id));
            }
        }
        if ([...select.options].some(option => option.value === selected)) select.value = selected;
        if (!select.options.length) select.add(new Option(text("noAvailable"), ""));
        select.disabled = busy || !!pending;
        element("hold").disabled = busy || !!pending || !select.value;
        element("confirm").disabled = busy || !pending;
        element("cancel").disabled = busy || !pending;
        element("refresh").disabled = busy;
        element("reset").disabled = busy || !!pending;
        element("pending").hidden = !pending;
        element("label").textContent = pending?.label ?? "";
        element("appointments").replaceChildren(...appointments.map(appointment => {
            const row = document.createElement("tr");
            for (const value of [appointment.label, appointment.status]) {
                const cell = document.createElement("td");
                cell.textContent = value;
                row.append(cell);
            }
            return row;
        }));
        tick();
    }

    function tick() {
        if (!pending) return;
        const seconds = Math.max(0, Math.ceil((pending.deadline - Date.now()) / 1000));
        element("countdown").textContent = seconds > 0
            ? String(Math.floor(seconds / 60)).padStart(2, "0") + ":" + String(seconds % 60).padStart(2, "0")
            : text("elapsed");
        // Only Oracle can decide whether the transaction is still resumable.
    }

    async function request(path, body, method = "POST") {
        const response = await fetch(base + path, {
            method, headers: {Accept: "application/json", ...(body ? {"Content-Type": "application/json"} : {})},
            ...(body ? {body: JSON.stringify(body)} : {})
        });
        const result = await response.json();
        if (!response.ok) {
            const error = new Error(result.message || text("error"));
            error.code = result.code;
            error.status = response.status;
            error.missingAppointment = response.status === 404 && (path === "/confirm" || path === "/cancel");
            throw error;
        }
        return result;
    }

    async function refresh() {
        const result = await request("/appointments", null, "GET");
        if (!Array.isArray(result)) throw new Error(text("error"));
        appointments = result;
    }

    async function action(operation) {
        if (busy) return;
        busy = true;
        message("working");
        render();
        try {
            await operation();
        } catch (error) {
            if (error.code === "UNAVAILABLE" || error.missingAppointment) remember(null);
            element("message").textContent = error.code ? text("error." + error.code)
                : error.status ? error.message : text("error");
            element("message").classList.add("text-danger");
            // Refreshing committed state never resumes or prolongs a hold.
            await refresh().catch(() => {});
        } finally {
            busy = false;
            render();
        }
    }

    element("hold").addEventListener("click", () => {
        if (busy || pending || !element("select").value) return;
        const appointmentId = Number(element("select").value);
        const label = appointments.find(a => a.id === appointmentId)?.label ?? "";
        action(async () => {
            const result = await request("/hold?appointmentId=" + encodeURIComponent(appointmentId));
            if (!result.transactionId || result.outcome !== "HELD") throw new Error(text("error"));
            remember({appointmentId, label, transactionId: result.transactionId,
                deadline: Date.now() + result.holdSeconds * 1000});
            appointments = result.appointments ?? [];
            message("held");
        });
    });

    for (const operation of ["confirm", "cancel"]) {
        element(operation).addEventListener("click", () => {
            if (busy || !pending) return;
            action(async () => {
                const result = await request("/" + operation,
                    {appointmentId: pending.appointmentId, transactionId: pending.transactionId});
                remember(null);
                appointments = result.appointments ?? [];
                message(operation === "confirm" ? "confirmed" : "cancelled");
            });
        });
    }

    element("refresh").addEventListener("click", () => action(async () => {
        await refresh();
        message(pending ? "held" : "ready");
    }));
    element("reset").addEventListener("click", () => {
        if (pending) return;
        action(async () => {
            appointments = await request("/reset");
            message("ready");
        });
    });

    // Seed from the server without embedding transaction IDs in the document.
    element("hold").disabled = true;
    action(async () => {
        await refresh();
        message(pending ? "held" : "ready");
    });
    timer = setInterval(tick, 1000);
    window.addEventListener("pagehide", () => clearInterval(timer));
    window.addEventListener("pageshow", event => {
        if (event.persisted) window.location.reload();
    });
}
