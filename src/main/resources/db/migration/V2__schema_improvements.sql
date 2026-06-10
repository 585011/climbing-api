-- NOT NULL on FK columns (all were nullable, allowing orphaned records)
ALTER TABLE walls ALTER COLUMN area_id SET NOT NULL;
ALTER TABLE routes ALTER COLUMN wall_id SET NOT NULL;
ALTER TABLE user_route_ticks ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE user_route_ticks ALTER COLUMN route_id SET NOT NULL;

-- Unique, non-null email
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- A user can only tick a route once
ALTER TABLE user_route_ticks ADD CONSTRAINT uq_tick_user_route UNIQUE (user_id, route_id);

-- Cascade deletes through the hierarchy: area -> walls -> routes -> ticks
ALTER TABLE walls DROP CONSTRAINT fk_wall_area;
ALTER TABLE walls ADD CONSTRAINT fk_wall_area
    FOREIGN KEY (area_id) REFERENCES climbing_areas(id) ON DELETE CASCADE;

ALTER TABLE routes DROP CONSTRAINT fk_route_wall;
ALTER TABLE routes ADD CONSTRAINT fk_route_wall
    FOREIGN KEY (wall_id) REFERENCES walls(id) ON DELETE CASCADE;

ALTER TABLE user_route_ticks DROP CONSTRAINT fk_tick_user;
ALTER TABLE user_route_ticks ADD CONSTRAINT fk_tick_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_route_ticks DROP CONSTRAINT fk_tick_route;
ALTER TABLE user_route_ticks ADD CONSTRAINT fk_tick_route
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE;

-- Indexes on FK columns (full table scans on every nested lookup without these)
CREATE INDEX idx_walls_area_id ON walls(area_id);
CREATE INDEX idx_routes_wall_id ON routes(wall_id);
CREATE INDEX idx_ticks_user_id ON user_route_ticks(user_id);
CREATE INDEX idx_ticks_route_id ON user_route_ticks(route_id);

-- Fix lat/lng precision (was unspecified DECIMAL, defaulting to integer precision)
ALTER TABLE climbing_areas
    ALTER COLUMN latitude TYPE DECIMAL(10, 8),
    ALTER COLUMN longitude TYPE DECIMAL(11, 8);
ALTER TABLE walls
    ALTER COLUMN latitude TYPE DECIMAL(10, 8),
    ALTER COLUMN longitude TYPE DECIMAL(11, 8);
