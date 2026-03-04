# Setup Requirements (For New Developers)

This file explains what must be installed before running the project.

## Do I need to install Spring Boot?

No separate Spring Boot installation is required.
Spring Boot is pulled automatically by Maven from `social-api/pom.xml`.

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

- `hamza.sql` (at repository root)

## Environment / credentials

Social API reads DB config from `social-api/src/main/resources/application.properties` with env fallback.

If local credentials differ, set:

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="hamza"
$env:DB_USER="safwen"
$env:DB_PASSWORD="YOUR_PASSWORD"
```

## Verify installed tools

```powershell
java -version
mvn -version
```

## First run (from one root path)

Terminal A:

```powershell
cd <PROJECT_ROOT>\social-api
mvn -q spring-boot:run
```

Terminal B:

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

```powershell
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### API DB access denied

Check `DB_USER` / `DB_PASSWORD` and verify DB login manually.
