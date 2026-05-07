# 💸 Expense Tracker — Backend Microservices

An AI-powered automatic expense tracking system built with Spring Boot Microservices, Apache Kafka, and Mistral AI.

> Automatically detects payment SMS/notifications, extracts expense data using AI, and saves it in real-time — no manual entry required.

---

## 🏗️ Architecture

```
📱 React Native App (Frontend)
         |
         ├──── REST ────────────────────────────┐
         ▼                                      ▼
+------------------+              +------------------+
|   AuthService    |──── Kafka ──▶|   UserService    |
|   Port: 9898     |  user_service|   Port: 9810     |
|  (JWT + BCrypt)  |    topic     |  (JWT Validated) |
+------------------+              +------------------+
                                       MySQL (userservice DB)
         |
         | SMS detected by App
         ▼
+----------------------+
|  DataScience Service | ← Flask + Mistral AI
|     Port: 8000       | ← Parses SMS text
+----------------------+
         |
         | Kafka (expense_service topic)
         ▼
+------------------+
|  ExpenseService  |
|   Port: 9820     |
+------------------+
         |
         ▼
   MySQL (expenseservice DB)
```

---

## 🚀 Features

- ✅ JWT Authentication with Access + Refresh Token rotation
- ✅ Real-time SMS detection using native Android module
- ✅ AI-powered expense parsing using **Mistral AI + LangChain**
- ✅ Async event-driven architecture using **Apache Kafka**
- ✅ Separate MySQL database per microservice
- ✅ User profile management with JWT-secured APIs
- ✅ Manual + automatic expense tracking
- ✅ React Native mobile app (Expo Dev Build)

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3, Java 21 |
| Messaging | Apache Kafka |
| AI/ML | Mistral AI, LangChain, Flask (Python) |
| Database | MySQL (separate DB per service) |
| Auth | JWT (Access + Refresh Token), Spring Security, BCrypt |
| Frontend | React Native (Expo) |
| SMS Detection | @maniac-tech/react-native-expo-read-sms |
| Containerization | Docker |

---

## 📦 Microservices

### 1. AuthService (Port: 9898)
Handles user registration and authentication. Publishes user events to Kafka.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/v1/signup` | Public | Register new user |
| POST | `/auth/v1/login` | Public | Login and get tokens |
| POST | `/auth/v1/refreshToken` | Public | Refresh access token |
| GET | `/auth/v1/ping` | JWT | Auth check |
| GET | `/health` | Public | Health check |

**On Signup:** Publishes `UserInfoEvent` to Kafka topic `user_service`

---

### 2. UserService (Port: 9810)
Manages user profile data. Consumes user events from Kafka. JWT validation on all endpoints.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/user/v1/me?userId=` | JWT | Get user profile |
| PUT | `/user/v1/update?userId=` | JWT | Update profile |
| GET | `/health` | Public | Health check |

**Kafka Consumer:** Listens to `user_service` topic → saves user profile to MySQL

---

### 3. DataScience Service (Port: 8000)
Flask service that uses Mistral AI + LangChain to parse payment SMS text.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/ds/message` | x-user-id header | Parse SMS and extract expense |
| GET | `/health` | Public | Health check |

**Request Body:**
```json
{
  "message": "Spent INR 350 at Zomato using HDFC card"
}
```

**Response:**
```json
{
  "amount": "350",
  "merchant": "Zomato",
  "currency": "INR"
}
```

**SMS Filter:** Only processes SMS containing keywords: `spent`, `bank`, `card`

**On Success:** Publishes parsed data to Kafka topic `expense_service`

---

### 4. ExpenseService (Port: 9820)
Stores and manages expense records. Acts as both Kafka consumer and REST API provider.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/expense/v1/getExpense` | X-User-Id header | Get all user expenses |
| POST | `/expense/v1/addExpense` | X-User-Id header | Manually add expense |
| GET | `/health` | Public | Health check |

**Kafka Consumer:** Listens to `expense_service` topic → saves expense to MySQL

---

## 🔄 How It Works

### Auto Flow (SMS Detection):
```
1. User receives payment SMS on phone
2. React Native app detects SMS in background (Android native module)
3. SMS filtered — contains "spent/bank/card" keywords?
4. POST /v1/ds/message → DataScience Service
5. Mistral AI extracts: amount, merchant, currency
6. Publishes to Kafka topic: expense_service
7. ExpenseService consumes → saves to MySQL
8. Home screen auto-refreshes with new expense
```

### Manual Flow:
```
1. User taps "+" button in app
2. POST /expense/v1/addExpense with X-User-Id header
3. ExpenseService saves to MySQL
4. UI updates
```

### Auth Flow:
```
1. POST /auth/v1/signup → JWT tokens + userId returned
2. AuthService publishes to Kafka → UserService saves profile
3. Tokens stored in AsyncStorage
4. All secured APIs use Bearer token or X-User-Id header
5. Token expired → POST /auth/v1/refreshToken → new access token
```

---

## ⚙️ Local Setup

### Prerequisites
- Java 21
- Python 3.9+
- Docker Desktop
- MySQL 8.0
- Node.js 18+

### Step 1 — Start Kafka (Docker)
```bash
docker-compose up -d
```

### Step 2 — Configure MySQL
```sql
CREATE DATABASE authservice;
CREATE DATABASE userservice;
CREATE DATABASE expenseservice;
```

### Step 3 — Configure application.properties
In each Spring Boot service:
```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

In `dsservice/.env`:
```
OPENAI_API_KEY=your_mistral_api_key
KAFKA_HOST=localhost
KAFKA_PORT=9092
```

### Step 4 — Start Services (in order)
```bash
# 1. AuthService (Port 9898)
cd authservice && ./mvnw spring-boot:run

# 2. UserService (Port 9810)
cd userservice && ./mvnw spring-boot:run

# 3. ExpenseService (Port 9820)
cd expenseservice && ./mvnw spring-boot:run

# 4. DataScience Service (Port 8000)
cd dsservice
python -m venv dsenv
dsenv\Scripts\activate
pip install flask langchain langchain-mistralai kafka-python python-dotenv
cd src && python -m app
```

### Step 5 — Run Frontend
```bash
cd expense-tracker-app
npm install
npx expo start
```

---

## 📱 Frontend Repository
[Expense Tracker App](https://github.com/KishanSingh29/expense-tracker-app)

---

## 👨‍💻 Author
**Kishan Singh** — 3rd Year CS Student  
[GitHub](https://github.com/KishanSingh29)
