import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { FormField } from "../components/FormField";
import type { User } from "../types";

export function RegisterPage() {
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    const nextFields: Record<string, string> = {};
    if (!email.trim()) nextFields.email = "メールアドレスを入力してください";
    else if (!email.includes("@")) nextFields.email = "メールアドレスの形式が正しくありません";
    if (password.length < 8 || password.length > 72) {
      nextFields.password = "パスワードは8文字以上72文字以内で入力してください";
    }
    if (!passwordConfirm) nextFields.passwordConfirm = "確認用パスワードを入力してください";
    else if (password !== passwordConfirm) nextFields.passwordConfirm = "パスワードが一致しません";
    setFields(nextFields);
    setMessage("");
    if (Object.keys(nextFields).length > 0) return;
    setSubmitting(true);
    try {
      const user = await api<User>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, passwordConfirm }),
      });
      setUser(user);
      navigate("/");
    } catch (error) {
      if (error instanceof ApiError) {
        setFields(error.fields);
        if (!error.fields.email) setMessage(error.message);
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
        <h1>新規登録</h1>
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
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </FormField>
        <FormField label="パスワード確認" error={fields.passwordConfirm}>
          <input
            type="password"
            autoComplete="new-password"
            value={passwordConfirm}
            onChange={(event) => setPasswordConfirm(event.target.value)}
          />
        </FormField>
        {message ? (
          <p className="form-message" role="alert">
            {message}
          </p>
        ) : null}
        <button className="primary" type="submit" disabled={submitting}>
          {submitting ? "登録しています" : "登録する"}
        </button>
        <p className="auth-link">
          すでにアカウントをお持ちの方は <Link to="/login">ログイン</Link>
        </p>
      </form>
    </div>
  );
}
