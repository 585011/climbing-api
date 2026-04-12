CREATE TABLE IF NOT EXISTS users (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255),
    display_name VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS climbing_areas (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(255),
    latitude DECIMAL,
    longitude DECIMAL,
    region VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS walls (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    area_id INT,
    name VARCHAR(255),
    description VARCHAR(255),
    latitude DECIMAL,
    longitude DECIMAL,
    approach_info VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wall_area FOREIGN KEY (area_id) REFERENCES climbing_areas(id)
);

CREATE TABLE IF NOT EXISTS routes (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wall_id INT,
    name VARCHAR(255),
    grade VARCHAR(50),
    length INTEGER,
    style VARCHAR(50),
    bolts INTEGER,
    rope_lengths INTEGER,
    first_ascendant VARCHAR(255),
    description VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_wall FOREIGN KEY (wall_id) REFERENCES walls(id)
);

CREATE TABLE IF NOT EXISTS user_route_ticks (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT,
    route_id INT,
    ticked_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    style VARCHAR(50),
    rating INTEGER,
    personal_note VARCHAR(255),
    CONSTRAINT fk_tick_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tick_route FOREIGN KEY (route_id) REFERENCES routes(id)
);