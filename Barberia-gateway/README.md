# servicio-gateway

API Gateway (Spring Cloud Gateway, WebFlux) de la barbería. Es el **punto único de
entrada** del frontend: enruta hacia los microservicios, valida el JWT de forma
centralizada y resuelve CORS.

Puerto: **8080**

## Versiones

| Componente | Versión | Nota |
|---|---|---|
| Spring Boot | 4.0.5 | heredado de `build.gradle` raíz |
| Spring Cloud | 2025.1.2 | línea alineada con Boot 4.0.x |
| Spring Cloud Gateway | 5.0.2 | la trae el BOM anterior |
| jjwt | 0.11.5 | misma versión que `servicio-auth` |

> Las versiones 4.x de Spring Cloud Gateway **no** sirven aquí: están compiladas
> contra Spring Framework 6 y el contexto falla al arrancar
> (`ClassNotFoundException: ...web.servlet.WebMvcAutoConfiguration`).
>
> Ojo también con el namespace de configuración: desde Gateway 4.2 las rutas van
> en `spring.cloud.gateway.server.webflux.routes`. Con el prefijo antiguo
> (`spring.cloud.gateway.routes`) Boot ignora el bloque en silencio y el gateway
> arranca **sin ninguna ruta**.

## Rutas

| Path | Destino | Variable de entorno |
|---|---|---|
| `/api/servicios/**` | servicio-catalogo:8081 | `SERVICIO_CATALOGO_URI` |
| `/api/barberos/**` | servicio-barberos:8082 | `SERVICIO_BARBEROS_URI` |
| `/api/citas/**` | servicio-citas:8083 | `SERVICIO_CITAS_URI` |
| `/auth/**` | servicio-auth:8084 | `SERVICIO_AUTH_URI` |

Por defecto apuntan a `localhost` (desarrollo). En `docker-compose.yml` se
sobreescriben con los nombres de servicio de la red de compose.

## Seguridad

`JwtAuthenticationFilter` es un `GlobalFilter` (orden `-100`) que corre antes del
enrutamiento, así que una petición no autorizada nunca llega al microservicio.

- Valida firma HS256 y expiración con el secreto de `gateway.security.secret`.
  **Debe ser idéntico al de `JwtService` en servicio-auth.**
- Responde `401` con cuerpo JSON si el token falta, está expirado o es inválido.
- Inyecta `X-Auth-User` y `X-Auth-Role` hacia el microservicio, y **borra** esas
  cabeceras si vienen del cliente (evita suplantación).

### Rutas públicas

Configurables en `gateway.security.public-endpoints`:

| Método | Patrón | Motivo |
|---|---|---|
| `OPTIONS` | `/**` | preflight CORS |
| cualquiera | `/auth/**` | puerta de entrada de la autenticación |
| `GET` | `/api/**` | el cliente anónimo ve servicios, barberos y citas |
| `POST` | `/api/citas` | el formulario de reserva de la landing es anónimo |

Todo lo demás (POST/PUT/DELETE sobre servicios y barberos, y PUT/DELETE sobre
citas) exige `Authorization: Bearer <token>`.

> `POST /api/citas` es la excepción deliberada a "toda escritura requiere token":
> sin ella, un cliente no autenticado no podría reservar desde la landing.

## Ejecutar

```bash
# local
./gradlew :app-service:bootJar
java -jar applications/app-service/build/libs/servicio-gateway.jar

# docker (desde la raíz del repo, tras compilar el jar)
docker compose up --build servicio-gateway
```

## Comprobación rápida

```bash
# público
curl -i http://localhost:8080/api/servicios

# protegido sin token -> 401
curl -i -X POST http://localhost:8080/api/servicios -d '{}'

# login y escritura con token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

curl -i -X POST http://localhost:8080/api/servicios \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Corte","precio":25000,"duracionMinutos":30}'
```
