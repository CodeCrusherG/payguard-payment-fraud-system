import numpy as np
from sklearn.ensemble import IsolationForest
from datetime import datetime, timedelta
from app.kafka_producer import publish_fraud_decision
from app.database import get_user_transaction_history


class FraudModel:

    def __init__(self):

        # --------------------------------------------------
        # HISTORICAL AMOUNT DATA
        # --------------------------------------------------

        historical_amounts = np.array([
            500, 700, 1200, 1500, 1800,
            2200, 2500, 3000, 3500, 4000,
            4500, 5000, 5500, 6000, 7000,
            7500, 8000, 9000, 10000, 12000
        ]).reshape(-1, 1)

        self.model = IsolationForest(
            contamination=0.1,
            random_state=42
        )

        self.model.fit(historical_amounts)

        # --------------------------------------------------
        # MERCHANT RISK PROFILES
        # --------------------------------------------------

        self.merchant_risk = {
            "Amazon": 0.10,
            "Apple Store": 0.15,
            "Walmart": 0.10,
            "Flipkart": 0.12,
            "Luxury Electronics": 0.45,
            "Unknown": 0.30
        }

    def calculate_risk(
        self,
        payment_id,
        user_id,
        amount,
        merchant
    ):

        # ==================================================
        # 1. AMOUNT RISK
        # ==================================================

        if amount <= 5000:

            amount_risk = (
                amount / 5000
            ) * 0.20

        elif amount <= 10000:

            amount_risk = 0.20 + (
                (amount - 5000) / 5000
            ) * 0.20

        elif amount <= 25000:

            amount_risk = 0.40 + (
                (amount - 10000) / 15000
            ) * 0.20

        elif amount <= 50000:

            amount_risk = 0.60 + (
                (amount - 25000) / 25000
            ) * 0.20

        else:

            amount_risk = 0.80 + min(
                (amount - 50000) / 100000,
                0.20
            )

        amount_risk = float(
            np.clip(
                amount_risk,
                0.0,
                1.0
            )
        )

        # ==================================================
        # 2. USER SPENDING HISTORY
        # ==================================================

        history = get_user_transaction_history(
            user_id=user_id,
            current_payment_id=payment_id
        )

        if history:

            previous_amounts = [
                float(transaction["amount"])
                for transaction in history
            ]

            # Median is more robust than average
            # against a single unusually large transaction.
            baseline_amount = np.median(
                previous_amounts
            )

            if baseline_amount > 0:

                spending_ratio = (
                    amount / baseline_amount
                )

                spending_risk = float(
                    np.clip(
                        (spending_ratio - 1) / 9,
                        0.0,
                        1.0
                    )
                )

            else:

                spending_risk = 0.0

        else:

            baseline_amount = 0.0
            spending_risk = 0.0

        # ==================================================
        # 3. TRANSACTION VELOCITY
        # ==================================================

        now = datetime.now()

        recent_transactions = []

        for transaction in history:

            timestamp = transaction["timestamp"]

            if timestamp is None:
                continue

            # Handle timezone-aware timestamps safely
            if timestamp.tzinfo is not None:
                timestamp = timestamp.replace(
                    tzinfo=None
                )

            if (
                now - timestamp
                <= timedelta(minutes=5)
            ):
                recent_transactions.append(
                    transaction
                )

        recent_count = len(
            recent_transactions
        )

        if recent_count >= 5:

            velocity_risk = 0.9

        elif recent_count >= 3:

            velocity_risk = 0.6

        elif recent_count >= 2:

            velocity_risk = 0.3

        else:

            velocity_risk = 0.0

        # ==================================================
        # 4. MERCHANT RISK
        # ==================================================

        merchant_risk = self.merchant_risk.get(
            merchant,
            self.merchant_risk["Unknown"]
        )

        # ==================================================
        # 5. COMBINED RISK SCORE
        # ==================================================

        risk_score = (

            0.40 * amount_risk

            + 0.30 * spending_risk

            + 0.20 * velocity_risk

            + 0.10 * merchant_risk
        )

        risk_score = float(
            np.clip(
                risk_score,
                0.0,
                1.0
            )
        )

        # ==================================================
        # 6. DECISION
        # ==================================================

        if risk_score >= 0.70:

            decision = "BLOCK"

        elif risk_score >= 0.40:

            decision = "REVIEW"

        else:

            decision = "APPROVE"

        # ==================================================
        # 7. PUBLISH FRAUD DECISION TO KAFKA
        # ==================================================

        fraud_decision = {

            "paymentId": payment_id,

            "userId": user_id,

            "amount": amount,

            "merchant": merchant,

            "riskScore": round(
                risk_score,
                3
            ),

            "decision": decision
        }

        publish_fraud_decision(
            fraud_decision
        )

        # ==================================================
        # 8. RETURN RESULT
        # ==================================================

        return {

            "riskScore": round(
                risk_score,
                3
            ),

            "decision": decision,

            "signals": {

                "amountRisk": round(
                    amount_risk,
                    3
                ),

                "spendingRisk": round(
                    spending_risk,
                    3
                ),

                "velocityRisk": round(
                    velocity_risk,
                    3
                ),

                "merchantRisk": round(
                    merchant_risk,
                    3
                )
            },

            "userBaselineAmount": round(
                float(baseline_amount),
                2
            )
        }