CREATE TABLE IF NOT EXISTS movie (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES user_profile(id)
);

CREATE INDEX IF NOT EXISTS idx_movies_user_id ON movie(user_id);
CREATE INDEX IF NOT EXISTS idx_movies_created_at ON movie(created_at);
CREATE INDEX IF NOT EXISTS idx_movies_title ON movie(title);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_movie_title
    ON movie (lower(title));

