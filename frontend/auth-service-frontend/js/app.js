let selectedRole = "CUSTOMER";

function selectRole(type) {
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
    } else {
        btnCustomer.classList.remove("active");
        btnCourier.classList.add("active");
        registerTab.classList.add("hidden");
        courierNotice.classList.remove("hidden");
        switchTab("login");
    }
}

function switchTab(tab) {
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
    } else {
        loginForm.classList.add("hidden");
        registerForm.classList.remove("hidden");
        loginTab.classList.remove("active");
        registerTab.classList.add("active");
    }
}

function showAlert(message, isError = true) {
    const alertBox = document.getElementById("alertBox");
    alertBox.innerText = message;
    alertBox.className = `alert ${isError ? 'alert-danger' : 'alert-success'}`;
}

function hideAlert() {
    const alertBox = document.getElementById("alertBox");
    if (alertBox) {
        alertBox.className = "alert hidden";
    }
}

async function onLogin(event) {
    event.preventDefault();
    hideAlert();

    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(errorMsg.message || "Не удалось выполнить вход.");
        }

        const data = await response.json();

        const expectedRole = selectedRole === "CUSTOMER"
                ? "ROLE_CUSTOMER"
                : "ROLE_COURIER";

        if (data.role !== expectedRole) {
            showAlert(selectedRole === "COURIER"
                    ? "Этот аккаунт не является аккаунтом курьера."
                    : "Этот аккаунт не является аккаунтом клиента."
            );
            return;
        }


        saveAuthData(data);
        redirectByRole(data.role);

    } catch (err) {
        showAlert(err.message);
    }
}

async function onRegister(event) {
    event.preventDefault();
    hideAlert();

    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value;
    const confirmPassword = document.getElementById("regConfirmPassword").value;

    if (password !== confirmPassword) {
        showAlert("Password don't matching!");
        return;
    }

    try {
        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email,
                password,
                confirmPassword
            })
        });

        if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(errorMsg);
        }

        const data = await response.json();

        saveAuthData(data);
        redirectByRole(data.role);

    } catch (err) {
        showAlert(err.message);
    }
}

function saveAuthData(data) {
    localStorage.setItem("accessToken", data.accessJwtToken);
    localStorage.setItem("refreshToken", data.refreshJwtToken);
    localStorage.setItem("userId", data.userId);
    localStorage.setItem("userEmail", data.email);
    localStorage.setItem("userRole", data.role);
}

function redirectByRole(role) {
    if (role === "ROLE_CUSTOMER") {
        window.location.href = "/customer/";
    } else if (role === "ROLE_COURIER") {
        window.location.href = "/courier/";
    }
}