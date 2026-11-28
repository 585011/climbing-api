-- Existing users never agreed to share: they start with Sharing off. New users start with it on (ADR-0002).
ALTER TABLE users ADD COLUMN share_activity BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ALTER COLUMN share_activity SET DEFAULT TRUE;

-- Activities are what the Feed shows; today every Activity is a Session (ADR-0001).
CREATE TABLE activities (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(30) NOT NULL CHECK (type IN ('session')),
    area_id       INT REFERENCES climbing_areas(id) ON DELETE CASCADE,
    activity_date DATE,
    shared        BOOLEAN NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_tick_at  TIMESTAMPTZ NOT NULL,
    CHECK (type <> 'session' OR (area_id IS NOT NULL AND activity_date IS NOT NULL))
);
-- Partial: future Activity types (e.g. partner requests) must not be bound by this rule.
CREATE UNIQUE INDEX uq_session_per_day ON activities (user_id, area_id, activity_date) WHERE type = 'session';
CREATE INDEX idx_activities_last_tick ON activities (last_tick_at DESC, id DESC);

-- No backfill: Ticks logged before the Feed keep activity_id = NULL.
ALTER TABLE user_route_ticks ADD COLUMN activity_id INT REFERENCES activities(id) ON DELETE SET NULL;
CREATE INDEX idx_ticks_activity ON user_route_ticks (activity_id);
