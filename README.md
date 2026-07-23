# Expense Tracker — Backend Microservices

An AI-powered expense tracking system built on Spring Boot microservices, Apache Kafka, and Mistral AI. It automatically reads incoming bank SMS, extracts the transaction details using an LLM, and records the expense in real time — no manual entry required.

---

## Architecture

```
                         React Native App (Frontend)
                                    |
                 ┌──────────────────┼───────────────────┐
                 │ REST + JWT       │                    │ Bank SMS detected
                 ▼                  ▼                    ▼
        +---------------+  +---------------+   +----------------------+
        |  AuthService  |  |  UserService  |   |  DSService (AI/ML)   |
        |  Port: 9898   |  |  Port: 9810   |   |  Port: 8000          |
        |  JWT + BCrypt |  |  Profile API  |   |  Flask + Mistral AI  |
        +-------+-------+  +-------+-------+   +----------+-----------+
                |                  |                       |
                |     Kafka        |                       | Kafka
                └──────Topic───────┘                       | (expense_service)
                 (user_service)                            ▼
                                                   +-------------------+
                                                   |   ExpenseService  |
                                                   |    Port: 9820     |
                                                   +---------+---------+
                                                             |
                 ┌───────────────────┬───────────────────────┘
                 ▼                   ▼
        +----------------+   +--------------------------+
        |     MySQL      |   |    Apache Kafka + ZK      |
        | (1 DB/service) |   |   (event-driven backbone) |
        +----------------+   +--------------------------+

                    All services containerized & orchestrated via Docker Compose
```

---

## Tech Stack

| Layer            | Technology                                              |
|-------------------|----------------------------------------------------------|
| Backend            | Java 21, Spring Boot 3, Spring Security                 |
| AI / ML             | Python, Flask, Mistral AI, LangChain                     |
| Messaging           | Apache Kafka + Zookeeper                                 |
| Database            | MySQL 8.0 (separate schema per service)                  |
| Auth                | JWT (Access + Refresh Token), BCrypt                     |
| Frontend            | React Native (Expo)                                      |
| SMS Detection       | @maniac-tech/react-native-expo-read-sms                  |
| Containerization    | Docker, Docker Compose                                   |

---

## How to Run

All services, Kafka, Zookeeper and MySQL are wired together in `docker-compose.yml`.

```bash
git clone https://github.com/KishanSingh29/Expence.git
cd Expence

# add your Mistral API key
echo "OPENAI_API_KEY=your_mistral_api_key" >> dsservice/.env

docker-compose up -d
```

| Service        | URL                          |
|-----------------|-------------------------------|
| AuthService      | http://localhost:9898         |
| UserService      | http://localhost:9810         |
| ExpenseService   | http://localhost:9820         |
| DSService (AI)   | http://localhost:8000         |
| Kafka            | localhost:29092                |
| MySQL            | localhost:3306                 |

Data persists across restarts via a named `mysql_data` Docker volume — `docker-compose down` does not wipe the database.

---

## API Endpoints

### AuthService (9898)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/v1/signup` | Register a new user, returns JWT tokens |
| POST | `/auth/v1/login` | Authenticate and get access + refresh token |
| POST | `/auth/v1/refreshToken` | Get a new access token |
| GET | `/auth/v1/ping` | Validate current session |

### UserService (9810)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/user/v1/me?userId=` | Get user profile |
| PUT | `/user/v1/update?userId=` | Update user profile |

### DSService — AI/ML (8000)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/v1/ds/message` | Parses a bank SMS and extracts amount, merchant, transaction type |

### ExpenseService (9820)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/expense/v1/getExpense` | Get all expenses for a user |
| POST | `/expense/v1/addExpense` | Add an expense (manual or AI-parsed) |
| GET | `/expense/v1/summary?days=` | Category-wise spending summary |
| POST | `/expense/v1/setLimit` | Set a monthly spending limit |
| GET | `/expense/v1/getLimit` | Get current spending limit status |

Every service also exposes `GET /health` for container health checks.

---

## Features

- Automatic expense detection from bank SMS using an LLM (Mistral AI + LangChain)
- Regex + keyword-based SMS pre-filtering before invoking the AI, to reduce cost and false positives
- JWT authentication with access + refresh token rotation
- Event-driven architecture using Apache Kafka (SMS parsing → expense creation pipeline)
- Manual expense entry alongside automatic detection
- Category-wise spending summaries and monthly spending limits with alerts
- Isolated MySQL database per microservice
- Fully containerized with Docker Compose, with persistent MySQL storage
- Cross-platform mobile client built with React Native (Expo)

---

## Frontend Repository
[Expense Tracker App](https://github.com/KishanSingh29/expense-tracker-app)

---

## Author
**Kishan Singh**
[GitHub](https://github.com/KishanSingh29)
