# ── Stage 1: Build ───────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
# Download deps first (cached layer)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# X11 + GTK libs needed for JavaFX GUI
RUN apt-get update && apt-get install -y --no-install-recommends \
        libgtk-3-0 \
        libgl1 \
        libglib2.0-0 \
        libx11-6 \
        libxext6 \
        libxrender1 \
        libxtst6 \
        libxi6 \
        libxrandr2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy app jar + dependencies from build stage
COPY --from=build /build/target/todo-app.jar ./todo-app.jar
COPY --from=build /build/target/libs ./libs

# SQLite db lives in a named volume so data persists across container restarts
VOLUME /data

# Launcher
ENTRYPOINT ["java", \
    "--module-path", "/app/libs", \
    "--add-modules", "javafx.controls,javafx.fxml", \
    "-cp", "/app/todo-app.jar:/app/libs/sqlite-jdbc-3.50.3.0.jar", \
    "-Dtodo.db=/data/todo.db", \
    "com.kamesh.todo.Main"]
