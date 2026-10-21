const TOKEN_KEY = "proofora.token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem("proofora.user", JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem("proofora.user");
}

export function getUser() {
  const raw = localStorage.getItem("proofora.user");
  return raw ? JSON.parse(raw) : null;
}

export async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.body && !(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  let res;
  try {
    res = await fetch(path, { ...options, headers });
  } catch {
    throw new Error("Cannot reach the API. The backend must be running on port 8080.");
  }
  if (res.status === 204) return null;
  const raw = await res.text();
  let data = {};
  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = { message: raw };
    }
  }
  if (!res.ok) {
    throw rejectApi(path, res.status, data);
  }
  return data;
}

export function rejectApi(path, status, data) {
  const message = (data && data.message) || (status === 401 || status === 403
    ? "Sign in to continue."
    : "Request failed");
  if (!String(path).startsWith("/api/auth/") && (status === 401 || message === "Sign in to continue.")) {
    clearSession();
    sessionStorage.setItem("proofora.notice", "Your session expired. Sign in again.");
    window.dispatchEvent(new Event("proofora-unauthorized"));
  }
  return new Error(message);
}
