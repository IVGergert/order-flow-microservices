import {
    logout
} from "./auth.js";

export async function request(url, options = {}) {
    const response = await fetch(url, options);

    if (response.status === 401 || response.status === 403) {
        logout();
        return null;
    }

    return response;
}

export async function get(url, headers = {}) {
    return request(url, {
        method: "GET",
        headers
    });
}

export async function post(url, body, headers = {}) {
    return request(url, {
        method: "POST",
        headers,
        body: JSON.stringify(body)
    });
}

export async function getErrorMessage(response) {
    try {
        const data = await response.json();
        return data.message ||
            data.error ||
            data.detail ||
            `Ошибка: ${response.status}`;
    } catch {
        return `Ошибка сервера: ${response.status}`;
    }
}