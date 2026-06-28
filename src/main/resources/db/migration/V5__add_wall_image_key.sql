-- Wall images: store the Cloudflare R2 object key (not a public URL).
-- Nullable — walls without an image are fully supported.
ALTER TABLE walls ADD COLUMN image_key VARCHAR(512);
