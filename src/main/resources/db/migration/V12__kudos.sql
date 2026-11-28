-- One Kudos per climber per Session (CONTEXT.md). Deleting a Session deletes its Kudos.
CREATE TABLE kudos (
    activity_id INT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    user_id     INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (activity_id, user_id)
);
