export const state = {
    courierStatus: "OFFLINE",
    currentDelivery: null,
    waitingDeliveries: [],
    historyDeliveries: []
};

export function getAccessToken() {
    return localStorage.getItem("accessToken");
}

export function getUserEmail() {
    return localStorage.getItem("userEmail");
}

export function buildAuthHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${getAccessToken()}`
    };
}

export function logout() {
    localStorage.clear();
    window.location.href = "/";
}
