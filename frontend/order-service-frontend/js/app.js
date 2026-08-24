const MINIO_BASE_URL = "http://localhost:9000/menu-images/";
const CART_STORAGE_KEY = "cartItems";

/** Список доступных категорий с иконками */
const categories = [
    { value: "ALL", title: "Все", icon: "🍽️" },
    { value: "PIZZA", title: "Пицца", icon: "🍕" },
    { value: "BURGERS", title: "Бургеры", icon: "🍔" },
    { value: "SUSHI", title: "Суши", icon: "🍣" },
    { value: "SNACKS", title: "Снеки", icon: "🍟" },
    { value: "DRINKS", title: "Напитки", icon: "🥤" }
];

let menuItems = [];
let cartItems = [];
let activeCategory = "ALL";
let selectedOrder = null;


/* ==========================================================================
   2. INITIALIZATION
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async () => {
    // Проверка авторизации
    if (!getAccessToken()) {
        window.location.href = "/";
        return;
    }

    // Инициализация интерфейса и данных
    loadCart();
    renderUserInfo();
    renderCategories();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();

    // Загрузка меню с бэкенда
    await loadMenu();
});


/* ==========================================================================
   3. AUTHENTICATION & USER DATA
   ========================================================================== */

/** Получить токен из хранилища */
function getAccessToken() {
    return localStorage.getItem("accessToken");
}

/** Получить Email пользователя */
function getUserEmail() {
    return localStorage.getItem("userEmail") || "Пользователь";
}

/** Сформировать заголовки запроса с Bearer-токеном */
function buildAuthHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${getAccessToken()}`
    };
}

/** Выход из системы с очисткой хранилища */
function logout() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userId");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userName");
    localStorage.removeItem(CART_STORAGE_KEY);

    window.location.href = "/";
}

/** Отображение Email и аватарки пользователя */
function renderUserInfo() {
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


/* ==========================================================================
   4. MENU & DYNAMIC CATEGORIES
   ========================================================================== */

/** Загрузка меню с API сервера */
async function loadMenu() {
    showMenuLoading();

    try {
        const response = await fetch("/api/menu", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            logout();
            return;
        }

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const data = await response.json();
        menuItems = Array.isArray(data) ? data : [];

        hideMenuLoading();
        renderCategories(); // Обновление плашек со счетчиками блюд
        renderMenu();

    } catch (error) {
        hideMenuLoading();
        showMenuError(error.message);
    }
}

/** Отрисовка плашек категорий со счетчиками блюд */
function renderCategories() {
    const container = document.getElementById("categories");
    if (!container) return;

    container.innerHTML = "";

    categories.forEach(category => {
        // Подсчет количества товаров для плашки
        const count = category.value === "ALL"
            ? menuItems.length
            : menuItems.filter(item => item.category === category.value).length;

        const button = document.createElement("button");
        button.type = "button";
        button.className = `category-button ${category.value === activeCategory ? 'active' : ''}`;
        button.dataset.category = category.value;

        button.innerHTML = `
            <span class="category-icon">${category.icon}</span>
            <span class="category-title">${escapeHtml(category.title)}</span>
            <span class="category-badge">${count}</span>
        `;

        button.addEventListener("click", () => {
            activeCategory = category.value;
            renderCategories();
            renderMenu();
        });

        container.appendChild(button);
    });
}

/** Отрисовка сетки меню в зависимости от выбранной категории */
function renderMenu() {
    const container = document.getElementById("menuGrid");
    const emptyState = document.getElementById("menuEmpty");

    if (!container) return;
    container.innerHTML = "";

    if (!menuItems || menuItems.length === 0) {
        if (emptyState) emptyState.classList.remove("hidden");
        return;
    }

    if (emptyState) emptyState.classList.add("hidden");

    // Режим "Все категории"
    if (activeCategory === "ALL") {
        renderAllCategories(container);
        return;
    }

    // Режим одной выбранной категории
    const filteredItems = menuItems.filter(item => item.category === activeCategory);

    if (filteredItems.length === 0) {
        renderEmptyCategory(container);
        return;
    }

    const categoryObj = categories.find(c => c.value === activeCategory);
    const categoryTitle = categoryObj ? categoryObj.title : activeCategory;
    const categoryIcon = categoryObj ? categoryObj.icon : "🍽️";

    const section = document.createElement("section");
    section.className = "menu-category-section";

    section.innerHTML = `
        <div class="menu-category-header">
            <div>
                <h2>${categoryIcon} ${escapeHtml(categoryTitle)}</h2>
                <span>${filteredItems.length} ${getRussianItemWord(filteredItems.length)}</span>
            </div>
        </div>
        <div class="menu-category-grid"></div>
    `;

    const grid = section.querySelector(".menu-category-grid");

    filteredItems.forEach(item => {
        grid.appendChild(createMenuCard(item));
    });

    container.appendChild(section);
}

/** Отрисовка всех категорий по разделам (для вкладки "Все") */
function renderAllCategories(container) {
    categories
        .filter(category => category.value !== "ALL")
        .forEach(category => {
            const items = menuItems.filter(item => item.category === category.value);
            if (items.length === 0) return;

            const section = document.createElement("section");
            section.className = "menu-category-section";

            section.innerHTML = `
                <div class="menu-category-header">
                    <div>
                        <h2>${category.icon} ${escapeHtml(category.title)}</h2>
                    </div>
                </div>
                <div class="menu-category-grid"></div>
            `;

            const grid = section.querySelector(".menu-category-grid");

            items.forEach(item => {
                grid.appendChild(createMenuCard(item));
            });

            container.appendChild(section);
        });

    if (!container.children.length) {
        renderEmptyCategory(container);
    }
}

/** Отрисовка состояния пустой категории */
function renderEmptyCategory(container) {
    container.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">🍽️</div>
            <h3>Ничего не найдено</h3>
            <p>В этой категории пока нет доступных блюд.</p>
        </div>
    `;
}

/** Создание карточки блюда */
function createMenuCard(item) {
    const card = document.createElement("article");
    card.className = "menu-card";

    const imageUrl = buildImageUrl(item.imageUrl);

    card.innerHTML = `
        <div class="menu-card-image">
            ${imageUrl ? `
                <img src="${escapeHtml(imageUrl)}" alt="${escapeHtml(item.name)}" loading="lazy">
            ` : `
                <div class="image-placeholder">🍽️</div>
            `}
        </div>

        <div class="menu-card-content">
            <span class="menu-card-category">
                ${escapeHtml(item.categoryTitle || getCategoryTitle(item.category))}
            </span>

            <h3 class="menu-card-title">${escapeHtml(item.name)}</h3>

            <p class="menu-card-description">
                ${escapeHtml(item.description || "Описание отсутствует")}
            </p>

            <div class="menu-card-bottom">
                <span class="menu-card-price">${formatPrice(item.price)}</span>
                <button type="button" class="add-to-cart-button">
                    В корзину
                </button>
            </div>
        </div>
    `;

    // Обработка ошибки загрузки картинки
    const image = card.querySelector("img");
    if (image) {
        image.addEventListener("error", () => {
            image.style.display = "none";
            const imageContainer = image.parentElement;
            if (imageContainer) {
                imageContainer.classList.add("image-error");
                imageContainer.innerHTML = '<span class="image-error-text">Изображение недоступно</span>';
            }
        });
    }

    // Обработчик добавления в корзину
    const addButton = card.querySelector(".add-to-cart-button");
    addButton.addEventListener("click", () => addToCart(item));

    return card;
}


/* ==========================================================================
   5. MENU LOADING & ERROR STATES
   ========================================================================== */

function showMenuLoading() {
    const loading = document.getElementById("menuLoading");
    const empty = document.getElementById("menuEmpty");
    const grid = document.getElementById("menuGrid");

    if (loading) loading.classList.remove("hidden");
    if (empty) empty.classList.add("hidden");
    if (grid) grid.innerHTML = "";
}

function hideMenuLoading() {
    const loading = document.getElementById("menuLoading");
    if (loading) loading.classList.add("hidden");
}

function showMenuError(message) {
    const grid = document.getElementById("menuGrid");
    if (!grid) return;

    grid.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">⚠️</div>
            <h3>Не удалось загрузить меню</h3>
            <p>${escapeHtml(message)}</p>
            <button type="button" class="primary-button" onclick="loadMenu()">
                Повторить попытку
            </button>
        </div>
    `;
}


/* ==========================================================================
   6. CART STATE MANAGEMENT
   ========================================================================== */

/** Загрузить корзину из localStorage */
function loadCart() {
    try {
        const savedCart = localStorage.getItem(CART_STORAGE_KEY);
        cartItems = savedCart ? JSON.parse(savedCart) : [];
        if (!Array.isArray(cartItems)) cartItems = [];
    } catch {
        cartItems = [];
    }
}

/** Сохранить корзину в localStorage */
function saveCart() {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cartItems));
}

/** Добавить товар в корзину */
function addToCart(item) {
    const existingItem = cartItems.find(cartItem => cartItem.itemId === item.id);

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

/** Удалить товар из корзины */
function removeFromCart(itemId) {
    cartItems = cartItems.filter(item => item.itemId !== itemId);

    saveCart();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();
}

/** Уменьшить количество товара */
function decreaseQuantity(itemId) {
    const item = cartItems.find(cartItem => cartItem.itemId === itemId);
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

/** Увеличить количество товара */
function increaseQuantity(itemId) {
    const item = cartItems.find(cartItem => cartItem.itemId === itemId);
    if (!item) return;

    item.quantity += 1;

    saveCart();
    updateCartCounter();
    updateCartItemsCount();
    renderCart();
}

/** Общее количество штук в корзине */
function getCartCount() {
    return cartItems.reduce((total, item) => total + item.quantity, 0);
}

/** Общая стоимость корзины */
function getCartTotal() {
    return cartItems.reduce((total, item) => total + Number(item.price) * item.quantity, 0);
}

/** Обновить счетчик плавающей кнопки корзины */
function updateCartCounter() {
    const counter = document.getElementById("cartCount");
    if (counter) {
        counter.textContent = getCartCount();
    }
}

/** Обновить счетчик в шапке сайдбара корзины */
function updateCartItemsCount() {
    const counter = document.getElementById("cartItemsCount");
    if (!counter) return;

    const count = getCartCount();
    counter.textContent = `${count} ${getRussianItemWord(count)}`;
}


/* ==========================================================================
   7. CART SIDEBAR UI
   ========================================================================== */

function openCart() {
    document.getElementById("cartSidebar")?.classList.add("open");
    document.getElementById("cartOverlay")?.classList.remove("hidden");
    renderCart();
}

function closeCart() {
    document.getElementById("cartSidebar")?.classList.remove("open");
    document.getElementById("cartOverlay")?.classList.add("hidden");
}

/** Отрисовка списка элементов корзины */
function renderCart() {
    const container = document.getElementById("cartItems");
    const empty = document.getElementById("cartEmpty");
    const totalElement = document.getElementById("cartTotal");
    const checkoutButton = document.getElementById("checkoutButton");

    if (!container) return;
    container.innerHTML = "";

    if (cartItems.length === 0) {
        if (empty) empty.classList.remove("hidden");
        if (totalElement) totalElement.textContent = formatPrice(0);
        if (checkoutButton) checkoutButton.disabled = true;
        return;
    }

    if (empty) empty.classList.add("hidden");

    cartItems.forEach(item => {
        container.appendChild(createCartItem(item));
    });

    if (totalElement) totalElement.textContent = formatPrice(getCartTotal());
    if (checkoutButton) checkoutButton.disabled = false;

    updateCartItemsCount();
}

/** Создание элемента товара в корзине */
function createCartItem(item) {
    const element = document.createElement("div");
    element.className = "cart-item";

    const imageUrl = buildImageUrl(item.imageUrl);

    element.innerHTML = `
        ${imageUrl ? `
            <img class="cart-item-image" src="${escapeHtml(imageUrl)}" alt="${escapeHtml(item.name)}">
        ` : `
            <div class="cart-item-image image-placeholder">🍽️</div>
        `}

        <div class="cart-item-content">
            <div class="cart-item-name">${escapeHtml(item.name)}</div>
            <div class="cart-item-price">${formatPrice(item.price)}</div>

            <div class="cart-item-controls">
                <button type="button" class="quantity-button decrease-button">−</button>
                <span class="quantity">${item.quantity}</span>
                <button type="button" class="quantity-button increase-button">+</button>
                <button type="button" class="remove-cart-item">Удалить</button>
            </div>
        </div>
    `;

    element.querySelector(".decrease-button").addEventListener("click", () => decreaseQuantity(item.itemId));
    element.querySelector(".increase-button").addEventListener("click", () => increaseQuantity(item.itemId));
    element.querySelector(".remove-cart-item").addEventListener("click", () => removeFromCart(item.itemId));

    return element;
}


/* ==========================================================================
   8. CHECKOUT & ORDER CREATION
   ========================================================================== */

/** Открытие модального окна оформления заказа */
function openCheckout() {
    if (cartItems.length === 0) {
        showError("Корзина пуста");
        return;
    }

    closeCart();

    const modal = document.getElementById("checkoutModal");
    if (!modal) return;

    clearCheckoutErrors();

    const totalElement = document.getElementById("checkoutTotal");
    const countElement = document.getElementById("checkoutItemsCount");

    if (totalElement) totalElement.textContent = formatPrice(getCartTotal());
    if (countElement) countElement.textContent = getCartCount();

    // Отрисовка товаров перед оплатой
    renderCheckoutItems();

    modal.classList.remove("hidden");
}

/** Отрисовка списка выбранных блюд в модалке оформления */
function renderCheckoutItems() {
    const container = document.getElementById("checkoutItems");
    if (!container) return;

    if (cartItems.length === 0) {
        container.innerHTML = "";
        return;
    }

    container.innerHTML = `
        <div class="checkout-items-header">
            <h3>Ваш заказ</h3>
            <span>${getCartCount()} ${getRussianItemWord(getCartCount())}</span>
        </div>

        <div class="checkout-items-list">
            ${cartItems.map(item => {
        const imageUrl = buildImageUrl(item.imageUrl);
        return `
                    <div class="checkout-item">
                        <div class="checkout-item-image">
                            ${imageUrl ? `<img src="${escapeHtml(imageUrl)}" alt="${escapeHtml(item.name)}">` : '🍽️'}
                        </div>

                        <div class="checkout-item-info">
                            <strong>${escapeHtml(item.name)}</strong>
                            <span>${item.quantity} × ${formatPrice(item.price)}</span>
                        </div>

                        <strong class="checkout-item-total">
                            ${formatPrice(Number(item.price) * item.quantity)}
                        </strong>
                    </div>
                `;
    }).join("")}
        </div>
    `;
}

function closeCheckout() {
    document.getElementById("checkoutModal")?.classList.add("hidden");
    clearCheckoutErrors();
}

/** Отправка заказа на сервер */
async function createOrder() {
    clearCheckoutErrors();

    if (cartItems.length === 0) {
        showCheckoutError("Корзина пуста");
        return;
    }

    const addressInput = document.getElementById("deliveryAddress");
    const address = addressInput ? addressInput.value.trim() : "";

    if (!address) {
        showCheckoutFieldError("deliveryAddress", "Укажите адрес доставки");
        return;
    }

    const selectedPayment = document.querySelector('input[name="paymentMethod"]:checked');
    if (!selectedPayment) {
        showCheckoutError("Выберите способ оплаты");
        return;
    }

    const paymentMethod = selectedPayment.value;
    const items = cartItems.map(item => ({
        itemId: item.itemId,
        quantity: item.quantity
    }));

    const button = document.getElementById("confirmOrderButton");
    setButtonLoading(button, true);

    try {
        // 1. Создание заказа
        const response = await fetch("/api/orders", {
            method: "POST",
            headers: buildAuthHeaders(),
            body: JSON.stringify({ address, items })
        });

        if (response.status === 401 || response.status === 403) {
            logout();
            return;
        }

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const order = await response.json();

        // 2. Оплата заказа
        const paymentResponse = await fetch(`/api/orders/${order.id}/pay`, {
            method: "POST",
            headers: buildAuthHeaders(),
            body: JSON.stringify({ paymentMethod })
        });

        if (!paymentResponse.ok) {
            throw new Error(await getErrorMessage(paymentResponse));
        }

        const paidOrder = await paymentResponse.json();

        // Очистка состояния
        cartItems = [];
        saveCart();
        updateCartCounter();
        updateCartItemsCount();
        renderCart();

        closeCheckout();
        showSuccess(`Заказ №${paidOrder.id} успешно оформлен`);

        // Если открыт раздел заказов — обновить список
        if (!document.getElementById("ordersSection")?.classList.contains("hidden")) {
            await loadMyOrders();
        }

    } catch (error) {
        showCheckoutError(error.message);
    } finally {
        setButtonLoading(button, false);
    }
}


/* ==========================================================================
   9. MY ORDERS & STATUS MAPPING
   ========================================================================== */

/** Маппинг заголовков Java OrderStatus Enum */
function getOrderStatusTitle(status) {
    const statuses = {
        PENDING_PAYMENT: "Ожидает оплаты",
        PAYMENT_FAILED: "Ошибка оплаты",
        CASH_ON_DELIVERY: "Оплата при получении",
        PAID: "Оплачен",
        DELIVERY_ASSIGNED: "Курьер назначен 🛵",
        IN_DELIVERY: "В пути 🚚",
        DELIVERED: "Доставлен 🎉",
        CANCELLED: "Отменён"
    };
    return statuses[status] || status || "Неизвестно";
}

/** Маппинг CSS-классов для статусов */
function getOrderStatusClass(status) {
    switch (status) {
        case "PAID":
        case "DELIVERED":
            return "status-success";

        case "DELIVERY_ASSIGNED":
        case "IN_DELIVERY":
            return "status-info";

        case "PENDING_PAYMENT":
        case "CASH_ON_DELIVERY":
            return "status-warning";

        case "PAYMENT_FAILED":
        case "CANCELLED":
            return "status-danger";

        default:
            return "status-default";
    }
}

/** Загрузка списка заказов пользователя */
async function loadMyOrders() {
    const container = document.getElementById("ordersList");
    const empty = document.getElementById("ordersEmpty");
    const loading = document.getElementById("ordersLoading");

    if (!container) return;

    if (loading) loading.classList.remove("hidden");
    if (empty) empty.classList.add("hidden");
    container.innerHTML = "";

    try {
        const response = await fetch("/api/orders/my", {
            method: "GET",
            headers: buildAuthHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            logout();
            return;
        }

        if (!response.ok) {
            throw new Error(await getErrorMessage(response));
        }

        const orders = await response.json();
        renderOrders(orders);

    } catch (error) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <h3>Не удалось загрузить заказы</h3>
                <p>${escapeHtml(error.message)}</p>
            </div>
        `;
    } finally {
        if (loading) loading.classList.add("hidden");
    }
}

/** Отрисовка списка заказов */
function renderOrders(orders) {
    const container = document.getElementById("ordersList");
    const empty = document.getElementById("ordersEmpty");

    if (!container) return;
    container.innerHTML = "";

    if (!orders || orders.length === 0) {
        if (empty) empty.classList.remove("hidden");
        return;
    }

    if (empty) empty.classList.add("hidden");

    orders.forEach(order => {
        container.appendChild(createOrderCard(order));
    });
}

/** Создание карточки заказа */
function createOrderCard(order) {
    const card = document.createElement("article");
    card.className = "order-card";

    const statusClass = getOrderStatusClass(order.orderStatus);

    card.innerHTML = `
        <div class="order-card-header">
            <div>
                <h3>Заказ №${order.id}</h3>
                <span class="order-date">${formatOrderDate(order.createdAt)}</span>
            </div>

            <span class="order-status ${statusClass}">
                ${escapeHtml(getOrderStatusTitle(order.orderStatus))}
            </span>
        </div>

        <div class="order-card-info">
            <div class="info-col-address">
                <span>Адрес</span>
                <strong title="${escapeHtml(order.address || '')}">
                    ${escapeHtml(order.address || "-")}
                </strong>
            </div>

            <div>
                <span>Позиций</span>
                <strong>${order.items?.length || 0}</strong>
            </div>

            <div>
                <span>Сумма</span>
                <strong>${formatPrice(order.totalAmount)}</strong>
            </div>

            <div class="order-card-action">
                <button type="button" class="secondary-button order-details-button">
                    Подробнее
                </button>
            </div>
        </div>
    `;

    card.querySelector(".order-details-button").addEventListener("click", () => {
        openOrderDetails(order);
    });

    return card;
}

/** Открыть подробную информацию о заказе */
function openOrderDetails(order) {
    selectedOrder = order;
    const modal = document.getElementById("orderModal");
    if (!modal) return;

    const title = document.getElementById("orderModalTitle");
    const status = document.getElementById("orderModalStatus");
    const address = document.getElementById("orderAddress");
    const courier = document.getElementById("orderCourier");
    const eta = document.getElementById("orderEta");
    const total = document.getElementById("orderTotal");

    if (title) title.textContent = `Заказ №${order.id}`;

    if (status) {
        status.textContent = getOrderStatusTitle(order.orderStatus);
        status.className = `order-status ${getOrderStatusClass(order.orderStatus)}`;
    }

    if (address) address.textContent = order.address ? order.address : "-";
    if (courier) courier.textContent = order.courierName ? order.courierName : "-";
    if (eta) eta.textContent = order.etaMinutes != null ? `${order.etaMinutes} мин.` : "-";
    if (total) total.textContent = formatPrice(order.totalAmount);

    renderOrderDetailsItems(order.items || []);

    modal.classList.remove("hidden");
}

/** Отрисовка состава заказа в модальном окне деталей */
function renderOrderDetailsItems(items) {
    const container = document.getElementById("orderItems");
    if (!container) return;

    container.innerHTML = "";

    if (!items || items.length === 0) {
        container.innerHTML = `
            <div class="order-items-empty">
                Состав заказа недоступен.
            </div>
        `;
        return;
    }

    items.forEach(item => {
        const element = document.createElement("div");
        element.className = "order-item";

        const price = item.priceAtPurchase || item.price || 0;
        const qty = item.quantity || 1;
        const itemTotal = Number(price) * Number(qty);

        element.innerHTML = `
            <div class="order-item-info">
                <strong>${escapeHtml(item.itemName || item.name || `Товар #${item.itemId}`)}</strong>
                <span>${qty} × ${formatPrice(price)}</span>
            </div>
            <strong class="order-item-total">${formatPrice(itemTotal)}</strong>
        `;

        container.appendChild(element);
    });
}

function closeOrderDetails() {
    document.getElementById("orderModal")?.classList.add("hidden");
    selectedOrder = null;
}


/* ==========================================================================
   10. PROFILE & NAVIGATION
   ========================================================================== */

/** Отображение секции профиля */
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

/** Динамическое создание структуры секции профиля */
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

/** Переключение разделов приложения */
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

/** Обновление активного состояния кнопок в сайдбаре */
function updateNavigation(section) {
    document.querySelectorAll(".nav-item").forEach(item => {
        item.classList.remove("active");
    });

    const activeItem = document.querySelector(`[data-section="${section}"]`);
    if (activeItem) {
        activeItem.classList.add("active");
    }
}

/** Обновление заголовков страницы */
function updatePageHeader(title, subtitle) {
    const titleElement = document.getElementById("pageTitle");
    const subtitleElement = document.getElementById("pageSubtitle");

    if (titleElement) titleElement.textContent = title;
    if (subtitleElement) subtitleElement.textContent = subtitle;
}


/* ==========================================================================
   11. HELPERS & FORMATTERS
   ========================================================================== */

async function getErrorMessage(response) {
    try {
        const data = await response.json();
        return data.message || data.error || data.detail || `Ошибка: ${response.status}`;
    } catch {
        return `Ошибка сервера: ${response.status}`;
    }
}

function showError(message) {
    const alertBox = document.getElementById("alertBox");
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-danger";
}

function showSuccess(message) {
    const alertBox = document.getElementById("alertBox");
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.className = "alert alert-success";

    clearTimeout(showSuccess.timeout);
    showSuccess.timeout = setTimeout(() => {
        alertBox.className = "alert hidden";
    }, 3500);
}

function showCheckoutError(message) {
    const element = document.getElementById("checkoutError");
    if (!element) return;
    element.textContent = message;
    element.classList.remove("hidden");
}

function showCheckoutFieldError(fieldId, message) {
    const input = document.getElementById(fieldId);
    const error = document.getElementById(`${fieldId}Error`);

    if (input) input.classList.add("error");
    if (error) {
        error.textContent = message;
        error.classList.remove("hidden");
    }
}

function clearCheckoutErrors() {
    const error = document.getElementById("checkoutError");
    if (error) {
        error.textContent = "";
        error.classList.add("hidden");
    }

    const input = document.getElementById("deliveryAddress");
    if (input) input.classList.remove("error");

    const fieldError = document.getElementById("deliveryAddressError");
    if (fieldError) {
        fieldError.textContent = "";
        fieldError.classList.add("hidden");
    }
}

function setButtonLoading(button, loading) {
    if (!button) return;

    if (loading) {
        if (!button.dataset.originalText) {
            button.dataset.originalText = button.textContent;
        }
        button.textContent = "Обработка...";
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalText || button.textContent;
        button.disabled = false;
    }
}

/** Форматирование цены (рубли) */
function formatPrice(price) {
    const value = Number(price);
    if (!Number.isFinite(value)) return "0,00 ₽";
    return `${value.toLocaleString("ru-RU", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽`;
}

/** Форматирование даты заказа */
function formatOrderDate(date) {
    if (!date) return "";
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return "";

    return parsed.toLocaleString("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

/** Формирование URL картинки товара из MinIO */
function buildImageUrl(imageUrl) {
    if (!imageUrl) return "";
    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
        return imageUrl;
    }
    return `${MINIO_BASE_URL}${imageUrl}`;
}

function getCategoryTitle(category) {
    const found = categories.find(item => item.value === category);
    return found ? found.title : category || "";
}

/** Склонение слова "позиция" */
function getRussianItemWord(count) {
    const lastTwo = count % 100;
    const last = count % 10;

    if (lastTwo >= 11 && lastTwo <= 14) return "позиций";
    if (last === 1) return "позиция";
    if (last >= 2 && last <= 4) return "позиции";
    return "позиций";
}

function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener("click", event => {
    const navItem = event.target.closest(".nav-item");
    if (navItem) {
        const section = navItem.dataset.section;
        if (section) showSection(section);
        return;
    }

    if (event.target.closest("#cartButton")) { openCart(); return; }
    if (event.target.closest("#closeCartButton") || event.target.closest("#cartOverlay")) { closeCart(); return; }
    if (event.target.closest("#checkoutButton")) { openCheckout(); return; }
    if (event.target.closest("#closeCheckoutButton")) { closeCheckout(); return; }
    if (event.target.closest("#closeOrderButton")) { closeOrderDetails(); return; }
    if (event.target.closest("#goToMenuButton")) { showSection("menu"); return; }
    if (event.target.closest("#refreshOrdersButton")) { loadMyOrders(); return; }
    if (event.target.closest("#logoutButton")) { logout(); return; }
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