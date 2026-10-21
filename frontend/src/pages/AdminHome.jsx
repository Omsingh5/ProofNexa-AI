import { useEffect, useState } from "react";
import { api } from "../api.js";
import AccountSecurity from "../components/AccountSecurity.jsx";

export default function AdminHome() {
  const [users, setUsers] = useState({ content: [] });
  const [audit, setAudit] = useState({ content: [] });
  const [analytics, setAnalytics] = useState(null);

  useEffect(() => {
    api("/api/admin/users").then(setUsers);
    api("/api/admin/audit").then(setAudit);
    api("/api/ai/analytics").then(setAnalytics);
  }, []);

  return (
    <div>
      <h2>Admin control</h2>
      {analytics && <div className="banner">{analytics.narrative}</div>}
      <h3>Users</h3>
      <table className="table">
        <thead><tr><th>Name</th><th>Email</th><th>Role</th></tr></thead>
        <tbody>
          {users.content.map((u) => (
            <tr key={u.id}><td>{u.fullName}</td><td>{u.email}</td><td>{u.role}</td></tr>
          ))}
        </tbody>
      </table>
      <h3>Audit log</h3>
      <table className="table">
        <thead><tr><th>Action</th><th>Entity</th><th>Detail</th></tr></thead>
        <tbody>
          {audit.content.map((a) => (
            <tr key={a.id}><td>{a.action}</td><td>{a.entityType} #{a.entityId}</td><td>{a.detail}</td></tr>
          ))}
        </tbody>
      </table>
      <AccountSecurity />
    </div>
  );
}
