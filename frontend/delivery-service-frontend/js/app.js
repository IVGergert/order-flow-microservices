import {
    state,
    getAccessToken,
    logout
} from "./state.js";

import {
    renderUserInfo,
    showSection
} from "./ui.js";

import {
    fetchCourierStatus,
    fetchTodayStats,
    fetchCurrentDelivery,
    fetchWaitingDeliveries,
    fetchHistoryDeliveries,
    goOnline,
    goOffline,
    pickUpOrder,
    completeDelivery
} from "./deliveries.js";

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

document.addEventListener("click", event => {
    const navItem = event.target.closest(".nav-item");

    if (navItem) {
        const section = navItem.dataset.section;

        if (section) {
            showSection(section);
        }

        return;
    }

    if (event.target.closest("#statusToggleButton")) {
        if (state.courierStatus === "OFFLINE") {
            goOnline();
        } else {
            goOffline();
        }

        return;
    }

    if (event.target.closest("#pickupButton")) {
        if (state.currentDelivery) {
            pickUpOrder(state.currentDelivery.orderId);
        }

        return;
    }

    if (event.target.closest("#completeButton")) {
        if (state.currentDelivery) {
            completeDelivery(state.currentDelivery.orderId);
        }

        return;
    }

    if (event.target.closest("#goToWaitingButton")) {
        showSection("waiting");
        return;
    }

    if (event.target.closest("#refreshWaitingButton")) {
        fetchWaitingDeliveries();
        return;
    }

    if (event.target.closest("#refreshHistoryButton")) {
        fetchHistoryDeliveries();
        return;
    }

    if (event.target.closest("#logoutButton")) {
        logout();
    }
});
