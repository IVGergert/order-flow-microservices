import {
    loginRequest,
    registerRequest
} from "./api.js";

import {
    saveAuthData
} from "./state.js";

import {
    showAlert,
    hideAlert
} from "./ui.js";

let selectedRole = "CUSTOMER";

export function selectRole(type) {
    const btnCustomer = document.getElementById("btnCustomer");
    const btnCourier = document.getElementById("btnCourier");
    const registerTab = document.getElementById("registerTab");
    const courierNotice = document.getElementById("courierNotice");

    selectedRole = type;

    hideAlert();

    if (type === "CUSTOMER") {
        btnCustomer.classList.add("active");
        btnCourier.classList.remove("active");

        registerTab.classList.remove("hidden");
        courierNotice.classList.add("hidden");

        return;
    }

    btnCustomer.classList.remove("active");
    btnCourier.classList.add("active");

    registerTab.classList.add("hidden");
    courierNotice.classList.remove("hidden");

    switchTab("login");
}


export function switchTab(tab) {
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");

    const loginTab = document.getElementById("loginTab");
    const registerTab = document.getElementById("registerTab");

    hideAlert();

    if (tab === "login") {
        loginForm.classList.remove("hidden");
        registerForm.classList.add("hidden");

        loginTab.classList.add("active");
        registerTab.classList.remove("active");

        return;
    }

    loginForm.classList.add("hidden");
    registerForm.classList.remove("hidden");

    loginTab.classList.remove("active");
    registerTab.classList.add("active");
}


export async function onLogin(event) {
    event.preventDefault();

    hideAlert();

    const email = document
            .getElementById("loginEmail")
            .value
            .trim();

    const password = document
            .getElementById("loginPassword")
            .value;

    try {
        const data = await loginRequest(email, password);

        const expectedRole =
            selectedRole === "CUSTOMER"
                ? "ROLE_CUSTOMER"
                : "ROLE_COURIER";

        if (data.role !== expectedRole) {
            showAlert(
                selectedRole === "COURIER"
                    ? "Этот аккаунт не является аккаунтом курьера."
                    : "Этот аккаунт не является аккаунтом клиента."
            );

            return;
        }

        saveAuthData(data);
        redirectByRole(data.role);

    } catch (error) {
        showAlert(error.message);
    }
}


export async function onRegister(event) {
    event.preventDefault();

    hideAlert();

    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value;

    const confirmPassword = document.getElementById("regConfirmPassword").value;

    if (password !== confirmPassword) {
        showAlert("Пароли не совпадают");
        return;
    }

    try {
        const data = await registerRequest(
            email,
            password,
            confirmPassword
        );

        saveAuthData(data);
        redirectByRole(data.role);

    } catch (error) {
        showAlert(error.message);
    }
}

function redirectByRole(role) {
    if (role === "ROLE_CUSTOMER") {
        window.location.href = "/customer/";
        return;
    }

    if (role === "ROLE_COURIER") {
        window.location.href = "/courier/";
    }
}