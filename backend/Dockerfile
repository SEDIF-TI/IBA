# =============================================================================
# SEA Backend - Build multietapa
#   Etapa 1: compila con Maven (JDK completo)
#   Etapa 2: ejecuta sobre JRE alpine con usuario NO-ROOT
# =============================================================================

# ---------- Etapa 1: build ----------
# Imagen fijada por digest (huella del contenido): la etiqueta puede moverse, el digest no.
# Para actualizar: docker pull <imagen:etiqueta> y copiar el nuevo digest (ver README).
FROM maven:3.9-eclipse-temurin-21@sha256:d7e7f57407437c014571f1ad5a9955f03fc3edcb1d964067ef351fa38e798665 AS build
WORKDIR /build

# Se copia primero el pom solo: si no cambian las dependencias, Docker reutiliza
# la capa de descarga y el build posterior es mucho mas rapido.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c
WORKDIR /app

# wget para el healthcheck del compose; tzdata para America/Mexico_City.
RUN apk add --no-cache wget tzdata
ENV TZ=America/Mexico_City

# Usuario sin privilegios: si alguien escapa del proceso, no cae como root.
RUN addgroup -S sea && adduser -S sea -G sea

COPY --from=build /build/target/*.jar app.jar

# Volumen de documentos personales. Pertenece al usuario de la aplicacion y
# queda fuera de cualquier document root: ningun servidor web lo alcanza.
RUN mkdir -p /var/sea/storage && chown -R sea:sea /var/sea /app

USER sea

EXPOSE 8080

# Contenedores: que la JVM respete los limites de memoria del cgroup.
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
