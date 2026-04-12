INSERT INTO walls (name, description, latitude, longitude, approach_info)
VALUES
    (
     'Main Wall',
     'Short approach and wall dries quickly even after rain.',
     60.3913,
     5.3221,
     'Park by the road and walk 10 minutes.'
    );

INSERT INTO routes (wall_id, name, grade, length, style, bolts, rope_lengths, first_ascendant, description)
VALUES
    (1, 'Warmup Arete', '5c', 18, 'sport', 8, 1, 'Local Team', 'Gentle opening pitch with positive holds.'),
    (1, 'Granite Line', '6b', 24, 'sport', 10, 1, 'A. Climber', 'Sustained face climbing on small edges.'),
    (1, 'Evening Crack', '6a+', 20, 'trad', NULL, 1, 'B. Climber', 'Straight crack with a steeper finish.');
