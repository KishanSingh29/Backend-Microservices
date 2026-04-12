# 💸 Expense Tracker — Backend Microservices

An AI-powered automatic expense tracking system built with Spring Boot Microservices, Apache Kafka, and Mistral AI.

> Automatically detects payment SMS/notifications, extracts expense data using AI, and saves it in real-time.

---

## 🏗️ Architecture

```
React Native App
        |
        ▼
+---------------+        +---------------+
|  AuthService  |------->|  UserService  |
|   Port: 9898  |        |   Port: 9810  |
+---------------+        +---------------+
        |
        | Kafka (user_service topic)
        ▼
+----------------------+
|  DataScience Service |  <- Mistral AI + Flask
|     Port: 8000       |  <- Parses SMS
+----------------------+
        |
        | Kafka (expense topic)
        ▼
+---------------+
| ExpenseService|
|  Port: 9820   |
+---------------+
        |
        ▼
    MySQL DB
```



---

## 🚀 Features

- ✅ JWT Authentication with Access + Refresh Token
- ✅ Auto SMS/Notification parsing using **Mistral AI**
- ✅ Async communication between services using **Apache Kafka**
- ✅ Separate MySQL database per microservice
- ✅ User profile management
- ✅ Real-time expense tracking
- ✅ React Native mobile app

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3, Java 21 |
| Messaging | Apache Kafka |
| AI/ML | Mistral AI, Flask (Python) |
| Database | MySQL |
| Auth | JWT (Access + Refresh Token) |
| Frontend | React Native (Expo) |
| Containerization | Docker |

---

## 📦 Microservices

### 1. AuthService (Port: 9898)
Handles user registration and authentication.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/v1/signup` | Register new user |
| POST | `/auth/v1/login` | Login and get tokens |
| POST | `/auth/v1/refreshToken` | Refresh access token |
| GET | `/auth/v1/ping` | Auth check |

### 2. UserService (Port: 9810)
Manages user profile data.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/user/v1/me?userId=` | Get user profile |
| PUT | `/user/v1/update?userId=` | Update profile |

### 3. DataScience Service (Port: 8000)
Flask service that uses Mistral AI to parse payment SMS.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/ds/v1/parse-expense` | Parse SMS and extract expense data |

### 4. ExpenseService (Port: 9820)
Stores and manages expense records.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/expense/v1/list?userId=` | Get all expenses |
| POST | `/expense/v1/add` | Add expense |
| DELETE | `/expense/v1/delete?id=` | Delete expense |

---

## ⚙️ Local Setup

### Prerequisites
- Java 21
- Python 3.9+
- Docker Desktop
- MySQL
- Node.js

### Step 1 — Start Kafka (Docker)
```bash
docker-compose up -d
```

### Step 2 — Configure MySQL
Create these databases:
```sql
CREATE DATABASE authservice;
CREATE DATABASE userservice;
CREATE DATABASE expenseservice;
```

### Step 3 — Configure application.properties
In each service, update:
```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### Step 4 — Start Services (in order)
```bash
# 1. AuthService
cd authservice && ./mvnw spring-boot:run

# 2. UserService  
cd userservice && ./mvnw spring-boot:run

# 3. ExpenseService
cd expenseservice && ./mvnw spring-boot:run

# 4. DataScience Service
cd dsservice && pip install -r requirements.txt && python app.py
```

---

## 🔄 How It Works

1- User receives payment SMS on phone
2- React Native app captures SMS in background
3- SMS sent to DataScience Service
4- Mistral AI extracts: amount, merchant, category, date
5- Parsed data sent via Kafka to ExpenseService
6- Expense saved in MySQL database
7- React Native app shows updated expense list


---

## 📱 Frontend Repository
[Expense Tracker App](https://github.com/KishanSingh29/expense-tracker-app.git)

---

## 👨‍💻 Author
**Kishan Singh** — 3rd Year CS Student
