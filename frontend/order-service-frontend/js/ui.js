const alertBox = document.getElementById("alertBox");

export function showError(message) {
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-danger";
}

export function showSuccess(message) {
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-success";

    clearTimeout(showSuccess.timeout);
    showSuccess.timeout = setTimeout(() => {
        alertBox.className = "alert hidden";
    }, 3500);
}

export function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

export function formatDate(date) {
    if (!date) return "";

    const parsed = new Date(date);

    if (Number.isNaN(parsed.getTime())) {
        return "";
    }

    return parsed.toLocaleString("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}