# =============================================================================
# SEA Frontend - Build multietapa
#   Etapa 1: compila con Vite
#   Etapa 2: sirve con Nginx, sin privilegios de root
# =============================================================================

# ---------- Etapa 1: build ----------
FROM node:22-alpine AS build
WORKDIR /build

# Se copian primero los manifiestos: si no cambian las dependencias, Docker
# reutiliza la capa de instalacion y el build posterior es mucho mas rapido.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite inyecta las variables VITE_* al compilar, no en ejecucion. En produccion
# vale /api: una ruta relativa que funciona sin importar el dominio.
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ---------- Etapa 2: servicio ----------
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist .

# Usuario sin privilegios. El puerto 8080 en lugar del 80 porque por debajo de
# 1024 hace falta ser root para escuchar.
#
# Nginx escribe su archivo de proceso y sus temporales al arrancar. Como no
# corre como root, hay que apuntarlos a rutas que si pueda escribir y dejarselas
# a su nombre; si no, ni siquiera llega a levantar.
RUN addgroup -g 1001 -S sea && adduser -u 1001 -S sea -G sea \
    && mkdir -p /tmp/nginx \
    && sed -i 's|^pid .*|pid /tmp/nginx/nginx.pid;|' /etc/nginx/nginx.conf \
    && sed -i '/^user /d' /etc/nginx/nginx.conf \
    && chown -R sea:sea /var/cache/nginx /tmp/nginx /usr/share/nginx/html /etc/nginx

USER sea

EXPOSE 8080

# Se usa 127.0.0.1 y no localhost: dentro del contenedor localhost resuelve
# primero a IPv6, y nginx escucha solo en IPv4, asi que la sonda fallaria
# aunque el servicio este perfectamente arriba.
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
    CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]
