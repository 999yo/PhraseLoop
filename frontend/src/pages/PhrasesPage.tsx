import { useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import { FormField } from "../components/FormField";
import type { Phrase, PhraseCategory, PhraseImportResult, PhraseInput } from "../types";

const emptyForm: PhraseInput = {
  englishText: "",
  japaneseText: "",
  exampleSentence: "",
  category: "",
};

function formatDate(iso: string): string {
  const [year, month, day] = iso.split("-");
  return `${year}/${Number(month)}/${Number(day)}`;
}

function validate(form: PhraseInput): Record<string, string> {
  const fields: Record<string, string> = {};
  if (!form.englishText.trim()) fields.englishText = "英語フレーズを入力してください";
  else if (form.englishText.trim().length > 500) fields.englishText = "英語フレーズは500文字以内で入力してください";
  if (!form.japaneseText.trim()) fields.japaneseText = "日本語訳を入力してください";
  else if (form.japaneseText.trim().length > 500) fields.japaneseText = "日本語訳は500文字以内で入力してください";
  if (form.exampleSentence.trim().length > 1000) fields.exampleSentence = "例文は1000文字以内で入力してください";
  if (form.category.trim().length > 100) fields.category = "カテゴリは100文字以内で入力してください";
  return fields;
}

function categoryLabel(name: string | null): string {
  return name ?? "未分類";
}

export function PhrasesPage() {
  const [phrases, setPhrases] = useState<Phrase[] | null>(null);
  const [categories, setCategories] = useState<PhraseCategory[]>([]);
  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [form, setForm] = useState<PhraseInput>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [loadError, setLoadError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [csvInputKey, setCsvInputKey] = useState(0);
  const [preview, setPreview] = useState<PhraseImportResult | null>(null);
  const [csvMessage, setCsvMessage] = useState("");
  const [csvFailed, setCsvFailed] = useState(false);
  const [csvBusy, setCsvBusy] = useState(false);

  async function loadCategories() {
    setCategories(await api<PhraseCategory[]>("/api/phrases/categories"));
  }

  async function load(search = query, filter = categoryFilter) {
    setLoading(true);
    setLoadError("");
    try {
      const params = new URLSearchParams();
      if (search.trim()) params.set("q", search.trim());
      if (filter === "uncategorized") params.set("uncategorized", "true");
      else if (filter !== "all") params.set("category", filter);
      const path = params.size > 0 ? `/api/phrases?${params.toString()}` : "/api/phrases";
      setPhrases(await api<Phrase[]>(path));
    } catch (error) {
      setLoadError(error instanceof ApiError ? error.message : "通信に失敗しました");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load("", "all");
    void loadCategories().catch(() => setLoadError("カテゴリを読み込めませんでした"));
  }, []);

  function updateField(key: keyof PhraseInput, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function startEdit(phrase: Phrase) {
    setEditingId(phrase.id);
    setConfirmingId(null);
    setFields({});
    setMessage("");
    setForm({
      englishText: phrase.englishText,
      japaneseText: phrase.japaneseText,
      exampleSentence: phrase.exampleSentence ?? "",
      category: phrase.category ?? "",
    });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyForm);
    setFields({});
    setMessage("");
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    const nextFields = validate(form);
    setFields(nextFields);
    setMessage("");
    if (Object.keys(nextFields).length > 0) return;
    setSaving(true);
    const body = JSON.stringify(form);
    try {
      if (editingId === null) {
        await api<Phrase>("/api/phrases", { method: "POST", body });
        setForm(emptyForm);
      } else {
        await api<Phrase>(`/api/phrases/${editingId}`, { method: "PUT", body });
        setEditingId(null);
        setForm(emptyForm);
      }
      await load();
      await loadCategories();
    } catch (error) {
      if (error instanceof ApiError) {
        setFields(error.fields);
        setMessage(error.message);
      } else {
        setMessage("通信に失敗しました");
      }
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: number) {
    setMessage("");
    try {
      await api<void>(`/api/phrases/${id}`, { method: "DELETE" });
      if (editingId === id) cancelEdit();
      setConfirmingId(null);
      await load();
      await loadCategories();
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "通信に失敗しました");
    }
  }

  async function previewCsv() {
    if (!csvFile) {
      setCsvFailed(true);
      setCsvMessage("ファイルを選んでください");
      setPreview(null);
      return;
    }
    setCsvBusy(true);
    setCsvFailed(false);
    setCsvMessage("");
    const body = new FormData();
    body.append("file", csvFile);
    try {
      setPreview(await api<PhraseImportResult>("/api/phrases/import/preview", { method: "POST", body }));
    } catch (error) {
      setCsvFailed(true);
      setPreview(null);
      setCsvMessage(error instanceof ApiError ? error.message : "通信に失敗しました");
    } finally {
      setCsvBusy(false);
    }
  }

  async function importCsv() {
    if (!csvFile) return;
    setCsvBusy(true);
    setCsvFailed(false);
    setCsvMessage("");
    const body = new FormData();
    body.append("file", csvFile);
    try {
      const result = await api<PhraseImportResult>("/api/phrases/import", { method: "POST", body });
      setPreview(null);
      setCsvFile(null);
      setCsvInputKey((current) => current + 1);
      setCsvFailed(false);
      setCsvMessage(`${result.savedCount}件登録しました`);
      await load();
      await loadCategories();
    } catch (error) {
      setCsvFailed(true);
      setCsvMessage(error instanceof ApiError ? error.message : "通信に失敗しました");
    } finally {
      setCsvBusy(false);
    }
  }

  const previewHasError = preview?.rows.some((row) => row.error) ?? false;

  return (
    <section className="page phrase-layout">
      <div>
        <h1>フレーズ</h1>
        <form
          className="search-form"
          onSubmit={(event) => {
            event.preventDefault();
            void load();
          }}
        >
          <input
            type="search"
            placeholder="キーワード検索"
            aria-label="キーワード検索"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button type="submit" className="secondary">
            検索
          </button>
        </form>
        <div className="filters">
          <label>
            カテゴリ
            <select
              aria-label="カテゴリ"
              value={categoryFilter}
              onChange={(event) => {
                const next = event.target.value;
                setCategoryFilter(next);
                void load(query, next);
              }}
            >
              <option value="all">すべて</option>
              {categories.map((item) => (
                <option key={item.name ?? "uncategorized"} value={item.name ?? "uncategorized"}>
                  {categoryLabel(item.name)}（{item.phraseCount}）
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <p className="status-line">読み込んでいます</p> : null}
        {loadError ? (
          <p className="form-message" role="alert">
            {loadError}
          </p>
        ) : null}
        {!loading && phrases && phrases.length === 0 ? (
          <p className="empty-copy">フレーズがまだありません。フォームから登録できます。</p>
        ) : null}
        <div className="phrase-list">
          {phrases?.map((phrase) => (
            <article className="phrase-card" key={phrase.id}>
              <h2>{phrase.japaneseText}</h2>
              <p className="english">{phrase.englishText}</p>
              <p className="meta">
                {phrase.category ? <span>{phrase.category}</span> : null}
                <span>次回復習 {formatDate(phrase.nextReviewDate)}</span>
              </p>
              <div className="row-actions">
                <button type="button" className="text-button" onClick={() => startEdit(phrase)}>
                  編集
                </button>
                <button type="button" className="text-button danger" onClick={() => setConfirmingId(phrase.id)}>
                  削除
                </button>
              </div>
              {confirmingId === phrase.id ? (
                <div className="confirm">
                  <p>このフレーズを削除しますか</p>
                  <button type="button" className="danger-button" onClick={() => void remove(phrase.id)}>
                    削除する
                  </button>
                  <button type="button" className="secondary" onClick={() => setConfirmingId(null)}>
                    キャンセル
                  </button>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      </div>

      <div>
        <form className="phrase-form" onSubmit={onSubmit}>
          <h2>{editingId === null ? "新しいフレーズ" : "フレーズを編集"}</h2>
          <FormField label="英語フレーズ" error={fields.englishText}>
            <textarea value={form.englishText} onChange={(event) => updateField("englishText", event.target.value)} rows={3} />
          </FormField>
          <FormField label="日本語訳" error={fields.japaneseText}>
            <textarea value={form.japaneseText} onChange={(event) => updateField("japaneseText", event.target.value)} rows={3} />
          </FormField>
          <FormField label="例文（任意）" error={fields.exampleSentence}>
            <textarea
              value={form.exampleSentence}
              onChange={(event) => updateField("exampleSentence", event.target.value)}
              rows={3}
            />
          </FormField>
          <FormField label="カテゴリ（任意）" error={fields.category}>
            <input value={form.category} onChange={(event) => updateField("category", event.target.value)} />
          </FormField>
          {message ? (
            <p className="form-message" role="alert">
              {message}
            </p>
          ) : null}
          <div className="row-actions">
            <button className="primary" type="submit" disabled={saving}>
              {saving ? "保存しています" : editingId === null ? "登録する" : "更新する"}
            </button>
            {editingId !== null ? (
              <button type="button" className="secondary" onClick={cancelEdit}>
                キャンセル
              </button>
            ) : null}
          </div>
        </form>

        <div className="import-block">
            <h2>CSVでまとめて登録</h2>
            <p className="import-note">ヘッダーは japanese, english, category です。</p>
            <input
              key={csvInputKey}
              type="file"
              accept=".csv,text/csv"
              aria-label="CSVファイル"
              onChange={(event) => {
                setCsvFile(event.target.files?.[0] ?? null);
                setPreview(null);
                setCsvFailed(false);
                setCsvMessage("");
              }}
            />
            <div className="row-actions">
              <button type="button" className="secondary" disabled={csvBusy} onClick={() => void previewCsv()}>
                プレビュー
              </button>
            </div>
            {csvMessage ? (
              <p className={csvFailed ? "form-message" : "import-note"} role={csvFailed ? "alert" : "status"}>
                {csvMessage}
              </p>
            ) : null}
            {preview ? (
              <>
                <p className="import-note">プレビュー {preview.rows.length}件</p>
                <table className="preview-table">
                  <thead>
                    <tr>
                      <th>日本語</th>
                      <th>英語</th>
                      <th>カテゴリ</th>
                    </tr>
                  </thead>
                  <tbody>
                    {preview.rows.map((row) => (
                      <tr key={row.line}>
                        <td>{row.japanese || "（空欄）"}</td>
                        <td>{row.english || "（空欄）"}</td>
                        <td>
                          {row.category || "未分類"}
                          {row.error ? <div className="row-error">{row.line}行目: {row.error}</div> : null}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {previewHasError ? null : (
                  <button type="button" className="primary" disabled={csvBusy} onClick={() => void importCsv()}>
                    この内容で登録
                  </button>
                )}
              </>
            ) : null}
        </div>
      </div>
    </section>
  );
}
