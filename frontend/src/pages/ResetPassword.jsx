import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../api.js";
import PasswordField from "../components/PasswordField.jsx";
import { passwordError } from "../utils/passwordRules.js";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const [token, setToken] = useState(params.get("token") || "");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    const problem = passwordError(password);
    if (problem) {
      setError(problem);
      return;
    }
    setError("");
    try {
      await api("/api/auth/reset-password", {
        method: "POST",
        body: JSON.stringify({ token, password })
      });
      setMessage("Password updated. You can sign in.");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="form card" onSubmit={onSubmit}>
      <p className="kicker">Password reset</p>
      <h2>Choose a new password</h2>
      <label>Reset token</label>
      <input value={token} onChange={(e) => setToken(e.target.value)} required />
      <label>New password</label>
      <PasswordField value={password} onChange={setPassword} autoComplete="new-password" />
      {message && <p className="banner">{message} <Link to="/login">Sign in</Link></p>}
      {error && <p className="error">{error}</p>}
      <button className="btn" style={{ marginTop: 16 }} type="submit">Update password</button>
    </form>
  );
}
