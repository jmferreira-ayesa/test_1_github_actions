# --- Etapa 1: build ---
# Usamos una imagen con Maven + JDK solo para compilar; no viaja a producción.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml y descargamos dependencias: así Docker
# cachea esta capa y, si solo cambias código Java, no vuelve a descargar
# medio internet en cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Etapa 2: runtime ---
# Imagen final solo con el JRE (no el JDK completo) sobre Alpine: imagen
# final de ~150-180MB en vez de los ~450MB+ de una imagen con Maven+JDK.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Buena práctica de seguridad: no correr el proceso como root dentro del
# contenedor. ECS/Fargate respeta el USER definido en la imagen.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Flags de JVM pensadas para contenedores:
# - UseContainerSupport (activado por defecto en JDK 21, explícito por claridad)
# - MaxRAMPercentage en vez de -Xmx fijo: la JVM calcula el heap como un
#   porcentaje de la memoria asignada al *contenedor* (la del task definition
#   de ECS), no de la memoria física del host. Si subes la memoria del task
#   de 1024 a 2048 MB, el heap escala solo, sin tocar este Dockerfile.
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
