import threading
import uvicorn

from fastapi import FastAPI

from app.fraud_model import FraudModel
from app.kafka_consumer import FraudConsumer
from app.database import initialize_database


app = FastAPI(
    title="Payment Fraud Detection Service",
    version="2.0.0"
)

model = FraudModel()


@app.get("/")
def health_check():

    return {
        "service": "fraud-service",
        "status": "UP",
        "version": "2.0.0"
    }


@app.get("/fraud/analyze")
def analyze_transaction(
    user_id: int,
    amount: float,
    merchant: str
):

    result = model.calculate_risk(
        payment_id=None,
        user_id=user_id,
        amount=amount,
        merchant=merchant
    )

    return {
        "userId": user_id,
        "amount": amount,
        "merchant": merchant,
        **result
    }


def start_kafka_consumer():

    consumer = FraudConsumer()
    consumer.start()


if __name__ == "__main__":

    # Initialize fraud database
    initialize_database()

    # Start Kafka consumer in background
    kafka_thread = threading.Thread(
        target=start_kafka_consumer,
        daemon=True
    )

    kafka_thread.start()

    # Start FastAPI
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        reload=False
    )