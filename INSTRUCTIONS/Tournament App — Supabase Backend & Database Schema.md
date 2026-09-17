# Tournament App — Supabase Backend & Database Schema

**Database:** PostgreSQL  
**Backend Platform:** Supabase  
**Authentication:** Supabase Auth

---

# 1. Database Philosophy

The database is the authoritative source of tournament state.

The Android application is a client.

Never assume that hiding a UI control is sufficient authorization.

All important permissions must be enforced using:

- PostgreSQL constraints
- Row Level Security
- server-side functions where required

---

# 2. Core Tables

```text
profiles
tournaments
tournament_members
registrations
participants
groups
group_members
matches
standings
draw_generations
audit_logs
```

---

# 3. profiles

Stores public application-level user information.

```sql
profiles
---------
id UUID PRIMARY KEY
display_name TEXT NOT NULL
avatar_url TEXT NULL
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

`id` references the authenticated Supabase user.

---

# 4. tournaments

```sql
tournaments
-----------
id UUID PRIMARY KEY
host_id UUID NOT NULL
name TEXT NOT NULL
game TEXT NOT NULL
format TEXT NOT NULL
status TEXT NOT NULL
max_participants INTEGER
registration_open BOOLEAN
draw_locked BOOLEAN
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Possible games:

```text
fc_mobile
efootball
```

Possible formats:

```text
knockout
league
groups_knockout
```

Possible statuses:

```text
draft
registration_open
registration_closed
draw_pending
draw_generated
draw_locked
in_progress
completed
```

---

# 5. tournament_members

Represents users associated with tournaments.

```sql
tournament_members
------------------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL
user_id UUID NOT NULL
role TEXT NOT NULL
created_at TIMESTAMPTZ
```

Roles:

```text
participant
host
```

A user can be a participant in one tournament and host in another.

---

# 6. registrations

Stores submitted registration data.

```sql
registrations
-------------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL
user_id UUID NOT NULL

name TEXT NOT NULL
in_game_id TEXT NOT NULL

game_metric_type TEXT NOT NULL
game_metric_value INTEGER NOT NULL

status TEXT NOT NULL

created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Metric types:

```text
ovr
team_strength
```

Registration status:

```text
pending
approved
rejected
confirmed
```

For MVP, registration can default directly to:

```text
confirmed
```

unless host approval is enabled.

---

# 7. participants

Once registration is accepted, create a tournament participant record.

```sql
participants
------------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL
user_id UUID NOT NULL

display_name TEXT NOT NULL
in_game_id TEXT NOT NULL

game_metric_type TEXT NOT NULL
game_metric_value INTEGER NOT NULL

seed INTEGER NULL
competitive_band TEXT NULL

created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

The participant snapshot is important.

Do not rely on the user's current profile to determine historical tournament data.

---

# 8. groups

```sql
groups
------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL
name TEXT NOT NULL
group_order INTEGER NOT NULL
```

Example:

```text
Group A
Group B
Group C
Group D
```

---

# 9. group_members

```sql
group_members
-------------
id UUID PRIMARY KEY
group_id UUID NOT NULL
participant_id UUID NOT NULL
```

Unique constraint:

```text
(group_id, participant_id)
```

---

# 10. matches

```sql
matches
-------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL

stage TEXT NOT NULL
round_number INTEGER NULL
match_number INTEGER NOT NULL

group_id UUID NULL

participant_a UUID NULL
participant_b UUID NULL

score_a INTEGER NULL
score_b INTEGER NULL

winner_id UUID NULL

status TEXT NOT NULL

scheduled_at TIMESTAMPTZ NULL

created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

Possible stages:

```text
group
round_of_64
round_of_32
round_of_16
quarter_final
semi_final
final
```

Possible status:

```text
scheduled
completed
disputed
cancelled
```

---

# 11. standings

```sql
standings
---------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL
group_id UUID NOT NULL
participant_id UUID NOT NULL

played INTEGER DEFAULT 0
wins INTEGER DEFAULT 0
draws INTEGER DEFAULT 0
losses INTEGER DEFAULT 0

goals_for INTEGER DEFAULT 0
goals_against INTEGER DEFAULT 0
goal_difference INTEGER DEFAULT 0

points INTEGER DEFAULT 0

updated_at TIMESTAMPTZ
```

Standings should be derived from confirmed match results wherever practical.

Avoid allowing clients to arbitrarily edit standings.

---

# 12. draw_generations

Used for auditability and reproducibility.

```sql
draw_generations
---------------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL

algorithm_version TEXT NOT NULL
random_seed TEXT NOT NULL

fairness_score NUMERIC NULL

generated_by UUID NOT NULL
generated_at TIMESTAMPTZ
locked_at TIMESTAMPTZ NULL
```

---

# 13. audit_logs

Important administrative actions should be recorded.

```sql
audit_logs
----------
id UUID PRIMARY KEY
tournament_id UUID NOT NULL
actor_id UUID NOT NULL

action TEXT NOT NULL
entity_type TEXT NOT NULL
entity_id UUID NULL

metadata JSONB NULL

created_at TIMESTAMPTZ
```

Examples:

```text
TOURNAMENT_CREATED
REGISTRATION_CLOSED
DRAW_GENERATED
DRAW_REGENERATED
DRAW_LOCKED
MATCH_RESULT_UPDATED
MATCH_RESULT_OVERRIDDEN
TOURNAMENT_COMPLETED
```

---

# 14. Critical Constraints

Database constraints must prevent:

### Duplicate registration

```text
UNIQUE(tournament_id, user_id)
```

### Duplicate group membership

```text
UNIQUE(group_id, participant_id)
```

### Duplicate match numbers within stage

Appropriate composite uniqueness constraints should be added.

### Invalid negative scores

```text
CHECK(score_a >= 0)
CHECK(score_b >= 0)
```

### Invalid metrics

Game-specific validation should exist at the application and server layers.

---

# 15. Row Level Security

RLS must be enabled on all user-facing tables.

## Participant

Can:

- read public tournament information
- read own registration
- create own registration
- read matches
- read standings

Cannot:

- update match results
- modify bracket
- modify tournament settings
- modify another participant

## Host

Can modify:

- tournaments they own
- participants of their tournaments
- registration state
- match results
- tournament configuration before lock

---

# 16. Server-side Operations

Important operations should eventually be implemented through secure PostgreSQL functions or Supabase Edge Functions.

Recommended operations:

```text
generate_draw()
lock_draw()
submit_match_result()
recalculate_standings()
advance_knockout_winner()
complete_tournament()
```

The Android client should request these operations rather than directly manipulating sensitive tournament state.

---

# 17. Realtime

Subscribe to tournament-specific changes.

Participants should receive updates for:

```text
match result
bracket progression
standings
tournament status
```

Avoid subscribing to unnecessary global database changes.

---

# 18. Data Lifecycle

Tournament creation:

```text
tournaments
    ↓
registrations
    ↓
participants
    ↓
groups / matches
    ↓
standings
```

Once a tournament is active, participant snapshots should remain stable.

Historical tournament results should remain immutable except through explicit host override actions.