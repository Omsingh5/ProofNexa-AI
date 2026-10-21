import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, clearSession, getUser, setSession } from "./api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getUser());

  useEffect(() => {
    function onExpired() {
      clearSession();
      setUser(null);
    }
    window.addEventListener("proofora-unauthorized", onExpired);
    return () => window.removeEventListener("proofora-unauthorized", onExpired);
  }, []);

  const value = useMemo(() => ({
    user,
    async login(email, password) {
      const data = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password })
      });
      setSession(data.token, data.user);
      setUser(data.user);
      return data.user;
    },
    async register(payload) {
      const data = await api("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      setSession(data.token, data.user);
      setUser(data.user);
      return data.user;
    },
    async logout() {
      try {
        await api("/api/auth/logout", { method: "POST" });
      } catch {
        // Local session is cleared even if the token is already expired.
      }
      clearSession();
      setUser(null);
    }
  }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
