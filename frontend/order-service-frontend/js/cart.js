import {
    post, getErrorMessage
} from "./api.js";

import {
    buildAuthHeaders
} from "./auth.js";

import {
    showError,
    showSuccess,
    escapeHtml
} from "./ui.js";

import {
    loadMyOrders
} from "./orders.js";

const MINIO_BASE_URL = "/menu-images/";
const CART_STORAGE_KEY = "cartItems";

let cartItems = [];

export function loadCart() {
    try {
        const savedCart = localStorage.getItem(CART_STORAGE_KEY);

        cartItems = savedCart
            ? JSON.parse(savedCart)
            : [];

        if (!Array.isArray(cartItems)) {
            cartItems = [];
        }
    } catch {
        cartItems = [];
    }
}

export function saveCart() {
    localStorage.setItem(
        CART_STORAGE_KEY,
        JSON.stringify(cartItems)
    );
}

export function addToCart(item) {
    const existingItem = cartItems.find(
        cartItem => cartItem.itemId === item.id
    );

    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cartItems.push({
            itemId: item.id,
            name: item.name,
            price: item.price,
            imageUrl: item.imageUrl,
            quantity: 1
        });
    }

    saveCart();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();

    showSuccess(`${item.name} добавлен в корзину`);
}

export function removeFromCart(itemId) {
    cartItems = cartItems.filter(
        item => item.itemId !== itemId
    );

    saveCart();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();
}

export function decreaseQuantity(itemId) {
    const item = cartItems.find(
        cartItem => cartItem.itemId === itemId
    );

    if (!item) return;

    item.quantity -= 1;

    if (item.quantity <= 0) {
        removeFromCart(itemId);
        return;
    }

    saveCart();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();
}

export function increaseQuantity(itemId) {
    const item = cartItems.find(
        cartItem => cartItem.itemId === itemId
    );

    if (!item) return;

    item.quantity += 1;

    saveCart();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();
}

export function getCartCount() {
    return cartItems.reduce(
        (total, item) => total + item.quantity,
        0
    );
}

export function getCartTotal() {
    return cartItems.reduce(
        (total, item) =>
            total + Number(item.price) * item.quantity,
        0
    );
}

export function updateCartCounter() {
    const counter = document.getElementById("cartCount");

    if (counter) {
        counter.textContent = getCartCount();
    }
}

export function updateCartItemsCount() {
    const counter = document.getElementById("cartItemsCount");

    if (!counter) return;

    const count = getCartCount();

    counter.textContent =
        `${count} ${getRussianItemWord(count)}`;
}

export function openCart() {
    document
        .getElementById("cartSidebar")
        ?.classList.add("open");

    document
        .getElementById("cartOverlay")
        ?.classList.remove("hidden");

    renderCart();
}

export function closeCart() {
    document
        .getElementById("cartSidebar")
        ?.classList.remove("open");

    document
        .getElementById("cartOverlay")
        ?.classList.add("hidden");
}

export function renderCart() {
    const container = document.getElementById("cartItems");
    const empty = document.getElementById("cartEmpty");
    const totalElement = document.getElementById("cartTotal");
    const checkoutButton =
        document.getElementById("checkoutButton");

    if (!container) return;

    container.innerHTML = "";

    if (cartItems.length === 0) {
        if (empty) {
            empty.classList.remove("hidden");
        }

        if (totalElement) {
            totalElement.textContent = formatPrice(0);
        }

        if (checkoutButton) {
            checkoutButton.disabled = true;
        }

        return;
    }

    if (empty) {
        empty.classList.add("hidden");
    }

    cartItems.forEach(item => {
        container.appendChild(
            createCartItem(item)
        );
    });

    if (totalElement) {
        totalElement.textContent =
            formatPrice(getCartTotal());
    }

    if (checkoutButton) {
        checkoutButton.disabled = false;
    }

    updateCartItemsCount();
}

export function createCartItem(item) {
    const element = document.createElement("div");
    element.className = "cart-item";

    const imageUrl = buildImageUrl(item.imageUrl);

    element.innerHTML = `
        ${imageUrl ? `
            <img
                class="cart-item-image"
                src="${escapeHtml(imageUrl)}"
                alt="${escapeHtml(item.name)}"
            >
        ` : `
            <div class="cart-item-image image-placeholder">
                🍽️
            </div>
        `}

        <div class="cart-item-content">
            <div class="cart-item-name">
                ${escapeHtml(item.name)}
            </div>

            <div class="cart-item-price">
                ${formatPrice(item.price)}
            </div>

            <div class="cart-item-controls">
                <button
                    type="button"
                    class="quantity-button decrease-button"
                >
                    −
                </button>

                <span class="quantity">
                    ${item.quantity}
                </span>

                <button
                    type="button"
                    class="quantity-button increase-button"
                >
                    +
                </button>

                <button
                    type="button"
                    class="remove-cart-item"
                >
                    Удалить
                </button>
            </div>
        </div>
    `;

    element
        .querySelector(".decrease-button")
        .addEventListener("click", () => {
            decreaseQuantity(item.itemId);
        });

    element
        .querySelector(".increase-button")
        .addEventListener("click", () => {
            increaseQuantity(item.itemId);
        });

    element
        .querySelector(".remove-cart-item")
        .addEventListener("click", () => {
            removeFromCart(item.itemId);
        });

    return element;
}

export function openCheckout() {
    if (cartItems.length === 0) {
        showError("Корзина пуста");
        return;
    }

    closeCart();

    const modal =
        document.getElementById("checkoutModal");

    if (!modal) return;

    clearCheckoutErrors();

    const totalElement =
        document.getElementById("checkoutTotal");

    const countElement =
        document.getElementById("checkoutItemsCount");

    if (totalElement) {
        totalElement.textContent =
            formatPrice(getCartTotal());
    }

    if (countElement) {
        countElement.textContent = getCartCount();
    }

    renderCheckoutItems();

    modal.classList.remove("hidden");
}

export function renderCheckoutItems() {
    const container =
        document.getElementById("checkoutItems");

    if (!container) return;

    if (cartItems.length === 0) {
        container.innerHTML = "";
        return;
    }

    container.innerHTML = `
        <div class="checkout-items-header">
            <h3>Ваш заказ</h3>

            <span>
                ${getCartCount()}
                ${getRussianItemWord(getCartCount())}
            </span>
        </div>

        <div class="checkout-items-list">
            ${cartItems.map(item => {
        const imageUrl =
            buildImageUrl(item.imageUrl);

        return `
                    <div class="checkout-item">
                        <div class="checkout-item-image">
                            ${imageUrl
            ? `<img
                                    src="${escapeHtml(imageUrl)}"
                                    alt="${escapeHtml(item.name)}"
                                >`
            : "🍽️"
        }
                        </div>

                        <div class="checkout-item-info">
                            <strong>
                                ${escapeHtml(item.name)}
                            </strong>

                            <span>
                                ${item.quantity} ×
                                ${formatPrice(item.price)}
                            </span>
                        </div>

                        <strong class="checkout-item-total">
                            ${formatPrice(
            Number(item.price) *
            item.quantity
        )}
                        </strong>
                    </div>
                `;
    }).join("")}
        </div>
    `;
}

export function closeCheckout() {
    document
        .getElementById("checkoutModal")
        ?.classList.add("hidden");

    clearCheckoutErrors();
}


export async function createOrder() {
    clearCheckoutErrors();

    if (cartItems.length === 0) {
        showCheckoutError("Корзина пуста");
        return;
    }

    const addressInput =
        document.getElementById("deliveryAddress");

    const address = addressInput
        ? addressInput.value.trim()
        : "";

    if (!address) {
        showCheckoutFieldError(
            "deliveryAddress",
            "Укажите адрес доставки"
        );

        return;
    }

    const selectedPayment =
        document.querySelector(
            'input[name="paymentMethod"]:checked'
        );

    if (!selectedPayment) {
        showCheckoutError("Выберите способ оплаты");
        return;
    }

    const paymentMethod = selectedPayment.value;

    const items = cartItems.map(item => ({
        itemId: item.itemId,
        quantity: item.quantity
    }));

    const button =
        document.getElementById("confirmOrderButton");

    setButtonLoading(button, true);

    try {
        const response = await post(
            "/api/orders",
            {
                address,
                items
            },
            buildAuthHeaders()
        );

        if (!response.ok) {
            throw new Error(
                await getErrorMessage(response)
            );
        }

        const order = await response.json();

        const paymentResponse = await post(
            `/api/orders/${order.id}/pay`,
            {
                paymentMethod
            },
            buildAuthHeaders()
        );

        if (!paymentResponse.ok) {
            throw new Error(
                await getErrorMessage(paymentResponse)
            );
        }

        const paidOrder =
            await paymentResponse.json();

        cartItems = [];

        saveCart();
        updateCartCounter();
        updateCartItemsCount();
        renderCart();

        closeCheckout();

        showSuccess(
            `Заказ №${paidOrder.id} успешно оформлен`
        );

        if (
            !document
                .getElementById("ordersSection")
                ?.classList.contains("hidden")
        ) {
            await loadMyOrders();
        }

    } catch (error) {
        showCheckoutError(error.message);
    } finally {
        setButtonLoading(button, false);
    }
}

function showCheckoutError(message) {
    const element =
        document.getElementById("checkoutError");

    if (!element) return;

    element.textContent = message;
    element.classList.remove("hidden");
}

function showCheckoutFieldError(fieldId, message) {
    const input =
        document.getElementById(fieldId);

    const error =
        document.getElementById(`${fieldId}Error`);

    if (input) {
        input.classList.add("error");
    }

    if (error) {
        error.textContent = message;
        error.classList.remove("hidden");
    }
}

function clearCheckoutErrors() {
    const error =
        document.getElementById("checkoutError");

    if (error) {
        error.textContent = "";
        error.classList.add("hidden");
    }

    const input =
        document.getElementById("deliveryAddress");

    if (input) {
        input.classList.remove("error");
    }

    const fieldError =
        document.getElementById(
            "deliveryAddressError"
        );

    if (fieldError) {
        fieldError.textContent = "";
        fieldError.classList.add("hidden");
    }
}

function setButtonLoading(button, loading) {
    if (!button) return;

    if (loading) {
        if (!button.dataset.originalText) {
            button.dataset.originalText =
                button.textContent;
        }

        button.textContent = "Обработка...";
        button.disabled = true;

    } else {
        button.textContent =
            button.dataset.originalText ||
            button.textContent;

        button.disabled = false;
    }
}

function formatPrice(price) {
    const value = Number(price);

    if (!Number.isFinite(value)) {
        return "0,00 ₽";
    }

    return `${value.toLocaleString("ru-RU", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })} ₽`;
}

function buildImageUrl(imageUrl) {
    if (!imageUrl) return "";

    if (
        imageUrl.startsWith("http://") ||
        imageUrl.startsWith("https://")
    ) {
        return imageUrl;
    }

    return `${MINIO_BASE_URL}${imageUrl}`;
}

function getRussianItemWord(count) {
    const lastTwo = count % 100;
    const last = count % 10;

    if (lastTwo >= 11 && lastTwo <= 14) {
        return "позиций";
    }

    if (last === 1) {
        return "позиция";
    }

    if (last >= 2 && last <= 4) {
        return "позиции";
    }

    return "позиций";
}