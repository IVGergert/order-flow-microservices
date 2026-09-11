import {
    buildAuthHeaders,
    logout
} from "./state.js";

import {
    getErrorMessage
} from "../../common/error-handler.js";

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            ...buildAuthHeaders(),
            ...options.headers
        }
    });

    if (response.status === 401) {
        logout();
        return null;
    }

    if (response.status === 204) {
        return null;
    }

    if (!response.ok) {
        const message = await getErrorMessage(response);
        throw new Error(message);
    }

    return response.json();
}

export const getCourierStatus = () => request("/api/deliveries/courier/status");

export const getTodayStatistics = () => request("/api/deliveries/statistics/today");

export const getCurrentDeliveryRequest = () => request("/api/deliveries/current");

export const getWaitingDeliveriesRequest = () => request("/api/deliveries/waiting");

export const getHistoryDeliveriesRequest = () => request("/api/deliveries/history");

export const goOnlineRequest = () =>
    request("/api/deliveries/courier/go-online", {
        method: "POST"
    });

export const goOfflineRequest = () =>
    request("/api/deliveries/courier/go-offline", {
        method: "POST"
    });

export const validateLogoutRequest = () =>
    request("/api/deliveries/courier/validate-logout", {
        method: "POST"
    });

export const acceptDeliveryRequest = orderId =>
    request(`/api/deliveries/${orderId}/accept`, {
        method: "POST"
    });

export const pickUpOrderRequest = orderId =>
    request(`/api/deliveries/${orderId}/pickup`, {
        method: "POST"
    });

export const completeDeliveryRequest = orderId =>
    request(`/api/deliveries/${orderId}/complete`, {
        method: "POST"
    });