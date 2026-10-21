import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import PasswordField from "../components/PasswordField.jsx";

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState("alex@proofora.dev");
  const [password, setPassword] = useState("Password123!");
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    try {
      const user = await login(email, password);
      if (user.role === "RECRUITER") nav("/hire");
      else if (user.role === "ADMIN") nav("/admin");
      else nav("/app");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="form card" onSubmit={onSubmit}>
      <p className="kicker">Welcome back</p>
      <h2>Sign in</h2>
      <label>Email</label>
      <input value={email} onChange={(e) => setEmail(e.target.value)} />
      <label>Password</label>
      <PasswordField value={password} onChange={setPassword} />
      <p><Link to="/forgot-password">Forgot password</Link></p>
      {error && <p className="error">{error}</p>}
      <button className="btn" style={{ marginTop: 16 }} type="submit">Sign in</button>
    </form>
  );
}
