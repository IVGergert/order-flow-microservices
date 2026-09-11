import {
    state,
    getUserEmail,
} from "./state.js";

// User

export function renderUserInfo() {
    const email = getUserEmail();

    const emailElement = document.getElementById("userEmail");
    const avatarElement = document.getElementById("userAvatar");

    if (emailElement) {
        emailElement.textContent = email;
    }

    if (avatarElement) {
        avatarElement.textContent = email.charAt(0).toUpperCase();
    }
}

// Courier status

export function updateStatusUI() {
    const toggleBtn = document.getElementById("statusToggleButton");
    const toggleText = document.getElementById("statusToggleText");
    const statusBadge = document.getElementById("courierStatusBadge");
    const isOnline = state.courierStatus !== "OFFLINE";

    if (toggleBtn) {
        toggleBtn.className = `status-toggle-button ${isOnline ? "online" : "offline"}`;
    }

    if (toggleText) {
        toggleText.textContent =
            isOnline ?
                "На линии (Завершить)"
                : "Выйти на линию";
    }

    if (statusBadge) {
        statusBadge.className =
            `courier-status-badge ${isOnline ? "online" : "offline"}`;

        statusBadge.textContent = isOnline ? "Онлайн" : "Оффлайн";
    }
}

export function showSection(section) {
    document.getElementById("currentSection")?.classList.add("hidden");
    document.getElementById("waitingSection")?.classList.add("hidden");
    document.getElementById("historySection")?.classList.add("hidden");
    document.getElementById("profileSection")?.classList.add("hidden");

    if (section === "current") {
        document.getElementById("currentSection")?.classList.remove("hidden");
        updatePageHeader("Текущий заказ", "Управление вашим активным заказом");
    }

    if (section === "waiting") {
        document.getElementById("waitingSection")?.classList.remove("hidden");
        updatePageHeader("Доступные заказы", "Выберите заказ для доставки");
    }

    if (section === "history") {
        document.getElementById("historySection")?.classList.remove("hidden");
        updatePageHeader("История доставок", "Выполненные заказы");
    }

    if (section === "profile") {
        showProfile();
        return;
    }

    updateNavigation(section);
}

function updateNavigation(section) {
    document.querySelectorAll(".nav-item").forEach(item => {
        item.classList.remove("active");
    });

    const activeItem = document.querySelector(`[data-section="${section}"]`);

    activeItem?.classList.add("active");
}

function updatePageHeader(title, subtitle) {
    const titleElement = document.getElementById("pageTitle");
    const subtitleElement = document.getElementById("pageSubtitle");

    if (titleElement) {
        titleElement.textContent = title;
    }

    if (subtitleElement) {
        subtitleElement.textContent = subtitle;
    }
}

// Profile

function showProfile() {
    let profileSection = document.getElementById("profileSection");

    if (!profileSection) {
        profileSection = createProfileSection();

        document.querySelector(".main-content")
            ?.appendChild(profileSection);
    }

    profileSection.classList.remove("hidden");
    updatePageHeader("Профиль", "Ваши данные курьера");

    updateNavigation("profile");
}

function createProfileSection() {
    const section = document.createElement("section");

    section.id = "profileSection";
    section.className = "content-section profile-section";

    const email = getUserEmail();

    section.innerHTML = `
        <div class="profile-card">
            <div class="profile-card-header">
                <div class="profile-avatar-large">
                    ${escapeHtml(email.charAt(0).toUpperCase())}
                </div>

                <div>
                    <h2>${escapeHtml(email)}</h2>
                </div>
            </div>

            <div class="profile-info">
                <div class="profile-info-row">
                    <span>Email курьера</span>
                    <strong>${escapeHtml(email)}</strong>
                </div>
            </div>

            <button
                type="button"
                class="logout-profile-button"
                id="profileLogoutButton"
            >
                Выйти из аккаунта
            </button>
        </div>
    `;

    return section;
}

// Alerts

export function showError(message) {
    const alertBox = document.getElementById("alertBox");

    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-danger";
}

export function showSuccess(message) {
    const alertBox = document.getElementById("alertBox");

    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-success";

    clearTimeout(showSuccess.timeout);

    showSuccess.timeout = setTimeout(() => {
        alertBox.className = "alert hidden";
    }, 3500);
}

// Loading

export function showCurrentLoading() {
    document.getElementById("currentLoading")?.classList.remove("hidden");
}

export function hideCurrentLoading() {
    document.getElementById("currentLoading")?.classList.add("hidden");
}

export function getDeliveryStatusTitle(status) {
    const statuses = {
        WAITING_FOR_COURIER: "Ожидает курьера",
        COURIER_ASSIGNED: "Назначен курьер",
        PICKED_UP: "Заказ забран",
        DELIVERED: "Доставлен 🎉"
    };

    return statuses[status] || status || "В работе";
}

export function formatDate(date) {
    if (!date) return "-";

    const parsed = new Date(date);

    if (Number.isNaN(parsed.getTime())) {
        return "-";
    }

    return parsed.toLocaleString("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

export function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}