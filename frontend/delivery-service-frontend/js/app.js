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
    acceptDelivery,
    pickUpOrder,
    completeDelivery,
    validateLogout
} from "./deliveries.js";

// Application start

document.addEventListener(
    "DOMContentLoaded",
    async () => {

        if (!getAccessToken()) {
            logout();
        }

        renderUserInfo();

        await fetchCourierStatus();
        await fetchTodayStats();
        await fetchCurrentDelivery();
    }
);


// User actions

document.addEventListener(
    "click",
    async event => {


        const navItem = event.target.closest(".nav-item");

        if (navItem) {
            const section = navItem.dataset.section;

            if (!section) return;

            showSection(section);

            if (section === "current") {
                await fetchCurrentDelivery();
            }

            if (section === "waiting") {
                await fetchWaitingDeliveries();
            }

            if (section === "history") {
                await fetchHistoryDeliveries();
            }

            return;
        }


        // Online / Offline

        if (event.target.closest("#statusToggleButton")) {
            if (state.courierStatus === "OFFLINE") {
                await goOnline();
            } else {
                await goOffline();
            }

            return;
        }


        // Accept delivery

        const acceptButton = event.target.closest(".accept-btn");

        if (acceptButton) {
            const orderId = acceptButton.dataset.orderId;

            if (orderId) {
                await acceptDelivery(orderId);
            }

            return;
        }


        // Pickup

        if (event.target.closest("#pickupButton")) {
            if (state.currentDelivery) {
                await pickUpOrder(state.currentDelivery.orderId);
            }

            return;
        }


        // Complete

        if (event.target.closest("#completeButton")) {
            if (state.currentDelivery) {
                await completeDelivery(state.currentDelivery.orderId);
            }

            return;
        }


        // Go to waiting

        if (event.target.closest("#goToWaitingButton")) {
            showSection("waiting");
            await fetchWaitingDeliveries();
            return;
        }


        // Go online from waiting

        if (event.target.closest("#goOnlineFromWaitingButton")) {
            await goOnline();
            return;
        }


        // Refresh waiting

        if (event.target.closest("#refreshWaitingButton")) {
            await fetchWaitingDeliveries();
            return;
        }


        // Refresh history

        if (event.target.closest("#refreshHistoryButton")) {
            await fetchHistoryDeliveries();
            return;
        }


        // Logout

        const logoutButton = event.target.closest("#logoutButton, #profileLogoutButton");

        if (logoutButton) {
            const canLogout = await validateLogout();

            if (canLogout) {
                logout();
            }

            return;
        }
    }
);