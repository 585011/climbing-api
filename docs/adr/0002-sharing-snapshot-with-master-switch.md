---
status: proposed
---

# Sharing: per-Session snapshot plus an account-wide master switch

Sharing must always be optional, and turning it on must never publish a climber's history without them choosing to. So each Session stores its own shared/hidden value, copied from the climber's account Sharing setting when the Session is created and changeable per Session afterwards. The account setting also acts as a master switch read at Feed time: a Session is visible to Followers only if the account setting is on **and** the Session is shared. Turning the account setting off hides everything; turning it back on restores exactly the Sessions that were shared before, but never the ones created while it was off.

Existing users start with Sharing off (they started logging before the Feed existed and never agreed to share); new users start with it on.

## Considered Options

- **Sessions inherit the account setting at read time** (no per-Session value unless overridden): turning Sharing on would instantly publish every past Session.
- **Per-Tick sharing flag**: finer-grained than anyone needs, and a Session with mixed hidden and shared Ticks is confusing to show.

## Consequences

- A Session created while Sharing was off stays hidden when the climber turns Sharing on, even later the same day. The climber can share it, or hide any shared Session, at any time afterwards with the per-Session toggle (owner only). A Session marked shared while the account setting is off appears as soon as the setting is turned back on.
- The Feed is ordered by each Session's most recent Tick, not by when it was shared, so sharing an old Session puts it at its original place in the Feed rather than at the top.
- A Follower asking for a Session they can't see gets 404, the same as a Session that doesn't exist.
