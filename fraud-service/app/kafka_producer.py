import json
from kafka import KafkaProducer
import os

producer = KafkaProducer(
    bootstrap_servers=os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092"
),
    value_serializer=lambda value:
        json.dumps(value).encode("utf-8")
)


def publish_fraud_decision(decision):

    producer.send(
        "fraud-decision",
        value=decision
    )

    producer.flush()