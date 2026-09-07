import { createContext, useContext, useMemo, useState } from "react";
import * as authApi from "../api/auth";
import { TOKEN_KEY } from "../api/client";

const USER_KEY = "smartinvoice.user";

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);

  const persist = (session) => {
    const { token, ...profile } = session;
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(profile));
    setUser(profile);
  };

  const login = async (email, password) => {
    const session = await authApi.login(email, password);
    persist(session);
    return session;
  };

  const register = async (fullName, email, password) => {
    await authApi.register(fullName, email, password);
    return login(email, password);
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(user), login, register, logout }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
