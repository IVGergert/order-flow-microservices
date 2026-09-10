import {
    getCourierStatus,
    goOnlineRequest,
    goOfflineRequest,
    getTodayStatistics,
    getCurrentDeliveryRequest,
    getWaitingDeliveriesRequest,
    getHistoryDeliveriesRequest,
    acceptDeliveryRequest,
    pickUpOrderRequest,
    completeDeliveryRequest,
    getErrorMessage
} from "./api.js";

import {
    state,
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

export async function fetchCourierStatus() {
    try {
        const response = await getCourierStatus();

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
        state.courierStatus = data.status;
        updateStatusUI();
    } catch (error) {
        showError(error.message);
    }
}

export async function goOnline() {
    try {
        const response = await goOnlineRequest();

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
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
        const response = await goOfflineRequest();

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
        state.courierStatus = data.status;
        updateStatusUI();

        showSuccess("Вы ушли с линии.");
    } catch (error) {
        showError(error.message);
    }
}

export async function fetchCurrentDelivery() {
    showCurrentLoading();

    try {
        const response = await getCurrentDeliveryRequest();

        if (response.status === 204) {
            state.currentDelivery = null;
            renderCurrentDelivery();
            return;
        }

        if (response.status === 401 || response.status === 403) {
            logout();
            return;
        }

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        state.currentDelivery = await response.json();
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

    const currentDelivery = state.currentDelivery;

    if (!currentDelivery) {
        card?.classList.add("hidden");
        empty?.classList.remove("hidden");
        return;
    }

    empty?.classList.add("hidden");
    card?.classList.remove("hidden");

    document.getElementById("activeOrderId").textContent =
        `Заказ №${currentDelivery.orderId}`;

    document.getElementById("activeAddress").textContent =
        currentDelivery.address || "Не указан";

    document.getElementById("activeEta").textContent =
        currentDelivery.etaMinutes ? `~${currentDelivery.etaMinutes} мин.` : "-";

    document.getElementById("activeDeliveryStatus").textContent =
        getDeliveryStatusTitle(currentDelivery.deliveryStatus);

    if (currentDelivery.deliveryStatus === "COURIER_ASSIGNED") {
        document.getElementById("activeCourierState").textContent =
            "Едет в ресторан 🏪";

        pickupBtn?.classList.remove("hidden");
        completeBtn?.classList.add("hidden");
    } else if (currentDelivery.deliveryStatus === "PICKED_UP") {
        document.getElementById("activeCourierState").textContent =
            "В пути к клиенту 🚚";

        pickupBtn?.classList.add("hidden");
        completeBtn?.classList.remove("hidden");
    }
}

export async function acceptDelivery(orderId) {
    try {
        const response = await acceptDeliveryRequest(orderId);

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

export async function pickUpOrder(orderId) {
    try {
        const response = await pickUpOrderRequest(orderId);

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

export async function completeDelivery(orderId) {
    try {
        const response = await completeDeliveryRequest(orderId);

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

export async function fetchWaitingDeliveries() {
    const container = document.getElementById("waitingList");
    const empty = document.getElementById("waitingEmpty");
    const loading = document.getElementById("waitingLoading");

    loading?.classList.remove("hidden");
    empty?.classList.add("hidden");

    if (container) container.innerHTML = "";

    if (state.courierStatus === "OFFLINE") {
        loading?.classList.add("hidden");

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

    if (state.courierStatus !== "AVAILABLE") {
        loading?.classList.add("hidden");

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
        const response = await getWaitingDeliveriesRequest();

        if (response.status === 401 || response.status === 403) {
            logout();
            return;
        }

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        state.waitingDeliveries = await response.json();
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

    const waitingDeliveries = state.waitingDeliveries;

    if (!waitingDeliveries || waitingDeliveries.length === 0) {
        empty?.classList.remove("hidden");
        return;
    }

    empty?.classList.add("hidden");

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

        card.querySelector(".accept-btn")?.addEventListener("click", () => {
            acceptDelivery(delivery.orderId);
        });

        container.appendChild(card);
    });
}

export async function fetchTodayStats() {
    try {
        const response = await getTodayStatistics();

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
        const countElem = document.getElementById("todayCount");

        if (countElem) {
            countElem.textContent =
                data.completedDeliveriesCount ||
                data.completedToday ||
                0;
        }
    } catch (error) {
        showError(error.message);
    }
}

export async function fetchHistoryDeliveries() {
    const container = document.getElementById("historyList");
    const empty = document.getElementById("historyEmpty");
    const loading = document.getElementById("historyLoading");

    loading?.classList.remove("hidden");
    empty?.classList.add("hidden");

    if (container) container.innerHTML = "";

    try {
        const response = await getHistoryDeliveriesRequest();

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        state.historyDeliveries = await response.json();
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

    const historyDeliveries = state.historyDeliveries;

    if (!historyDeliveries || historyDeliveries.length === 0) {
        empty?.classList.remove("hidden");
        return;
    }

    empty?.classList.add("hidden");

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
                    <strong>
                        ${delivery.etaMinutes ? `${delivery.etaMinutes} мин.` : "-"}
                    </strong>
                </div>
            </div>
        `;

        container.appendChild(card);
    });
}
