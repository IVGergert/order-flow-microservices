import {
    getErrorMessage
} from "../../common/error-handler.js";

export async function request(url, options = {}) {
    return fetch(url, options);
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

export {
    getErrorMessage
};