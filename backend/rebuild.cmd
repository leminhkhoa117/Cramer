@echo off
echo === Cleaning Lombok cache ===
rmdir /s /q "%USERPROFILE%\.m2\repository\org\projectlombok" 2>nul
echo === Building project ===
call .\mvnw.cmd clean package -DskipTests -U
