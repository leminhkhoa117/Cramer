# Backend Build & Run Instructions

## Quick Start

### Linux / macOS: run-app.sh (Recommended for Unix-like systems)

```bash
cd backend
./run-app.sh
```

- This script automatically:
- Loads environment variables from root `.env` (if present)
- Uses system `JAVA_HOME` or system java when not set
- Builds with the Maven wrapper if no JAR is present, then runs the JAR

### Option (Windows): Using run-app.ps1 (Recommended for Windows)

This script automatically:
- Sets JAVA_HOME to Eclipse Adoptium JDK 21
- Loads environment variables from `.env`
- Runs the pre-built JAR file

### Option 2: Manual Build

If you need to rebuild manually on Linux (or macOS):

```bash
# Set JAVA_HOME if required
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH="$JAVA_HOME/bin:$PATH"

# Build and run using Maven wrapper
cd backend
./mvnw clean package -DskipTests

# Run the JAR
java -jar target/cramer-backend-0.0.1-SNAPSHOT.jar
```

On Windows you can use PowerShell version shown above (run-app.ps1).

### Note about .env file line endings

If your `.env` was created or edited on Windows it may use CRLF line endings which cause the shell to show errors like `$'\r': command not found` when the file is sourced.

Recommended fixes:
- Convert using `dos2unix`:
    ```bash
    sudo pacman -S dos2unix  # if not installed
    dos2unix .env
    ```
- Or convert with `sed`:
    ```bash
    sed -i 's/\r$//' .env
    ```

The `run-app.sh` script includes a handler that tries `dos2unix` automatically, and safely parses `.env` to avoid CRLF issues. If you still see an error, try converting the file first.

## Maven Build Issue Fix

If `.\mvnw.cmd` shows "No compiler provided":

### Solution 1: Fix Maven Wrapper

Edit `backend\.mvn\wrapper\maven-wrapper.properties` and ensure it downloads Maven correctly.

### Solution 2: Use System Maven

```powershell
# Install Maven if needed
choco install maven

# Use system Maven
cd backend
mvn clean package -DskipTests
```

### Solution 3: Use IntelliJ IDEA

1. Open `backend` folder in IntelliJ
2. Right-click `pom.xml` → "Add as Maven Project"
3. Run → Edit Configurations → Add Spring Boot
4. Run application

## Environment Variables

Create `backend\.env` file (copy from `.env.example`):

```properties
# Server
SERVER_PORT=8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://db.jpocdgkrvohmjkejclpl.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-password

# Supabase Auth
SUPABASE_JWT_SECRET=your-jwt-secret
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-key
```

## Testing API

### With Swagger UI (Easy!)

1. Start backend: `.\run-app.ps1`
2. Open browser: `http://localhost:8080/swagger-ui.html`
3. Click "Authorize" → Enter JWT token from Supabase
4. Try any endpoint!

### With PowerShell

```powershell
# Test public endpoint
Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/count" -Method Get

# Test with auth (need JWT token)
$token = "your-jwt-token-here"
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/profiles" -Headers $headers -Method Get
```

### With curl

```bash
# Test public
curl http://localhost:8080/api/profiles/count

# Test with auth
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8080/api/profiles
```

## Troubleshooting

### Issue: "No compiler is provided"
**Cause:** Maven Wrapper trying to use JRE instead of JDK  
**Fix:** Use `.\run-app.ps1` or set JAVA_HOME manually

### Issue: "Port 8080 already in use"
**Cause:** Another instance running  
**Fix:** 
```powershell
# Find process
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /PID <process-id> /F
```

### Issue: "Cannot connect to database"
**Cause:** Wrong database URL or credentials  
**Fix:** Check `.env` file, verify Supabase password

### Issue: "JWT validation failed"
**Cause:** Wrong JWT secret or expired token  
**Fix:** 
1. Get fresh JWT secret from Supabase Dashboard → Settings → API
2. Update `SUPABASE_JWT_SECRET` in `.env`
3. Restart backend

## Development Tips

### Hot Reload (with spring-boot-devtools)

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### Database Logs

Enable SQL logging in `application.properties`:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### Actuator Health Check

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

## Next Steps

1. ✅ Backend is running
2. ✅ Swagger UI accessible
3. ✅ APIs tested
4. 🔜 Connect frontend
5. 🔜 Add more features
