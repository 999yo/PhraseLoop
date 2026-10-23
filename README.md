# PhraseLoop

日本語訳を見て英語フレーズを思い出すための復習アプリです。登録したフレーズは、覚えたかどうかの自己評価に応じて 1 日後・3 日後・7 日後に再び出題されます。

## 開発背景

単語帳に書いたフレーズは、見返す日を自分で決めないと埋もれます。PhraseLoop は「今日やる分」だけを出し、思い出せたかどうかを本人が判断して次の予定日を決めます。英語表現には別解があるため、入力した英文は自動採点しません。

## 機能

- メールアドレスとパスワードでの新規登録、ログイン、ログアウト
- フレーズの登録、編集、削除、キーワード検索
- 日本語を問題にした復習。英語を入力し、模範解答を見てから「覚えた」「まだ覚えていない」を選ぶ
- 自己評価に応じた次回復習日の更新と、回答履歴の保存
- ダッシュボードに、今日の件数、登録数、習得済み数、連続日数、当月の復習カレンダー

入力した英文は保存しません。比較は送信前の画面だけで行い、サーバーが受け取るのは自己評価だけです。

## 使用技術

- Java 21
- Spring Boot 4.1.2（Spring Security、Spring Data JPA、Validation、Flyway、Actuator）
- PostgreSQL 16
- React 19、TypeScript、Vite 7
- JUnit 5、MockMvc
- Maven、Docker

## システム構成

本番は 1 つの Spring Boot が API とビルド済みの React を同じオリジンで配信します。ホスティングは Render の無料プランで、データベースは Render の無料 PostgreSQL です。

```text
ブラウザ
  └─ Spring Boot（静的ファイル + /api）
       └─ PostgreSQL
```

開発中は Vite が `http://localhost:5173` で画面を出し、`/api` を `http://localhost:8080` へ転送します。セッション Cookie はブラウザから見て Vite のホストに付きます。

認証はサーバーセッションです。Cookie は HttpOnly、SameSite=Lax、本番では Secure です。更新系リクエストは CSRF トークン（`X-XSRF-TOKEN`）が必要です。パスワードは BCrypt でハッシュし、ログには出しません。

## ER図

```mermaid
erDiagram
    users ||--o{ phrases : owns
    users ||--o{ review_histories : reviews
    phrases ||--o{ review_histories : has

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        timestamptz created_at
        timestamptz updated_at
    }
    phrases {
        bigint id PK
        bigint user_id FK
        varchar english_text
        varchar japanese_text
        varchar example_sentence
        varchar category
        smallint interval_stage
        date next_review_date
        timestamptz created_at
        timestamptz updated_at
    }
    review_histories {
        bigint id PK
        bigint phrase_id FK
        bigint user_id FK
        boolean remembered
        timestamptz reviewed_at
        date next_review_date
    }
```

`interval_stage` は 0（新規）、1（3 日間隔）、2（7 日間隔、習得済み）です。忘れた場合は 0 に戻り、翌日が次の予定日になります。

| 現在 | 覚えた | まだ覚えていない |
| --- | --- | --- |
| 0 | 段階 1、3 日後 | 段階 0、翌日 |
| 1 | 段階 2、7 日後 | 段階 0、翌日 |
| 2 | 段階 2、7 日後 | 段階 0、翌日 |

「今日」は `Asia/Tokyo` の日付です。連続日数は、今日までに 1 問以上復習した日が途切れず続いている日数です。今日がまだでも、昨日まで続いていれば連続は切れません。

## API

認証が必要な API は、セッションのユーザー ID だけで所有者を決めます。他人のフレーズ ID は 404 です。

| API | 成功 | 主な失敗 |
| --- | --- | --- |
| `POST /api/auth/register` | 201 | 400、409 |
| `POST /api/auth/login` | 200 | 400、401 |
| `POST /api/auth/logout` | 204 | 401、403 |
| `GET /api/auth/me` | 200 | 401 |
| `GET /api/auth/csrf` | 204 | |
| `GET /api/phrases?q=` | 200 | 401 |
| `GET /api/phrases/{id}` | 200 | 401、404 |
| `POST /api/phrases` | 201 | 400、401 |
| `PUT /api/phrases/{id}` | 200 | 400、401、404 |
| `DELETE /api/phrases/{id}` | 204 | 401、404 |
| `GET /api/reviews/today` | 200 | 401 |
| `POST /api/reviews/{phraseId}/answer` | 200 | 400、401、404、409 |
| `GET /api/reviews/history` | 200 | 401 |
| `GET /api/dashboard` | 200 | 401 |
| `GET /actuator/health` | 200 | |

`POST /api/reviews/{phraseId}/answer` の body は `{ "remembered": true }` だけです。期限前のフレーズは 409 です。履歴は新しい順で最大 50 件です。

## 起動方法

PostgreSQL 16 を用意します。

```bash
docker compose up -d
```

Docker を使わない場合は、ユーザー `phraseloop`、パスワード `phraseloop`、データベース `phraseloop` と `phraseloop_test` を作ります。ローカルの初期値は `backend/src/main/resources/application.yml` にあります。本番の接続情報は環境変数で渡し、リポジトリには置きません。

バックエンド:

```bash
cd backend
mvn spring-boot:run
```

API は `http://localhost:8080` です。

フロントエンドの開発サーバー:

```bash
cd frontend
npm ci
npm run dev
```

画面は `http://localhost:5173` です。

本番と同じ形で確認する場合は、フロントをビルドして Spring Boot に同梱します。

```bash
cd frontend && npm ci && npm run build
cp -r dist/* ../backend/src/main/resources/static/
cd ../backend && mvn spring-boot:run
```

ビルド成果物は Git に含めません。

## テスト方法

PostgreSQL の `phraseloop_test` が起動している状態で実行します。接続先は環境変数 `TEST_DATASOURCE_URL`、`TEST_DATASOURCE_USERNAME`、`TEST_DATASOURCE_PASSWORD` で変えられます。

```bash
cd backend
mvn test
```

検証している内容は次のとおりです。

- 新規登録の成功と、パスワードが BCrypt で保存されること
- メールアドレスの重複拒否と、不正入力の拒否
- ログインの成功と失敗。失敗メッセージは登録の有無を区別しない
- 未認証の API が 401 になること
- フレーズの登録、更新、検索、削除
- ユーザー A がユーザー B のフレーズを閲覧、更新、削除、回答できないこと
- 覚えた場合は 3 日後、その次は 7 日後。まだ覚えていない場合は翌日
- 回答履歴が保存されること
- 期限前の回答が 409 になること
- ダッシュボードの連続日数が、保存された履歴と一致すること
- ヘルスチェックがデータベース接続を含むこと

フロントエンドの自動テストはありません。画面はブラウザで確認します。

## デプロイ

本番は Render の無料プランに公開しています。`render.yaml` の Web Service と PostgreSQL はどちらも `plan: free` です。Web Service はリポジトリの `Dockerfile` からビルドし、画面と API を同じオリジンで配信します。公開アドレスは、Render ダッシュボードの Web Service に表示される `onrender.com` の URL です。

無料プランでは次の制限があります。

- Web Service は 0.1 CPU、512 MB です。15 分間アクセスがないと休止し、次のアクセスで復帰するまで約 1 分かかります。休止中は月 750 時間の無料インスタンス時間を消費しません。
- ログインセッションはメモリ上にあります。休止や再起動でログアウト状態に戻ります。フレーズと復習履歴は PostgreSQL に残ります。
- 無料の PostgreSQL は保存容量 1 GB で、ワークスペースに 1 つまでです。バックアップはありません。作成から 30 日で期限が切れ、その後 14 日の猶予を過ぎるとデータごと削除されます。面接用の短期間公開を想定しています。

環境変数:

| 名前 | 値 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | Render の `postgres://` 接続文字列。Blueprint が PostgreSQL から渡し、起動時に JDBC URL へ変換する |
| `SPRING_DATASOURCE_URL` | これを指定した場合は `DATABASE_URL` より優先する |
| `PORT` | Render が割り当てるポート |
| `APP_TIMEZONE` | 未指定時は `Asia/Tokyo` |

ヘルスチェックパスは `/actuator/health` です。本番プロファイルではセッション Cookie を Secure にし、`X-Forwarded-Proto` を信頼します。CORS は開きません。

ローカルで本番イメージを確認する場合:

```bash
docker build -t phraseloop .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/phraseloop \
  -e SPRING_DATASOURCE_USERNAME=phraseloop \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  phraseloop
```

## 工夫した点

- 画面と API を同じオリジンにし、HttpOnly のセッション Cookie と CSRF トークンで認証する
- 復習日の計算をデータベースに依存しないクラスに切り、日付を固定してテストする
- 他人の ID は 403 ではなく 404 とし、存在を推測しにくくする
- 連続日数とカレンダーの日付を `Asia/Tokyo` で揃える
- スキーマ変更は Flyway だけが行い、Hibernate は検証のみ行う

## 今後の改善点

- 入力した英文を、振り返り画面と一緒に保存する
- 自己評価の履歴に応じて間隔を変える
- カレンダーで過去の月を表示する
- フレーズ一覧のページング
- パスワード再設定
