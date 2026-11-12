# Banco Gocomezbo

<p align="center">
  Banca en línea de práctica con Spring Boot y React. El banco es ficticio y no se maneja dinero real.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-b07219?style=for-the-badge&logo=openjdk" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?style=for-the-badge&logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Security-JWT-111827?style=for-the-badge&logo=springsecurity" alt="Spring Security con JWT" />
  <img src="https://img.shields.io/badge/DB-H2-1d4ed8?style=for-the-badge" alt="H2" />
  <img src="https://img.shields.io/badge/Frontend-React%2018-0f172a?style=for-the-badge&logo=react" alt="React" />
  <img src="https://img.shields.io/badge/Lenguaje-TypeScript-2563eb?style=for-the-badge&logo=typescript" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Tests-JUnit%205-25a162?style=for-the-badge&logo=junit5" alt="JUnit 5" />
</p>

## Contenido

- [Qué tiene](#qué-tiene)
- [Stack](#stack)
- [Cómo correrlo](#cómo-correrlo)
- [Variables de entorno](#variables-de-entorno)
- [Estructura](#estructura)
- [Rutas](#rutas)
- [Arquitectura](#arquitectura)
- [Pendientes](#pendientes)
- [Datos, licencia y aviso](#-datos-licencia-y-aviso)
- [Autor](#-autor)

## Qué tiene

- Registro, login con JWT y bloqueo después de 5 intentos fallidos
- Cuentas de ahorro y corriente, depósitos simulados y movimientos
- Transferencias en dos pasos: primero se cotiza (muestra comisión y total) y después se confirma
- Retracto de cuentas nuevas dentro de 5 días hábiles
- Disputas: las comisiones se devuelven automáticamente, las transferencias las revisa el admin
- Menores de edad necesitan autorización del tutor
- Consentimientos de publicidad y analítica (apagados por defecto) y link de baja
- Exportar y borrar los datos personales
- Reseñas solo de clientes con movimientos
- Páginas de términos, privacidad, cookies, devoluciones, aviso de IA, tarifas, datos de la empresa, licencias e integraciones

## Stack

| Parte | Tecnologías |
|-------|-------------|
| Backend | Java 17+, Spring Boot 3.5, Spring Web, Spring Data JPA, Bean Validation |
| Seguridad | Spring Security, JWT (HS256), BCrypt |
| Base de datos | H2 en archivo (pruebas en memoria) |
| Frontend | React 18, TypeScript, React Router, Vite |
| Pruebas | JUnit 5, MockMvc, AssertJ |

## Cómo correrlo

Requisitos: Java 17+ y Node 18+. Maven es opcional, `run.cmd` lo descarga si no está instalado.

Backend (puerto 8080):

```
cd backend
run.cmd          (o run.cmd test para las pruebas)
```

Frontend (puerto 5173):

```
cd frontend
npm install
npm run dev
```

Usuario admin: `admin@gocomezbo.demo` / `Admin123!`

Los correos no se envían de verdad, quedan guardados y se pueden ver en `/dev/correos` (ahí salen los links de autorización del tutor y de baja).

La base de datos queda en `backend/data/`. Si se borra esa carpeta arranca de cero.

## Variables de entorno

Son opcionales en desarrollo, si no se definen se usan valores de prueba.

| Variable | Para qué |
|----------|----------|
| `APP_JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres) |
| `APP_ADMIN_PASSWORD` | Contraseña del usuario admin que se crea al arrancar |

## Estructura

```bash
online-banking/
├── backend/
│   ├── pom.xml
│   ├── run.cmd / run.ps1
│   └── src/
│       ├── main/java/com/onlinebanking/
│       │   ├── account/        cuentas y movimientos
│       │   ├── transfer/       cotización y confirmación
│       │   ├── dispute/        disputas y devoluciones
│       │   ├── auth/           registro, login y JWT
│       │   ├── customer/
│       │   ├── consent/        consentimientos
│       │   ├── privacy/        exportar y borrar datos
│       │   ├── review/         reseñas
│       │   ├── publicinfo/     textos legales, tarifas, promesas
│       │   ├── notification/   correos simulados
│       │   ├── fees/
│       │   ├── analytics/
│       │   ├── admin/
│       │   ├── common/         errores, dinero, días hábiles
│       │   └── config/         seguridad y usuario admin
│       └── test/
└── frontend/
    └── src/
        ├── pages/
        ├── components/
        ├── api.ts
        └── auth.tsx
```

## Rutas

Frontend:

| Ruta | Qué hay |
|------|---------|
| `/` | Inicio, promesas con evidencia y tarifas |
| `/registro`, `/ingresar` | Crear cuenta e iniciar sesión |
| `/cuentas`, `/cuentas/:id` | Mis cuentas, movimientos, depósito, retracto |
| `/transferir` | Transferencia en dos pasos |
| `/disputas` | Mis disputas |
| `/privacidad` | Permisos, descargar y borrar datos |
| `/admin` | Disputas pendientes y boletín (solo admin) |
| `/legal/:doc` | Términos, privacidad, cookies, devoluciones, IA |
| `/dev/correos` | Correos simulados |

API (todo bajo `/api`):

| Método | Ruta | Qué hace |
|--------|------|----------|
| POST | `/auth/register`, `/auth/login` | Registro y login |
| GET, POST | `/accounts`, `/accounts/{id}/deposits` | Cuentas y depósito simulado |
| GET | `/accounts/{id}/transactions` | Movimientos |
| POST | `/accounts/{id}/retract`, `/accounts/{id}/close` | Retracto y cierre |
| POST | `/transfers/quotes`, `/transfers/quotes/{id}/confirm` | Cotizar y confirmar |
| POST | `/transactions/{id}/disputes` | Abrir una disputa |
| GET, PUT, POST | `/me`, `/me/consents/{tipo}`, `/me/export`, `/me/delete` | Perfil y privacidad |
| GET | `/public/fees`, `/public/legal/{doc}`, `/public/promises`... | Información pública |
| GET, POST | `/admin/disputes`, `/admin/newsletter` | Solo admin |

## Arquitectura

### Componentes

```mermaid
flowchart LR
    subgraph FE["Frontend: React + TypeScript (Vite)"]
        UI["Páginas<br/>cuentas, transferir, disputas,<br/>privacidad, legales"]
        AUTH["AuthProvider<br/>sesión en sessionStorage"]
        CK["Aviso de cookies<br/>analítica opcional"]
        API["api.ts<br/>fetch + token JWT"]
        UI --> AUTH
        UI --> API
        CK --> API
    end

    subgraph BE["Backend: Spring Boot 3.5"]
        SEC["Spring Security<br/>valida el JWT"]
        CTRL["Controllers REST<br/>/api/..."]
        SVC["Services<br/>@Transactional"]
        REPO["Repositories<br/>Spring Data JPA"]
        ERR["ApiExceptionHandler<br/>ProblemDetail"]
        FEES["FeeSchedule<br/>tarifas"]
        MAIL["Mailer<br/>correos simulados"]
        MEM["En memoria<br/>intentos de login, analítica"]
        SEC --> CTRL --> SVC --> REPO
        CTRL -. errores .-> ERR
        SVC --> FEES
        SVC --> MAIL
        SVC --> MEM
    end

    DB[("H2")]

    API -- "HTTP + JSON (proxy de Vite)" --> SEC
    REPO --> DB
    MAIL --> DB
```

### Modelo de datos

```mermaid
erDiagram
    CUSTOMER ||--o{ ACCOUNT : tiene
    CUSTOMER ||--o{ CONSENT_RECORD : registra
    CUSTOMER ||--o| REVIEW : escribe
    CUSTOMER ||--o{ DISPUTE : abre
    CUSTOMER ||--o{ TRANSFER_QUOTE : cotiza
    ACCOUNT ||--o{ BANK_TRANSACTION : registra
    ACCOUNT ||--o{ TRANSFER_QUOTE : "origen o destino"
    BANK_TRANSACTION ||--o{ DISPUTE : "se disputa"

    CUSTOMER {
        long id PK
        string email
        string status
        string role
    }
    ACCOUNT {
        long id PK
        string account_number
        decimal balance
        string status
        long version
    }
    BANK_TRANSACTION {
        long id PK
        string type
        decimal amount
        decimal balance_after
        string reference
    }
    TRANSFER_QUOTE {
        string id PK "UUID"
        decimal amount
        decimal fee
        decimal total
        string status
        timestamp expires_at
    }
    DISPUTE {
        long id PK
        string status
        date due_date
    }
```

### Cómo funciona una transferencia

Primero se pide una cotización y después se confirma. Si alguien le da dos veces a confirmar no pasa nada, y si se demora más de 2 minutos toca cotizar otra vez.

```mermaid
sequenceDiagram
    participant U as Usuario
    participant F as Frontend
    participant S as TransferService
    participant DB as BD

    U->>F: pone la cuenta destino y el monto
    F->>S: POST /api/transfers/quotes
    S->>DB: revisa que las cuentas existan y que alcance la plata
    S->>DB: guarda la cotización
    S-->>F: a quién le llega, la comisión y el total
    F-->>U: muestra el resumen
    U->>F: le da confirmar
    F->>S: POST /api/transfers/quotes/ID/confirm
    S->>DB: lock de la cotización
    alt ya estaba confirmada
        S-->>F: devuelve lo mismo de antes
    else se venció
        S-->>F: 410, toca cotizar de nuevo
    else todo bien
        S->>DB: lock de las dos cuentas (primero la de id menor)
        S->>DB: débito, comisión y crédito
        S-->>F: referencia y saldo nuevo
    end
```

## Pendientes

- Los días hábiles no tienen en cuenta festivos
- El bloqueo de login está en memoria
- Los textos legales son de ejemplo

## 📜 Datos, licencia y aviso

- **Datos:** no usa datos reales. Los clientes, cuentas y movimientos se crean al usar la app, y Banco Gocomezbo es un banco ficticio.
- **Código:** © 2026 [EstebanC111s](https://github.com/EstebanC111s).
- **Uso:** proyecto académico y de práctica. No es una entidad financiera, no está vigilado por la Superintendencia Financiera y no debe usarse para manejar dinero real. Los textos legales son de ejemplo y no son asesoría legal.

## 👥 Autor

- [EstebanC111s](https://github.com/EstebanC111s)
