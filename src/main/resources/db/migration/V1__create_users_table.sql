CREATE TABLE IF NOT EXISTS user_profile(
    id BIGSERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    role TEXT NOT NULL DEFAULT 'USER'
);

CREATE INDEX IF NOT EXISTS idx_users_username ON user_profile(username);

