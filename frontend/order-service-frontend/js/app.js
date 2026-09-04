import {
    getAccessToken,
    getUserEmail,
    renderUserInfo,
    logout
} from "./auth.js";

import {
    loadMenu,
    renderCategories
} from "./menu.js";

import {
    loadCart,
    updateCartCounter,
    updateCartItemsCount,
    renderCart,
    openCart,
    closeCart,
    openCheckout,
    closeCheckout,
    createOrder
} from "./cart.js";

import {
    loadMyOrders,
    closeOrderDetails
} from "./orders.js";

import {
    escapeHtml
} from "./ui.js";

document.addEventListener("DOMContentLoaded", async () => {
    if (!getAccessToken()) {
        window.location.href = "/";
        return;
    }

    loadCart();
    renderUserInfo();
    renderCategories();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();

    await loadMenu();
});

function showProfile() {
    document.getElementById("menuSection")?.classList.add("hidden");
    document.getElementById("ordersSection")?.classList.add("hidden");

    let profileSection = document.getElementById("profileSection");

    if (!profileSection) {
        profileSection = createProfileSection();
        document.querySelector(".main-content").appendChild(profileSection);
    }

    profileSection.classList.remove("hidden");
    updatePageHeader("Профиль", "Ваши личные данные");
    updateNavigation("profile");
}

function createProfileSection() {
    const section = document.createElement("section");
    section.id = "profileSection";
    section.className = "content-section profile-section";

    const email = getUserEmail();

    section.innerHTML = `
        <div class="profile-card">
            <div class="profile-card-header">
                <div class="profile-avatar-large">
                    ${escapeHtml(email.charAt(0).toUpperCase())}
                </div>
                <div>
                    <h2>${escapeHtml(email)}</h2>
                </div>
            </div>

            <div class="profile-info">
                <div class="profile-info-row">
                    <span>Email</span>
                    <strong>${escapeHtml(email)}</strong>
                </div>
            </div>

            <button type="button" class="logout-profile-button" id="profileLogoutButton">
                Выйти из аккаунта
            </button>
        </div>
    `;

    section.querySelector("#profileLogoutButton").addEventListener("click", logout);
    return section;
}

function showSection(section) {
    document.getElementById("menuSection")?.classList.add("hidden");
    document.getElementById("ordersSection")?.classList.add("hidden");
    document.getElementById("profileSection")?.classList.add("hidden");

    if (section === "menu") {
        document.getElementById("menuSection")?.classList.remove("hidden");
        updatePageHeader("Меню", "Выберите блюда для заказа");
    }

    if (section === "orders") {
        document.getElementById("ordersSection")?.classList.remove("hidden");
        updatePageHeader("Мои заказы", "История ваших заказов");
        loadMyOrders();
    }

    if (section === "profile") {
        showProfile();
        return;
    }

    updateNavigation(section);
}

function updateNavigation(section) {
    document.querySelectorAll(".nav-item")
        .forEach(item => {
            item.classList.remove("active");
        });

    const activeItem = document.querySelector(`[data-section="${section}"]`);

    if (activeItem) {
        activeItem.classList.add("active");
    }
}

function updatePageHeader(title, subtitle) {
    const titleElement = document.getElementById("pageTitle");
    const subtitleElement = document.getElementById("pageSubtitle");

    if (titleElement) titleElement.textContent = title;
    if (subtitleElement) subtitleElement.textContent = subtitle;
}

document.addEventListener("click", event => {
    const navItem = event.target.closest(".nav-item");

    if (navItem) {
        const section = navItem.dataset.section;
        if (section) showSection(section);
        return;
    }

    if (event.target.closest("#cartButton")) {
        openCart();
        return;
    }

    if (event.target.closest("#closeCartButton") || event.target.closest("#cartOverlay")) {
        closeCart();
        return;
    }

    if (event.target.closest("#checkoutButton")) {
        openCheckout();
        return;
    }

    if (event.target.closest("#closeCheckoutButton")) {
        closeCheckout();
        return;
    }

    if (event.target.closest("#closeOrderButton")) {
        closeOrderDetails();
        return;
    }

    if (event.target.closest("#goToMenuButton")) {
        showSection("menu");
        return;
    }

    if (event.target.closest("#refreshOrdersButton")) {
        loadMyOrders();
        return;
    }

    if (event.target.closest("#logoutButton")) {
        logout();
        return;
    }
});

document.addEventListener("submit", event => {
    if (event.target.id === "checkoutForm") {
        event.preventDefault();
        createOrder();
    }
});

document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        closeCart();
        closeCheckout();
        closeOrderDetails();
    }
});