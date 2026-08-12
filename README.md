# 🛰️ Community Signal Platform

> AI-assisted content curation platform with human-in-the-loop review, built for editorial teams.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red?logo=angular)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://www.docker.com/)

---

## 🌐 Live Demo

| Service | URL |
|---------|-----|
| **Frontend** | https://community-signal.vercel.app |
| **API Health** | https://community-signal-api.onrender.com/actuator/health |
| **Database** | Neon (serverless PostgreSQL) |

### 🔑 Demo Credentials

```
Username: demo
Password: Demo2026!
```

⚠️ Demonstration environment. Data may be reset periodically.

---

## 📖 Overview

**Community Signal** is a distributed content curation pipeline that ingests signals from multiple sources, enriches them through semantic clustering and LLM-generated drafts, and surfaces them to human reviewers for approval before publication.

Built as a **portfolio-grade production system** demonstrating:

- **Human-in-the-loop (HITL)** review workflow
- **JWT-authenticated** REST API with role-based access
- **Modern SPA frontend** with standalone components
- **Production deployment** on Render + Vercel + Neon

---

## 🏗️ Architecture

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Vercel     │      │    Render    │      │    Neon      │
│   (Angular)  │─────▶│ (Spring Boot)│─────▶│ (PostgreSQL) │
└──────────────┘      └──────────────┘      └──────────────┘
```

The full event-driven pipeline (11 microservices with Kafka, embeddings, LLM) runs in the private development repository. This public repo contains the **production-scoped** Review API + Frontend.

---

## 🛠️ Tech Stack

### Backend
| Layer | Technology |
|-------|------------|
| **Runtime** | Java 21 · Spring Boot 3.3 |
| **Security** | Spring Security 6 · JJWT 0.12.6 · BCrypt |
| **Persistence** | JPA/Hibernate · Flyway migrations |
| **Build** | Maven · Docker multi-stage |

### Frontend
| Layer | Technology |
|-------|------------|
| **Framework** | Angular 17 · Standalone Components |
| **Styling** | SCSS · Design tokens |
| **HTTP** | HttpClient · Functional Interceptors |
| **State** | RxJS · BehaviorSubject |

### Production Infrastructure
| Component | Provider |
|-----------|----------|
| **API Hosting** | Render (Dockerized Spring Boot) |
| **Database** | Neon (Serverless PostgreSQL) |
| **Frontend** | Vercel (Edge-optimized Angular) |

---

## 🚀 Quick Start (Local Development)

### Prerequisites

- Java 21 + Maven
- Node.js 20 + npm
- Docker

### 1. Clone

```bash
git clone https://github.com/MatheusNRusso/community-signal.git
cd community-signal
```

### 2. Start PostgreSQL

```bash
docker run -d \
  --name community-signal-db \
  -e POSTGRES_DB=community_signal \
  -e POSTGRES_USER=signal_user \
  -e POSTGRES_PASSWORD=$(openssl rand -base64 16) \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### 3. Initialize schema

```bash
docker cp infra/postgres/init.sql community-signal-db:/tmp/init.sql
docker cp infra/postgres/migrations.sql community-signal-db:/tmp/migrations.sql
docker exec -i community-signal-db psql -U signal_user -d community_signal -f /tmp/init.sql
docker exec -i community-signal-db psql -U signal_user -d community_signal -f /tmp/migrations.sql
```

### 4. Start the Review API

```bash
cd services/java/review-api
export DATABASE_URL=jdbc:postgresql://localhost:5432/community_signal
export DATABASE_USER=signal_user
export DATABASE_PASSWORD=$(docker exec community-signal-db printenv POSTGRES_PASSWORD)
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=$(openssl rand -base64 16)
export JWT_SECRET=$(openssl rand -base64 64)
mvn spring-boot:run
```

API: `http://localhost:8085`

### 5. Start the Frontend

```bash
cd frontend
npm install && npm start
```

Frontend: `http://localhost:4200`

Login with the `ADMIN_PASSWORD` printed in step 4.

---

## 📡 API Reference

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/login` | Get JWT token | Public |
| POST | `/api/auth/register` | Create new user | Admin only |

### Drafts (HITL Review)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/drafts?status={PENDING\|IN_REVIEW\|APPROVED\|REJECTED}` | List drafts |
| GET | `/api/drafts/{id}` | Get single draft |
| GET | `/api/drafts/stats` | Aggregate counts by status |
| POST | `/api/drafts/{id}/review` | Move to IN_REVIEW |
| POST | `/api/drafts/{id}/approve` | Approve and publish |
| POST | `/api/drafts/{id}/reject` | Reject with required note |

All draft endpoints require `Authorization: Bearer <token>` header.

### Observability

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Service health check |
| GET | `/actuator/info` | Build info |
| GET | `/actuator/metrics` | Micrometer metrics |

---

## 📁 Project Structure

```
community-signal/
├── services/java/review-api/        # Spring Boot HITL API
│   ├── src/main/java/               # Controllers, services, security
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/            # Flyway migrations
│   └── Dockerfile                   # Multi-stage production build
│
├── frontend/                        # Angular 17 SPA
│   └── src/app/
│       ├── core/                    # Services, guards, interceptors
│       ├── features/auth/login/
│       └── features/review/         # HITL dashboard
│
├── infra/postgres/                  # Database scripts
├── docs/                            # Architecture docs
├── scripts/                         # Smoke tests
└── README.md
```

---

## 🧪 Testing

```bash
cd services/java/review-api
mvn test
```

---

## 🔐 Environment Variables (Production)

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | Neon JDBC URL (injected by Render) |
| `DATABASE_USER` | DB user (injected) |
| `DATABASE_PASSWORD` | DB password (secret) |
| `JWT_SECRET` | Min 64 chars signing key (secret) |
| `JWT_EXPIRATION` | Token TTL in ms (default: 86400000) |
| `ADMIN_USERNAME` | Bootstrap admin (secret) |
| `ADMIN_PASSWORD` | Bootstrap admin password (secret) |

**No secrets are versioned.** All sensitive values are injected via provider dashboards.

---

## 🎯 Design Decisions

- **HITL by default** — Every generated draft requires human approval before publication.
- **JWT + BCrypt** — Stateless authentication compatible with any HTTP client.
- **Admin bootstrap via env vars** — Production pattern used by Grafana/Keycloak.
- **Standalone Angular components** — Tree-shakable, no NgModules.
- **Multi-stage Docker build** — Small production image (JRE only, non-root user).
- **Flyway migrations** — Version-controlled schema evolution.
- **Secrets externalized** — Zero hardcoded credentials in version control.

---

## 🗺️ Roadmap

- [x] Production deployment (Render + Vercel + Neon)
- [ ] GitHub OAuth integration
- [ ] WebSocket for real-time draft updates
- [ ] Reviewer analytics dashboard
- [ ] Audit log for all review actions
- [ ] Full Kafka pipeline deployment (Upstash)
- [ ] Grafana observability dashboards

---

## 👤 Author

**Matheus N. Russo**  
Senior Backend Engineer & Systems Architect

- GitHub: [@MatheusNRusso](https://github.com/MatheusNRusso)

---

## 📄 License

MIT License.
