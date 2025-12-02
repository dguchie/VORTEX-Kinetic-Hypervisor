# --- STAGE 1: BUILD (The Factory) ---
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.3.0 as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the build definition
COPY build.sbt .
COPY project project

# Copy the Source Code
COPY src src

# COMPILE (This turns logic into binary)
RUN sbt compile

# --- STAGE 2: RUNTIME (The Vault) ---
# We use a lightweight Java Runtime. NO COMPILER. NO SOURCE CODE.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy ONLY the compiled artifacts from the builder
COPY --from=builder /app/target /app/target
COPY --from=builder /app/build.sbt /app/build.sbt
COPY --from=builder /app/project /app/project

# Install sbt runner (lightweight) just to launch
RUN apt-get update && apt-get install -y curl &&     curl -L -o sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.9.7.deb &&     dpkg -i sbt.deb &&     rm sbt.deb

# Expose the API Port
EXPOSE 8080

# The Entry Point
CMD ["sbt", "run"]
