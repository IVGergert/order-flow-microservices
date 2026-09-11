import {
    getCourierStatus,
    goOnlineRequest,
    goOfflineRequest,
    validateLogoutRequest,
    getTodayStatistics,
    getCurrentDeliveryRequest,
    getWaitingDeliveriesRequest,
    getHistoryDeliveriesRequest,
    acceptDeliveryRequest,
    pickUpOrderRequest,
    completeDeliveryRequest
} from "./api.js";

import {
    state
} from "./state.js";

import {
    updateStatusUI,
    showSection,
    showError,
    showSuccess,
    showCurrentLoading,
    hideCurrentLoading,
    getDeliveryStatusTitle,
    formatDate,
    escapeHtml
} from "./ui.js";

// Courier status

export async function fetchCourierStatus() {
    try {
        const data = await getCourierStatus();

        if (!data) return;

        state.courierStatus = data.status;
        updateStatusUI();
    } catch (error) {
        showError(error.message);
    }
}

export async function goOnline() {
    try {
        const data = await goOnlineRequest();

        if (!data) return;

        state.courierStatus = data.status;
        updateStatusUI();

        showSuccess("Вы успешно вышли на линию!");

        await fetchWaitingDeliveries();
    } catch (error) {
        showError(error.message);
    }
}

export async function goOffline() {
    try {
        const data = await goOfflineRequest();

        if (!data) return;

        state.courierStatus = data.status;
        updateStatusUI();

        showSuccess("Вы ушли с линии.");
    } catch (error) {
        showError(error.message);
    }
}

// Logout

export async function validateLogout() {
    try {
        await validateLogoutRequest();
        return true;
    } catch (error) {
        showError(error.message);
        return false;
    }
}

// Current delivery

export async function fetchCurrentDelivery() {
    showCurrentLoading();

    try {
        const data = await getCurrentDeliveryRequest();

        state.currentDelivery = data;
        renderCurrentDelivery();
    } catch (error) {
        state.currentDelivery = null;
        renderCurrentDelivery();
        showError(error.message);
    } finally {
        hideCurrentLoading();
    }
}

function renderCurrentDelivery() {
    const card = document.getElementById("activeDeliveryCard");
    const empty = document.getElementById("noActiveDelivery");
    const pickupBtn = document.getElementById("pickupButton");
    const completeBtn = document.getElementById("completeButton");

    const delivery = state.currentDelivery;

    if (!delivery) {
        card?.classList.add("hidden");
        empty?.classList.remove("hidden");
        return;
    }

    empty?.classList.add("hidden");
    card?.classList.remove("hidden");

    document.getElementById("activeOrderId").textContent =
        `Заказ №${delivery.orderId}`;

    document.getElementById("activeAddress").textContent =
        delivery.address || "Не указан";

    document.getElementById("activeEta").textContent =
        delivery.etaMinutes
            ? `~${delivery.etaMinutes} мин.`
            : "-";

    document.getElementById("activeDeliveryStatus").textContent =
        getDeliveryStatusTitle(delivery.deliveryStatus);

    if (delivery.deliveryStatus === "COURIER_ASSIGNED") {
        document.getElementById("activeCourierState").textContent =
            "Едет в ресторан 🏪";

        pickupBtn?.classList.remove("hidden");
        completeBtn?.classList.add("hidden");
    }

    if (delivery.deliveryStatus === "PICKED_UP") {
        document.getElementById("activeCourierState").textContent =
            "В пути к клиенту 🚚";

        pickupBtn?.classList.add("hidden");
        completeBtn?.classList.remove("hidden");
    }
}

// Delivery actions

export async function acceptDelivery(orderId) {
    try {
        await acceptDeliveryRequest(orderId);

        showSuccess(`Заказ №${orderId} успешно принят!`);

        await fetchCourierStatus();
        await fetchCurrentDelivery();

        showSection("current");
    } catch (error) {
        showError(error.message);
    }
}

export async function pickUpOrder(orderId) {
    try {
        await pickUpOrderRequest(orderId);

        showSuccess("Заказ забран из ресторана! Направляйтесь к клиенту.");

        await fetchCourierStatus();
        await fetchCurrentDelivery();
    } catch (error) {
        showError(error.message);
    }
}

export async function completeDelivery(orderId) {
    try {
        await completeDeliveryRequest(orderId);

        showSuccess(`Заказ №${orderId} успешно доставлен!`);

        await fetchCourierStatus();
        await fetchTodayStats();
        await fetchCurrentDelivery();
    } catch (error) {
        showError(error.message);
    }
}

// Waiting deliveries

export async function fetchWaitingDeliveries() {
    const container = document.getElementById("waitingList");
    const empty = document.getElementById("waitingEmpty");
    const loading = document.getElementById("waitingLoading");

    loading?.classList.remove("hidden");
    empty?.classList.add("hidden");

    if (container) {
        container.innerHTML = "";
    }

    try {
        const data = await getWaitingDeliveriesRequest();

        state.waitingDeliveries = data || [];

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
        loading?.classList.add("hidden");
    }
}

function renderWaitingDeliveries() {
    const container = document.getElementById("waitingList");
    const empty = document.getElementById("waitingEmpty");

    if (!container) return;

    container.innerHTML = "";

    const deliveries = state.waitingDeliveries;

    if (!deliveries?.length) {
        empty?.classList.remove("hidden");
        return;
    }

    empty?.classList.add("hidden");

    deliveries.forEach(delivery => {
        const card = document.createElement("article");

        card.className = "delivery-card";

        card.innerHTML = `
            <div class="delivery-card-header">
                <h3>Заказ №${delivery.orderId}</h3>

                <span class="order-status status-warning">
                    Ожидает курьера
                </span>
            </div>

            <div class="delivery-card-body">
                <div>
                    <span>Адрес доставки</span>
                    <strong>
                        ${escapeHtml(delivery.address)}
                    </strong>
                </div>

                <div>
                    <span>Время на доставку</span>
                    <strong>
                        ~${delivery.etaMinutes} мин.
                    </strong>
                </div>

                <div>
                    <button
                        type="button"
                        class="primary-button accept-btn"
                        data-order-id="${delivery.orderId}"
                    >
                        Принять заказ
                    </button>
                </div>
            </div>
        `;

        container.appendChild(card);
    });
}

// Statistics

export async function fetchTodayStats() {
    try {
        const data = await getTodayStatistics();

        if (!data) return;

        const countElement = document.getElementById("todayCount");

        if (countElement) {
            countElement.textContent =
                data.completedDeliveriesCount ??
                data.completedToday ??
                0;
        }
    } catch (error) {
        showError(error.message);
    }
}

// History

export async function fetchHistoryDeliveries() {
    const container = document.getElementById("historyList");
    const empty = document.getElementById("historyEmpty");
    const loading = document.getElementById("historyLoading");

    loading?.classList.remove("hidden");
    empty?.classList.add("hidden");

    if (container) {
        container.innerHTML = "";
    }

    try {
        const data = await getHistoryDeliveriesRequest();

        state.historyDeliveries = data || [];

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
        loading?.classList.add("hidden");
    }
}

function renderHistory() {
    const container = document.getElementById("historyList");
    const empty = document.getElementById("historyEmpty");

    if (!container) return;

    container.innerHTML = "";

    const deliveries = state.historyDeliveries;

    if (!deliveries?.length) {
        empty?.classList.remove("hidden");
        return;
    }

    empty?.classList.add("hidden");

    deliveries.forEach(delivery => {
        const card = document.createElement("article");

        card.className = "delivery-card";

        card.innerHTML = `
            <div class="delivery-card-header">
                <div>
                    <h3>Заказ №${delivery.orderId}</h3>

                    <span class="order-date">
                        Доставка завершена:
                        ${formatDate(delivery.completedAt)}
                    </span>
                </div>

                <span class="order-status status-success">
                    ${getDeliveryStatusTitle(delivery.deliveryStatus)}
                </span>
            </div>

            <div class="delivery-card-body">
                <div>
                    <span>Адрес</span>
                    <strong>
                        ${escapeHtml(delivery.address || "-")}
                    </strong>
                </div>

                <div>
                    <span>Время доставки</span>
                    <strong>
                        ${delivery.etaMinutes
            ? `${delivery.etaMinutes} мин.`
            : "-"}
                    </strong>
                </div>
            </div>
        `;

        container.appendChild(card);
    });
}
