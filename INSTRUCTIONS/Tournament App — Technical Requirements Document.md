# Tournament App — Technical Requirements Document

**Version:** 1.0  
**Platform:** Android  
**Language:** Kotlin  
**UI:** Jetpack Compose  
**Architecture:** Clean Architecture + MVVM  
**Backend:** Supabase  
**Database:** PostgreSQL

---

# 1. Technical Philosophy

The application should prioritize:

- correctness
- reliability
- maintainability
- predictable state management
- offline resilience where practical
- secure authorization
- testability

Avoid unnecessary architectural complexity.

Do not introduce:

- microservices
- unnecessary backend servers
- complex dependency injection frameworks unless justified
- over-engineered abstractions
- AI/ML where deterministic algorithms are sufficient

---

# 2. Technology Stack

## Android

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- Kotlin Coroutines
- Kotlin Flow
- Room for local caching where required

## Backend

Supabase:

- PostgreSQL
- Supabase Auth
- Row Level Security
- Realtime
- Edge Functions when server-side logic is required
- Storage only if future media requirements appear

---

# 3. Application Architecture

Recommended architecture:

```text
Presentation
    │
    ↓
ViewModel
    │
    ↓
Use Cases
    │
    ↓
Repository
    │
    ├──────────────┐
    ↓              ↓
Supabase       Local Cache
```

Use a feature-oriented project structure.

Example:

```text
app/
├── core/
│   ├── navigation/
│   ├── ui/
│   ├── network/
│   ├── database/
│   ├── util/
│   └── auth/
│
├── feature/
│   ├── auth/
│   ├── home/
│   ├── tournament/
│   ├── registration/
│   ├── bracket/
│   ├── standings/
│   ├── host/
│   └── profile/
│
└── domain/
    ├── model/
    ├── repository/
    └── usecase/
```

---

# 4. Authentication

Use Supabase Auth.

MVP authentication:

- email
- password

Future options may include:

- Google authentication
- phone authentication

Authentication state must persist across application launches.

The application must determine whether a user is:

- unauthenticated
- authenticated

before rendering protected screens.

---

# 5. Navigation

Primary navigation should adapt to user context.

Participant-oriented navigation:

```text
Home
Tournaments
My Tournaments
Profile
```

Host-oriented navigation:

```text
Home
My Tournaments
Create
Profile
```

Do not create separate applications for hosts and participants.

---

# 6. Domain Models

Core domain models:

```text
User
Tournament
TournamentParticipant
Registration
Match
Group
Standing
BracketNode
TournamentSettings
```

---

# 7. Tournament Engine

The tournament engine is the most important domain component.

It must be independent of Compose UI.

It should receive structured participant data and tournament configuration.

Example conceptual API:

```text
generateTournament(participants, configuration)
```

Output:

```text
TournamentStructure
├── rounds
├── matches
├── groups
└── progression rules
```

The UI must never directly calculate tournament progression.

---

# 8. Fair Draw Engine

The draw engine should use deterministic constrained randomization.

General pipeline:

```text
Participants
     ↓
Normalize Metrics
     ↓
Assign Seeds / Competitive Bands
     ↓
Generate Candidate Structures
     ↓
Evaluate Candidates
     ↓
Apply Constraints
     ↓
Select Valid Draw
```

Candidate evaluation can consider:

```text
Competitive Balance
Rating Distribution
Bracket Distribution
Randomness
Underdog Opportunity
```

The exact scoring weights must remain configurable.

Do not use machine learning in MVP.

---

# 9. Deterministic Reproducibility

A generated draw should have a stored random seed.

Example:

```text
draw_seed = UUID / secure random seed
```

This allows the host to reproduce or audit a generated draw.

Store:

- generation timestamp
- draw seed
- participant ordering
- algorithm version

---

# 10. Match Engine

Each match has:

```text
match_id
tournament_id
stage
round_number
player_a
player_b
score_a
score_b
winner
status
```

Match states:

```text
SCHEDULED
LIVE
RESULT_PENDING
COMPLETED
DISPUTED
CANCELLED
```

MVP may use:

```text
SCHEDULED
COMPLETED
DISPUTED
```

and expand later.

---

# 11. Score Update Security

Score updates must not be trusted from the client alone.

The host permission must be verified server-side through Supabase Row Level Security and/or Edge Functions.

The client may display:

```text
Update Score
```

but the database must independently verify:

```text
Is authenticated?
Is tournament host?
Is match part of host's tournament?
Is match editable?
```

Only then should the update be accepted.

---

# 12. Automatic Progression

When a knockout match is completed:

```text
Winner
  ↓
Next Match
  ↓
Populate Participant Slot
```

If both participants in the next match are available:

```text
Next Match = READY
```

For groups:

```text
Match Result
     ↓
Update Standing
     ↓
Recalculate Table
     ↓
Determine Qualification
```

Standings must not be manually edited.

---

# 13. Realtime

Supabase Realtime should be used for tournament updates where appropriate.

Examples:

- score update
- bracket progression
- standings update
- tournament status change

Participants should not need to manually refresh after host updates a result.

---

# 14. Error Handling

Every network operation must have explicit UI states:

```text
Loading
Success
Empty
Error
Retry
```

Never silently fail.

Example:

```text
Unable to save result.

Check your connection and try again.

[ Retry ]
```

---

# 15. Offline Behavior

The application should gracefully handle temporary connectivity problems.

Read-only tournament information may be cached locally.

Score updates should require confirmed connectivity.

Never falsely display a score as successfully submitted when the server has not accepted it.

---

# 16. Testing Requirements

Minimum unit tests:

### Registration

- valid registration
- empty name
- empty ID
- invalid metric
- duplicate registration

### Draw Engine

- valid participant count
- uneven participant count
- seed distribution
- duplicate prevention
- bracket validity
- reproducibility using seed

### Match Engine

- winner determination
- next-round progression
- invalid score handling
- completed match protection

### Standings

- win
- draw
- loss
- points
- goal difference
- tie-breaking

### Permissions

- participant cannot update score
- non-host cannot modify tournament
- host can update own tournament
- unauthenticated user cannot access protected management operations

---

# 17. Performance

The application should remain responsive on budget Android devices.

Avoid:

- unnecessary recompositions
- large unpaginated queries
- expensive draw generation on the main thread
- loading entire tournament histories unnecessarily

Tournament generation should run away from the main UI thread.

---

# 18. Security

Never store:

- Supabase service-role keys
- privileged credentials
- secret API keys

inside the Android application.

Only publishable/client-safe credentials may be included.

Authorization must be enforced by Supabase policies and trusted server-side operations.

---

# 19. Development Priority

Implementation order:

```text
1. Project setup
2. Authentication
3. Database integration
4. User profile
5. Tournament creation
6. Registration
7. Participant management
8. Tournament engine
9. Draw generation
10. Bracket
11. Score management
12. Standings
13. Realtime
14. Error handling
15. Testing
16. UI polish
```