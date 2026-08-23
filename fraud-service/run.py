import threading

import uvicorn

from app.kafka_consumer import FraudConsumer
from app.database import initialize_database


def start_kafka_consumer():

    consumer = FraudConsumer()
    consumer.start()


if __name__ == "__main__":

    initialize_database()

    kafka_thread = threading.Thread(
        target=start_kafka_consumer,
        daemon=True
    )

    kafka_thread.start()

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=False
    )