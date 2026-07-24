# Food Delivery System — Event-Driven Microservices Architecture

Событийно-ориентированная микросервисная система доставки еды (аналог **Яндекс Еды** / **Delivery Club**), построенная на **Spring Boot 3**, **Apache Kafka** и **PostgreSQL**.

Проект демонстрирует практическую реализацию асинхронного взаимодействия микросервисов, паттерн **Database per Service**, обеспечение **идемпотентности консьюмеров** и транзакционную целостность в изолированных Docker-контейнерах.

---

## Технологический стек

* **Language:** Java 17+
* **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Kafka)
* **Message Broker:** Apache Kafka 8.x (Confluent CP-Kafka в режиме **KRaft**)
* **Database:** PostgreSQL 17 Alpine (3 отделенные БД для каждого сервиса)
* **Containerization:** Docker & Docker Compose (Multi-stage builds, Healthchecks)
* **Tools & Libraries:** MapStruct, Lombok, Springdoc OpenAPI (Swagger UI)

---

## Архитектура системы

В системе реализовано 3 независимых микросервиса, каждый из которых запускается в изолированном контейнере со своей собственной БД PostgreSQL:

1. **`order-service` (Port 8081):** Управление созданием заказа, жизненным циклом его состояний и координацией с клиентом.
2. **`payment-service` (Port 8082):** Эквайринг и проведение платежей (Синхронный HTTP REST API).
3. **`delivery-service` (Port 8083):** Управление логистикой, назначением свободных курьеров и отслеживанием доставки.

### Схема взаимодействия (Event Flow)

```text
[ Client ] 
    │
    │ 1. POST /api/orders (Create)
    ▼
┌─────────────────┐       HTTP (REST)      ┌─────────────────┐
│  order-service  │ ─────────────────────> │ payment-service │
└─────────────────┘                        └─────────────────┘
    │                                               
    │ 2. OrderPaidEventDto
    ▼
┌────────────────────────────────────────────────────────────┐
│                  APACHE KAFKA (kafka:29092)                │
│                                                            │
│  • order-paid-topic           • order-picked-up-topic     │
│  • delivery-assigned-topic    • delivery-completed-topic  │
└────────────────────────────────────────────────────────────┘
    │                                               ▲
    │ 3. Consume OrderPaidEvent                     │ 4. Produce Events
    ▼                                               │
┌─────────────────┐                                 │
│delivery-service │ ────────────────────────────────┘
└─────────────────┘

```

## Жизненный цикл статусов (State Machine)

### Order Service (`OrderStatus`)
`PENDING_PAYMENT` ➔ `PAID` ➔ `DELIVERY_ASSIGNED` ➔ `IN_DELIVERY` ➔ `DELIVERED`

### Delivery Service (`DeliveryStatus` / `CourierStatus`)
* **Delivery:** `COURIER_ASSIGNED` ➔ `PICKED_UP` ➔ `DELIVERED`
* **Courier:** `AVAILABLE` ➔ `ON_THE_WAY_TO_RESTAURANT` ➔ `ON_THE_WAY_TO_CUSTOMER` ➔ `AVAILABLE`

---

## Архитектурные особенности (Key Features)

* **Decoupling (Слабая связанность):** `order-service` и `delivery-service` полностью изолированы. Общение происходит асинхронно через событийно-ориентированный подход.
* **Database per Service:** Каждая предметная область имеет собственную изолированную PostgreSQL БД (`order-db`, `payment-db`, `delivery-db`).
* **Idempotency (Идемпотентность):** Все `@KafkaListener` защищены от дубликатов сообщений (At-Least-Once Delivery) путем проверки текущего состояния сущностей в БД перед обработкой.
* **Healthcheck Cascading:** Все зависимости в `docker-compose.yml` увязаны через `condition: service_healthy`, что предотвращает упадок сервисов при старте (Wait-For-It pattern).

---

## Быстрый запуск системы в Docker

### 1. Подготовка переменных окружения
Создайте файл `.env` в корневом каталоге проекта со следующими параметрами:

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

ORDER_DB_NAME=order_db
ORDER_DB_PORT=5433

PAYMENT_DB_NAME=payment_db
PAYMENT_DB_PORT=5434

DELIVERY_DB_NAME=delivery_db
DELIVERY_DB_PORT=5435
```

### 2. Запуск всего приложения и инфраструктуры

Выполните команду для сборки Docker-образов и поднятия контейнеров:

```bash
docker compose up -d --build
```

### Доступные сервисы после старта:
* **Order Service:** `http://localhost:8081`
* **Payment Service:** `http://localhost:8082`
* **Delivery Service:** `http://localhost:8083`
* **Kafka Broker (Host):** `localhost:9092`

---

## Интерактивная документация (Swagger UI)

После запуска контейнеров Swagger UI доступен для каждого сервиса:
* **Order Service Swagger:** `http://localhost:8081/swagger-ui.html`
* **Delivery Service Swagger:** `http://localhost:8083/swagger-ui.html`

---

## Сценарий сквозного тестирования (E2E Test Flow)

1. **Создание и Оплата заказа (`order-service`):**
   ```http
   POST http://localhost:8081/api/payments?orderId=1&userId=USER-1&amount=1500
   ```
   _Статус заказа сменится на `PAID` ➔ `delivery-service` через Kafka автоматически назначит курьера ➔ Статус заказа станет `DELIVERY_ASSIGNED`._

2. **Курьер забрал заказ из ресторана (`delivery-service`):**
   ```http
   POST http://localhost:8083/api/deliveries/1/pickup
   ```
   _Статус заказа в Order Service через Kafka станет `IN_DELIVERY`._

3. **Курьер доставил заказ клиенту (`delivery-service`):**
   ```http
   POST http://localhost:8083/api/deliveries/1/complete
   ```
   _Статус заказа в Order Service через Kafka станет `DELIVERED`, курьер снова перейдет в статус `AVAILABLE`._