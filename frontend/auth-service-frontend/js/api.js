import {
    getErrorMessage
} from "/common/error-handler.js";

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        }
    });

    if (!response.ok) {
        throw new Error(await getErrorMessage(response));
    }

    return response.json();
}

export function loginRequest(email, password) {
    return request("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
            email,
            password
        })
    });
}

export function registerRequest(email, password, confirmPassword) {
    return request("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
            email,
            password,
            confirmPassword
        })
    });
}