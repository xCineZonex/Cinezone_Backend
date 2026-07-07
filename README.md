<div align="center">

# 🎬 CineZone Backend

**API REST robusta para la gestión integral de un sistema de cine**

[![Build Status](https://github.com/xCineZonex/Cinezone_Backend/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/xCineZonex/Cinezone_Backend/actions)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Cloud Run](https://img.shields.io/badge/Deploy-Google%20Cloud%20Run-4285F4?logo=googlecloud)](https://cloud.google.com/run)

</div>

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Arquitectura](#-arquitectura)
- [Stack Tecnológico](#-stack-tecnológico)
- [Módulos del Sistema](#-módulos-del-sistema)
- [Seguridad](#-seguridad)
- [API Endpoints](#-api-endpoints)
- [Variables de Entorno](#-variables-de-entorno)
- [Instalación y Ejecución Local](#-instalación-y-ejecución-local)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Monitorización](#-monitorización)
- [Base de Datos](#-base-de-datos)
- [Estructura del Proyecto](#-estructura-del-proyecto)

---

## 📖 Descripción

CineZone Backend es una API REST completa desarrollada con Spring Boot 3 que gestiona todas las operaciones de un sistema de cine moderno. Incluye gestión de películas, reservas de asientos, venta de entradas, dulcería, sistema de lealtad, pagos con Mercado Pago y un módulo completo de administración multi-sede.

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTES                              │
│          Frontend (Vercel)  │  Apps Móviles  │  Staff        │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Google Cloud Run (us-east1)                     │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              CineZone Backend (Spring Boot)          │    │
│  │                                                      │    │
│  │  SecurityFilter → RateLimiting → Controllers        │    │
│  │       │                              │               │    │
│  │  JwtAuthFilter              Services + Repos        │    │
│  └──────────────────────────────────────────────────────┘   │
└────────┬──────────────┬──────────────┬───────────────────────┘
         │              │              │
         ▼              ▼              ▼
   PostgreSQL        Redis         Cloudinary
   (Base de datos)  (Cache/Stock)  (Imágenes)
         │
         ▼
   Grafana Cloud ← OTLP Push (métricas en tiempo real)
```

---

## 🛠️ Stack Tecnológico

| Categoría | Tecnología | Versión |
|---|---|---|
| **Lenguaje** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.3.3 |
| **Seguridad** | Spring Security + JWT | — |
| **Base de Datos** | PostgreSQL | — |
| **ORM** | Spring Data JPA + Hibernate | — |
| **Migraciones** | Flyway | — |
| **Cache / Stock** | Redis | — |
| **Rate Limiting** | Bucket4j | 8.1.0 |
| **Mapeo** | MapStruct + ModelMapper | 1.5.5 |
| **Documentación API** | SpringDoc OpenAPI (Swagger) | 2.0.3 |
| **Pagos** | Mercado Pago SDK | 3.3.0 |
| **Imágenes** | Cloudinary | 1.36.0 |
| **Emails** | Spring Mail (SMTP Gmail) | — |
| **QR** | ZXing (Google) | 3.5.3 |
| **Build** | Maven | 3.9.6 |
| **Deploy** | Google Cloud Run | — |
| **Imágenes Docker** | Artifact Registry (us-east1) | — |
| **Monitorización** | Grafana Cloud + OTLP | — |
| **CI/CD** | GitHub Actions | — |

---

## 📦 Módulos del Sistema

### 🎭 Catálogo de Películas
Gestión completa del ciclo de vida de películas: creación, actualización, cartelera activa, próximos estrenos y archivo. Incluye scheduler automático que actualiza el estado según fechas configuradas.

### 🎟️ Reservas y Tickets
Sistema de reserva de asientos en tiempo real usando Redis para control de stock concurrente. Generación de QR único por ticket para validación en puerta.

### 🏢 Multi-Sede
Soporte para múltiples sedes con configuraciones independientes de precios, salas y horarios.

### 🛒 Dulcería / Cocina
Módulo de venta de productos de cafetería con control de inventario, gestión de stock y sistema de reemplazos.

### 💳 Pagos
Integración con **Mercado Pago** para procesamiento de pagos en línea. Webhook para confirmación asíncrona de pagos.

### 🏆 Lealtad
Sistema de puntos acumulables por compras. Tiers de membresía con beneficios diferenciados. Historial de puntos consultable.

### 👥 Gestión de Usuarios y Staff
Roles diferenciados: `SUPER_ADMIN`, `ADMIN_SEDE`, `JEFE_SALA`, `TAQUILLA`, `DULCERIA`, `PORTERO`, `CLIENTE`. Control de acceso por rol y sede.

### 📊 Dashboard y Analytics
Panel administrativo con métricas de ventas, ocupación de salas, productos más vendidos y análisis de rendimiento por sede.

### 🔔 Alertas del Sistema
Sistema de alertas internas para notificar eventos críticos a administradores.

### 📧 Emails
Envío de correos automáticos para confirmación de reservas, recuperación de contraseña y notificaciones del sistema.

### 🔍 Auditoría
Registro completo de acciones sensibles del sistema para trazabilidad.

---

## 🔐 Seguridad

### Autenticación
- **JWT (JSON Web Tokens)** para autenticación stateless
- Tokens firmados con algoritmo HMAC-SHA256
- Expiración configurable via `JWT_SECRET`

### Autorización
- Control de acceso basado en roles (RBAC) con `@EnableMethodSecurity`
- Endpoints protegidos por rol en `SecurityConfig`

### Rate Limiting
- Protección contra abuso de API con **Bucket4j**
- Límite de intentos fallidos de login con bloqueo temporal

### Headers de Seguridad
```
/actuator/health     → Público (Cloud Run health check)
/actuator/*          → Bloqueado (no expuesto)
/api/v1/auth/**      → Público (login, registro)
/api/v1/public/**    → Público (cartelera, catálogo)
/api/v1/admin/**     → SUPER_ADMIN, ADMIN_SEDE, JEFE_SALA
/api/v1/taquilla/**  → TAQUILLA, STAFF, admins
Todo lo demás        → JWT requerido
```

---

## 🌐 API Endpoints

La documentación interactiva completa está disponible en:

```
https://TU_CLOUD_RUN_URL/swagger-ui.html
https://TU_CLOUD_RUN_URL/v3/api-docs
```

### Resumen de Controllers

| Controller | Prefijo | Descripción |
|---|---|---|
| `AuthController` | `/api/v1/auth` | Login, registro, refresh token |
| `PublicMovieController` | `/api/v1/peliculas` | Cartelera pública |
| `PublicController` | `/api/v1/public` | Sedes, funciones públicas |
| `BookingController` | `/api/v1/booking` | Reservas de asientos |
| `ReservationController` | `/api/v1/reservations` | Gestión de reservaciones |
| `TaquillaController` | `/api/v1/taquilla` | Venta en taquilla |
| `DulceriaController` | `/api/v1/dulceria` | Venta de productos |
| `KitchenController` | `/api/v1/cocina` | Gestión de cocina |
| `GatekeeperController` | `/api/v1/portero` | Validación de tickets QR |
| `LoyaltyController` | `/api/v1/loyalty` | Puntos y membresías |
| `UserController` | `/api/v1/users` | Perfil de usuario |
| `AdminCatalogController` | `/api/v1/admin/catalog` | Gestión de películas |
| `AdminUserController` | `/api/v1/admin/users` | Gestión de usuarios |
| `AdminComplaintController` | `/api/v1/admin/complaints` | Gestión de quejas |
| `DashboardController` | `/api/v1/dashboard` | Métricas y analytics |
| `InventoryController` | `/api/v1/inventory` | Control de inventario |
| `JefeSalaController` | `/api/v1/operaciones` | Operaciones de sala |
| `MercadoPagoController` | `/api/v1/payments` | Procesamiento de pagos |
| `ComplaintController` | `/api/v1/complaints` | Quejas de usuarios |
| `AuditController` | `/api/v1/audit` | Logs de auditoría |
| `SystemAlertController` | `/api/v1/alerts` | Alertas del sistema |
| `UploadController` | `/api/v1/uploads` | Subida de archivos |

---

## ⚙️ Variables de Entorno

Todas las variables se configuran como **GitHub Secrets** y se inyectan automáticamente al contenedor de Cloud Run via el pipeline de CI/CD.

### Base de Datos
| Variable | Descripción |
|---|---|
| `DB_URL` | URL de conexión PostgreSQL (ej: `jdbc:postgresql://host:5432/db`) |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |

### Redis
| Variable | Descripción |
|---|---|
| `REDIS_HOST` | Host del servidor Redis |
| `REDIS_PORT` | Puerto de Redis (default: 6379) |
| `REDIS_PASSWORD` | Contraseña de Redis |

### Seguridad
| Variable | Descripción |
|---|---|
| `JWT_SECRET` | Clave secreta para firmar tokens JWT (mínimo 256 bits) |
| `ACTUATOR_TOKEN` | Token Bearer para proteger endpoints de Actuator |

### Servicios Externos
| Variable | Descripción |
|---|---|
| `CLOUDINARY_URL` | URL completa de Cloudinary (`cloudinary://key:secret@cloud`) |
| `MAIL_USERNAME` | Correo Gmail para envío de emails |
| `MAIL_PASSWORD` | App Password de Gmail (no la contraseña normal) |
| `MP_ACCESS_TOKEN` | Token de acceso de Mercado Pago |
| `MP_PUBLIC_KEY` | Clave pública de Mercado Pago |
| `FRONTEND_URL` | URL del frontend (para configuración de CORS) |

### Google Cloud
| Variable | Descripción |
|---|---|
| `GCP_PROJECT_ID` | ID del proyecto en Google Cloud |
| `GCP_SERVICE_ACCOUNT` | Email del Service Account (`github-actions@...`) |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Resource name del WIF Provider |

### Monitorización
| Variable | Descripción |
|---|---|
| `GRAFANA_USERNAME` | Instance ID de Grafana Cloud (número de 6 cifras) |
| `GRAFANA_API_TOKEN` | API Token de Grafana Cloud (rol: MetricsPublisher) |
| `GRAFANA_OTLP_AUTH` | Se genera automáticamente en el pipeline (Base64 de user:token) |

---

## 🚀 Instalación y Ejecución Local

### Prerrequisitos
- Java 21+
- Maven 3.9+
- PostgreSQL 14+
- Redis 7+

### 1. Clonar el repositorio
```bash
git clone https://github.com/xCineZonex/Cinezone_Backend.git
cd Cinezone_Backend
```

### 2. Configurar variables de entorno
Crea un archivo `.env` o configura las variables en tu sistema:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/cinezone
export DB_USERNAME=postgres
export DB_PASSWORD=tu_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export JWT_SECRET=tu_secreto_muy_largo_de_256_bits_minimo
export FRONTEND_URL=http://localhost:3000
export CLOUDINARY_URL=cloudinary://key:secret@cloud_name
export MAIL_USERNAME=tu@gmail.com
export MAIL_PASSWORD=app_password_de_gmail
export MP_ACCESS_TOKEN=TEST-xxx
export MP_PUBLIC_KEY=TEST-xxx
```

### 3. Compilar y ejecutar
```bash
# Solo compilar
mvn clean compile

# Compilar y ejecutar tests
mvn clean verify

# Ejecutar la aplicación
mvn spring-boot:run

# O compilar el JAR y ejecutarlo
mvn clean package -DskipTests
java -jar target/demo-0.0.1.jar
```

### 4. Verificar que funciona
```bash
curl http://localhost:8080/actuator/health
# Respuesta esperada: {"status":"UP"}
```

### 5. Acceder a la documentación API
```
http://localhost:8080/swagger-ui.html
```

---

## 🔄 CI/CD Pipeline

El pipeline de GitHub Actions en [`.github/workflows/backend-ci.yml`](.github/workflows/backend-ci.yml) se ejecuta automáticamente en cada push o Pull Request.

### Flujo del Pipeline

```
Push a main/master  ─────────────────────────────────────────►
  o Pull Request

  JOB 1: Build & Test (siempre)
  ├── Checkout código
  ├── Configurar JDK 21 (Temurin)
  └── mvn -B verify (compilar + tests)
        │
        │ (solo si es push a main/master)
        ▼
  JOB 2: Deploy (solo en merge a main)
  ├── Auth Google Cloud (Workload Identity Federation)
  ├── Configurar gcloud SDK
  ├── Autenticar Docker con Artifact Registry
  ├── Crear repo en Artifact Registry (si no existe)
  ├── docker build → imagen multi-stage (Maven + JRE Alpine)
  ├── docker push → us-east1-docker.pkg.dev
  ├── Calcular credencial OTLP (Base64)
  └── Deploy → Cloud Run cinezone-backend (us-east1)
        └── Inyectar todos los secrets como env vars
```

### Autenticación con Google Cloud
Se usa **Workload Identity Federation (OIDC)** — sin credenciales JSON almacenadas en GitHub. El token de GitHub Actions se valida directamente contra Google Cloud IAM.

### Secrets requeridos en GitHub
Ver sección [Variables de Entorno](#️-variables-de-entorno) — todos los valores deben estar en **Settings → Secrets → Actions**.

---

## 📊 Monitorización

### Grafana Cloud (OTLP Push)
El backend envía métricas automáticamente a **Grafana Cloud** cada 15 segundos usando el protocolo OTLP. No requiere agente externo.

**Métricas disponibles:**
- 🖥️ **JVM**: Heap memory, GC, threads, CPU
- 🌐 **HTTP**: Requests/segundo, latencia (p50, p95, p99), errores 4xx/5xx
- 🗄️ **HikariCP**: Conexiones PostgreSQL activas, pool size
- 🔴 **Redis**: Operaciones, memoria usada
- ⚙️ **Process**: Uptime, file descriptors

**Dashboards recomendados (importar en Grafana por ID):**

| Dashboard | ID | Qué muestra |
|---|---|---|
| JVM Micrometer | `4701` | Heap, GC, threads |
| Spring Boot Statistics | `12685` | HTTP, latencia, errores |
| HikariCP | `6083` | Pool de conexiones DB |
| Redis | `11835` | Cache performance |

### Health Check
```
GET /actuator/health → {"status": "UP"}
```
Usado por Cloud Run para verificar que el servicio está activo.

---

## 🗄️ Base de Datos

### Migraciones con Flyway
Las migraciones de base de datos se gestionan automáticamente con **Flyway** al arrancar la aplicación.

- Archivos de migración: `src/main/resources/db/migration/`
- Naming convention: `V{versión}__{descripción}.sql`
- Se ejecutan en orden automáticamente

### Diagrama de entidades principales
```
Usuario ──────┬── Ticket ──── Función ──── Película
              │       │
              │    Asiento ──── Sala ──── Sede
              │
              ├── PuntoHistorial ──── TierLealtad
              │
              └── Queja
              
Producto ──── ProductoStock ──── Sede
         └─── SolicitudReemplazo
```

---

## 📁 Estructura del Proyecto

```
cinezone/
├── .github/
│   └── workflows/
│       └── backend-ci.yml          # Pipeline CI/CD
├── src/
│   └── main/
│       ├── java/com/cinezone/demo/
│       │   ├── api/                # Controllers REST (22 controllers)
│       │   ├── config/             # Configuración Spring (Security, Redis, CORS)
│       │   ├── dto/                # Data Transfer Objects
│       │   ├── model/              # Entidades JPA
│       │   ├── repository/         # Repositorios Spring Data
│       │   ├── scheduler/          # Tareas programadas
│       │   ├── security/           # JWT Filter, Rate Limiting
│       │   ├── service/            # Interfaces de servicios
│       │   │   └── impl/           # Implementaciones
│       │   ├── enums/              # Enumeraciones del dominio
│       │   └── util/               # Utilidades (QR, constantes)
│       └── resources/
│           ├── application.properties  # Configuración principal
│           ├── db/migration/           # Scripts SQL de Flyway
│           └── templates/              # Templates de email
├── Dockerfile                      # Imagen multi-stage (Maven + JRE Alpine)
├── pom.xml                         # Dependencias Maven
└── README.md
```

---

## 🐳 Docker

El `Dockerfile` usa una construcción multi-stage para optimizar el tamaño de la imagen:

```dockerfile
# Etapa 1: Compilación con Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
# Compila el JAR

# Etapa 2: Imagen de producción (solo JRE, sin Maven)
FROM eclipse-temurin:21-jre-alpine
# ~200MB vs ~500MB de una imagen con JDK completo
```

**Imagen desplegada en:** `us-east1-docker.pkg.dev/woven-nova-423120-k2/cinezone-repo/cinezone-backend`

---

## 🤝 Contribución

1. Fork del repositorio
2. Crear rama: `git checkout -b feature/nueva-funcionalidad`
3. Commit: `git commit -m "feat: descripción del cambio"`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Abrir Pull Request hacia `main`

El pipeline validará automáticamente que el código compila y los tests pasan antes de permitir el merge.

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Ver [LICENSE](LICENSE) para más detalles.

---

<div align="center">

Desarrollado con ❤️ por el equipo **CineZone**

</div>
