INSERT INTO climbing_areas (name, description, latitude, longitude, region)
VALUES
    (
        'Lone',
        'The most popular sport climbing crag near Bergen, with well-bolted routes on solid gneiss.',
        60.2754,
        5.4012,
        'Bergen'
    ),
    (
        'Ulriken',
        'One of Bergen''s seven mountains, offering exposed trad climbing with views over the city and fjords.',
        60.3701,
        5.3831,
        'Bergen'
    ),
    (
        'Haukås',
        'Roadside crag north of Bergen with a mix of short sport routes and bouldering, quick to dry.',
        60.4612,
        5.3287,
        'Bergen'
    );

UPDATE walls SET area_id = 1 WHERE id = 1;

INSERT INTO walls (area_id, name, description, latitude, longitude, approach_info)
VALUES
    (
        2,
        'East Face',
        'Steep gneiss face with sustained technical climbing and panoramic views over Bergen.',
        60.3708,
        5.3845,
        'Take the Ulriken cable car or hike the trail from Haukeland. 20 minutes from the top station.'
    );

INSERT INTO routes (wall_id, name, grade, length, style, bolts, rope_lengths, first_ascendant, description)
VALUES
    (2, 'Byfjorden', '6b', 28, 'trad', NULL, 1, 'A. Voss', 'Elegant crack line with a tricky mantle finish and stunning views.'),
    (2, 'Vestland Vertical', '6c', 22, 'sport', 9, 1, 'K. Nes', 'Pumpy sequence up the steepest part of the face, good holds throughout.');
