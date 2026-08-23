import json
from kafka import KafkaProducer
import os

producer = KafkaProducer(
    bootstrap_servers=os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092"
),
    security_protocol="SASL_SSL",
    sasl_mechanism="SCRAM-SHA-256",
    sasl_plain_username=os.getenv("KAFKA_SASL_USERNAME"),
    sasl_plain_password=os.getenv("KAFKA_SASL_PASSWORD"),
    value_serializer=lambda value:
        json.dumps(value).encode("utf-8")
)


def publish_fraud_decision(decision):

    producer.send(
        "fraud-decision",
        value=decision
    )

    producer.flush()
