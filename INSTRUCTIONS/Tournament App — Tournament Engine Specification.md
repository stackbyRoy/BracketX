# Tournament App — Tournament Engine Specification

**Version:** 1.0  
**Status:** MVP Core Specification  
**Purpose:** Define the deterministic, explainable tournament-generation and progression engine.

---

# 1. Purpose

The Tournament Engine is the core business-logic component of the application.

It is responsible for transforming registered participants and tournament configuration into a valid, competitive and reasonably fair tournament structure.

The engine must handle:

```text
Participants
      ↓
Validation
      ↓
Competitive Classification
      ↓
Seeding
      ↓
Draw Generation
      ↓
Bracket / Groups
      ↓
Match Progression
      ↓
Standings
      ↓
Qualification
      ↓
Champion
```

The engine must be independent of:

- Jetpack Compose
- Android UI
- Supabase UI logic
- navigation
- presentation-layer code

It should be possible to unit-test the entire engine without launching Android.

---

# 2. Core Philosophy

The application's defining tournament philosophy is:

> **Fair opportunity, not artificial equality.**

The engine must not intentionally make weaker players win.

It must instead avoid unnecessarily deterministic tournament structures where stronger players receive systematically advantageous or disadvantageous paths.

The objective is to produce tournaments where:

- stronger participants remain appropriately seeded
- strong participants are distributed across the tournament
- weaker participants have legitimate upset opportunities
- randomness remains meaningful
- the final outcome is determined by gameplay
- the draw cannot be manipulated through arbitrary manual pairing

The system must never claim:

> "This draw guarantees fairness."

Instead:

> **"This draw was generated according to the selected fairness model."**

---

# 3. Supported Tournament Formats

MVP supports:

```text
SINGLE ELIMINATION
LEAGUE / GROUPS
GROUPS → KNOCKOUT
```

---

# 4. Participant Model

Every tournament participant must contain:

```text
Participant
├── participant_id
├── user_id
├── display_name
├── in_game_id
├── game
├── metric_type
├── metric_value
├── seed
└── competitive_band
```

Example:

```text
FC Mobile

Name: Arjun
In-game ID: ArjunFC
Metric Type: OVR
Metric Value: 112
```

Example:

```text
eFootball

Name: Rahul
In-game ID: Rahul10
Metric Type: TEAM_STRENGTH
Metric Value: 3180
```

---

# 5. Metric Normalization

Different games use different competitive metrics.

Therefore, the engine must convert the raw metric into an internal normalized competitive score.

Conceptually:

```text
raw game metric
      ↓
game-specific normalization
      ↓
normalized competitive score
```

Internal representation:

```text
competitive_score: 0–100
```

The normalization function must be game-specific.

For example:

```text
FC Mobile:
OVR → normalized score

eFootball:
Team Strength → normalized score
```

Do not assume that OVR and Team Strength are directly comparable.

The normalized score is only used within the same tournament/game context.

---

# 6. Competitive Bands

Participants may optionally be classified into competitive bands.

Example:

```text
S — Elite
A — Strong
B — Competitive
C — Intermediate
D — Developing
```

The exact thresholds should not be hardcoded globally.

The engine should preferably calculate bands relative to the tournament participant distribution.

Example:

```text
Top 10%       → S
Next 20%      → A
Middle 40%    → B
Next 20%      → C
Bottom 10%    → D
```

This prevents the algorithm from behaving poorly when the absolute rating range changes between tournaments.

---

# 7. Seeding

Seeding should be derived primarily from the competitive metric.

Higher competitive score receives a stronger seed.

Example:

```text
92 → Seed 1
91 → Seed 2
89 → Seed 3
87 → Seed 4
...
68 → Seed 16
```

If two participants have identical competitive scores:

```text
tie-break:
1. previous seed if available
2. organizer-defined seed
3. randomized ordering
```

Never use participant name alphabetically as a competitive tie-breaker.

---

# 8. Why Pure Seeding Is Not Enough

A traditional bracket may produce:

```text
Seed 1 vs Seed 16
Seed 2 vs Seed 15
Seed 3 vs Seed 14
...
```

This is highly predictable.

The Tournament Engine must instead distribute participants using controlled randomization.

The system should prevent obvious clustering such as:

```text
Top half:
1, 2, 3, 4, 5, 6

Bottom half:
12, 13, 14, 15, 16
```

Strong participants should be reasonably distributed across the bracket.

---

# 9. Draw Generation Model

Use a **candidate-generation and scoring model**.

High-level algorithm:

```text
Input participants

        ↓

Create candidate draw

        ↓

Validate candidate

        ↓

Calculate fairness score

        ↓

Calculate constraint violations

        ↓

Repeat N times

        ↓

Select highest-quality valid candidate
```

The system must NOT simply generate one random draw and call it fair.

---

# 10. Candidate Draw Generation

For each candidate:

1. Shuffle participants using a seeded random generator.
2. Assign participants to bracket slots.
3. Evaluate the resulting bracket.
4. Reject candidates violating hard constraints.
5. Score valid candidates.

Example:

```text
candidate_001 → score 71
candidate_002 → score 83
candidate_003 → invalid
candidate_004 → score 89
candidate_005 → score 86
```

Select:

```text
candidate_004
```

---

# 11. Hard Constraints

Hard constraints must always be satisfied.

Examples:

### No duplicate participant

A participant may appear exactly once in a knockout bracket.

### Complete participant coverage

Every registered participant must be assigned.

### Valid bracket size

The bracket must support the participant count.

### Valid progression

Every match must have a valid destination.

### No impossible references

A match cannot reference itself or an invalid future match.

### Draw lock

Once locked, the generated structure must not change through normal operations.

---

# 12. Soft Constraints

Soft constraints influence the fairness score.

Examples:

- strong-seed distribution
- rating balance
- bracket diversity
- underdog opportunity
- excessive same-band pairings
- excessive mismatch frequency

Soft constraints can be violated if necessary, but the candidate should receive a lower score.

---

# 13. Fairness Score

The MVP fairness score should be explainable.

Conceptually:

```text
Fairness Score =
    30% Strength Distribution
  + 25% Bracket Balance
  + 20% Competitive Diversity
  + 15% Opportunity
  + 10% Randomness
```

The exact coefficients should be configurable in code.

Do not expose the mathematical formula to normal participants.

The host may see:

```text
Fairness Score: 87 / 100
```

with a short explanation.

---

# 14. Strength Distribution

Measure whether strong participants are reasonably distributed throughout the bracket.

For example:

```text
Quarter A
Average rating: 84

Quarter B
Average rating: 83

Quarter C
Average rating: 82

Quarter D
Average rating: 84
```

is preferable to:

```text
Quarter A
Average rating: 91

Quarter B
Average rating: 89

Quarter C
Average rating: 76

Quarter D
Average rating: 77
```

The exact acceptable variance should be configurable.

---

# 15. Competitive Diversity

The engine should avoid unnecessarily creating repeated same-strength matchups.

For example, avoid excessive:

```text
Elite vs Elite
Developing vs Developing
```

in the opening round.

However, it must NOT prohibit all same-band matchups.

Otherwise the algorithm becomes predictable.

---

# 16. Underdog Opportunity

The engine should preserve legitimate opportunities for lower-rated participants.

This does NOT mean:

```text
weak participant must receive an easy opponent
```

Instead, the system should avoid a structure where every weaker participant is systematically paired against the strongest available opponent.

The scoring model should reward a healthy distribution of:

```text
Underdog vs Mid-level
Underdog vs Strong
Mid-level vs Strong
Strong vs Strong
```

rather than eliminating any category.

---

# 17. Randomness

Randomness is mandatory.

Two tournaments containing the same participants and configuration should not automatically produce the same bracket.

However, each individual draw must be reproducible using its stored random seed.

Therefore:

```text
Randomness
+
Reproducibility
```

must coexist.

---

# 18. Draw Seed

Every generation must produce:

```text
random_seed
algorithm_version
generated_at
```

Example:

```text
Algorithm Version:
fair-draw-v1

Seed:
8d2a...f91c

Generated:
2026-09-15T12:30:00Z
```

This allows debugging and auditing.

---

# 19. Regeneration

Before the draw is locked:

```text
Host
 ↓
Generate
 ↓
Review
 ↓
Regenerate
 ↓
Review
 ↓
Lock
```

Each regeneration must use a new seed.

The system must not silently reuse the previous seed.

---

# 20. Draw Lock

Once the host selects:

```text
LOCK DRAW
```

the following become immutable under normal operations:

- participant placement
- bracket structure
- seed assignments
- match relationships

The database must record:

```text
locked_at
locked_by
```

---

# 21. Non-Power-of-Two Knockout Tournaments

Knockout tournaments may have:

```text
8
16
32
64
```

participants.

They may also have:

```text
10
12
18
24
30
```

etc.

The engine must support non-power-of-two participant counts.

Use appropriate byes.

Example:

```text
12 Participants

Round of 16
↓
4 byes
↓
8 players play
↓
8 quarter-finalists
```

Byes must be distributed fairly.

Do not simply give every bye to the lowest-rated participant or highest-rated participant.

Bye allocation should consider:

- seed
- bracket balance
- competitive distribution
- randomness

and must avoid giving one participant multiple unnecessary advantages.

---

# 22. Bye Handling

A participant receiving a bye must be represented explicitly.

Example:

```text
Match #1

Seed 1
BYE

Winner: Seed 1
```

Do not create fake participants such as:

```text
"BYE PLAYER"
```

in the user-facing participant list.

---

# 23. Group Generation

For groups → knockout tournaments:

Input:

```text
Participants
Number of groups
Qualification slots
```

Example:

```text
24 players
4 groups
6 players/group
Top 2 qualify
```

The engine should distribute competitive strength across groups.

Avoid:

```text
Group A:
92
91
90
89
...
```

and:

```text
Group D:
72
71
70
69
...
```

Instead, use a serpentine or equivalent balanced seeding strategy combined with controlled randomization.

Example:

```text
Seeds:

Group A → 1, 8, 9, 16, 17, 24
Group B → 2, 7, 10, 15, 18, 23
Group C → 3, 6, 11, 14, 19, 22
Group D → 4, 5, 12, 13, 20, 21
```

Then randomize within acceptable constraints.

---

# 24. Group Matches

For a standard round-robin group:

Each participant plays every other participant once.

For `N` participants:

```text
Matches = N × (N - 1) / 2
```

Example:

```text
6 players

6 × 5 / 2
= 15 matches
```

---

# 25. Group Standings

Default points:

```text
Win   = 3
Draw  = 1
Loss  = 0
```

Standings should automatically calculate:

```text
Played
Wins
Draws
Losses
Goals For
Goals Against
Goal Difference
Points
```

---

# 26. Tie-Breakers

Default ordering:

```text
1. Points
2. Goal Difference
3. Goals Scored
4. Head-to-Head
5. Organizer-defined tiebreaker
```

The exact rules should be configurable during tournament creation.

Do not hardcode the tie-breaker order into the UI.

---

# 27. Qualification

Example:

```text
Group A
Top 2 qualify

1. Player A
2. Player B
----------------
Qualified
3. Player C
4. Player D
...
```

The engine must automatically determine qualification after all required group matches are complete.

If qualification is mathematically unresolved, do not prematurely lock the qualified participants.

---

# 28. Groups → Knockout Seeding

Qualified participants must be mapped into knockout slots.

Example:

```text
Group A Winner
vs
Group B Runner-up
```

The mapping should prevent immediate rematches where tournament rules prohibit them.

If rematch restrictions are enabled, they should be implemented as explicit constraints.

---

# 29. Match Progression

Knockout match:

```text
Match Completed
      ↓
Winner Determined
      ↓
Winner assigned to next match
```

The engine must never require the host to manually advance a winner.

Host responsibility:

```text
Enter score
Confirm result
```

Engine responsibility:

```text
Determine winner
Advance winner
Update next match
Update tournament state
```

---

# 30. Draw / Match Integrity

The engine must guarantee:

```text
No participant plays two simultaneous knockout matches.

No participant can appear twice in the same match.

A completed match cannot be accidentally reopened.

A winner cannot be advanced twice.

A participant cannot be assigned to an impossible future slot.
```

---

# 31. Score Validation

For a normal match:

```text
score >= 0
```

Invalid:

```text
-1
```

For completed matches:

```text
score_a != score_b
```

unless the tournament configuration explicitly supports draws or a separate tie-breaking mechanism.

---

# 32. Tournament Types and Draw Philosophy

### Knockout

Prioritize:

```text
bracket balance
strength distribution
upset opportunity
```

### Groups

Prioritize:

```text
group strength balance
competitive diversity
```

### Groups → Knockout

First optimize group distribution.

Then optimize the knockout bracket independently.

Do not optimize the entire structure blindly as one giant operation.

---

# 33. Host Override

Hosts may need exceptional control.

Example:

```text
Wrong score entered
Participant withdrew
Technical issue
Disqualification
```

Overrides must:

1. Require explicit confirmation.
2. Be permission-protected.
3. Be recorded in audit logs.
4. Trigger recalculation where appropriate.

Example:

```text
MATCH RESULT OVERRIDE

Current:
Player A 3 — 1 Player B

New:
Player A 2 — 1 Player B

Reason:
[________________________]

[ CANCEL ] [ CONFIRM OVERRIDE ]
```

---

# 34. Withdrawals

If a participant withdraws before draw lock:

```text
Remove participant
↓
Regenerate affected structure
```

If they withdraw after draw lock:

The tournament must use the configured withdrawal policy.

MVP default:

```text
Participant withdrawn
↓
Opponent receives advancement
```

The event must be recorded.

---

# 35. Disqualification

Disqualification should be treated differently from a normal loss.

Store:

```text
result_type = disqualification
winner_id = opponent
```

Audit:

```text
DISQUALIFICATION
actor
participant
reason
timestamp
```

---

# 36. Algorithm Versioning

Every generated tournament must store the algorithm version.

Example:

```text
fair-draw-v1
```

When the algorithm changes:

```text
fair-draw-v2
```

Do not silently change the algorithm for already-generated tournaments.

---

# 37. Performance

The draw algorithm must not block the Android main thread.

Candidate generation should run in a coroutine/background dispatcher.

For typical college tournaments:

```text
8–64 participants
```

the engine should generate a valid draw quickly enough for interactive use.

For unusually large tournaments, generation may move to a server-side Edge Function.

---

# 38. Testing Requirements

The engine must be tested independently.

Minimum tests:

### 8 players

- valid bracket
- no duplicate participants

### 16 players

- correct round structure
- correct progression

### 32 players

- performance
- balanced distribution

### Non-power-of-two

- 10 participants
- 12 participants
- 18 participants
- 24 participants

### Groups

- 4 groups × 4
- 4 groups × 6
- uneven participant counts where allowed

### Draw

- reproducibility
- seed changes
- regeneration
- fairness scoring

### Match

- valid result
- invalid result
- winner progression
- completed match protection

### Standings

- win
- draw
- loss
- goal difference
- tie-breaking

---

# 39. Property-Based Validation

Where practical, test tournament invariants rather than only specific examples.

For every generated knockout:

```text participant_count =
unique participant assignments
```

For every match:

```text participant references are valid
```

For every completed match:

```text exactly one winner
```

For every tournament:

```text no participant appears twice in the same round
```

These invariants should be automatically tested.

---

# 40. Engine Output

The engine should return a structured tournament object.

Conceptually:

```text
TournamentStructure
├── participants
├── seeds
├── groups
├── matches
├── rounds
├── standings
├── progression_map
├── fairness_score
├── algorithm_version
└── random_seed
```

The UI should consume this structure.

The UI must not recreate it.

---

# 41. Critical Principle

The Tournament Engine is not an AI chatbot.

It is not required to use machine learning.

The preferred MVP implementation is:

```text
Deterministic algorithms
+
Constrained randomization
+
Statistical scoring
+
Explicit rules
```

This makes tournament generation:

- explainable
- testable
- reproducible
- auditable
- easier to debug

Machine learning may be considered later only if real tournament data demonstrates a meaningful need.

---

# 42. Definition of Done

The Tournament Engine is considered complete when:

```text
✓ Participants can be classified
✓ Seeds can be generated
✓ Valid knockout brackets can be generated
✓ Non-power-of-two brackets work
✓ Fair draw candidates can be generated
✓ Candidates can be scored
✓ Best valid candidate can be selected
✓ Draws are reproducible
✓ Draws can be regenerated
✓ Draws can be locked
✓ Groups can be generated
✓ Group strength can be balanced
✓ Standings calculate automatically
✓ Tie-breakers work
✓ Qualified participants are identified
✓ Knockout progression is automatic
✓ Match results update tournament state
✓ Host overrides are audited
✓ Unit tests cover core invariants
```

---

# 43. Final Engineering Principle

The Tournament Engine must never make the tournament outcome predictable.

It should make the **tournament structure intelligently fair** while leaving the actual outcome to the players.

The intended philosophy is:

> **Better structure. Better competition. More meaningful upsets.**