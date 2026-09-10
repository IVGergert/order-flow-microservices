import {
    buildAuthHeaders
} from "./state.js";

import {
    getErrorMessage
} from "../../common/error-handler.js";

export async function getCourierStatus() {
    return fetch("/api/deliveries/courier/status", {
        method: "GET",
        headers: buildAuthHeaders()
    });
}

export async function goOnlineRequest() {
    return fetch("/api/deliveries/courier/go-online", {
        method: "POST",
        headers: buildAuthHeaders()
    });
}

export async function goOfflineRequest() {
    return fetch("/api/deliveries/courier/go-offline", {
        method: "POST",
        headers: buildAuthHeaders()
    });
}

export async function getTodayStatistics() {
    return fetch("/api/deliveries/statistics/today", {
        method: "GET",
        headers: buildAuthHeaders()
    });
}

export async function getCurrentDeliveryRequest() {
    return fetch("/api/deliveries/current", {
        method: "GET",
        headers: buildAuthHeaders()
    });
}

export async function getWaitingDeliveriesRequest() {
    return fetch("/api/deliveries/waiting", {
        method: "GET",
        headers: buildAuthHeaders()
    });
}

export async function getHistoryDeliveriesRequest() {
    return fetch("/api/deliveries/history", {
        method: "GET",
        headers: buildAuthHeaders()
    });
}

export async function acceptDeliveryRequest(orderId) {
    return fetch(`/api/deliveries/${orderId}/accept`, {
        method: "POST",
        headers: buildAuthHeaders()
    });
}

export async function pickUpOrderRequest(orderId) {
    return fetch(`/api/deliveries/${orderId}/pickup`, {
        method: "POST",
        headers: buildAuthHeaders()
    });
}

export async function completeDeliveryRequest(orderId) {
    return fetch(`/api/deliveries/${orderId}/complete`, {
        method: "POST",
        headers: buildAuthHeaders()
    });
}

export {
    getErrorMessage
};