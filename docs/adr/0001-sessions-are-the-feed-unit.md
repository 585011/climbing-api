---
status: proposed
---

# Sessions, not Ticks, are the unit of the Feed

One gym visit can produce many Ticks, and posting each one would flood Followers' Feeds and split Kudos across many items. So the Feed shows **Sessions**: one climber, one Climbing Area, one calendar day in Europe/Oslo. A Session is created by the day's first Tick and grows with each later one; it appears in the Feed straight away and is ordered by its most recent Tick, so an ongoing Session moves back to the top. Kudos (and later comments) attach to the Session, not to individual Ticks.

## Considered Options

- **One Tick per Feed item**: simplest, but floods the Feed and needs no grouping table, which would make adding other Activity types later harder.
- **Session the climber starts and ends explicitly**: accurate, but needs client UI and a session the climber can forget to close.
- **Post only once the Session goes quiet (e.g. 3 hours after the last Tick)**: needs a scheduled job, and Followers can't give Kudos until the climber has left.

## Consequences

- Sessions live in a general `activities` table with a `type` column, so future Activity types (e.g. a partner request) reuse the Feed, Kudos and visibility rules. The uniqueness rule (one Session per climber, area and day) must only apply to `type = 'session'`.
- Deleting a Session's last Tick deletes the Session and its Kudos.
- Ticks that existed before the Feed are not grouped into Sessions; the Feed starts empty.
- Because Sessions move in the order, offset-paginated Feed pages can shift while scrolling; a cursor fixes that if it ever matters.
