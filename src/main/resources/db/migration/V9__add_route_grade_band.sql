-- The gym's grade colour (a range such as 5+ to 6) a route was set in. routes.grade stays the exact grade.
ALTER TABLE routes
    ADD COLUMN grade_band VARCHAR(30);
