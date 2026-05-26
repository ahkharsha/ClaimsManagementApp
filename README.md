# InsuranceIQ — AI-Powered Claims Management Platform

An enterprise insurance platform that automates policy issuance, claims processing, fraud detection, and real-time notifications — built with a microservices architecture and deployed on AWS.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React + Vite |
| Backend API | Spring Boot 3.2 (Java 17) |
| ML & Analytics | Python FastAPI |
| Notifications | Node.js + Socket.IO |
| Database | MySQL |
| Cloud | AWS EC2 + S3 |

## Project Structure

```
ClaimsManagementApp/
├── client/                  → React frontend (Vite)
├── server/                  → Spring Boot backend (Maven)
├── ml-service/              → Python FastAPI ML service
├── notification-service/    → Node.js Socket.IO service
└── README.md
```

## Architecture

```
 ┌────────────────┐       HTTP/REST        ┌──────────────────────┐
 │                │ ─────────────────────> │                      │
 │  React Client  │                        │  Spring Boot API     │
 │  (Port 5173)   │ <───────────────────── │    (Port 8080)       │
 │                │                        │                      │
 └──────┬─────────┘                        └──────┬─────────┬─────┘
        │                                         │         │
        │ WebSockets                              │ REST    │ MySQL
        │                                         ▼         ▼
 ┌──────▼─────────┐  Internal Events       ┌──────────────────────┐
 │                │ <───────────────────── │                      │
 │  Notification  │                        │  Python ML Service   │
 │  Service       │                        │  (Port 8000)         │
 │  (Port 5001)   │                        │                      │
 └────────────────┘                        └──────────────────────┘
```

## Services

### Spring Boot Backend (`server/`)
- JWT authentication with role-based access (Admin, Agent, Customer, Claims Manager)
- Full CRUD for Policies, Claims, Customers, Agents, Products, Payments
- Fraud prediction proxy (calls Python ML service)
- Analytics endpoints (dashboard summary, trends, loss ratio)
- Swagger docs at `/swagger-ui.html`

### React Frontend (`client/`)
- 10+ pages: Login, Admin/Agent/Customer dashboards, Policy & Claims management, Fraud reports, Analytics, Notifications
- Real-time notifications via Socket.IO
- Recharts for analytics visualization
- Role-based routing guards

### Python ML Service (`ml-service/`)
- Fraud detection (Rule-based + ML with scikit-learn)
- ETL pipeline for CSV data processing
- Analytics computation
- FastAPI with Pydantic validation

### Node.js Notification Service (`notification-service/`)
- Socket.IO with JWT authentication
- 6 event types: Claim Filed, Status Updated, Fraud Alert, Payment Received, Claim Settled, Policy Renewal Due
- MySQL-backed notification persistence
- Daily renewal reminder cron job

## Team

| Member | Role | Branch |
|--------|------|--------|
| Neel | Spring Boot Backend | `dev-spring-backend` |
| Ghouse | React Frontend | `dev-react-frontend` |
| Rithvik | Node.js Notifications | `dev-node-notifications-backend` |
| Shrivatsa | Python ML Service | `dev-python-ml-service` |
| Harsha | Cloud & DevOps | `dev-cloud-deployment` |

## Setup

### Prerequisites
- Java 17+, Maven
- Node.js 18+
- Python 3.10+
- MySQL 8.0+

### Quick Start

```bash
# 1. MySQL — create databases
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS insuranceiq_db; CREATE DATABASE IF NOT EXISTS insuranceiq_notifications;"

# 2. Spring Boot
cd server && mvn spring-boot:run

# 3. Python ML Service
cd ml-service && pip install -r requirements.txt && uvicorn main:app --port 8000

# 4. Node Notification Service
cd notification-service && npm install && npm start

# 5. React Frontend
cd client && npm install && npm run dev
```
