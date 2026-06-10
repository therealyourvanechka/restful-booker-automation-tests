# restful-booker-automation-tests

[![Tests](https://github.com/therealyourvanechka/restful-booker-automation-tests/actions/workflows/test.yml/badge.svg)](https://github.com/therealyourvanechka/restful-booker-automation-tests/actions/workflows/test.yml)

Allure-отчёт на GitHub Pages: https://therealyourvanechka.github.io/restful-booker-automation-tests

Проект написан, чтобы отработать на практике слоистую архитектуру автотестов и закрепить навыки работы с REST Assured и Java

**Цель**: не просто покрыть эндпоинты тестами, а выстроить структуру которую легко читать, поддерживать и расширять — как это делается в реальных командах

В качестве объекта тестирования выбран публичный сервис [Restful Booker](https://restful-booker.herokuapp.com) — REST API для управления бронированиями.

В процессе тестирования были обнаружены баги API: некорректные статус-коды, отсутствие валидации входных данных, расхождение поведения фильтров с документацией, все они зафиксированы через `@Disabled`-тесты с описанием

## Стек

| Инструмент | Версия | Назначение |
|---|---|---|
| Java | 21 | Язык разработки |
| REST Assured | 5.5.0 | HTTP-клиент для тестирования API |
| JUnit 5 | 5.11.0 | Фреймворк для тестов |
| AssertJ | 3.26.3 | Утверждения, включая soft assertions |
| Allure Report | 2.42.0 | HTML-отчётность по результатам тестов |
| Jackson + JavaTimeModule | 2.18.3 | Сериализация/десериализация JSON, поддержка LocalDate |
| JSON Schema Validator | 5.5.0 | Валидация структуры ответов |
| DataFaker | 2.4.3 | Генерация тестовых данных |
| Lombok | 1.18.36 | Снижение boilerplate-кода в моделях |
| dotenv-java | 3.1.0 | Загрузка конфигурации из .env файла |
| Gradle | 9.0.0 | Сборщик проекта |
| GitHub Actions | — | CI/CD: сборка, тесты, публикация Allure-отчёта |

## Архитектура

Проект разбит на слои:

```
src/
├── main/java/com/booker/
│   ├── client/                   # HTTP-клиенты для каждого эндпоинта
│   │   ├── BaseClient.java       # Базовый клиент
│   │   ├── AuthClient.java       # POST /auth
│   │   ├── BookingClient.java    # CRUD /booking
│   │   └── HealthCheckClient.java
│   ├── model/                    # DTO
│   │   ├── BookingDates.java
│   │   ├── request/
│   │   └── response/
│   ├── exception/
│   │   └── AuthenticationException.java
│   └── util/
│       ├── Config.java           
│       └── Specifications.java   # Базовая спецификация REST Assured
│
└── test/java/com/booker/
    ├── BaseTest.java             # @BeforeAll: Jackson, Allure, инициализация клиентов
    ├── util/
    │   └── BookingDataFactory.java  # Генерация тестовых данных
    └── controllers/              # Тест-контроллеры по эндпоинтам
        ├── HealthCheckControllerTest.java
        ├── AuthControllerTest.java
        └── booking/              # Разбиты по эндпоинтам
            ├── BaseBookingControllerTest.java
            ├── PostBookingControllerTest.java
            ├── GetBookingControllerTest.java
            ├── GetBookingByIdControllerTest.java
            ├── PutBookingControllerTest.java
            ├── PatchBookingControllerTest.java
            └── DeleteBookingControllerTest.java
```

**Client-слой** инкапсулирует всю HTTP-логику: каждый эндпоинт представлен двумя методами: типизированным для позитивных сценариев, где нужен десериализованный объект, и raw — для негативных где важен статус-код и тело ответа

**Controller-слой** содержит только проверки

**POJO-классы (DTO)** необходимы при сериализации запросов и десериализации ответов, для работы с датами подключён `JavaTimeModule`

**BookingDataFactory** отвечает за генерацию тестовых данных, используется `DataFaker`, возвращает builder для гибкой кастомизации данных в тестах

**Booking-контроллеры** разбиты по эндпоинтам: каждый HTTP-метод (`POST`, `GET`, `PUT`, `PATCH`, `DELETE`) — в отдельном классе в пакете `controllers/booking/`

**`@Disabled` вместо удаления тестов с багами**: баги API не удаляются — они документируются, каждый `@Disabled`-тест содержит описание ожидаемого и фактического поведения

## CI/CD

**GitHub Actions** (`.github/workflows/test.yml`):

| Стадия | Что делает |
|--------|------------|
| `build` | Компиляция Java-кода |
| `test` | Запуск тестов, генерация Allure-отчёта с историей прогонов, публикация на GitHub Pages |

Триггеры: `push` и `pull_request` в ветку `main`

Allure-отчёт автоматически обновляется после каждого запуска тестов

## Тесты

Обозначения: ОК — прошёл, KO — не прошёл (баг API, тест отключён через `@Disabled`)

### HealthCheck

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 1 | OK | GET /ping | 201, тело = "Created" |

### Auth — POST /auth

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 2 | OK | Валидные креды | 200, токен есть, схема валидна |
| 3 | OK | Невалидные креды | 200, Bad credentials, токена нет |
| 4 | OK | Пустое тело | 200, Bad credentials, токена нет |

### Booking — GET /booking

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 5 | OK | Без фильтров | 200, список непустой, схема валидна |
| 6 | OK | Фильтр по `firstname` + `lastname` | Созданная бронь в списке |
| 7 | OK | `firstname` + `checkin` | Созданная бронь в списке |
| 8 | OK | Несуществующий `firstname` | 200, пустой список |
| 9 | OK | `checkin > param` (бронь с checkin=15, param=16) | Бронь не найдена |
| 10 | KO | Невалидный `checkin` (5 вариантов) | 500 вместо 400 |
| 11 | KO | Невалидный `checkout` (5 вариантов) | 500 вместо 400 |
| 12 | KO | `checkin >= param` (бронь с checkin=15, params 14 и 15) | checkin=15 не найден (`>=` как `>`) |
| 13 | KO | `checkout >= param` (бронь с checkout=22, params 21 и 22) | checkout=21 не найден (`>=` как `<=`) |
| 14 | KO | `checkout` вне границы (бронь с checkout=22, param=23) | бронь найдена (`<=` включает 22) |

### Booking — GET /booking/{id}

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 15 | OK | Существующий id | 200, поля совпадают, схема валидна |
| 16 | OK | Несуществующий id | 404 |
| 17 | KO | Невалидный формат id (строка) | 404 вместо 400 |

### Booking — POST /booking

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 18 | OK | Валидные данные | 200, поля совпадают + GET 200 |
| 19 | KO | Отсутствует `firstname` | 500 вместо 400 |

### Booking — PUT /booking/{id}

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 20 | OK | Полное обновление с токеном | 200, данные обновились, схема валидна |
| 21 | OK | Полное обновление с Basic Auth | 200, данные обновились, схема валидна |
| 22 | OK | Без авторизации | 403 |
| 23 | OK | Неполный payload | 400 |

### Booking — PATCH /booking/{id}

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 24 | OK | Частичное обновление с токеном | 200, поле обновилось, остальные нет |
| 25 | OK | Без авторизации | 403 |

### Booking — DELETE /booking/{id}

| # | | Проверка | Ожидаемый результат |
|---|---|---|---|
| 26 | OK | Удаление с токеном | 201, затем GET → 404 |
| 27 | OK | Без авторизации | 403 |
| 28 | KO | Несуществующий id | 405 вместо 404 |

**28 тестов** — 20 OK, 8 KO (баги API, `@Disabled`)

## Запуск

### Требования

- Java 21+
- Gradle (или использовать `./gradlew`)

### Локальный запуск

```bash
./gradlew test
```

### Через .env файл

Создать файл `.env` в корне проекта:

```
BASE_URL=https://restful-booker.herokuapp.com
AUTH_USERNAME=admin
AUTH_PASSWORD=password123
```

### Через переменные окружения

```bash
BASE_URL=https://restful-booker.herokuapp.com \
AUTH_USERNAME=username \
AUTH_PASSWORD=password \
./gradlew test
```

### Allure-отчёт

Отчёт генерируется автоматически после запуска тестов:

```bash
./gradlew test
```

HTML-отчёт сохраняется в папку `build/allureReport/`. Откройте `build/allureReport/index.html` в браузере:

```bash
open build/allureReport/index.html
```

Или через Allure CLI:

```bash
allure serve build/allureReport
```

Allure-отчёт на GitHub Pages: https://therealyourvanechka.github.io/restful-booker-automation-tests

## Конфигурация

Приоритет загрузки: `.env` файл (1) -> переменные окружения (2)

| Переменная | Значение по умолчанию |
|---|---|
| `BASE_URL` | `https://restful-booker.herokuapp.com` |
| `AUTH_USERNAME` | `admin` |
| `AUTH_PASSWORD` | `password123` |

