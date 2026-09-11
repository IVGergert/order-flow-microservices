const ERROR_TRANSLATIONS = {
    // Authentication
    "Authentication required": "Требуется авторизация",
    "Access denied": "Доступ запрещён",
    "Invalid email or password": "Неверный email или пароль",

    // Registration / validation
    "Passwords do not match": "Пароли не совпадают",
    "Email cannot be empty": "Email не может быть пустым",
    "Incorrect format email": "Некорректный формат email",
    "Password cannot be empty": "Пароль не может быть пустым",
    "Password must be between 6 and 16 characters long.": "Пароль должен содержать от 6 до 16 символов",
    "Confirm password must be between 6 and 16 characters long.": "Подтверждение пароля должно содержать от 6 до 16 символов",
    "Confirm password cannot be empty": "Подтверждение пароля не может быть пустым",
    "Name cannot be empty": "Имя не может быть пустым",
    "Transport type cannot be empty": "Тип транспорта не может быть пустым",
    "Validation failed": "Проверьте правильность введённых данных",

    // Users
    "User not found": "Пользователь не найден",

    // Courier
    "Courier is not available.": "Курьер сейчас недоступен.",
    "Courier not found": "Курьер не найден",

    // Orders
    "Order not found": "Заказ не найден",

    // Payments
    "Payment not found": "Платёж не найден",

    // Deliveries
    "Delivery not found": "Доставка не найдена",
    "Delivery has already been accepted.": "Эта доставка уже была принята.",
    "Cannot logout while courier is online or has an active delivery!": "Нельзя выйти из аккаунта, пока курьер находится на линии или выполняет доставку.",
    "You cannot access someone else's delivery!": "Вы не можете получить доступ к чужой доставке."
};

const DEFAULT_ERROR_MESSAGES = {
    400: "Некорректный запрос.",
    401: "Необходима авторизация.",
    403: "Доступ запрещён.",
    404: "Ресурс не найден.",
    409: "Конфликт данных.",
    500: "Ошибка сервера."
};

export async function getErrorMessage(response) {
    const data = await response.json().catch(() => null);

    if (data?.message) {
        const firstError = Object.values(data.errors)[0];

        if (firstError) {
            return translateError(data.message);
        }
    }

    if (data?.message) {
        return translateError(data.message);
    }

    return getDefaultErrorMessage(response.status);
}

export function translateError(message) {
    if (!message) {
        return "Произошла ошибка";
    }

    if (ERROR_TRANSLATIONS[message]) {
        return ERROR_TRANSLATIONS[message];
    }

    if (message.startsWith("User with email ")) {
        return "Пользователь с таким email уже существует";
    }

    return message;
}

function getDefaultErrorMessage(status) {
    return DEFAULT_ERROR_MESSAGES[status] || "Произошла неизвестная ошибка.";
}