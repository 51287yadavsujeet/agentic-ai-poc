@echo off
REM Script to run the application with Netty warnings suppressed

set JAVA_OPTS=^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-opens=java.base/java.nio=ALL-UNNAMED ^
    -XX:+IgnoreUnrecognizedVMOptions ^
    -Dio.netty.tryReflectionSetAccessible=false ^
    -Dio.netty.noUnsafe=true

set OPENAI_API_KEY=ADD YOUR KEY

echo Starting Agentic AI POC...
echo.

mvnw spring-boot:run

pause
