import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { FormField } from "../components/FormField";
import type { User } from "../types";

export function LoginPage() {
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    const nextFields: Record<string, string> = {};
    if (!email.trim()) nextFields.email = "メールアドレスを入力してください";
    if (!password) nextFields.password = "パスワードを入力してください";
    setFields(nextFields);
    setMessage("");
    if (Object.keys(nextFields).length > 0) return;
    setSubmitting(true);
    try {
      const user = await api<User>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setUser(user);
      navigate("/");
    } catch (error) {
      if (error instanceof ApiError) {
        setFields(error.fields);
        setMessage(error.message);
      } else {
        setMessage("通信に失敗しました");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={onSubmit}>
        <p className="brand-mark">PhraseLoop</p>
        <h1>ログイン</h1>
        <FormField label="メールアドレス" error={fields.email}>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </FormField>
        <FormField label="パスワード" error={fields.password}>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </FormField>
        {message ? (
          <p className="form-message" role="alert">
            {message}
          </p>
        ) : null}
        <button className="primary" type="submit" disabled={submitting}>
          {submitting ? "ログインしています" : "ログイン"}
        </button>
        <p className="auth-link">
          アカウントをお持ちでない方は <Link to="/register">新規登録</Link>
        </p>
      </form>
    </div>
  );
}
