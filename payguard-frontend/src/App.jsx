import { useEffect, useState } from "react";
import "./App.css";

const API = "http://localhost:8080";

function App() {
  const [activeTab, setActiveTab] = useState("dashboard");

  const [userId, setUserId] = useState("1");
  const [amount, setAmount] = useState("500");
  const [merchant, setMerchant] = useState("Amazon");

  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [transactions, setTransactions] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [selectedTransaction, setSelectedTransaction] = useState(null);

  // -----------------------------
  // CREATE PAYMENT
  // -----------------------------

  const processPayment = async () => {
    setError("");
    setPayment(null);

    if (!userId || !amount || !merchant) {
      setError("Please fill in all fields.");
      return;
    }

    if (Number(amount) <= 0) {
      setError("Amount must be greater than ₹0.");
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API}/api/payments`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Idempotency-Key": `web-${Date.now()}`,
        },
        body: JSON.stringify({
          userId: Number(userId),
          amount: Number(amount),
          currency: "INR",
          merchant: merchant.trim(),
        }),
      });

      if (!response.ok) {
        throw new Error("Payment request failed");
      }

      const data = await response.json();

      setPayment(data);

      // Poll backend until fraud service finishes
      await pollPayment(data.id);
    } catch (err) {
      console.error(err);
      setError("Unable to process payment. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // -----------------------------
  // POLL PAYMENT STATUS
  // -----------------------------

  const pollPayment = async (id) => {
    for (let i = 0; i < 15; i++) {
      await new Promise((resolve) => setTimeout(resolve, 1000));

      try {
        const response = await fetch(`${API}/api/payments/${id}`);

        if (!response.ok) {
          continue;
        }

        const data = await response.json();

        setPayment(data);

        if (data.status !== "PROCESSING") {
          return data;
        }
      } catch (err) {
        console.error(err);
      }
    }

    return null;
  };

  // -----------------------------
  // LOAD HISTORY
  // -----------------------------

  const loadHistory = async () => {
    setHistoryLoading(true);

    try {
      const response = await fetch(`${API}/api/payments`);

      if (!response.ok) {
        throw new Error("Could not load transactions");
      }

      const data = await response.json();

      setTransactions(data);
    } catch (err) {
      console.error(err);
      setError("Unable to load transaction history.");
    } finally {
      setHistoryLoading(false);
    }
  };

  // Load history whenever History tab is opened
  useEffect(() => {
    if (activeTab === "history") {
      loadHistory();
    }
  }, [activeTab]);

  // -----------------------------
  // STATUS HELPERS
  // -----------------------------

  const statusClass = (status) => {
    if (status === "APPROVE") return "approved";
    if (status === "BLOCK") return "blocked";
    if (status === "REVIEW") return "review";
    return "processing";
  };

  const statusText = (status) => {
    if (status === "APPROVE") return "APPROVED";
    if (status === "BLOCK") return "BLOCKED";
    if (status === "REVIEW") return "REVIEW";
    return "PROCESSING";
  };

  const statusMessage = (status) => {
    if (status === "APPROVE") {
      return "Payment approved by fraud engine";
    }

    if (status === "REVIEW") {
      return "Payment flagged for additional verification";
    }

    if (status === "BLOCK") {
      return "Payment blocked due to high fraud risk";
    }

    return "Fraud analysis in progress...";
  };

  // -----------------------------
  // FILTER HISTORY
  // -----------------------------

  const filteredTransactions = transactions.filter((tx) => {
    const query = search.toLowerCase().trim();

    const matchesSearch =
      !query ||
      String(tx.id).includes(query) ||
      String(tx.userId).includes(query) ||
      tx.merchant.toLowerCase().includes(query);

    const matchesFilter =
      filter === "ALL" || tx.status === filter;

    return matchesSearch && matchesFilter;
  });

  // -----------------------------
  // FORMAT DATE
  // -----------------------------

  const formatDate = (date) => {
    if (!date) return "-";

    return new Date(date).toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="app">

      {/* ================= NAVBAR ================= */}

      <header className="navbar">

        <div className="brand">
          <div className="brand-icon">P</div>

          <div>
            <h1>PayGuard</h1>
            <span>Payment Security Platform</span>
          </div>
        </div>

        <nav className="nav-tabs">

          <button
            className={activeTab === "dashboard" ? "active" : ""}
            onClick={() => {
              setActiveTab("dashboard");
              setSelectedTransaction(null);
            }}
          >
            Dashboard
          </button>

          <button
            className={activeTab === "history" ? "active" : ""}
            onClick={() => {
              setActiveTab("history");
              setSelectedTransaction(null);
            }}
          >
            History
          </button>

        </nav>

        <div className="live">
          <span></span>
          LIVE
        </div>

      </header>


      {/* ================= DASHBOARD ================= */}

      {activeTab === "dashboard" && (

        <main className="container">

          <section className="hero">

            <p className="eyebrow">
              SECURE PAYMENTS
            </p>

            <h2>
              Process a payment
            </h2>

            <p className="subtitle">
              Secure payment processing with real-time fraud detection.
            </p>

          </section>


          <div className="dashboard">

            {/* PAYMENT FORM */}

            <section className="card payment-form">

              <div className="card-header">

                <h3>Create Payment</h3>

                <p>
                  Enter transaction details
                </p>

              </div>


              <div className="form-group">

                <label>
                  User ID
                </label>

                <input
                  type="number"
                  value={userId}
                  onChange={(e) =>
                    setUserId(e.target.value)
                  }
                  placeholder="1"
                />

              </div>


              <div className="form-group">

                <label>
                  Amount
                </label>

                <div className="amount-input">

                  <span>₹</span>

                  <input
                    type="number"
                    value={amount}
                    onChange={(e) =>
                      setAmount(e.target.value)
                    }
                    placeholder="500"
                  />

                </div>

              </div>


              <div className="form-group">

                <label>
                  Merchant
                </label>

                <input
                  type="text"
                  value={merchant}
                  onChange={(e) =>
                    setMerchant(e.target.value)
                  }
                  placeholder="Amazon"
                />

              </div>


              {error && (
                <div className="error">
                  {error}
                </div>
              )}


              <button
                className="process-button"
                onClick={processPayment}
                disabled={loading}
              >
                {loading
                  ? "PROCESSING..."
                  : "PROCESS PAYMENT →"}
              </button>

            </section>


            {/* PAYMENT RESULT */}

            <section className="card status-card">

              <div className="card-header">

                <h3>
                  Payment Status
                </h3>

                <p>
                  {payment
                    ? `PAYMENT #${payment.id}`
                    : "No payment selected"}
                </p>

              </div>


              {!payment ? (

                <div className="empty-state">

                  <div className="empty-icon">
                    ₹
                  </div>

                  <h4>
                    No payment yet
                  </h4>

                  <p>
                    Create a payment to see
                    its fraud detection result.
                  </p>

                </div>

              ) : (

                <div className="payment-result">

                  <div className="amount-display">
                    ₹
                    {Number(payment.amount).toLocaleString(
                      "en-IN"
                    )}
                  </div>


                  <div
                    className={`status-badge ${statusClass(
                      payment.status
                    )}`}
                  >

                    <span className="status-dot"></span>

                    {statusText(payment.status)}

                  </div>


                  <div className="details">

                    <div>
                      <span>User</span>
                      <strong>
                        {payment.userId}
                      </strong>
                    </div>

                    <div>
                      <span>Merchant</span>
                      <strong>
                        {payment.merchant}
                      </strong>
                    </div>

                    <div>
                      <span>Currency</span>
                      <strong>
                        {payment.currency}
                      </strong>
                    </div>

                    <div>
                      <span>Transaction ID</span>
                      <strong>
                        #{payment.id}
                      </strong>
                    </div>

                    <div>
                      <span>Idempotency</span>
                      <strong>
                        {payment.idempotencyKey}
                      </strong>
                    </div>

                  </div>


                  <div
                    className={`decision-message ${statusClass(
                      payment.status
                    )}`}
                  >
                    {payment.status === "APPROVE" && "✓ "}
                    {payment.status === "REVIEW" && "⚠ "}
                    {payment.status === "BLOCK" && "✕ "}
                    {payment.status === "PROCESSING" && "◌ "}

                    {statusMessage(payment.status)}

                  </div>

                </div>

              )}

            </section>

          </div>

        </main>

      )}


      {/* ================= HISTORY ================= */}

      {activeTab === "history" && (

        <main className="container history-page">

          <section className="hero">

            <p className="eyebrow">
              TRANSACTION HISTORY
            </p>

            <h2>
              Payment history
            </h2>

            <p className="subtitle">
              View and inspect transactions processed
              through PayGuard.
            </p>

          </section>


          <section className="card history-card">

            <div className="history-toolbar">

              <div>

                <h3>
                  All Transactions
                </h3>

                <p>
                  {transactions.length} transactions
                </p>

              </div>


              <button
                className="refresh-button"
                onClick={loadHistory}
                disabled={historyLoading}
              >
                ↻ Refresh
              </button>

            </div>


            <div className="history-controls">

              <input
                type="text"
                placeholder="Search payment, user or merchant..."
                value={search}
                onChange={(e) =>
                  setSearch(e.target.value)
                }
              />


              <div className="filter-buttons">

                <button
                  className={
                    filter === "ALL"
                      ? "selected"
                      : ""
                  }
                  onClick={() =>
                    setFilter("ALL")
                  }
                >
                  All
                </button>

                <button
                  className={
                    filter === "APPROVE"
                      ? "selected"
                      : ""
                  }
                  onClick={() =>
                    setFilter("APPROVE")
                  }
                >
                  Approved
                </button>

                <button
                  className={
                    filter === "REVIEW"
                      ? "selected"
                      : ""
                  }
                  onClick={() =>
                    setFilter("REVIEW")
                  }
                >
                  Review
                </button>

                <button
                  className={
                    filter === "BLOCK"
                      ? "selected"
                      : ""
                  }
                  onClick={() =>
                    setFilter("BLOCK")
                  }
                >
                  Blocked
                </button>

              </div>

            </div>


            {historyLoading ? (

              <div className="history-empty">
                Loading transactions...
              </div>

            ) : filteredTransactions.length === 0 ? (

              <div className="history-empty">
                No transactions found.
              </div>

            ) : (

              <div className="transaction-list">

                {filteredTransactions.map((tx) => (

                  <div
                    className="transaction"
                    key={tx.id}
                    onClick={() =>
                      setSelectedTransaction(tx)
                    }
                  >

                    <div className="transaction-id">

                      <div className="merchant-icon">
                        {tx.merchant
                          .charAt(0)
                          .toUpperCase()}
                      </div>

                      <div>

                        <strong>
                          {tx.merchant}
                        </strong>

                        <span>
                          Payment #{tx.id}
                        </span>

                      </div>

                    </div>


                    <div className="transaction-user">

                      <span>
                        USER
                      </span>

                      <strong>
                        User {tx.userId}
                      </strong>

                    </div>


                    <div className="transaction-amount">

                      ₹
                      {Number(tx.amount).toLocaleString(
                        "en-IN"
                      )}

                    </div>


                    <div
                      className={`mini-status ${statusClass(
                        tx.status
                      )}`}
                    >
                      {statusText(tx.status)}
                    </div>

                  </div>

                ))}

              </div>

            )}

          </section>


          {/* TRANSACTION DETAILS */}

          {selectedTransaction && (

            <section className="card transaction-detail">

              <div className="detail-heading">

                <div>

                  <p className="eyebrow">
                    TRANSACTION DETAILS
                  </p>

                  <h3>
                    Payment #{selectedTransaction.id}
                  </h3>

                </div>

                <button
                  onClick={() =>
                    setSelectedTransaction(null)
                  }
                >
                  ✕
                </button>

              </div>


              <div className="detail-main">

                <div className="detail-amount">

                  ₹
                  {Number(
                    selectedTransaction.amount
                  ).toLocaleString("en-IN")}

                </div>

                <div
                  className={`status-badge ${statusClass(
                    selectedTransaction.status
                  )}`}
                >
                  <span className="status-dot"></span>

                  {statusText(
                    selectedTransaction.status
                  )}
                </div>

              </div>


              <div className="details expanded">

                <div>
                  <span>User ID</span>
                  <strong>
                    {selectedTransaction.userId}
                  </strong>
                </div>

                <div>
                  <span>Merchant</span>
                  <strong>
                    {selectedTransaction.merchant}
                  </strong>
                </div>

                <div>
                  <span>Currency</span>
                  <strong>
                    {selectedTransaction.currency}
                  </strong>
                </div>

                <div>
                  <span>Created</span>
                  <strong>
                    {formatDate(
                      selectedTransaction.createdAt
                    )}
                  </strong>
                </div>

                <div>
                  <span>Idempotency Key</span>
                  <strong>
                    {selectedTransaction.idempotencyKey}
                  </strong>
                </div>

              </div>

            </section>

          )}

        </main>

      )}


      <footer>

        <span>
          PayGuard
        </span>

        <span>
          Real-time payment fraud detection
        </span>

      </footer>

    </div>
  );
}

export default App;