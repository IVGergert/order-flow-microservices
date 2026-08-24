let courierStatus = "OFFLINE";
let currentDelivery = null;
let waitingDeliveries = [];
let historyDeliveries = [];

document.addEventListener("DOMContentLoaded", async () => {
    if (!getAccessToken()) {
        window.location.href = "/";
        return;
    }

    renderUserInfo();

    await fetchCourierStatus();
    await fetchTodayStats();
    await fetchCurrentDelivery();
});

function getAccessToken() {
    return localStorage.getItem("accessToken");
}

function getUserEmail() {
    return localStorage.getItem("userEmail") || "Курьер";
}

function buildAuthHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${getAccessToken()}`
    };
}

function logout() {
    localStorage.clear();
    window.location.href = "/";
}

function renderUserInfo() {
    const email = getUserEmail();
    const emailElement = document.getElementById("userEmail");
    const avatarElement = document.getElementById("userAvatar");

    if (emailElement) emailElement.textContent = email;
    if (avatarElement) avatarElement.textContent = email.charAt(0).toUpperCase();
}


/** Запрос статуса курьера */
async function fetchCourierStatus() {
    try {
        const response = await fetch("/api/deliveries/courier/status", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            logout();
            return;
        }

        if (response.ok) {
            const data = await response.json();
            courierStatus  = data.status;
            updateStatusUI();
        }
    } catch (error) {
        showError(error.message);
    }
}

/** Выйти на линию (goOnline) */
async function goOnline() {
    try {
        const response = await fetch("/api/deliveries/courier/go-online", {
            method: "POST",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
        courierStatus = data.status;
        updateStatusUI()
        showSuccess("Вы успешно вышли на линию!");
        await fetchWaitingDeliveries();

    } catch (error) {
        showError(error.message);
    }
}

/** Уйти с линии (goOffline) */
async function goOffline() {
    try {
        const response = await fetch("/api/deliveries/courier/go-offline", {
            method: "POST",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
        courierStatus = data.status;
        updateStatusUI();
        showSuccess("Вы ушли с линии.");

    } catch (error) {
        showError(error.message);
    }
}

/** Обновление кнопок и бейджей статуса в UI */
function updateStatusUI() {
    const toggleBtn = document.getElementById("statusToggleButton");
    const toggleText = document.getElementById("statusToggleText");
    const statusBadge = document.getElementById("courierStatusBadge");

    const isOnline = courierStatus !== "OFFLINE";

    if (toggleBtn) {
        toggleBtn.className = `status-toggle-button ${isOnline ? 'online' : 'offline'}`;
    }

    if (toggleText) {
        toggleText.textContent = isOnline ? "На линии (Завершить)" : "Выйти на линию";
    }

    if (statusBadge) {
        statusBadge.className = `courier-status-badge ${isOnline ? 'online' : 'offline'}`;
        statusBadge.textContent = isOnline ? "Онлайн" : "Оффлайн";
    }
}

/** Загрузка активного заказа курьера */
async function fetchCurrentDelivery() {
    showCurrentLoading();

    try {
        const response = await fetch("/api/deliveries/current", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (response.status === 204) {
            currentDelivery = null;
            renderCurrentDelivery();
            return;
        }

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        currentDelivery = await response.json();
        renderCurrentDelivery();

    } catch (error) {
        currentDelivery = null;
        renderCurrentDelivery();
        showError(error.message);
    } finally {
        hideCurrentLoading();
    }
}

/** Отрисовка активного заказа */
function renderCurrentDelivery() {
    const card = document.getElementById("activeDeliveryCard");
    const empty = document.getElementById("noActiveDelivery");
    const pickupBtn = document.getElementById("pickupButton");
    const completeBtn = document.getElementById("completeButton");

    if (!currentDelivery) {
        if (card) card.classList.add("hidden");
        if (empty) empty.classList.remove("hidden");
        return;
    }

    if (empty) empty.classList.add("hidden");
    if (card) card.classList.remove("hidden");

    document.getElementById("activeOrderId").textContent = `Заказ №${currentDelivery.orderId}`;
    document.getElementById("activeAddress").textContent = currentDelivery.address || "Не указан";
    document.getElementById("activeEta").textContent = currentDelivery.etaMinutes ? `~${currentDelivery.etaMinutes} мин.` : "-";
    document.getElementById("activeDeliveryStatus").textContent = getDeliveryStatusTitle(currentDelivery.deliveryStatus);

    // Управление пошаговыми кнопками
    if (currentDelivery.deliveryStatus === "COURIER_ASSIGNED") {
        document.getElementById("activeCourierState").textContent = "Едет в ресторан 🏪";
        pickupBtn?.classList.remove("hidden");
        completeBtn?.classList.add("hidden");
    } else if (currentDelivery.deliveryStatus === "PICKED_UP") {
        document.getElementById("activeCourierState").textContent = "В пути к клиенту 🚚";
        pickupBtn?.classList.add("hidden");
        completeBtn?.classList.remove("hidden");
    }
}

/** Принять заказ в работу */
async function acceptDelivery(orderId) {
    try {
        const response = await fetch(`/api/deliveries/${orderId}/accept`, {
            method: "POST",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        showSuccess(`Заказ №${orderId} успешно принят!`);

        await fetchCourierStatus();
        await fetchCurrentDelivery();

        showSection("current");

    } catch (error) {
        showError(error.message);
    }
}

/** Забрать заказ из ресторана */
async function pickUpOrder(orderId) {
    try {
        const response = await fetch(`/api/deliveries/${orderId}/pickup`, {
            method: "POST",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        showSuccess("Заказ забран из ресторана! Направляйтесь к клиенту.");

        await fetchCourierStatus();
        await fetchCurrentDelivery();

    } catch (error) {
        showError(error.message);
    }
}

/** Завершить доставку */
async function completeDelivery(orderId) {
    try {
        const response = await fetch(`/api/deliveries/${orderId}/complete`, {
            method: "POST",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        showSuccess(`Заказ №${orderId} успешно доставлен! 🎉`);

        await fetchCourierStatus();
        await fetchTodayStats();
        await fetchCurrentDelivery();

    } catch (error) {
        showError(error.message);
    }
}

async function fetchWaitingDeliveries() {
    const container = document.getElementById("waitingList");
    const empty = document.getElementById("waitingEmpty");
    const loading = document.getElementById("waitingLoading");

    if (loading) loading.classList.remove("hidden");
    if (empty) empty.classList.add("hidden");
    if (container) container.innerHTML = "";

    if (courierStatus === "OFFLINE") {
        if (loading) loading.classList.add("hidden");

        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🟠</div>
                    <h3>Вы сейчас офлайн</h3>
                    <p>Выйдите на линию, чтобы увидеть доступные заказы.</p>
                    <button type="button" class="primary-button" id="goOnlineFromWaitingButton">
                        Выйти на линию
                    </button>
                </div>
            `;

            document
                .getElementById("goOnlineFromWaitingButton")
                ?.addEventListener("click", goOnline);
        }

        return;
    }

    if (courierStatus !== "AVAILABLE") {
        if (loading) loading.classList.add("hidden");

        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🚚</div>
                    <h3>У вас есть активная доставка</h3>
                    <p>Доступные заказы будут доступны после завершения текущей доставки.</p>
                </div>
            `;
        }

        return;
    }

    try {
        const response = await fetch("/api/deliveries/waiting", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        waitingDeliveries = await response.json();
        renderWaitingDeliveries();

    } catch (error) {
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h3>Не удалось загрузить заказы</h3>
                    <p>${escapeHtml(error.message)}</p>
                </div>
            `;
        }
    } finally {
        if (loading) loading.classList.add("hidden");
    }
}

function renderWaitingDeliveries() {
    const container = document.getElementById("waitingList");
    const empty = document.getElementById("waitingEmpty");

    if (!container) return;
    container.innerHTML = "";

    if (!waitingDeliveries || waitingDeliveries.length === 0) {
        if (empty) empty.classList.remove("hidden");
        return;
    }

    if (empty) empty.classList.add("hidden");

    waitingDeliveries.forEach(delivery => {
        const card = document.createElement("article");
        card.className = "delivery-card";

        card.innerHTML = `
            <div class="delivery-card-header">
                <h3>Заказ №${delivery.orderId}</h3>
                <span class="order-status status-warning">Ожидает курьера</span>
            </div>

            <div class="delivery-card-body">
                <div>
                    <span>Адрес доставки</span>
                    <strong>${escapeHtml(delivery.address || "Не указан")}</strong>
                </div>

                <div>
                    <span>Время на доставку</span>
                    <strong>~${delivery.etaMinutes || 30} мин.</strong>
                </div>

                <div>
                    <button type="button" class="primary-button accept-btn">
                        Принять заказ
                    </button>
                </div>
            </div>
        `;

        card.querySelector(".accept-btn").addEventListener("click", () => {
            acceptDelivery(delivery.orderId);
        });

        container.appendChild(card);
    });
}

async function fetchTodayStats() {
    try {
        const response = await fetch("/api/deliveries/statistics/today", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (response.ok) {
            const data = await response.json();
            const countElem = document.getElementById("todayCount");
            if (countElem) {
                countElem.textContent = data.completedDeliveriesCount || data.completedToday || 0;
            }
        }
    } catch {}
}

async function fetchHistoryDeliveries() {
    const container = document.getElementById("historyList");
    const empty = document.getElementById("historyEmpty");
    const loading = document.getElementById("historyLoading");

    if (loading) loading.classList.remove("hidden");
    if (empty) empty.classList.add("hidden");
    if (container) container.innerHTML = "";

    try {
        const response = await fetch("/api/deliveries/history", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        historyDeliveries = await response.json();
        renderHistory();

    } catch (error) {
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h3>Не удалось загрузить историю</h3>
                    <p>${escapeHtml(error.message)}</p>
                </div>
            `;
        }
    } finally {
        if (loading) loading.classList.add("hidden");
    }
}

function renderHistory() {
    const container = document.getElementById("historyList");
    const empty = document.getElementById("historyEmpty");

    if (!container) return;

    container.innerHTML = "";

    if (!historyDeliveries || historyDeliveries.length === 0) {
        if (empty) empty.classList.remove("hidden");
        return;
    }

    if (empty) empty.classList.add("hidden");

    historyDeliveries.forEach(delivery => {
        const card = document.createElement("article");
        card.className = "delivery-card";

        card.innerHTML = `
            <div class="delivery-card-header">
                <div>
                    <h3>Заказ №${delivery.orderId}</h3>
                    <span class="order-date">
                        Доставка завершена: ${formatDate(delivery.completedAt)}
                    </span>
                </div>
                
                <span class="order-status status-success">
                    ${getDeliveryStatusTitle(delivery.deliveryStatus)}
                </span>
                
            </div>

            <div class="delivery-card-body">
                <div>
                    <span>Адрес</span>
                    <strong>${escapeHtml(delivery.address || "-")}</strong>
                </div>

                <div>
                    <span>Время доставки</span>
                    <strong>${delivery.etaMinutes ? `${delivery.etaMinutes} мин.` : "-"}</strong>
                </div>
            </div>
        `;

        container.appendChild(card);
    });
}

function showSection(section) {
    document.getElementById("currentSection")?.classList.add("hidden");
    document.getElementById("waitingSection")?.classList.add("hidden");
    document.getElementById("historySection")?.classList.add("hidden");
    document.getElementById("profileSection")?.classList.add("hidden");

    if (section === "current") {
        document.getElementById("currentSection")?.classList.remove("hidden");
        updatePageHeader("Текущий заказ", "Управление вашим активным заказом");
        fetchCurrentDelivery();
    }

    if (section === "waiting") {
        document.getElementById("waitingSection")?.classList.remove("hidden");
        updatePageHeader("Доступные заказы", "Выберите заказ для доставки");
        fetchWaitingDeliveries();
    }

    if (section === "history") {
        document.getElementById("historySection")?.classList.remove("hidden");
        updatePageHeader("История доставок", "Выполненные заказы");
        fetchHistoryDeliveries();
    }

    if (section === "profile") {
        showProfile();
        return;
    }

    updateNavigation(section);
}

function showProfile() {
    let profileSection = document.getElementById("profileSection");
    if (!profileSection) {
        profileSection = createProfileSection();
        document.querySelector(".main-content").appendChild(profileSection);
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

            <button type="button" class="logout-profile-button" id="profileLogoutButton">
                Выйти из аккаунта
            </button>
        </div>
    `;

    section.querySelector("#profileLogoutButton").addEventListener("click", logout);
    return section;
}

function updateNavigation(section) {
    document.querySelectorAll(".nav-item").forEach(item => {
        item.classList.remove("active");
    });

    const activeItem = document.querySelector(`[data-section="${section}"]`);
    if (activeItem) activeItem.classList.add("active");
}

function updatePageHeader(title, subtitle) {
    const titleElement = document.getElementById("pageTitle");
    const subtitleElement = document.getElementById("pageSubtitle");

    if (titleElement) titleElement.textContent = title;
    if (subtitleElement) subtitleElement.textContent = subtitle;
}

function showCurrentLoading() {
    document.getElementById("currentLoading")?.classList.remove("hidden");
}

function hideCurrentLoading() {
    document.getElementById("currentLoading")?.classList.add("hidden");
}

function getDeliveryStatusTitle(status) {
    const statuses = {
        WAITING_FOR_COURIER: "Ожидает курьера",
        COURIER_ASSIGNED: "Назначен курьер",
        PICKED_UP: "Заказ забран",
        DELIVERED: "Доставлен 🎉"
    };
    return statuses[status] || status || "В работе";
}

async function getErrorMessage(response) {
    try {
        const data = await response.json();
        return data.message || data.error || data.detail || `Ошибка: ${response.status}`;
    } catch {
        return `Ошибка сервера: ${response.status}`;
    }
}

function showError(message) {
    const alertBox = document.getElementById("alertBox");
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-danger";
}

function showSuccess(message) {
    const alertBox = document.getElementById("alertBox");
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-success";

    clearTimeout(showSuccess.timeout);
    showSuccess.timeout = setTimeout(() => {
        alertBox.className = "alert hidden";
    }, 3500);
}

function formatDate(date) {
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

function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener("click", event => {
    const navItem = event.target.closest(".nav-item");
    if (navItem) {
        const section = navItem.dataset.section;
        if (section) showSection(section);
        return;
    }

    // Переключатель Онлайн / Оффлайн
    if (event.target.closest("#statusToggleButton")) {
        if (courierStatus === "OFFLINE") {
            goOnline();
        } else {
            goOffline();
        }
        return;
    }

    // Пошаговые кнопки рабочего процесса
    if (event.target.closest("#pickupButton")) {
        if (currentDelivery) pickUpOrder(currentDelivery.orderId);
        return;
    }

    if (event.target.closest("#completeButton")) {
        if (currentDelivery) completeDelivery(currentDelivery.orderId);
        return;
    }

    if (event.target.closest("#goToWaitingButton")) { showSection("waiting"); return; }
    if (event.target.closest("#refreshWaitingButton")) { fetchWaitingDeliveries(); return; }
    if (event.target.closest("#refreshHistoryButton")) { fetchHistoryDeliveries(); return; }
    if (event.target.closest("#logoutButton")) { logout(); return; }
});