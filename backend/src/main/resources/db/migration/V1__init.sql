CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE phrases (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    english_text VARCHAR(500) NOT NULL,
    japanese_text VARCHAR(500) NOT NULL,
    example_sentence VARCHAR(1000),
    category VARCHAR(100),
    interval_stage SMALLINT NOT NULL DEFAULT 0,
    next_review_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_phrases_interval_stage CHECK (interval_stage BETWEEN 0 AND 2)
);

CREATE INDEX ix_phrases_user_next ON phrases (user_id, next_review_date);

CREATE TABLE review_histories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    phrase_id BIGINT NOT NULL REFERENCES phrases (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    remembered BOOLEAN NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    next_review_date DATE NOT NULL
);

CREATE INDEX ix_review_histories_user_reviewed ON review_histories (user_id, reviewed_at);
