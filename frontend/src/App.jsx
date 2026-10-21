import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";
import Landing from "./pages/Landing.jsx";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import ForgotPassword from "./pages/ForgotPassword.jsx";
import ResetPassword from "./pages/ResetPassword.jsx";
import Jobs from "./pages/Jobs.jsx";
import JobDetail from "./pages/JobDetail.jsx";
import CandidateHome from "./pages/CandidateHome.jsx";
import RecruiterHome from "./pages/RecruiterHome.jsx";
import AdminHome from "./pages/AdminHome.jsx";
import NexaChat from "./components/NexaChat.jsx";

function Guard({ roles, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [notice, setNotice] = useState(() => sessionStorage.getItem("proofora.notice") || "");

  useEffect(() => {
    function onExpired() {
      setNotice(sessionStorage.getItem("proofora.notice") || "Your session expired. Sign in again.");
    }
    window.addEventListener("proofora-unauthorized", onExpired);
    return () => window.removeEventListener("proofora-unauthorized", onExpired);
  }, []);

  useEffect(() => {
    if (!user) return;
    sessionStorage.removeItem("proofora.notice");
    setNotice("");
  }, [user]);

  function dismissNotice() {
    sessionStorage.removeItem("proofora.notice");
    setNotice("");
  }

  return (
    <div className="shell">
      <header className="nav">
        <NavLink to="/" className="brand" aria-label="ProofNexa AI">
          <img className="logo" src="/logo.png?v=2" alt="ProofNexa AI" />
        </NavLink>
        <nav className="nav-links">
          <NavLink to="/jobs">Jobs</NavLink>
          {user?.role === "CANDIDATE" && <NavLink to="/app">Workspace</NavLink>}
          {user?.role === "RECRUITER" && <NavLink to="/hire">Hiring</NavLink>}
          {user?.role === "ADMIN" && <NavLink to="/admin">Admin</NavLink>}
          {user ? (
            <>
              <span className="muted">{user.fullName}</span>
              <button className="btn ghost small" onClick={() => { logout(); nav("/"); }}>Sign out</button>
            </>
          ) : (
            <>
              <NavLink to="/login">Sign in</NavLink>
              <NavLink to="/register"><button className="btn small">Create account</button></NavLink>
            </>
          )}
        </nav>
      </header>
      <main className="page">
      {notice && (
        <p className="banner">
          {notice}{" "}
          <button className="btn ghost small" type="button" onClick={dismissNotice}>Dismiss</button>
        </p>
      )}
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/jobs" element={<Jobs />} />
        <Route path="/jobs/:id" element={<JobDetail />} />
        <Route path="/app/*" element={<Guard roles={["CANDIDATE"]}><CandidateHome /></Guard>} />
        <Route path="/hire/*" element={<Guard roles={["RECRUITER", "ADMIN"]}><RecruiterHome /></Guard>} />
        <Route path="/admin/*" element={<Guard roles={["ADMIN"]}><AdminHome /></Guard>} />
      </Routes>
      </main>
      {user && <NexaChat role={user.role} />}
    </div>
  );
}
