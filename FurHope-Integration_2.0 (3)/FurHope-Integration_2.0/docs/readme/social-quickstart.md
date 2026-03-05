# Social Module Quick Start (JDBC Only)

Social features run directly against MariaDB from the JavaFX app.
No separate social Spring Boot process is required.

## 1) Prerequisites

- MariaDB/XAMPP running on `127.0.0.1:3306`
- Schema: `hamza`
- Java + Maven installed

If schema/tables are missing, import `infrastructure/database/hamza.sql`.

## 2) Configure DB

Set values in `modules/common/resources/application.properties`:

- `db.host`
- `db.port`
- `db.name`
- `db.user`
- `db.password`

Or override with environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`).

## 3) Run

```powershell
cd <PROJECT_ROOT>
mvn -q javafx:run
```

In UI: sign in -> `Social`.

## 4) Build Check

```powershell
cd <PROJECT_ROOT>
mvn -q clean package
```

## 5) Troubleshooting

### DB access denied

- Verify DB credentials in `application.properties` or env vars.
- Confirm login works in MariaDB/phpMyAdmin.

### Port 8081 issue

The social module no longer depends on port `8081`.
If another legacy process is running there, it is not needed for social feed.
