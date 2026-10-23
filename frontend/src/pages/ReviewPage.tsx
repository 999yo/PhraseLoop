import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api/client";
import type { Phrase } from "../types";

type Phase = "answer" | "revealed";

export function ReviewPage() {
  const [phrases, setPhrases] = useState<Phrase[] | null>(null);
  const [index, setIndex] = useState(0);
  const [done, setDone] = useState(false);
  const [phase, setPhase] = useState<Phase>("answer");
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api<Phrase[]>("/api/reviews/today")
      .then((data) => {
        if (!active) return;
        setPhrases(data);
        setDone(data.length === 0);
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

  async function grade(remembered: boolean) {
    if (!phrases) return;
    const current = phrases[index];
    setSaving(true);
    setError("");
    try {
      await api(`/api/reviews/${current.id}/answer`, {
        method: "POST",
        body: JSON.stringify({ remembered }),
      });
      if (index + 1 >= phrases.length) {
        setDone(true);
      } else {
        setIndex(index + 1);
        setPhase("answer");
        setDraft("");
      }
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "通信に失敗しました");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="status-line">読み込んでいます</p>;
  if (error && !phrases) {
    return (
      <div className="status-block">
        <p role="alert">{error}</p>
        <button type="button" className="primary" onClick={() => window.location.reload()}>
          再読み込み
        </button>
      </div>
    );
  }
  if (!phrases || phrases.length === 0) {
    return (
      <div className="status-block">
        <h1>今日の復習はありません。</h1>
        <p>次回復習日が来ると、ここに表示されます。</p>
        <div className="row-actions">
          <Link className="primary button-link" to="/phrases">
            フレーズを登録
          </Link>
          <Link className="secondary button-link" to="/">
            ダッシュボードへ
          </Link>
        </div>
      </div>
    );
  }
  if (done) {
    return (
      <div className="status-block review-finish">
        <p className="progress">
          {phrases.length} / {phrases.length}
        </p>
        <h1>今日の {phrases.length} 問が終わりました。</h1>
        <Link className="primary button-link" to="/">
          ダッシュボードへ
        </Link>
      </div>
    );
  }

  const current = phrases[index];
  const ownAnswer = draft.trim() ? draft.trim() : "（未入力）";

  return (
    <section className="review">
      <div className="review-top">
        <h1>復習</h1>
        <p className="progress">
          {index + 1} / {phrases.length}
        </p>
      </div>
      {current.category ? <p className="category">{current.category}</p> : null}
      <p className="prompt">{current.japaneseText}</p>

      {phase === "answer" ? (
        <div className="answer-entry">
          <label htmlFor="answer">あなたの回答</label>
          <textarea
            id="answer"
            rows={4}
            value={draft}
            autoComplete="off"
            onChange={(event) => setDraft(event.target.value)}
          />
          <button type="button" className="primary" onClick={() => setPhase("revealed")}>
            模範解答を表示
          </button>
        </div>
      ) : (
        <div className="revealed">
          <div className="compare">
            <section>
              <h2>あなたの回答</h2>
              <p>{ownAnswer}</p>
            </section>
            <section>
              <h2>模範解答</h2>
              <p>{current.englishText}</p>
            </section>
          </div>
          {current.exampleSentence ? (
            <p className="example">
              <span>例文</span>
              {current.exampleSentence}
            </p>
          ) : null}
          {error ? (
            <p className="form-message" role="alert">
              {error}
            </p>
          ) : null}
          <div className="grade-actions">
            <button type="button" className="primary" disabled={saving} onClick={() => void grade(true)}>
              覚えた
            </button>
            <button type="button" className="secondary" disabled={saving} onClick={() => void grade(false)}>
              まだ覚えていない
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
