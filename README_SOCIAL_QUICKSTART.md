# Social Module Quick Start (Single Root Path)

This repo now contains both:

- Integration desktop app (repo root)
- Social Spring Boot API (`social-api` subfolder)

## 1) Required Paths (same repository)

Use one project root only:

- `<PROJECT_ROOT>\pom.xml` (Integration app)
- `<PROJECT_ROOT>\social-api\pom.xml` (Spring Boot API)

Example:

- `D:\workspace\FurHope-Integration_1.0\pom.xml`
- `D:\workspace\FurHope-Integration_1.0\social-api\pom.xml`

## 2) Prerequisites

- MariaDB running on `127.0.0.1:3306`
- Database/schema: `hamza`
- Java + Maven installed

## 3) DB Expectations

The Social API expects:

- tables: `post`, `comment`, `notification`, `friendship`, `friend_request`, `user`
- views: `api_user`, `api_post`, `api_comment`

If missing, import `hamza.sql`.

## 4) Launch Commands (Ready to Use)

Start API first, then app.

### Terminal A (Spring Boot API)

```powershell
cd <PROJECT_ROOT>\social-api
mvn -q spring-boot:run
```

Health check:

```powershell
curl http://127.0.0.1:8081/api/health
```

Expected:

```json
{"status":"ok"}
```

### Terminal B (Integration JavaFX app)

```powershell
cd <PROJECT_ROOT>
mvn -q javafx:run
```

In UI: sign in -> `Social`.

## 5) Config Notes

`social-api\src\main\resources\application.properties`:

- `server.port=${API_PORT:8081}`
- datasource defaults to MariaDB `hamza`

Integration Social UI points to `http://127.0.0.1:8081`.

## 6) Troubleshooting

### A) `Port 8081 was already in use`

```powershell
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

Then restart API.

### B) DB access denied

- Verify `DB_USER` / `DB_PASSWORD` or values in API properties.
- Confirm login works in MariaDB/phpMyAdmin.

### C) Social shows API 500 on `/api/posts/feed`

- Verify `api_user`, `api_post`, `api_comment` views exist.
- Ensure you are running API from `<PROJECT_ROOT>\social-api`.

## 7) Build Checks

API:

```powershell
cd <PROJECT_ROOT>\social-api
mvn -q clean package
```

Integration:

```powershell
cd <PROJECT_ROOT>
mvn -q clean package
```
