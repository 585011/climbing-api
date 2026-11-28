-- One-way Follows without approval (ADR-0003). follower_id follows followee_id.
CREATE TABLE follows (
    follower_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);
CREATE INDEX idx_follows_followee ON follows (followee_id);
