import psycopg2


import os

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "5432")),
    "database": os.getenv("DB_NAME", "payments"),
    "user": os.getenv("DB_USERNAME", "payment_user"),
    "password": os.getenv("DB_PASSWORD")
}


def get_connection():
    return psycopg2.connect(**DB_CONFIG)
def get_user_transaction_history(user_id, current_payment_id=None):

    connection = get_connection()

    try:
        cursor = connection.cursor()

        if current_payment_id is not None:

            cursor.execute("""
                SELECT amount, created_at
                FROM payments
                WHERE user_id = %s
                  AND id != %s
                  AND status = 'APPROVE'
                ORDER BY created_at ASC
            """, (
                user_id,
                current_payment_id
            ))

        else:

            cursor.execute("""
                SELECT amount, created_at
                FROM payments
                WHERE user_id = %s
                  AND status = 'APPROVE'
                ORDER BY created_at ASC
            """, (
                user_id,
            ))

        rows = cursor.fetchall()

        return [
            {
                "amount": float(row[0]),
                "timestamp": row[1]
            }
            for row in rows
        ]

    finally:
        connection.close()

def initialize_database():

    connection = get_connection()

    try:
        cursor = connection.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS fraud_decisions (
                id SERIAL PRIMARY KEY,

                payment_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,

                amount NUMERIC(19, 2) NOT NULL,
                merchant VARCHAR(255) NOT NULL,

                risk_score DOUBLE PRECISION NOT NULL,
                decision VARCHAR(20) NOT NULL,

                amount_risk DOUBLE PRECISION,
                spending_risk DOUBLE PRECISION,
                velocity_risk DOUBLE PRECISION,
                merchant_risk DOUBLE PRECISION,

                user_baseline_amount NUMERIC(19, 2),

                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """)

        connection.commit()

        print("Fraud database initialized.")

    finally:
        connection.close()


def save_fraud_decision(
    payment_id,
    user_id,
    amount,
    merchant,
    result
):

    connection = get_connection()

    try:

        cursor = connection.cursor()

        signals = result["signals"]

        cursor.execute("""
            INSERT INTO fraud_decisions (
                payment_id,
                user_id,
                amount,
                merchant,
                risk_score,
                decision,
                amount_risk,
                spending_risk,
                velocity_risk,
                merchant_risk,
                user_baseline_amount
            )
            VALUES (
                %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s
            )
        """, (
            payment_id,
            user_id,
            amount,
            merchant,
            result["riskScore"],
            result["decision"],
            signals["amountRisk"],
            signals["spendingRisk"],
            signals["velocityRisk"],
            signals["merchantRisk"],
            result["userBaselineAmount"]
        ))

        connection.commit()

    finally:
        connection.close()