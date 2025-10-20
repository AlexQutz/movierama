CREATE TABLE IF NOT EXISTS movie_reaction (
    id BIGSERIAL PRIMARY KEY,
    reaction_type TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL REFERENCES user_profile(id),
    movie_id BIGINT NOT NULL REFERENCES movie(id)
);

CREATE INDEX IF NOT EXISTS idx_movie_reactions_user ON movie_reaction (user_id);
CREATE INDEX IF NOT EXISTS idx_movie_reactions_movie ON movie_reaction (movie_id);
CREATE INDEX IF NOT EXISTS idx_movie_reactions_type ON movie_reaction (reaction_type);

CREATE UNIQUE INDEX IF NOT EXISTS unique_reaction ON movie_reaction(user_id, movie_id)