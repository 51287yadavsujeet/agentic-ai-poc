@echo off
REM Script to run the application with Netty warnings suppressed

set JAVA_OPTS=^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-opens=java.base/java.nio=ALL-UNNAMED ^
    -XX:+IgnoreUnrecognizedVMOptions ^
    -Dio.netty.tryReflectionSetAccessible=false ^
    -Dio.netty.noUnsafe=true

set OPENAI_API_KEY=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

echo Starting Agentic AI POC...
echo.

mvnw spring-boot:run

pause
