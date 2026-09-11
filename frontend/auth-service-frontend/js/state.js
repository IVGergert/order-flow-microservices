const AUTH_KEYS = {
    ACCESS_TOKEN: "accessToken",
    REFRESH_TOKEN: "refreshToken",
    USER_ID: "userId",
    USER_EMAIL: "userEmail",
    USER_ROLE: "userRole"
};


export function saveAuthData(data) {
    localStorage.setItem(AUTH_KEYS.ACCESS_TOKEN, data.accessJwtToken);
    localStorage.setItem(AUTH_KEYS.REFRESH_TOKEN, data.refreshJwtToken);
    localStorage.setItem(AUTH_KEYS.USER_ID, data.userId);
    localStorage.setItem(AUTH_KEYS.USER_EMAIL, data.email);
    localStorage.setItem(AUTH_KEYS.USER_ROLE, data.role);
}

export function getAccessToken() {
    return localStorage.getItem(AUTH_KEYS.ACCESS_TOKEN);
}

export function getRefreshToken() {
    return localStorage.getItem(AUTH_KEYS.REFRESH_TOKEN);
}

export function getUserId() {
    return localStorage.getItem(AUTH_KEYS.USER_ID);
}

export function getUserEmail() {
    return localStorage.getItem(AUTH_KEYS.USER_EMAIL);
}

export function getUserRole() {
    return localStorage.getItem(AUTH_KEYS.USER_ROLE);
}

export function isAuthenticated() {
    return Boolean(getAccessToken());
}