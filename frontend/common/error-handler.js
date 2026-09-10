const ERROR_TRANSLATIONS = {
    "Authentication required": "Требуется авторизация",
    "Access denied": "Доступ запрещён",
    "Invalid email or password": "Неверный email или пароль",
    "Passwords do not match": "Пароли не совпадают",
    "Email cannot be empty": "Email не может быть пустым",
    "Incorrect format email": "Некорректный формат email",
    "Password cannot be empty": "Пароль не может быть пустым",
    "Confirm password cannot be empty": "Подтверждение пароля не может быть пустым",
    "Name cannot be empty": "Имя не может быть пустым",
    "Transport type cannot be empty": "Тип транспорта не может быть пустым",
    "Validation failed": "Проверьте правильность введённых данных",
    "User not found": "Пользователь не найден",
    "Courier not found": "Курьер не найден",
    "Order not found": "Заказ не найден",
    "Payment not found": "Платёж не найден",
    "Delivery not found": "Доставка не найдена"
};

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

export async function getErrorMessage(response) {
    try {
        const data = await response.json();

        if (data.errors && typeof data.errors === "object") {
            const messages = Object.values(data.errors)
                .filter(Boolean)
                .map(translateError);

            if (messages.length > 0) {
                return messages.join("\n");
            }
        }

        return translateError(
            data.message
            || data.error
            || data.detail
            || `Ошибка: ${response.status}`
        );
    } catch {
        return `Ошибка сервера: ${response.status}`;
    }
}