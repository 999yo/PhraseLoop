import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api/client";
import type { Phrase, PhraseCategory } from "../types";

type Phase = "answer" | "revealed";
type QuestionCount = 10 | 20 | 30 | "all";

const COUNTS: QuestionCount[] = [10, 20, 30, "all"];

function categoryKey(name: string | null): string {
  return name ?? "";
}

export function ReviewPage() {
  const [categories, setCategories] = useState<PhraseCategory[] | null>(null);
  const [allCategories, setAllCategories] = useState(true);
  const [selected, setSelected] = useState<string[]>([]);
  const [questionCount, setQuestionCount] = useState<QuestionCount>(10);
  const [phrases, setPhrases] = useState<Phrase[] | null>(null);
  const [index, setIndex] = useState(0);
  const [done, setDone] = useState(false);
  const [phase, setPhase] = useState<Phase>("answer");
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api<PhraseCategory[]>("/api/phrases/categories")
      .then((data) => {
        if (active) setCategories(data);
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

  const totalCount = categories?.reduce((sum, item) => sum + item.phraseCount, 0) ?? 0;
  const selectedCount = allCategories
    ? totalCount
    : (categories ?? [])
        .filter((item) => selected.includes(categoryKey(item.name)))
        .reduce((sum, item) => sum + item.phraseCount, 0);
  const questionTotal = questionCount === "all" ? selectedCount : Math.min(questionCount, selectedCount);

  function toggleAll() {
    setAllCategories(true);
    setSelected([]);
  }

  function toggleCategory(key: string) {
    setSelected((current) => {
      const next = current.includes(key) ? current.filter((item) => item !== key) : [...current, key];
      setAllCategories(next.length === 0);
      return next;
    });
  }

  async function startReview() {
    setStarting(true);
    setError("");
    const named = selected.filter((key) => key !== "");
    try {
      const result = await api<Phrase[]>("/api/reviews/session", {
        method: "POST",
        body: JSON.stringify({
          all: allCategories,
          categories: named,
          includeUncategorized: !allCategories && selected.includes(""),
          questionCount: questionCount === "all" ? null : questionCount,
        }),
      });
      setPhrases(result);
      setIndex(0);
      setDraft("");
      setPhase("answer");
      setDone(result.length === 0);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "通信に失敗しました");
    } finally {
      setStarting(false);
    }
  }

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
  if (error && !categories && !phrases) {
    return (
      <div className="status-block">
        <p role="alert">{error}</p>
        <button type="button" className="primary" onClick={() => window.location.reload()}>
          再読み込み
        </button>
      </div>
    );
  }
  if (!phrases) {
    return (
      <section className="review-setup">
        <h1>復習の設定</h1>
        <h2>出題するカテゴリ</h2>
        <ul className="choice-list">
          <li>
            <label>
              <input type="checkbox" checked={allCategories} onChange={toggleAll} />
              すべて
              <span className="choice-count">{totalCount}問</span>
            </label>
          </li>
          {categories?.map((item) => {
            const key = categoryKey(item.name);
            return (
              <li key={key || "uncategorized"}>
                <label>
                  <input
                    type="checkbox"
                    checked={!allCategories && selected.includes(key)}
                    onChange={() => toggleCategory(key)}
                  />
                  {item.name ?? "未分類"}
                  <span className="choice-count">{item.phraseCount}問</span>
                </label>
              </li>
            );
          })}
        </ul>
        <h2>出題数</h2>
        <ul className="choice-list">
          {COUNTS.map((count) => (
            <li key={String(count)}>
              <label>
                <input
                  type="radio"
                  name="question-count"
                  checked={questionCount === count}
                  onChange={() => setQuestionCount(count)}
                />
                {count === "all" ? "全問" : `${count}問`}
              </label>
            </li>
          ))}
        </ul>
        <p className="import-note">
          {questionTotal === 0 ? "この条件のフレーズはありません。" : `この条件では ${questionTotal}問出題します`}
        </p>
        {error ? (
          <p className="form-message" role="alert">
            {error}
          </p>
        ) : null}
        <button type="button" className="primary" disabled={starting || questionTotal === 0} onClick={() => void startReview()}>
          復習を始める
        </button>
      </section>
    );
  }
  if (phrases.length === 0 || (done && phrases.length === 0)) {
    return (
      <div className="status-block">
        <h1>この条件のフレーズはありません。</h1>
        <button type="button" className="primary" onClick={() => setPhrases(null)}>
          設定に戻る
        </button>
      </div>
    );
  }
  if (done) {
    return (
      <div className="status-block">
        <div className="review-finish">
          <p className="progress">
            {phrases.length} / {phrases.length}
          </p>
          <h1>今日の {phrases.length} 問が終わりました。</h1>
        </div>
        <div className="row-actions finish-actions">
          <Link className="primary button-link" to="/">
            ダッシュボードへ
          </Link>
          <button
            type="button"
            className="secondary"
            onClick={() => {
              setPhrases(null);
              setDone(false);
              setIndex(0);
              setDraft("");
              setPhase("answer");
            }}
          >
            設定に戻る
          </button>
        </div>
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
