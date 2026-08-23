import json
import os
from kafka import KafkaConsumer, KafkaProducer

from app.fraud_model import FraudModel
from app.database import save_fraud_decision


class FraudConsumer:

    def __init__(self):

        self.model = FraudModel()

        self.consumer = KafkaConsumer(
            "payment-created",

            bootstrap_servers=os.getenv(
                "KAFKA_BOOTSTRAP_SERVERS",
                "localhost:9092"
            ),
            security_protocol="SASL_SSL",
            sasl_mechanism="SCRAM-SHA-256",
            sasl_plain_username=os.getenv("KAFKA_SASL_USERNAME"),
            sasl_plain_password=os.getenv("KAFKA_SASL_PASSWORD"),
    

            group_id="fraud-service",

            auto_offset_reset="earliest",

            enable_auto_commit=False,

            value_deserializer=lambda value:
                json.loads(value.decode("utf-8"))
        )

        self.producer = KafkaProducer(
            bootstrap_servers=os.getenv(
            "KAFKA_BOOTSTRAP_SERVERS",
            "localhost:9092"
        ),

            value_serializer=lambda value:
                json.dumps(value).encode("utf-8")
        )

        self.max_retries = 3

    def start(self):

        print(
            "Fraud service listening for payments..."
        )

        for message in self.consumer:

            payment = message.value

            payment_id = payment["paymentId"]

            print()
            print("=" * 50)

            print(
                f"Payment received: {payment_id}"
            )

            try:

                self.process_payment(payment)

                # Successfully processed
                self.consumer.commit()

                print(
                    "Payment processed successfully."
                )

            except Exception as e:

                print(
                    f"ERROR processing payment {payment_id}: {e}"
                )

                # Retry processing
                success = False

                for attempt in range(1, self.max_retries + 1):

                    try:

                        print(
                            f"Retry {attempt}/{self.max_retries} "
                            f"for payment {payment_id}"
                        )

                        self.process_payment(payment)

                        success = True

                        self.consumer.commit()

                        print(
                            f"Payment {payment_id} "
                            f"processed successfully on retry."
                        )

                        break

                    except Exception as retry_error:

                        print(
                            f"Retry {attempt} failed: "
                            f"{retry_error}"
                        )

                # All retries failed
                if not success:

                    print(
                        f"Sending payment {payment_id} "
                        f"to DLT."
                    )

                    dlt_message = {
                        "originalTopic": message.topic,
                        "partition": message.partition,
                        "offset": message.offset,
                        "payment": payment,
                        "error": str(e)
                    }

                    self.producer.send(
                        "fraud-decision.DLT",
                        value=dlt_message
                    )

                    self.producer.flush()

                    # Commit only after DLT publish succeeds
                    self.consumer.commit()

                    print(
                        f"Payment {payment_id} "
                        f"sent to fraud-decision.DLT."
                    )

            print("=" * 50)

    def process_payment(self, payment):

        payment_id = payment["paymentId"]
        user_id = payment["userId"]
        amount = float(payment["amount"])
        merchant = payment["merchant"]

        print(
            f"User: {user_id}"
        )

        print(
            f"Amount: ₹{amount}"
        )

        print(
            f"Merchant: {merchant}"
        )

        # Run fraud analysis
        result = self.model.calculate_risk(
            payment_id=payment_id,
            user_id=user_id,
            amount=amount,
            merchant=merchant
        )

        # Persist fraud decision
        save_fraud_decision(
            payment_id=payment_id,
            user_id=user_id,
            amount=amount,
            merchant=merchant,
            result=result
        )

        print(
            f"Risk Score: {result['riskScore']}"
        )

        print(
            f"Decision: {result['decision']}"
        )

        print(
            f"Signals: {result['signals']}"
        )

        print(
            "Fraud decision saved to PostgreSQL."
        )
