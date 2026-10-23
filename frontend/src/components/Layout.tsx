import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { email, logout } = useAuth();
  const navigate = useNavigate();

  async function onLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <NavLink className="brand" to="/">
          PhraseLoop
        </NavLink>
        <nav className="nav" aria-label="主要">
          <NavLink to="/" end>
            ダッシュボード
          </NavLink>
          <NavLink to="/phrases">フレーズ</NavLink>
          <NavLink to="/review">復習</NavLink>
        </nav>
        <div className="account">
          <span className="email">{email}</span>
          <button type="button" className="text-button" onClick={onLogout}>
            ログアウト
          </button>
        </div>
      </header>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
