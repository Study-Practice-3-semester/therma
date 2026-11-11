# Интеллектуальная система рекомендаций по управлению воздухонагревателями 
*Индустриальный кейс ООО «ЕвразТехника ИС»*
## Структура репозитория
- `/backend` — расчетное ядро, бизнес-логика и API (REST + WebSockets).
- `/simulator` — легковесный воркер-генератор синтетической телеметрии.
- `/frontend` — веб-интерфейс оператора.
- `/docs` — архитектурная документация, контракты (OpenAPI/STOMP) и матрицы правил.

## Стек
**Backend & Simulator**
- Java 25 LTS
- Spring Boot 4.x
- Gradle
- jOOQ
- Flyway
- PostgreSQL 16
- WebSockets (STOMP)

**Frontend**
- TypeScript / React
- Vite / Bun
- Mantine UI 7
- Recharts
## Требования
- JDK 25
- Bun (последняя версия)
- Docker + Docker Compose

---
## Быстрый старт
Поднимите контейнер
```bash
docker compose up -d
```

Запустите сервисы (в разных вкладках терминала):
```bash
# Бэкенд
cd backend && ./gradlew bootRun

# Симулятор
cd simulator && ./gradlew bootRun

# Фронтенд
cd frontend && bun install && bun run dev
```
## Запуск собранного JAR и Отладка
Собрать исполняемые .jar файлы для Java-модулей:
```bash
# В папке backend или simulator
./gradlew bootJar
```

Запуск собранного jar:
```bash
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

Запуск с отладкой (JDWP) без сборки jar (по умолчанию порт 5005):
```bash
./gradlew bootRun --debug-jvm
```
## Тесты
Запуск тестов в бэкенде:

```bash
cd backend
./gradlew test
```

Интеграционные тесты слоя данных поднимают полный Spring-контекст с Testcontainers: `postgres:16-alpine` (через аннотацию `@ServiceConnection` параметры подключения подставляются автоматически).

Без запущенного Docker-демона тесты автоматически пропускаются (`disabledWithoutDocker = true`), сборка при этом остаётся зелёной. Экспертное ядро покрывается unit-тестами (JUnit 5 + Mockito), которым Docker не нужен.
## Дефолтные порты сервисов

|**Сервис**|**HTTP**|**WebSocket / Прочее**|
|---|---|---|
|**Frontend (React)**|`5173`|-|
|**Backend (API)**|`8080`|`ws://localhost:8080/ws-endpoint` (STOMP)|
|**Simulator**|`8081`|-|
|**PostgreSQL**|`5432`|Пользователь: `evraz_user`|
