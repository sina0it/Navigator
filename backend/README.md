# Sina Navigator - Production Backend API

**Application Name:** Sina Navigator  
**Lead Architect & Developer:** Sina Naderi ([sinananderi203@gmail.com](mailto:sinananderi203@gmail.com))

---

## 🚀 Overview

The Sina Navigator Backend is a containerized, production-grade microservice supporting:
- User Authentication (JWT-based with SHA-256 / bcrypt password security)
- Role-Based Access Control (`SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `USER`)
- Route Calculation Caching & Audit Logging
- Multi-Stop Trip Planning Persistence
- Favorite Places and Custom Categorization
- Push & System Broadcast Notifications
- Health Check and Telemetry Monitoring
- Real-time AI Copilot integration with Gemini models

---

## 🛠️ Tech Stack

- **Runtime:** Node.js 20+ & TypeScript
- **Framework:** Express.js
- **Database:** PostgreSQL 16
- **ORM & Migrations:** Prisma ORM
- **Containerization:** Docker & Docker Compose

---

## 🏃 Running with Docker

```bash
cd backend
docker-compose up --build
```

The API will be available at `http://localhost:5000/api`.

---

## 📡 Key API Endpoints

- `GET /api/health` - Health check and system telemetry
- `POST /api/auth/login` - User authentication
- `POST /api/auth/register` - User registration
- `GET /api/favorites` - Fetch user saved places
- `POST /api/favorites` - Add favorite place
- `DELETE /api/favorites/:id` - Delete favorite
- `GET /api/trips` - Retrieve multi-stop saved trips
- `POST /api/trips` - Save multi-stop trip
- `GET /api/notifications` - Notifications list
- `POST /api/notifications/read-all` - Mark notifications as read
- `GET /api/admin/stats` - Admin KPI metrics
- `POST /api/admin/broadcast` - Broadcast system announcement to all users
