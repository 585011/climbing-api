-- Block (ADR-0003): removing a Follower turns their Follow row into a block. follower_id is the blocked climber,
-- followee_id the blocker. Only 'active' rows count as Follows.
ALTER TABLE follows
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'blocked'));
