# Climbing

Climbers log the routes they have climbed at crags and gyms, and follow each other's climbing through a feed.

## Places and routes

**Climbing Area**:
A place to climb, either a crag (outdoor rock) or a gym (indoor). Every area has exactly one of those two types.
_Avoid_: Location, venue, spot

**Wall**:
A section of a Climbing Area that holds routes. At a crag it is a rock face; at a gym it is a sector set up by an admin.
_Avoid_: Sector (except when talking about gyms), face, crag (a crag is an area, not a wall)

**Route**:
A specific line on a Wall, with an exact grade and a style (sport, trad, boulder, toprope, speed).
_Avoid_: Problem, line, climb

**Retired Route**:
A gym Route that has been taken down when the gym reset the wall. It is never deleted, so the Ticks on it survive.
_Avoid_: Deleted route, removed route, archived route

**Grade Band**:
A colour range some gyms grade by instead of exact grades (e.g. Yellow = 5+ to 6). A Route with a band still has an exact grade.
_Avoid_: Colour grade, grade colour

## Climbing activity

**Tick**:
A climber's record of having climbed a Route, with how they climbed it (onsight, flash, redpoint, free solo). A climber can tick each Route at most once.
_Avoid_: Ascent, log entry, user route

**Personal Note**:
A climber's private text on a Tick. Only that climber ever sees it, even when the Tick is part of a shared Session.
_Avoid_: Caption, comment, description

**Session**:
All of one climber's Ticks at one Climbing Area on one calendar day (Norwegian time). A Session grows as the climber logs more Ticks that day.
_Avoid_: Workout, visit, outing, activity (Session is one kind of Activity)

## Social

**Activity**:
Something that appears in the Feed. Today every Activity is a Session; later, other kinds (such as a request to find a climbing partner) may be added.
_Avoid_: Post, feed item, event

**Following Feed**:
The Activities a climber sees: their own Sessions plus the shared Sessions of the climbers they follow, most recently active first. Called just "the Feed" when there is no ambiguity.
_Avoid_: Friends feed, timeline, wall (Wall is a place), stream

**Follow**:
A one-way relationship where one climber sees another climber's shared Sessions in their Feed. No approval is needed.
_Avoid_: Friend, friendship, connection, subscription

**Follower**:
A climber who follows you.
_Avoid_: Friend, fan

**Block** (not built yet):
What happens when a climber removes a Follower: the Follower can no longer see or follow that climber, and their Kudos on that climber's Sessions are removed.
_Avoid_: Ban, mute, remove (removing a Follower *is* blocking them); also don't use "blocked" for someone Auth0 refuses to log in

**Kudos**:
A one-tap sign of appreciation one climber gives another climber's Session. A climber gives at most one Kudos per Session and cannot give Kudos to their own.
_Avoid_: Like, props, reaction

**Sharing**:
Whether a climber's Sessions are visible to their Followers. A climber has an account-wide Sharing setting that acts as a master switch, and each Session is separately either shared or hidden.
_Avoid_: Privacy, public/private, visibility

**Hidden Session**:
A Session its climber has chosen not to share. Only the climber sees it.
_Avoid_: Private session, draft
