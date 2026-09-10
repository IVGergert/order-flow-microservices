# Food Delivery System — Event-Driven Microservices Architecture

Событийно-ориентированная микросервисная система доставки еды (аналог **Яндекс Еды** / **Delivery Club**), построенная на **Spring Boot 3**, **Apache Kafka** и **PostgreSQL**.

Проект демонстрирует практическую реализацию полнофункционального приложения: от клиентского интерфейса (Frontend) 
до асинхронного взаимодействия бэкенд-микросервисов, паттерна Database per Service, обеспечения идемпотентности консьюмеров, 
безопасного файлового хранилища и транзакционной целостности в изолированных Docker-контейнерах.

---

## Технологический стек

* **Language:** Java 17+
* **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Kafka)
* **Security:** Spring Security + JWT (JSON Web Tokens)
* **Frontend:** SPA-приложения (Интерфейсы для клиентов и курьеров)
* **Message Broker:** Apache Kafka 8.x (Confluent CP-Kafka в режиме **KRaft**)
* **Database:** PostgreSQL 17 Alpine (3 отделенные БД для каждого сервиса)
* **Containerization:** Docker & Docker Compose (Multi-stage builds, Healthchecks)
* **Object Storage:** MinIO (S3-совместимое хранилище для медиафайлов)
* **Tools & Libraries:** MapStruct, Lombok, Springdoc OpenAPI (Swagger UI)

---

## Архитектура системы

В системе реализованы независимые микросервисы и компоненты, каждый из которых запускается в изолированном контейнере:

1. **`auth-service` (Port 8084):** Централизованное управление пользователями (клиентами, курьерами, ресторанами), генерация и валидация JWT-токенов.
2. **`order-service` (Port 8081):** Управление созданием заказа, жизненным циклом его состояний и координацией с клиентом.
3. **`payment-service` (Port 8082):** Эквайринг и проведение платежей (Синхронный HTTP REST API).
4. **`delivery-service` (Port 8083):** Управление логистикой, назначением свободных курьеров и отслеживанием доставки.
5. **`minio` (Port 9000/9001):** S3-хранилище для загрузки и раздачи статических файлов (аватары пользователей, фотографии блюд и ресторанов).
6. **`frontend`:** Пользовательские веб-интерфейсы (для заказа еды и рабочее место курьера).

### Схема взаимодействия (Event Flow)

```text

[ Web Frontend / Mobile ] ──(1. Auth / Login)──> [ auth-service ] (Generates JWT)
        │ 
        │ (JWT in Header)
        ├───(2. Fetch Images)───> [ MinIO (S3) ] 
        │
        └───(3. POST /orders)───> [ order-service ] ──(HTTP REST)──> [ payment-service ]
                                        │
                                        │ 4. Produce: OrderPaidEvent
                                        ▼
      ┌──────────────────────────────────────────────────────────────────┐
      │                   APACHE KAFKA (kafka:29092)                     │
      │ • order-paid-topic               • delivery-assigned-topic       │
      │ • order-picked-up-topic          • delivery-completed-topic      │
      └──────────────────────────────────────────────────────────────────┘
                                        │
                                        │ 5. Consume & Assign Courier
                                        ▼
                                [ delivery-service ]
```

## Жизненный цикл статусов (State Machine)

### Order Service (`OrderStatus`)
`PENDING_PAYMENT` ➔ `PAID` ➔ `DELIVERY_ASSIGNED` ➔ `IN_DELIVERY` ➔ `DELIVERED`

### Delivery Service (`DeliveryStatus` / `CourierStatus`)
* **Delivery:** `WAITING_FOR_COURIER` ➔ `COURIER_ASSIGNED` ➔ `PICKED_UP` ➔ `DELIVERED`
* **Courier:** `OFFLINE` ➔ `AVAILABLE` ➔ `ON_THE_WAY_TO_RESTAURANT` ➔ `ON_THE_WAY_TO_CUSTOMER` ➔ `AVAILABLE`

---

## Архитектурные особенности (Key Features)

* **Decoupling (Слабая связанность):** `order-service` и `delivery-service` полностью изолированы. Общение происходит асинхронно через событийно-ориентированный подход.
* **Database per Service:** Каждая предметная область имеет собственную изолированную PostgreSQL БД (`order-db`, `payment-db`, `delivery-db`).
* **JWT Security:** Защита всех эндпоинтов. Сервисы валидируют токены, выданные auth-service, обеспечивая безопасный межсервисный и клиентский обмен данными.
* **S3 Integration:** Независимое хранение изображений в MinIO с доступом по прямым ссылкам, что разгружает бэкенд.
* **Idempotency (Идемпотентность):** Все `@KafkaListener` защищены от дубликатов сообщений (At-Least-Once Delivery) путем проверки текущего состояния сущностей в БД перед обработкой.
* **Healthcheck Cascading:** Все зависимости в `docker-compose.yml` увязаны через `condition: service_healthy`, что предотвращает упадок сервисов при старте (Wait-For-It pattern).

---

## Быстрый запуск системы в Docker

### 1. Подготовка переменных окружения

Создайте файл `.env` в корневом каталоге проекта со следующими параметрами:

```env
# Общие настройки БД
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Настройки баз данных микросервисов
AUTH_DB_NAME=auth_db
AUTH_DB_PORT=5432

ORDER_DB_NAME=order_db
ORDER_DB_PORT=5433

PAYMENT_DB_NAME=payment_db
PAYMENT_DB_PORT=5434

DELIVERY_DB_NAME=delivery_db
DELIVERY_DB_PORT=5435

# Настройки MinIO
MINIO_USER=admin
MINIO_PASSWORD=minio_secret_password

# JWT Settings
JWT_SECRET=secret
JWT_EXPIRATION_MS_JWT_TOKEN=время
JWT_EXPIRATION_MS_REFRESH_TOKEN=время
```

### 2. Запуск всего приложения и инфраструктуры

Выполните команду для сборки Docker-образов и поднятия всех контейнеров (БД, Kafka, MinIO, Backend, Frontend):

```bash
docker compose up -d --build
```

### Доступные сервисы после старта:
* **Order Service:** `http://localhost:8081`
* **Payment Service:** `http://localhost:8082`
* **Delivery Service:** `http://localhost:8083`
* **Auth Service:** `http://localhost:8084`
* **Minio(API / Web UI)** `http://localhost:9000 / http://localhost:9001`
* **Kafka Broker (Host):** `http://localhost:9092`

---

## Интерактивная документация (Swagger UI)

После запуска контейнеров Swagger UI доступен для каждого сервиса:
* **Auth Service Swagger:** `http://localhost:8084/swagger-ui.html`
* **Order Service Swagger:** `http://localhost:8081/swagger-ui.html`
* **Delivery Service Swagger:** `http://localhost:8083/swagger-ui.html`
* **Delivery Service Swagger:** `http://localhost:8083/swagger-ui.html`

---

## Сценарий сквозного тестирования (E2E Test Flow)

**Предварительный шаг для всех сценариев — Аутентификация (auth-service):**

   ```http
   POST http://localhost:8084/api/auth/login
   ```
*Получаем JWT-токен для подстановки в заголовок Authorization: Bearer <token> во всех последующих запросах*

### **Сценарий 1: Оплата картой онлайн (PaymentMethod = CARD)**

В этом сценарии логистика запускается только после успешной транзакции.

1. **Создание и оплата заказа (`order-service`):**

   ```http
   POST http://localhost:8081/api/payments?orderId=1&userId=USER-1&amount=1500
   ```
   _Заказ создается в БД с начальным статусом оплаты `PAYMENT_SUCCEEDED` и статусом заказа `PAID` ➔ `order-service` публикует событие в Kafka ➔ `delivery-service` считывает событие и ставит статус доставки `WAITING_FOR_COURIER`._


2. **Курьер принял заказ и едет в ресторан (`delivery-service`):**

   ```http
   POST http://localhost:8083/api/deliveries/1/accept
   ```
   _Статус доставки сменится на `COURIER_ASSIGNED` ➔ `delivery-service` отправляет событие в Kafka ➔ `order-service` считывает событие и меняет статус заказа на `DELIVERY_ASSIGNED`._


3. **Курьер забрал заказ из ресторана и направляется к клиенту (`delivery-service`):**

   ```http
   POST http://localhost:8083/api/deliveries/1/pickup
   ```

   _Статус доставки сменится на `PICKED_UP` ➔ `delivery-service` публикует событие в Kafka ➔ `order-service` считывает событие и обновляет статус заказа на `IN_DELIVERY`._


4. **Курьер доставил заказ клиенту (`delivery-service`):**

   ```http
   POST http://localhost:8083/api/deliveries/1/complete
   ```
   _Статус доставки сменится на `DELIVERED` ➔ `delivery-service` публикует событие в Kafka ➔ `order-service` считывает событие и обновляет статус заказа на `DELIVERED` ➔ Статус курьера сменится на `AVAILABLE`._

### **Сценарий 2: Оплата наличными (PaymentMethod = CASH)**

В этом сценарии заказ переходит в логистику без предварительной онлайн-оплаты, а статус "Оплачено" фиксируется только после вручения заказа.

1. **Создание заказа (`order-service`):**

   ```http
   POST http://localhost:8081/api/orders
   ```
   _Заказ создается в БД с начальным статусом оплаты `PENDING_PAYMENT` и статусом заказа `CASH_ON_DELIVERY` ➔ `order-service` публикует событие готовности к доставке в Kafka ➔ `delivery-service` считывает событие и ставит статус доставки `WAITING_FOR_COURIER`._


2. **Курьер принял заказ и едет в ресторан (`delivery-service`):**

   ```http
   POST http://localhost:8083/api/deliveries/1/accept
   ```
   _Статус доставки сменится на `COURIER_ASSIGNED` ➔ `delivery-service` отправляет событие в Kafka ➔ `order-service` считывает событие и меняет статус заказа на `DELIVERY_ASSIGNED`._


3. **Курьер забрал заказ из ресторана и направляется к клиенту (`delivery-service`):**

   ```http
   POST http://localhost:8083/api/deliveries/1/pickup
   ```

   _Статус доставки сменится на `PICKED_UP` ➔ `delivery-service` публикует событие в Kafka ➔ `order-service` считывает событие и обновляет статус заказа на `IN_DELIVERY`._


4. **Курьер доставил заказ клиенту (`delivery-service`):**

   ```http
   POST http://localhost:8083/api/deliveries/1/complete
   ```
   _Статус доставки сменится на `DELIVERED` ➔ `delivery-service` публикует событие в Kafka ➔ `order-service` считывает событие и обновляет статус заказа на `DELIVERED` и меняет статус оплаты заказа на `PAID` ➔ Статус курьера сменится на `AVAILABLE`._