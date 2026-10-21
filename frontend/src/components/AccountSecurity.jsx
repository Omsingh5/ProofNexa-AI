import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import PasswordField from "./PasswordField.jsx";

export default function AccountSecurity() {
  const { logout } = useAuth();
  const nav = useNavigate();
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function deactivate(e) {
    e.preventDefault();
    if (!window.confirm("Deactivate this account? Sign-in will stop immediately.")) return;
    setError("");
    try {
      await api("/api/auth/deactivate", { method: "POST", body: JSON.stringify({ password }) });
      await logout();
      nav("/");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="card" onSubmit={deactivate} style={{ marginTop: 16 }}>
      <h3>Account</h3>
      <p className="muted">Deactivation blocks sign-in. Applications and profile rows stay in the database until a deletion workflow exists.</p>
      <label>Confirm with your password</label>
      <PasswordField value={password} onChange={setPassword} />
      {error && <p className="error">{error}</p>}
      <button className="btn ghost" type="submit" style={{ marginTop: 12 }}>Deactivate account</button>
    </form>
  );
}
