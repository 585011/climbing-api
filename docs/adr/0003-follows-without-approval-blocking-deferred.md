---
status: proposed
---

# Follows need no approval; blocking is deferred until signup opens

Any climber can search for another by display name and follow them immediately, without the other climber accepting. This keeps the Feed simple (one-way Follows, no pending state). Climbers stay in control through Sharing (ADR-0002), not through approving Followers.

Today an Auth0 email allowlist means only approved people can log in, so v1 ships **without Block** (removing a Follower). The allowlist is temporary and signup is expected to open to anyone, so the API must not rely on it for security: everything else (404 for Sessions you can't see, a public user profile without email or `auth0Id`, Personal Notes never shown to Followers) ships in v1 as if signup were already open.

## Considered Options

- **Mutual friendships with request and accept**: more endpoints and a pending state, and on Strava-style use mostly redundant with Sharing.
- **Approval required to follow**: costs about the same as friendships and adds friction for a small group of friends.

## Consequences

- **Block must ship before the allowlist is removed.** As designed: removing a Follower blocks them (they can't follow again or see you; following again looks like you don't exist), it also deletes their Kudos on your Sessions, blocks are permanent until an unblock endpoint is needed, and blocking is one-way: the blocker can still follow and see the blocked climber.
- Follower and following lists are visible only to their owner, so strangers can't map someone's friend group once signup is open.
