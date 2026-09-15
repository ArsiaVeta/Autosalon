# Autosalon — архитектура автосалона

Два микросервиса на Spring Boot: оформление и сопровождение заказов на автомобили — из наличия
и с индивидуальной комплектацией, заявки на тест-драйв — и складской учёт моделей, комплектующих
и сборки. Синхронно сервисы общаются по gRPC, асинхронно — через Kafka.

Стек: Spring Boot 3.2 (Java 17), PostgreSQL + Liquibase, Kafka, gRPC, Keycloak, Docker Compose.

**2** сервиса · **2** базы PostgreSQL · **4** топика + DLT · **9** статусов заказа · **4** роли Keycloak · **93** теста

## Содержание

1. [Состав системы](#1-состав-системы)
2. [Как сервисы общаются](#2-как-сервисы-общаются)
3. [Заказ и его статусы](#3-заказ-и-его-статусы)
4. [Диаграммы](#4-диаграммы)
5. [Надёжность обмена](#5-надёжность-обмена)
6. [Роли и доступ](#6-роли-и-доступ)
7. [API](#7-api)
8. [Запуск и токен](#8-запуск-и-токен)

## 1. Состав системы

| Компонент | Технология | Порт | Назначение |
|---|---|---|---|
| **OrderService** | Spring Boot 3.2, Java 17 | `8080` | Клиенты, заказы, тест-драйвы, конечный автомат статусов заказа, transactional outbox |
| **StorageService** | Spring Boot 3.2, Java 17 | `8082` (HTTP), `9090` (gRPC) | Модели автомобилей, комплектующие, сборочные заказы, проверка наличия |
| **order-db · storage-db** | PostgreSQL 15 | `5432` · `5433` | По собственной базе на сервис (database per service) |
| **Kafka** | confluentinc/cp-kafka 7.6, режим KRaft | `9092` | Доменные события между сервисами |
| **Kafka UI** | provectuslabs/kafka-ui | `8090` | Просмотр топиков, сообщений и consumer-групп |
| **Keycloak** | quay.io/keycloak 24, realm `autosalon` | `8081` | OAuth2 / OIDC, выдача JWT, роли |

Схема БД в обоих сервисах создаётся Liquibase, миграции лежат в `src/main/resources/changelog`.

## 2. Как сервисы общаются

* **gRPC — когда ответ нужен сразу.** OrderService спрашивает у склада список доступных автомобилей
  в рамках HTTP-запроса пользователя (`CarAvailabilityService.ListAvailableCars`, `GetAvailableCar`).
  Контракт — `src/main/proto/car_availability.proto`.
* **Kafka — когда операция длительная.** Согласование комплектации, сборка и резервирование
  автомобиля. Пользователь не ждёт: заказ меняет статус по мере прихода событий.
* **REST + JWT — наружу.** Внешний API обоих сервисов. Токен выдаёт Keycloak, сервисы проверяют
  подпись по JWKS и достают роли из `realm_access`.

### Топики Kafka

| Топик | Направление | Событие | Смысл |
|---|---|---|---|
| `order.sent` | Order → Storage | `OrderSentForApprovalEvent` | Запрос согласования комплектации (`phase=APPROVAL`) или запрос сборки и резерва (`phase=ASSEMBLY`) |
| `order.approved` | Storage → Order | `OrderApprovedEvent` (`phase`) | Комплектация согласована либо автомобиль готов |
| `order.accepted` | Storage → Order | `OrderAcceptedEvent` | Сборочный заказ принят в работу |
| `order.rejected` | Storage → Order | `OrderRejectedEvent` (`reason`) | Отказ склада с причиной |
| `<topic>.DLT` | — | — | Сообщение после трёх неудачных попыток обработки, с заголовками `kafka_dlt-*` |

## 3. Заказ и его статусы

Заказ (`orders`) связан с:

* **клиентом** — `client_id`, идентификатор пользователя из Keycloak (`sub` токена);
* **менеджером** — `manager_id`, назначается **автоматически и произвольно** из справочника
  `managers` при оформлении заказа (`ManagerAssignmentService`);
* **автомобилем** — `car_model_id`, а для заказа с комплектацией ещё и таблица `order_components`
  (тип компонента → идентификатор детали).

Переходы описаны в `OrderStateMachine` и проверяются при каждом изменении. Два маршрута и общий переход в отмену:

```mermaid
stateDiagram-v2
    state "IN_STOCK — автомобиль в наличии" as IN_STOCK {
        state "CREATED — оформлен" as CREATED_IS
        state "APPROVED_BY_MANAGER — согласован менеджером" as APPROVED_M
        state "AWAIT_FOR_PAYMENT — ожидает оплаты" as AWAIT_PAY_IS
        state "PAID — оплачен" as PAID_IS
        state "READY_FOR_PICKUP — готов к выдаче" as READY_IS
        state "COMPLETED — завершён" as DONE_IS

        [*] --> CREATED_IS: оформлен клиентом
        CREATED_IS --> APPROVED_M: approve (менеджер)
        APPROVED_M --> AWAIT_PAY_IS: invoice (менеджер)
        AWAIT_PAY_IS --> PAID_IS: pay (клиент)
        PAID_IS --> READY_IS: order.approved (склад)
        READY_IS --> DONE_IS: complete (менеджер)
        DONE_IS --> [*]
    }

    state "CUSTOM — автомобиль с комплектацией" as CUSTOM {
        state "CREATED — оформлен" as CREATED_C
        state "APPROVED_BY_STORAGE — согласован складом" as APPROVED_S
        state "AWAIT_FOR_PAYMENT — ожидает оплаты" as AWAIT_PAY_C
        state "PAID — оплачен" as PAID_C
        state "AWAIT_DELIVERY — ожидает доставки" as DELIVERY
        state "READY_FOR_PICKUP — готов к выдаче" as READY_C
        state "COMPLETED — завершён" as DONE_C

        [*] --> CREATED_C: оформлен клиентом
        CREATED_C --> APPROVED_S: order.approved, phase = APPROVAL
        APPROVED_S --> AWAIT_PAY_C: invoice (менеджер)
        AWAIT_PAY_C --> PAID_C: pay (клиент)
        PAID_C --> DELIVERY: order.accepted (склад)
        DELIVERY --> READY_C: order.approved, phase = ASSEMBLY
        READY_C --> DONE_C: complete (менеджер)
        DONE_C --> [*]
    }

    state "CANCELLED — отменён" as CANCELLED

    IN_STOCK --> CANCELLED: cancel / order.rejected
    CUSTOM --> CANCELLED: cancel / order.rejected
    CANCELLED --> [*]

    note right of CANCELLED
        Переход в CANCELLED возможен из любого
        незавершённого статуса. COMPLETED и CANCELLED —
        конечные: дальнейшие переходы запрещены
        (HTTP 400, событие игнорируется)
    end note
```

> **CANCELLED** — из любого незавершённого статуса: по запросу клиента или менеджера либо при отказе
> склада. `COMPLETED` и `CANCELLED` конечные: попытка перехода из них возвращает 400, а событие
> с недопустимым переходом игнорируется и пишется в лог (предупреждение).

### Кто и чем двигает заказ

| Статус | Смысл | Инициатор | Действие |
|---|---|---|---|
| `CREATED` | Оформлен | клиент | `POST /api/orders` |
| `APPROVED_BY_MANAGER` | Согласован менеджером, только `IN_STOCK` | назначенный менеджер | `POST /api/orders/{id}/approve` |
| `APPROVED_BY_STORAGE` | Согласован складом, только `CUSTOM` | StorageService | `order.approved`, `phase=APPROVAL` |
| `AWAIT_FOR_PAYMENT` | Ожидает оплаты | назначенный менеджер | `POST /api/orders/{id}/invoice` |
| `PAID` | Оплачен, публикуется `order.sent` с `phase=ASSEMBLY` | клиент | `POST /api/orders/{id}/pay` |
| `AWAIT_DELIVERY` | Ожидает доставки автомобиля, только `CUSTOM` | StorageService | `order.accepted` |
| `READY_FOR_PICKUP` | Автомобиль готов к выдаче | StorageService | `order.approved`, `phase=ASSEMBLY` |
| `COMPLETED` | Завершён | назначенный менеджер | `POST /api/orders/{id}/complete` |
| `CANCELLED` | Отменён | клиент, менеджер, склад | `POST /api/orders/{id}/cancel` · `order.rejected` |

## 4. Диаграммы

### 4.1. Варианты использования

Роли и сценарии. Администратор наследует права менеджера и кладовщика.

```mermaid
flowchart LR
    client["Клиент<br>(USER)"]
    manager["Менеджер<br>(MANAGER)"]
    warehouse["Кладовщик<br>(WAREHOUSE_ADMIN)"]
    admin["Администратор<br>(ADMIN)"]

    subgraph salon["Автосалон"]
        subgraph orders["Заказы"]
            UC1(["Просмотреть автомобили<br>в наличии"])
            UC2(["Оформить заказ<br>(в наличии)"])
            UC3(["Оформить заказ<br>(с комплектацией)"])
            UC4(["Назначить менеджера<br>автоматически"])
            UC5(["Согласовать заказ<br>менеджером"])
            UC6(["Согласовать комплектацию<br>складом"])
            UC7(["Выставить счёт"])
            UC8(["Оплатить заказ"])
            UC9(["Отследить статус<br>заказа"])
            UC10(["Завершить заказ<br>(выдать автомобиль)"])
            UC11(["Отменить заказ"])
        end
        subgraph testdrive["Тест-драйв"]
            UC12(["Подать заявку<br>на тест-драйв"])
            UC13(["Обработать заявку<br>на тест-драйв"])
        end
        subgraph storage["Склад"]
            UC14(["Вести каталог<br>моделей"])
            UC15(["Вести склад<br>комплектующих"])
            UC16(["Собрать автомобиль<br>по заказу"])
            UC17(["Проверить наличие<br>автомобиля"])
        end
        subgraph clientsPkg["Клиенты"]
            UC18(["Вести карточки<br>клиентов"])
        end
    end

    client --> UC1 & UC2 & UC3 & UC8 & UC9 & UC11 & UC12
    manager --> UC5 & UC7 & UC9 & UC10 & UC11 & UC13 & UC18
    warehouse --> UC14 & UC15 & UC16
    admin ==>|наследует| manager
    admin ==>|наследует| warehouse

    UC2 -.->|«include»| UC4
    UC3 -.->|«include»| UC4
    UC3 -.->|«include»| UC6
    UC1 -.->|«include»| UC17
    UC8 -.->|«include»| UC17
    UC8 -.->|«include»| UC16

    noteUC4["Менеджер выбирается произвольно<br>из справочника менеджеров"]
    noteUC6["Выполняет StorageService<br>асинхронно через Kafka"]
    noteJwt["Все сценарии, кроме проверки<br>доступности сервиса, требуют<br>JWT, выданный Keycloak"]
    UC4 -.- noteUC4
    UC6 -.- noteUC6

    classDef note stroke-dasharray: 4 3
    class noteUC4,noteUC6,noteJwt note
```

### 4.2. Компоненты

Внутреннее устройство сервисов и связи между ними: REST, gRPC, Kafka, JDBC, JWKS.

```mermaid
flowchart LR
    user["Клиент / Менеджер /<br>Кладовщик"]

    subgraph kc["Keycloak :8081"]
        realm["realm autosalon<br>роли USER, MANAGER,<br>WAREHOUSE_ADMIN, ADMIN"]
    end

    subgraph osSvc["OrderService :8080"]
        direction TB
        os_api["<b>REST API</b><br>OrderController, ClientController,<br>TestDriveController, CarController"]
        os_sec["<b>Security</b><br>JwtAuthConverter, OrderSecurity"]
        os_app["<b>Application</b><br>OrderService, ManagerAssignmentService,<br>MessageIdempotencyService"]
        os_domain["<b>Domain</b><br>OrderContext, OrderStateMachine"]
        os_outbox["<b>Outbox</b><br>OutboxService, OutboxScheduler"]
        os_listener["<b>Kafka listeners</b><br>StorageEventListener, DeadLetterListener"]
        os_grpc["<b>gRPC client</b><br>StorageGrpcClient"]
    end

    odb[("<b>order-db</b> (PostgreSQL)<br>orders, order_components, clients,<br>managers, outbox_event,<br>processed_message, test_drive_applications")]

    kafka[["<b>Kafka :9092</b><br>order.sent, order.approved,<br>order.accepted, order.rejected<br>+ topic.DLT для каждого:<br>туда после 3 неудачных попыток"]]

    subgraph ssSvc["StorageService :8082 / :9090"]
        direction TB
        ss_api["<b>REST API</b><br>CarModelController, PartController,<br>AssemblyOrderController"]
        ss_sec["<b>Security</b>"]
        ss_grpc["<b>gRPC server</b><br>CarAvailabilityGrpcService"]
        ss_listener["<b>Kafka listeners</b><br>OrderEventListener, DeadLetterListener"]
        ss_app["<b>Application</b><br>CarAvailabilityService, AssemblyOrderService,<br>CarModelService, PartService"]
        ss_pub["<b>Kafka publisher</b><br>OrderEventPublisher"]
    end

    sdb[("<b>storage-db</b> (PostgreSQL)<br>car_models, parts,<br>car_model_components,<br>car_assembly_orders, processed_message")]

    user -->|password grant| realm
    user -->|"HTTPS/JSON, Bearer JWT"| os_api
    user -->|"HTTPS/JSON, Bearer JWT"| ss_api

    os_sec -.->|JWKS| realm
    ss_sec -.->|JWKS| realm

    os_api --> os_sec
    os_api --> os_app
    os_api --> os_grpc
    os_app --> os_domain
    os_app --> os_outbox
    os_app --> odb
    os_outbox --> odb
    os_listener --> os_app

    os_grpc -->|"gRPC<br>car_availability.proto"| ss_grpc
    ss_grpc --> ss_app
    ss_api --> ss_sec
    ss_api --> ss_app
    ss_app --> sdb
    ss_listener --> ss_app
    ss_listener --> ss_pub

    os_outbox -->|"publish<br>order.sent"| kafka
    kafka -->|"consume<br>order.sent"| ss_listener
    ss_pub -->|"publish<br>order.approved, order.accepted,<br>order.rejected"| kafka
    kafka -->|"consume<br>order.approved, order.accepted,<br>order.rejected"| os_listener
```

### 4.3. Покупка автомобиля в наличии (`IN_STOCK`)

От получения токена до выдачи автомобиля, с outbox и идемпотентной обработкой событий.

```mermaid
sequenceDiagram
    actor client as Клиент
    actor manager as Менеджер
    participant kc as Keycloak
    participant os as OrderService
    participant odb as order-db
    participant kafka as Kafka
    participant ss as StorageService
    participant sdb as storage-db

    client->>kc: POST /token (password grant)
    kc-->>client: access_token (JWT, роль USER)

    client->>os: POST /api/orders<br>{type: IN_STOCK, carModelId}
    activate os
    os->>os: проверка JWT, clientId = sub
    os->>odb: выбрать случайного менеджера
    odb-->>os: managerId
    os->>odb: INSERT order (status = CREATED)
    os-->>client: 200 {status: CREATED, managerId}
    deactivate os

    manager->>os: POST /api/orders/{id}/approve
    activate os
    os->>os: CREATED → APPROVED_BY_MANAGER
    os->>odb: UPDATE status
    os-->>manager: 200 {status: APPROVED_BY_MANAGER}
    deactivate os

    manager->>os: POST /api/orders/{id}/invoice
    activate os
    os->>os: APPROVED_BY_MANAGER → AWAIT_FOR_PAYMENT
    os->>odb: UPDATE status
    os-->>manager: 200 {status: AWAIT_FOR_PAYMENT}
    deactivate os

    client->>os: POST /api/orders/{id}/pay
    activate os
    os->>os: AWAIT_FOR_PAYMENT → PAID
    Note right of os: одна транзакция:<br>статус заказа + запись outbox
    os->>odb: UPDATE status + INSERT outbox_event<br>(OrderSentForApproval, phase = ASSEMBLY)
    os-->>client: 200 {status: PAID}
    deactivate os

    os->>os: OutboxScheduler (fixedDelay 5s)
    os->>kafka: publish order.sent<br>(key = orderId, header X-Trace-Id)
    os->>odb: outbox_event.processed = true

    kafka->>ss: consume order.sent
    activate ss
    ss->>sdb: SELECT processed_message (идемпотентность)
    ss->>sdb: проверка наличия модели (stock_count > 0)
    ss->>sdb: INSERT car_assembly_order (CREATED → ASSEMBLED)
    ss->>kafka: publish order.approved (phase = ASSEMBLY)
    ss->>sdb: INSERT processed_message
    deactivate ss

    kafka->>os: consume order.approved
    activate os
    os->>odb: SELECT processed_message (идемпотентность)
    os->>os: PAID → READY_FOR_PICKUP
    os->>odb: UPDATE status + INSERT processed_message
    deactivate os

    client->>os: GET /api/orders/{id}
    os-->>client: 200 {status: READY_FOR_PICKUP}

    manager->>os: POST /api/orders/{id}/complete
    activate os
    os->>os: READY_FOR_PICKUP → COMPLETED
    os->>odb: UPDATE status
    os-->>manager: 200 {status: COMPLETED}
    deactivate os
```

### 4.4. Покупка автомобиля с комплектацией (`CUSTOM`)

Согласование складом до оплаты, сборка и доставка после.

```mermaid
sequenceDiagram
    actor client as Клиент
    actor manager as Менеджер
    participant os as OrderService
    participant odb as order-db
    participant kafka as Kafka
    participant ss as StorageService
    participant sdb as storage-db

    client->>os: POST /api/orders<br>{type: CUSTOM, carModelId, components}
    activate os
    os->>odb: выбрать случайного менеджера
    os->>odb: INSERT order (CREATED) + order_components<br>+ outbox_event (phase = APPROVAL)
    os-->>client: 200 {status: CREATED, managerId}
    deactivate os

    os->>kafka: publish order.sent (phase = APPROVAL)
    kafka->>ss: consume order.sent
    activate ss
    ss->>sdb: проверка модели и комплектующих
    alt комплектация доступна
        ss->>kafka: publish order.approved (phase = APPROVAL)
    else отказ
        ss->>kafka: publish order.rejected (reason)
    end
    deactivate ss

    kafka->>os: consume order.approved (phase = APPROVAL)
    os->>os: CREATED → APPROVED_BY_STORAGE
    os->>odb: UPDATE status

    manager->>os: POST /api/orders/{id}/invoice
    os->>os: APPROVED_BY_STORAGE → AWAIT_FOR_PAYMENT
    os-->>manager: 200 {status: AWAIT_FOR_PAYMENT}

    client->>os: POST /api/orders/{id}/pay
    activate os
    os->>os: AWAIT_FOR_PAYMENT → PAID
    os->>odb: UPDATE status + INSERT outbox_event<br>(phase = ASSEMBLY)
    os-->>client: 200 {status: PAID}
    deactivate os

    os->>kafka: publish order.sent (phase = ASSEMBLY)
    kafka->>ss: consume order.sent
    activate ss
    ss->>sdb: INSERT car_assembly_order (CREATED)
    ss->>kafka: publish order.accepted (assemblyOrderId)
    ss->>sdb: car_assembly_order → ASSEMBLED
    ss->>kafka: publish order.approved (phase = ASSEMBLY)
    deactivate ss

    kafka->>os: consume order.accepted
    os->>os: PAID → AWAIT_DELIVERY
    os->>odb: UPDATE status

    kafka->>os: consume order.approved (phase = ASSEMBLY)
    os->>os: AWAIT_DELIVERY → READY_FOR_PICKUP
    os->>odb: UPDATE status

    manager->>os: POST /api/orders/{id}/complete
    os->>os: READY_FOR_PICKUP → COMPLETED
    os-->>manager: 200 {status: COMPLETED}
```

### 4.5. Аутентификация

Password grant, проверка подписи по JWKS, роли и ответы 401 / 403.

```mermaid
sequenceDiagram
    actor user as Пользователь (Postman)
    participant kc as Keycloak :8081
    participant os as OrderService :8080
    participant chain as SecurityFilterChain
    participant conv as JwtAuthConverter
    participant ctrl as OrderController

    user->>kc: POST /realms/autosalon/protocol/openid-connect/token<br>grant_type=password, client_id=autosalon-app,<br>client_secret, username, password
    activate kc
    kc->>kc: проверка учётных данных,<br>сбор realm-ролей
    kc-->>user: access_token (JWT), refresh_token, expires_in
    deactivate kc

    Note over user: Pre-request скрипт коллекции хранит токен<br>и перезапрашивает его за 30 секунд<br>до истечения срока

    user->>os: GET /api/orders<br>Authorization: Bearer JWT
    activate os
    os->>chain: BearerTokenAuthenticationFilter
    chain->>kc: GET /protocol/openid-connect/certs (JWKS, кэшируется)
    kc-->>chain: публичные ключи
    chain->>chain: проверка подписи, issuer, exp
    chain->>conv: convert(Jwt)
    conv-->>chain: JwtAuthenticationToken<br>(ROLE_USER / ROLE_MANAGER / ...)
    chain->>ctrl: @PreAuthorize пройден
    ctrl-->>os: список заказов
    os-->>user: 200 OK
    deactivate os

    alt токен отсутствует или просрочен
        user->>os: GET /api/orders
        os-->>user: 401 {error: UNAUTHORIZED}
    else роли не хватает
        user->>os: POST /api/clients (роль USER)
        os-->>user: 403 {error: FORBIDDEN}
    end
```

### 4.6. Отказы и сбои

Отказ склада, три попытки обработки и DLT, недоступность Kafka при публикации.

```mermaid
sequenceDiagram
    actor client as Клиент
    participant os as OrderService
    participant kafka as Kafka
    participant ss as StorageService
    participant dlt as order.sent.DLT

    rect rgba(160, 160, 160, 0.12)
        Note over client,dlt: Отказ склада: автомобиля нет в наличии
        client->>os: POST /api/orders/{id}/pay
        os->>kafka: publish order.sent (phase = ASSEMBLY)
        kafka->>ss: consume order.sent
        activate ss
        ss->>ss: ensureAvailable() → CarNotAvailableException
        ss->>ss: car_assembly_order → FAIL
        ss->>kafka: publish order.rejected<br>(reason = "Car not in stock")
        deactivate ss
        kafka->>os: consume order.rejected
        os->>os: PAID → CANCELLED
        client->>os: GET /api/orders/{id}
        os-->>client: 200 {status: CANCELLED}
    end

    rect rgba(160, 160, 160, 0.12)
        Note over client,dlt: Сбой обработки события: невалидный payload
        kafka->>ss: consume order.sent (битое сообщение)
        activate ss
        ss->>ss: попытка 1 — ConversionException
        ss->>ss: пауза 3 с, попытка 2 — ошибка
        ss->>ss: пауза 3 с, попытка 3 — ошибка
        ss->>dlt: DeadLetterPublishingRecoverer<br>+ заголовки kafka_dlt-*
        deactivate ss
        dlt->>ss: DeadLetterListener логирует сообщение
    end

    rect rgba(160, 160, 160, 0.12)
        Note over client,dlt: Kafka недоступна при публикации
        os->>os: OutboxScheduler: publish → ошибка
        os->>os: attempts++, last_error,<br>запись остаётся необработанной
        os->>os: повтор через 5 с<br>(до app.outbox.max-attempts = 5)
        Note right of os: Заказ уже сохранён в БД,<br>событие не теряется
    end
```

## 5. Надёжность обмена

* **Transactional outbox.** Событие пишется в `outbox_event` в одной транзакции с изменением заказа.
  Планировщик публикует его в Kafka и помечает обработанным; при ошибке растит счётчик попыток
  до `app.outbox.max-attempts`.
* **Идемпотентность потребителя.** Таблица `processed_message` хранит `event_id`. Повторная доставка
  того же события не меняет статус заказа во второй раз.
* **Повторы и DLT.** Три попытки с паузой 3 секунды, затем `DeadLetterPublishingRecoverer` отправляет
  сообщение в `<topic>.DLT` вместе с заголовками с описанием ошибки. Слушатель DLT читает сырую
  строку, поэтому сам не падает на битом payload, и логирует сообщение.
* **Сквозной traceId.** Заголовок `X-Trace-Id` в HTTP, метаданных gRPC и заголовках Kafka попадает
  в MDC и в каждую строку лога обоих сервисов.

## 6. Роли и доступ

Роли realm `autosalon`: `USER`, `MANAGER`, `WAREHOUSE_ADMIN`, `ADMIN`.

| Действие | Кто может |
|---|---|
| Оформить заказ, оплатить, отменить свой заказ | `USER` (владелец) · `MANAGER` · `ADMIN` |
| Согласовать заказ, выставить счёт, завершить заказ | назначенный менеджер заказа · `ADMIN` |
| Видеть все заказы | `MANAGER` · `ADMIN` |
| Карточки клиентов (CRUD) | `MANAGER` · `ADMIN` |
| Модели, комплектующие, сборочные заказы | `WAREHOUSE_ADMIN` · `ADMIN` |
| Health, Swagger | без аутентификации |

Проверки — `@PreAuthorize` на контроллерах и сервисах; принадлежность заказа клиенту и назначенному
менеджеру проверяет бин `orderSecurity` (`isOwner`, `isAssignedManager`).

## 7. API

### OrderService (`http://localhost:8080`)

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/v1/health` | Состояние сервиса, БД и gRPC-связи со складом |
| `POST` | `/api/orders` | Оформить заказ: назначается менеджер, статус `CREATED` |
| `GET` | `/api/orders` | Список заказов: клиент видит только свои |
| `GET` | `/api/orders/{id}` | Заказ по идентификатору |
| `POST` | `/api/orders/{id}/approve` | Согласовать менеджером |
| `POST` | `/api/orders/{id}/invoice` | Выставить счёт |
| `POST` | `/api/orders/{id}/pay` | Оплатить, публикуется запрос складу |
| `POST` | `/api/orders/{id}/complete` | Завершить: автомобиль выдан |
| `POST` | `/api/orders/{id}/cancel` | Отменить |
| `DELETE` | `/api/orders/{id}` | Удалить |
| `POST` | `/api/clients` | Создать клиента |
| `GET/POST/DELETE` | `/api/test_drives` | Заявки на тест-драйв |
| `GET` | `/api/v1/cars`, `/api/v1/cars/{id}` | Автомобили в наличии (данные склада по gRPC) |

### StorageService (`http://localhost:8082`)

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/v1/health` | Состояние сервиса и БД |
| `GET/POST/DELETE` | `/api/car-models` | Модели автомобилей, фильтры: `brand`, `model`, `minPrice`, `maxPrice`, `bodyType`, `fuelType`, `gearBox`, `driveType`, `color`, `inStockOnly`, `component[WHEEL]=<uuid>` |
| `GET/POST/DELETE` | `/api/parts` | Комплектующие |
| `GET/POST/PUT/DELETE` | `/api/assembly-orders` | Сборочные заказы |

Swagger UI — на `/swagger-ui.html` обоих сервисов.

## 8. Запуск и токен

```bash
./gradlew :OrderService:bootJar :StorageService:bootJar
docker compose up -d --build
```

| Сервис | Адрес |
|---|---|
| OrderService | http://localhost:8080 (Swagger: `/swagger-ui.html`) |
| StorageService | http://localhost:8082 (Swagger: `/swagger-ui.html`), gRPC 9090 |
| Keycloak | http://localhost:8081 (admin/admin), realm `autosalon` |
| Kafka UI | http://localhost:8090 |

Realm Keycloak импортируется автоматически из `keycloak/realm-autosalon.json`. Пользователи
(логин = пароль): `admin`, `manager`, `manager2`, `manager3`, `warehouse`, `user`.

Получение токена:

```bash
curl -X POST http://localhost:8081/realms/autosalon/protocol/openid-connect/token \
  -d grant_type=password -d client_id=autosalon-app -d client_secret=autosalon-secret \
  -d username=user -d password=user
```

### Postman

Импортировать `postman/Autosalon.postman_collection.json` и `postman/Autosalon.postman_environment.json`.
То же, что `curl` выше, делает pre-request скрипт коллекции: токен запрашивается сам и обновляется
за 30 секунд до истечения, роль переключается запросами `Token: admin | manager | warehouse | user`
в папке `Auth`.

### Тесты

```bash
./gradlew test
```

Интеграционные тесты поднимают PostgreSQL через Testcontainers, поэтому нужен запущенный Docker.
