import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, ensureCsrf } from "../api/client";
import type { User } from "../types";

type AuthStatus = "loading" | "anonymous" | "authenticated";

type AuthContextValue = {
  status: AuthStatus;
  email: string;
  setUser: (user: User) => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [email, setEmail] = useState("");

  useEffect(() => {
    let active = true;
    ensureCsrf()
      .then(() => api<User>("/api/auth/me"))
      .then((user) => {
        if (!active) return;
        setEmail(user.email);
        setStatus("authenticated");
      })
      .catch(() => {
        if (active) setStatus("anonymous");
      });
    return () => {
      active = false;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      email,
      setUser: (user) => {
        setEmail(user.email);
        setStatus("authenticated");
      },
      logout: async () => {
        await api<void>("/api/auth/logout", { method: "POST" });
        setEmail("");
        setStatus("anonymous");
      },
    }),
    [status, email],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("AuthProvider が必要です");
  }
  return value;
}
