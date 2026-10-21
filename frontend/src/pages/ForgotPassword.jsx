import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api.js";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setResetToken("");
    try {
      const data = await api("/api/auth/forgot-password", {
        method: "POST",
        body: JSON.stringify({ email })
      });
      setMessage(data.message);
      if (data.resetToken) setResetToken(data.resetToken);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="form card" onSubmit={onSubmit}>
      <p className="kicker">Password reset</p>
      <h2>Forgot your password?</h2>
      <p className="muted">
        Email delivery is not configured. If an account exists, a reset token is stored.
        A local server returns that token only when PROOFORA_EXPOSE_RESET_TOKEN is true.
      </p>
      <label>Email</label>
      <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      {message && <p className="banner">{message}</p>}
      {resetToken && (
        <p>
          Local reset token:{" "}
          <Link to={`/reset-password?token=${encodeURIComponent(resetToken)}`}>continue</Link>
        </p>
      )}
      {error && <p className="error">{error}</p>}
      <button className="btn" style={{ marginTop: 16 }} type="submit">Request reset</button>
    </form>
  );
}
