# 💳 Fraud Detector

A real-time transaction fraud detection microservice built with Python, FastAPI, PostgreSQL, and Apache Kafka.

The service analyzes payment transactions using multiple behavioral and transaction-level signals, generates a normalized fraud risk score, and returns one of three decisions:

APPROVE | REVIEW | REJECT

---

## 🚀 Features

- Real-time transaction fraud analysis through REST APIs
- Rule-based fraud risk scoring
- Amount-based risk detection
- User spending behavior analysis
- Transaction velocity analysis
- Merchant risk analysis
- PostgreSQL persistence for fraud decisions
- Apache Kafka integration for event-driven communication
- FastAPI REST API
- Dockerized deployment
- Interactive Swagger API documentation

---

## 🏗️ System Architecture

                         Payment Request
                                |
                                v
                  +-------------------------+
                  |    Payment Service      |
                  |      Spring Boot        |
                  +------------+------------+
                               |
                               | Transaction Event
                               v
                  +-------------------------+
                  |          Kafka          |
                  |    Event Streaming      |
                  +------------+------------+
                               |
                               v
                  +-------------------------+
                  |     Fraud Detector      |
                  |        FastAPI          |
                  +------------+------------+
                               |
              +----------------+----------------+
              |                |                |
              v                v                v
        Amount Risk      Spending Risk     Velocity Risk
              |                |                |
              +----------------+----------------+
                               |
                               v
                  +-------------------------+
                  |     Risk Scoring        |
                  |        Engine            |
                  +------------+------------+
                               |
                               v
                  +-------------------------+
                  | APPROVE / REVIEW /      |
                  |        REJECT           |
                  +------------+------------+
                               |
                               v
                  +-------------------------+
                  |       PostgreSQL        |
                  |    Fraud Decisions     |
                  +-------------------------+

The architecture separates payment processing from fraud analysis, allowing the fraud detection service to operate independently from the payment service.

---

## 🔍 Fraud Detection Signals

### 1. Amount Risk

The transaction amount is compared against the user's historical spending behavior.

Large deviations from the user's normal transaction amount increase the risk.

Example:

Historical Average: ₹1,500
Current Transaction: ₹25,000

→ High Amount Risk

This helps identify unusually large transactions.

---

### 2. Spending Risk

The system analyzes the current transaction against the user's historical spending pattern.

Example:

Normal User Spending:

₹500
₹900
₹1,200
₹800
₹1,100

Current Transaction: ₹15,000

→ Higher Spending Risk

A transaction that significantly differs from normal user behavior contributes additional risk.

---

### 3. Velocity Risk

Transaction frequency is considered during risk evaluation.

A large number of transactions within a short period can indicate suspicious activity such as automated attacks or account takeover.

Example:

Transaction 1
      |
      v
Transaction 2
      |
      v
Transaction 3
      |
      v
Transaction 4
      |
      v
Transaction 5
      |
      v
Short Time Window
      |
      v
Increased Velocity Risk

---

### 4. Merchant Risk

The transaction's merchant is also considered during risk evaluation.

Transactions involving higher-risk, unusual, or suspicious merchants can contribute additional risk to the final score.

---

## 📊 Risk Scoring

Each fraud signal contributes to the final risk score.

Conceptually:

Risk Score =
    Amount Risk
  + Spending Risk
  + Velocity Risk
  + Merchant Risk

The resulting score is normalized and mapped to a final decision.

Example:

| Risk Score | Decision |
|------------|----------|
| 0.00–0.40  | APPROVE  |
| 0.40–0.75  | REVIEW   |
| 0.75–1.00  | REJECT   |

The exact thresholds can be configured according to the requirements of the payment system.

---

## 🔌 REST API

The service exposes a REST API using FastAPI.

### Analyze Transaction

POST /fraud/analyze

### Request

{
  "userId": "user123",
  "amount": 25000,
  "currency": "INR",
  "merchant": "Electronics Store"
}

### Response

{
  "userId": "user123",
  "amount": 25000,
  "merchant": "Electronics Store",
  "riskScore": 0.73,
  "decision": "REVIEW",
  "signals": {
    "amountRisk": 0.81,
    "spendingRisk": 0.72,
    "velocityRisk": 0.40,
    "merchantRisk": 0.55
  },
  "userAverageAmount": 4200
}

---

## 🧪 Testing the API

The API can be tested using curl.

curl -X POST http://localhost:8000/fraud/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "amount": 25000,
    "currency": "INR",
    "merchant": "Electronics Store"
  }'

---

## 🗄️ PostgreSQL Persistence

Fraud analysis results are persisted in PostgreSQL.

A fraud decision contains information such as:

fraud_decisions
│
├── id
├── user_id
├── amount
├── merchant
├── risk_score
├── decision
├── amount_risk
├── spending_risk
├── velocity_risk
├── merchant_risk
├── user_average_amount
└── created_at

Persisting fraud decisions allows the system to:

- Maintain an audit trail
- Analyze historical fraud patterns
- Retrieve previous decisions
- Track user spending behavior
- Build datasets for future fraud models
- Improve risk-scoring strategies

---

## ⚡ Apache Kafka Integration

Kafka is used for asynchronous communication within the distributed payment system.

The Payment Service publishes transaction events to Kafka, allowing the Fraud Detector to remain decoupled from the payment service.

Payment Service
      |
      | Publish Transaction Event
      v
    Kafka
      |
      | Consume Event
      v
Fraud Detector
      |
      v
Risk Analysis

### Benefits of Kafka

- Asynchronous processing
- Loose coupling between services
- Fault isolation
- Scalable event streaming
- Independent service development
- Easy integration with additional downstream services

---

## 📁 Project Structure

fraud-service/
│
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── models.py
│   ├── schemas.py
│   ├── database.py
│   ├── fraud_engine.py
│   └── kafka_consumer.py
│
├── requirements.txt
├── run.py
├── Dockerfile
└── README.md

### Components

| Component | Responsibility |
|-----------|----------------|
| main.py | FastAPI application and API routes |
| fraud_engine.py | Fraud signal calculation and risk scoring |
| schemas.py | Request and response validation |
| models.py | PostgreSQL database models |
| database.py | Database connection and configuration |
| kafka_consumer.py | Kafka event consumption |
| run.py | Application entry point |
| Dockerfile | Container configuration |

---

## 🛠️ Technology Stack

| Technology | Purpose |
|------------|---------|
| Python | Core application and fraud detection logic |
| FastAPI | REST API framework |
| PostgreSQL | Fraud decision persistence |
| SQLAlchemy | Database ORM |
| Apache Kafka | Event-driven communication |
| Uvicorn | ASGI application server |
| Docker | Containerization |

---

## ⚙️ Running Locally

### Prerequisites

Install the following:

- Python 3.10+
- PostgreSQL
- Apache Kafka
- Docker (optional)

### 1. Clone the Repository

git clone <YOUR_REPOSITORY_URL>
cd fraud-service

### 2. Create a Virtual Environment

python3 -m venv venv

Activate on macOS/Linux:

source venv/bin/activate

Activate on Windows:

venv\Scripts\activate

### 3. Install Dependencies

pip install -r requirements.txt

### 4. Configure Environment Variables

Create a .env file:

DATABASE_URL=postgresql://username:password@localhost:5432/fraud_db
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=payment-events

Replace the database credentials and Kafka configuration with your local environment.

### 5. Start PostgreSQL

Create the required database:

CREATE DATABASE fraud_db;

Make sure PostgreSQL is running before starting the application.

### 6. Start Kafka

Make sure the Kafka broker is running and accessible at:

localhost:9092

Create the transaction event topic if required:

kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic payment-events

### 7. Start the Fraud Service

python run.py

Alternatively:

uvicorn app.main:app --host 0.0.0.0 --port 8000

The service will be available at:

http://localhost:8000

---

## 📚 API Documentation

FastAPI automatically generates interactive API documentation.

Swagger UI:

http://localhost:8000/docs

ReDoc:

http://localhost:8000/redoc

Swagger UI allows you to send test transactions directly to the fraud detection service.

---

## 🐳 Docker

The service includes a Dockerfile for containerized deployment.

### Build the Image

docker build -t fraud-service .

### Run the Container

docker run -p 8000:8000 fraud-service

For complete deployment, Docker Compose can be used to run:

Payment Service
      |
      +--- Kafka
      |
      +--- Fraud Service
      |
      +--- PostgreSQL

---

## 🧪 Example Fraud Scenarios

### Normal Transaction

Amount:        ₹800
User Average:  ₹1,000
Velocity:      Normal
Merchant Risk: Low

Risk Score:    Low
Decision:      APPROVE

### Unusually Large Transaction

Amount:        ₹50,000
User Average:  ₹1,200
Velocity:      Normal
Merchant Risk: Medium

Risk Score:    High
Decision:      REVIEW / REJECT

### High Transaction Velocity

Multiple transactions
        +
Short time interval
        +
Unusual spending
        |
        v
Higher Risk Score
        |
        v
REVIEW

---

## 🎯 Design Goals

The project focuses on the following system-design goals:

1. Low-latency fraud analysis
2. Independent microservice deployment
3. Event-driven communication
4. Persistent fraud decision history
5. Extensible risk scoring
6. Service isolation
7. Containerized deployment

---

## 🔮 Future Improvements

The current rule-based fraud engine can be extended into a more sophisticated fraud detection platform.

Potential improvements include:

- Machine-learning-based fraud classification
- Real-time feature engineering
- Redis-based transaction velocity tracking
- Device fingerprinting
- IP and geolocation analysis
- Graph-based fraud detection
- Model versioning
- A/B testing of fraud models
- Kafka consumer groups for horizontal scaling
- Dead-letter queues for failed events
- Prometheus and Grafana monitoring
- Distributed tracing
- Automated model retraining
- Feature stores for real-time fraud features

---

## 🚀 Possible Production Architecture

A production-oriented version could evolve into:

                         ┌──────────────────┐
                         │    API Gateway   │
                         └────────┬─────────┘
                                  |
                                  v
                         ┌──────────────────┐
                         │ Payment Service  │
                         └────────┬─────────┘
                                  |
                                  v
                         ┌──────────────────┐
                         │      Kafka       │
                         └────────┬─────────┘
                                  |
                   ┌──────────────┼──────────────┐
                   |              |              |
                   v              v              v
             Fraud Service   Analytics     Notification
                   |
                   v
             Feature Store
                   |
                   v
             ML Risk Model
                   |
                   v
             Fraud Decision
                   |
                   v
              PostgreSQL

This architecture could support high-volume transaction processing while allowing fraud models and downstream services to evolve independently.

---

## 💡 Key Concepts Demonstrated

This project demonstrates practical implementation of:

- Microservice architecture
- Event-driven architecture
- REST API design
- Apache Kafka
- PostgreSQL
- SQLAlchemy
- FastAPI
- Rule-based fraud detection
- Risk scoring
- Behavioral analysis
- Docker containerization
- Asynchronous processing
- Service decoupling
- Backend API development
- Distributed system design

---

## ⚠️ Disclaimer

This project is an educational implementation of a transaction fraud detection system.

The rule-based scoring engine demonstrates backend engineering, fraud detection concepts, microservice architecture, and event-driven system design.

It should not be considered a production-ready financial fraud detection solution without extensive validation, security controls, monitoring, model evaluation, regulatory compliance, and real-world fraud datasets.

---

## 👨‍💻 Author

Gopesh Singhal

B.Tech Computer Science
Indian Institute of Technology, Mandi
