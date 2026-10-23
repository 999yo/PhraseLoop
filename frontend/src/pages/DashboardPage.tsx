import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api/client";
import type { Dashboard } from "../types";

const WEEKDAYS = ["日", "月", "火", "水", "木", "金", "土"];

function streakNote(dashboard: Dashboard): string {
  if (dashboard.streak.days === 0) return "復習すると連続記録が始まります";
  if (dashboard.streak.todayReviewed) return "今日の復習は完了しています";
  return `今日復習すると${dashboard.streak.days + 1}日連続になります`;
}

function buildWeeks(year: number, month: number): Array<Array<number | null>> {
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  const startPad = new Date(Date.UTC(year, month - 1, 1)).getUTCDay();
  const cells: Array<number | null> = Array.from({ length: startPad }, () => null);
  for (let day = 1; day <= daysInMonth; day += 1) cells.push(day);
  while (cells.length % 7 !== 0) cells.push(null);
  const weeks: Array<Array<number | null>> = [];
  for (let index = 0; index < cells.length; index += 7) {
    weeks.push(cells.slice(index, index + 7));
  }
  return weeks;
}

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api<Dashboard>("/api/dashboard")
      .then((data) => {
        if (active) setDashboard(data);
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof ApiError ? reason.message : "通信に失敗しました");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <p className="status-line">読み込んでいます</p>;
  if (error || !dashboard) {
    return (
      <div className="status-block">
        <p role="alert">{error || "読み込めませんでした"}</p>
        <button type="button" className="primary" onClick={() => window.location.reload()}>
          再読み込み
        </button>
      </div>
    );
  }

  const todayParts = dashboard.today.split("-").map(Number);
  const todayDay = todayParts[2];
  const reviewed = new Set(dashboard.calendar.reviewedDates);
  const weeks = buildWeeks(dashboard.calendar.year, dashboard.calendar.month);

  return (
    <section className="page">
      <div className="stats">
        <article>
          <span>今日の復習</span>
          <strong>{dashboard.dueTodayCount}</strong>
        </article>
        <article>
          <span>登録フレーズ</span>
          <strong>{dashboard.totalPhraseCount}</strong>
        </article>
        <article>
          <span>習得済み</span>
          <strong>{dashboard.masteredCount}</strong>
        </article>
      </div>

      <div className="streak-panel">
        <div className="streak-summary">
          <p className="streak-count">{dashboard.streak.days}</p>
          <p className="streak-label">日連続</p>
          <p className="streak-note">{streakNote(dashboard)}</p>
        </div>
        <div className="calendar">
          <h2>
            {dashboard.calendar.year}年{dashboard.calendar.month}月
          </h2>
          <table aria-label={`${dashboard.calendar.year}年${dashboard.calendar.month}月の復習`}>
            <thead>
              <tr>
                {WEEKDAYS.map((day) => (
                  <th key={day} scope="col">
                    {day}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {weeks.map((week, weekIndex) => (
                <tr key={weekIndex}>
                  {week.map((day, dayIndex) => {
                    if (day === null) return <td key={dayIndex} />;
                    const iso = `${dashboard.calendar.year}-${String(dashboard.calendar.month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
                    const isToday = day === todayDay;
                    const didReview = reviewed.has(iso);
                    const isFuture = day > todayDay;
                    return (
                      <td key={dayIndex}>
                        <span className={isToday ? "day today" : isFuture ? "day future" : "day"}>{day}</span>
                        {didReview ? <span className="dot" aria-label="復習した日" /> : <span className="dot spacer" />}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
          <p className="legend">
            <span className="dot" aria-hidden="true" /> 復習した日
            <span className="today-sample" aria-hidden="true">
              {todayDay}
            </span>
            今日{dashboard.streak.todayReviewed ? "、完了" : "、まだ"}
          </p>
        </div>
      </div>

      <div className="center-action">
        <Link className="primary button-link" to="/review">
          復習を始める
        </Link>
      </div>
    </section>
  );
}
