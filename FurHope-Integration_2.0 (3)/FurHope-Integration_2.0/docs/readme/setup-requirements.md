# Setup Requirements (For New Developers)

This file explains what must be installed before running the project.

## Do I need to install Spring Boot?

No separate Spring Boot installation is required for the social module.
Social feed features are now JDBC-only and run inside the JavaFX app.

## What must be installed

1. JDK 21 (recommended)
2. Maven 3.9+
3. MariaDB 10.4+ (or MySQL-compatible MariaDB server)
4. Git

## Required database

- Host: `127.0.0.1`
- Port: `3306`
- Schema: `hamza`

If schema/tables/views are missing, import:

- `infrastructure/database/hamza.sql`

## Environment / credentials

The JavaFX app reads DB config from `modules/common/resources/application.properties` with env fallback.

If local credentials differ, set:

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="hamza"
$env:DB_USER="safwen"
$env:DB_PASSWORD="YOUR_PASSWORD"
```

Optional local override files are stored under `infrastructure/secrets/`.

## Verify installed tools

```powershell
java -version
mvn -version
```

## First run (from one root path)

```powershell
cd <PROJECT_ROOT>
mvn -q javafx:run
```

## Common setup issues

### `mvn` is not recognized

Maven is not installed or not in `PATH`.

### `release version 21 not supported`

Wrong Java version is active. Switch to JDK 21.

### `Port 8081 already in use`

This is no longer required for social features because the social Spring Boot API is disabled.

### DB access denied

Check `DB_USER` / `DB_PASSWORD` and verify DB login manually.
