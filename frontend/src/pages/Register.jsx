import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import PasswordField from "../components/PasswordField.jsx";
import { PASSWORD_HINT, passwordError } from "../utils/passwordRules.js";

export default function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState({ email: "", password: "", fullName: "", role: "CANDIDATE", companyName: "" });
  const [error, setError] = useState("");

  function set(k, v) { setForm({ ...form, [k]: v }); }

  async function onSubmit(e) {
    e.preventDefault();
    const problem = passwordError(form.password);
    if (problem) {
      setError(problem);
      return;
    }
    try {
      const user = await register(form);
      nav(user.role === "RECRUITER" ? "/hire" : "/app");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="form card" onSubmit={onSubmit}>
      <p className="kicker">New account</p>
      <h2>Create your workspace</h2>
      <label>Full name</label>
      <input value={form.fullName} onChange={(e) => set("fullName", e.target.value)} required />
      <label>Email</label>
      <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)} required />
      <label>Password</label>
      <PasswordField value={form.password} onChange={(value) => set("password", value)} autoComplete="new-password" />
      <p className="muted">{PASSWORD_HINT}</p>
      <label>Role</label>
      <select value={form.role} onChange={(e) => set("role", e.target.value)}>
        <option value="CANDIDATE">Candidate</option>
        <option value="RECRUITER">Recruiter</option>
      </select>
      {form.role === "RECRUITER" && (
        <>
          <label>Company</label>
          <input value={form.companyName} onChange={(e) => set("companyName", e.target.value)} />
        </>
      )}
      {error && <p className="error">{error}</p>}
      <button className="btn" style={{ marginTop: 16 }} type="submit">Create account</button>
    </form>
  );
}
