-- R2 object key of a gym tick's photo (resized JPEG); NULL when the tick has no photo.
ALTER TABLE user_route_ticks
    ADD COLUMN photo_key VARCHAR(512);
