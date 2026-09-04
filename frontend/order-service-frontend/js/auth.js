const CART_STORAGE_KEY = "cartItems";

export function getAccessToken() {
    return localStorage.getItem("accessToken");
}

export function getUserEmail() {
    return localStorage.getItem("userEmail") || "Пользователь";
}

export function buildAuthHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${getAccessToken()}`
    };
}

export function logout() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userId");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userName");
    localStorage.removeItem(CART_STORAGE_KEY);

    window.location.href = "/";
}

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