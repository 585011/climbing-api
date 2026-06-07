ALTER TABLE users ADD COLUMN auth0_id VARCHAR(128);
ALTER TABLE users ADD CONSTRAINT uq_users_auth0_id UNIQUE (auth0_id);
CREATE INDEX idx_users_auth0_id ON users(auth0_id);
