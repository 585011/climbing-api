-- Climbing gyms: areas are either outdoor crags or indoor gyms
ALTER TABLE climbing_areas
    ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'crag',
    ADD CONSTRAINT chk_climbing_areas_type CHECK (type IN ('crag', 'gym'));

-- Gym routes: identified by hold colour, retired (not deleted) when rotated out so ticks survive,
-- and may be created by regular users while ticking
ALTER TABLE routes
    ADD COLUMN hold_color VARCHAR(30),
    ADD COLUMN retired_at TIMESTAMPTZ,
    ADD COLUMN created_by INT,
    ADD CONSTRAINT fk_route_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- routes.style becomes a fixed set of disciplines; map known synonyms, null out anything else
UPDATE routes SET style = LOWER(TRIM(style)) WHERE style IS NOT NULL;
UPDATE routes SET style = 'boulder' WHERE style IN ('bouldering', 'buldring');
UPDATE routes SET style = 'toprope' WHERE style IN ('top-rope', 'top_rope', 'topptau');
UPDATE routes SET style = 'trad' WHERE style IN ('traditional');
UPDATE routes SET style = NULL
    WHERE style IS NOT NULL AND style NOT IN ('sport', 'trad', 'boulder', 'toprope', 'speed');
ALTER TABLE routes
    ADD CONSTRAINT chk_routes_style CHECK (style IS NULL OR style IN ('sport', 'trad', 'boulder', 'toprope', 'speed'));

CREATE INDEX idx_routes_active_wall_id ON routes(wall_id) WHERE retired_at IS NULL;
