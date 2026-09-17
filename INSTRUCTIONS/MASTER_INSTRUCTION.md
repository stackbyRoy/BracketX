# MASTER ENGINEERING INSTRUCTION

## Tournament Management Platform — Build Phase

**Document Status:** FINAL  
**Purpose:** Master instruction for AI coding agents  
**Authority:** Highest implementation-level instruction  
**Phase:** BUILD  
**Primary Platform:** Android  
**Backend:** Supabase + PostgreSQL  
**Language:** Kotlin  
**UI:** Jetpack Compose + Material 3  

---

# 1. ROLE

You are the **Principal Android Engineer and Systems Architect** responsible for implementing this tournament management platform.

You are not acting as a prototype generator.

You are building a production-oriented system whose core responsibilities include:

- Tournament creation
- Participant registration
- Competitive classification
- Fair draw generation
- Bracket/group generation
- Match progression
- Host-controlled scoring
- Standings
- Tournament completion
- Realtime synchronization
- Secure authorization

You must prioritize:

> **Correctness → integrity → maintainability → usability → visual polish**

Do not sacrifice business correctness for faster UI implementation.

---

# 2. SOURCE OF TRUTH

The project specification consists of the following documents:

```text
PRD.md
TRD.md
UI_UX.md
BACKEND_SCHEMA.md
TOURNAMENT_ENGINE_SPEC.md
DRAW_ALGORITHM_SPEC.md
MASTER_INSTRUCTION.md
```

These documents collectively define the product.

## Authority Order

When interpreting implementation requirements, use this priority:

```text
1. MASTER_INSTRUCTION.md
2. DRAW_ALGORITHM_SPEC.md
3. TOURNAMENT_ENGINE_SPEC.md
4. BACKEND_SCHEMA.md
5. TRD.md
6. PRD.md
7. UI_UX.md
```

However, this hierarchy does NOT mean a lower document can be ignored.

All documents must be satisfied together.

If two requirements genuinely conflict:

1. Do not silently choose one.
2. Identify the conflict.
3. Prefer the higher-priority specification.
4. Preserve the original product intent.
5. Document the resolution.

---

# 3. ABSOLUTE RULE

## DO NOT WING THE BUSINESS LOGIC.

Do not invent:

- Tournament rules
- Draw algorithms
- Fairness calculations
- Participant classification logic
- Match progression rules
- Permission models
- Database relationships
- Registration fields
- Tournament states

when they are already defined in the specifications.

If something is unspecified, implement the smallest reasonable abstraction required for the existing architecture.

Do not expand scope simply because a feature seems useful.

---

# 4. PRODUCT PURPOSE

The application is a mobile-first tournament management platform initially designed for college gaming tournaments.

Initial target games include:

- FC Mobile
- eFootball

The platform should make tournament organization significantly easier by automating:

```text
Registration
    ↓
Participant classification
    ↓
Draw generation
    ↓
Bracket / groups
    ↓
Match progression
    ↓
Results
    ↓
Standings
    ↓
Tournament completion
```

The central product philosophy is:

> **Fair opportunity, not artificial equality.**

The system must not manipulate gameplay outcomes.

It should construct a tournament structure that is:

- Legitimately competitive
- Structurally balanced
- Randomized
- Explainable
- Reproducible
- Resistant to obvious unfair clustering

---

# 5. MVP SCOPE

The MVP supports:

### Tournament Formats

```text
Single Elimination
League / Groups
Groups → Knockout
```

### Participant Features

- Create account
- Discover tournaments
- Join tournament
- Register
- View tournament
- View bracket
- View groups
- View standings
- View matches
- View official results
- Track progression

### Host Features

- Create tournament
- Configure tournament
- Open/close registration
- View participants
- Manage registrations
- Generate draw
- Preview draw
- Regenerate draw before lock
- Lock draw
- Manage matches
- Enter official scores
- Resolve exceptional situations
- Complete tournament

---

# 6. EXPLICITLY OUT OF SCOPE

Do not implement unless the project specifications are later changed explicitly:

```text
Chat
Social feed
Payments
Entry fees
Sponsorship management
Streaming
AI chatbot
Global rankings
Cross-platform web client
Marketplace
In-app messaging
Automated cheating detection
Computer vision result verification
Winner prediction
AI skill prediction
Machine-learning draw generation
Native advertisements
```

Do not introduce "future-ready" infrastructure that materially increases MVP complexity without a concrete requirement.

---

# 7. ACCOUNT MODEL

There is only **one account system**.

Do not create:

```text
Host Account
Participant Account
```

as separate account types.

A user can:

```text
Host Tournament A
Participant Tournament B
```

and potentially:

```text
Host Tournament C
Participant Tournament A
```

Permissions are determined by the user's relationship to a specific tournament.

Example:

```text
User
 ├── Tournament A → HOST
 ├── Tournament B → PARTICIPANT
 └── Tournament C → PARTICIPANT
```

Authorization must therefore be tournament-specific.

---

# 8. REGISTRATION REQUIREMENTS

The participant registration form contains exactly:

```text
NAME
IN-GAME ID
OVR / TEAM STRENGTH
```

The third field is game-dependent:

```text
FC Mobile → OVR
eFootball → TEAM STRENGTH
```

Do not add:

```text
Phone
Email
College Roll
Age
Address
Social Media
```

unless explicitly requested later.

---

# 9. REGISTRATION VALIDATION

Registration must validate:

```text
Name is not empty
In-game ID is not empty
Competitive metric is numeric
Competitive metric is within configured bounds
Duplicate registration is prevented
One registration per user per tournament
```

Before draw lock:

```text
Host may correct registration information.
```

After draw lock:

```text
Competitive metric and participant placement
must not change through normal operations.
```

Exceptional changes require an explicit audited override.

---

# 10. CRITICAL ARCHITECTURAL PRINCIPLE

## BUILD THE TOURNAMENT ENGINE FIRST.

Do not begin by building screens.

Do not begin by building the complete host dashboard.

Do not begin by polishing Compose components.

The tournament engine is the core of the product.

The correct dependency order is:

```text
DOMAIN MODELS
      ↓
TOURNAMENT ENGINE
      ↓
DRAW ALGORITHM
      ↓
ENGINE TESTS
      ↓
BACKEND DATA MODEL
      ↓
BACKEND AUTHORIZATION
      ↓
BACKEND TOURNAMENT OPERATIONS
      ↓
APPLICATION STATE / REPOSITORIES
      ↓
HOST UI
      ↓
PARTICIPANT UI
      ↓
REALTIME
      ↓
POLISH
```

Do not invert this order merely because UI work is easier to see.

---

# 11. MANDATORY BUILD ORDER

The implementation must proceed through these phases.

---

## PHASE 0 — PROJECT FOUNDATION

Set up:

```text
Kotlin
Jetpack Compose
Material 3
Navigation Compose
ViewModel
Coroutines
Flow
Dependency Injection where appropriate
Testing infrastructure
```

Establish the project structure.

Do not implement complete screens yet.

### Deliverable

A compiling Android project with clean architecture boundaries.

---

# 12. PHASE 1 — DOMAIN MODEL

Implement core domain models first.

Required concepts include:

```text
User
Tournament
TournamentParticipant
Registration
Participant
Group
GroupMember
Match
Standing
BracketNode
TournamentSettings
DrawGeneration
AuditLog
```

Use domain models independent of UI and backend-specific representations.

Do not leak Supabase DTOs directly into domain logic.

---

# 13. PHASE 2 — TOURNAMENT STATE MACHINE

Implement the tournament lifecycle:

```text
DRAFT
    ↓
REGISTRATION_OPEN
    ↓
REGISTRATION_CLOSED
    ↓
DRAW_PENDING
    ↓
DRAW_GENERATED
    ↓
DRAW_LOCKED
    ↓
IN_PROGRESS
    ↓
COMPLETED
```

Invalid transitions must be rejected.

For example:

```text
DRAFT → IN_PROGRESS
```

must not be possible.

The state machine belongs in domain/business logic, not merely in UI navigation.

---

# 14. PHASE 3 — TOURNAMENT ENGINE

Implement the standalone tournament engine.

The engine must support:

```text
Single Elimination
League / Groups
Groups → Knockout
```

The engine must be testable without:

```text
Android UI
Compose
Activity
Fragment
Supabase
Network
Database
```

The engine must operate on domain inputs and produce structured tournament output.

---

# 15. PHASE 4 — DRAW ALGORITHM

`DRAW_ALGORITHM_SPEC.md` is the authoritative specification for fair draw generation.

Do not simplify it into:

```text
participants.shuffled()
```

Do not implement pure seeding.

Do not implement hidden heuristics.

The required approach is:

```text
Participant Input
      ↓
Metric Normalization
      ↓
Competitive Bands
      ↓
Seed Generation
      ↓
Candidate Draw Generation
      ↓
Hard Constraint Validation
      ↓
Fairness Scoring
      ↓
Repeat N Times
      ↓
Best Valid Candidate
```

---

# 16. FAIRNESS MODEL

The default fairness score is:

```text
30% Strength Distribution
25% Bracket Balance
20% Competitive Diversity
15% Opportunity
10% Randomness
```

All components must be independently implemented and testable.

The final score must be:

```text
0–100
```

Do not replace these weights with arbitrary alternatives.

If weights later become configurable, preserve these as the default configuration.

---

# 17. FAIRNESS PHILOSOPHY

The algorithm must optimize:

> **Fair opportunity without artificial equality.**

It must:

- Recognize stronger participants.
- Distribute strong participants.
- Prevent obvious strength clustering.
- Avoid systematically exposing weaker players to the strongest players.
- Preserve meaningful randomness.
- Allow same-band matches.
- Allow upsets naturally.

It must NOT:

- Guarantee underdogs easy matches.
- Guarantee strong players easy routes.
- Manipulate gameplay.
- Predict winners.
- Force an upset.
- Equalize participants artificially.

---

# 18. HARD CONSTRAINTS

Every generated structure must satisfy:

```text
No duplicate participant
No missing participant
Valid bracket size
Valid match references
Valid progression
No duplicate assignment
No impossible progression
No invalid group membership
```

A candidate violating a hard constraint is invalid regardless of its fairness score.

Never select an invalid candidate because it has a high fairness score.

---

# 19. RANDOMNESS AND REPRODUCIBILITY

Every draw generation must have:

```text
randomSeed
algorithmVersion
```

Given:

```text
same participants
same configuration
same seed
same algorithm version
```

the engine must reproduce the same result.

Given a new seed, the engine should normally produce a different valid result.

Never use hidden randomness that cannot be reproduced during debugging.

---

# 20. DRAW GENERATION

The default candidate-generation count is:

```text
500
```

Suggested scaling:

```text
8–16 participants    → 250–500
17–32 participants   → 500–1000
33–64 participants   → 750–1500
```

Make this configurable.

Do not hardcode `500` throughout the codebase.

---

# 21. DRAW REGENERATION

Before locking:

```text
Host → Regenerate Draw
```

must:

```text
Generate new random seed
Generate candidates
Validate candidates
Score candidates
Select best valid candidate
Persist generation
Replace preview
```

Previous generations should remain auditable.

After:

```text
DRAW_LOCKED
```

regeneration must be rejected.

---

# 22. DRAW LOCK

A locked draw freezes:

```text
Participant placement
Seeds
Bracket structure
Initial matches
Group assignments
Progression relationships
```

Normal operations must not modify them.

Any exceptional modification requires:

```text
Explicit host action
Confirmation
Reason
Audit log
```

---

# 23. TOURNAMENT PROGRESSION

Match results must drive progression automatically.

Example:

```text
Host submits score
        ↓
Server validates authorization
        ↓
Result is committed
        ↓
Winner determined
        ↓
Next match populated
        ↓
Bracket updated
        ↓
Participant receives realtime update
```

The host should not manually move winners through the bracket.

---

# 24. OFFICIAL SCORE SECURITY

Participants cannot modify official scores.

Hiding a button in the UI is NOT sufficient.

Authorization must be enforced server-side using Supabase:

```text
Row Level Security
and/or
Secure database functions / Edge Functions
```

The backend must verify:

```text
Authenticated user
+
Tournament membership
+
Host role
+
Tournament state
+
Match validity
```

before accepting an official result.

Never place:

```text
service_role key
```

or equivalent privileged credentials in the Android application.

---

# 25. MATCH RESULT INTEGRITY

The system must prevent:

```text
Double result submission
Double winner advancement
Invalid score
Result modification after completion
Participant appearing in conflicting active matches
Invalid next-match assignment
Unauthorized score modification
```

Completed matches must be protected against accidental mutation.

Exceptional corrections require an explicit override mechanism and audit record.

---

# 26. GROUPS AND STANDINGS

Default standings:

```text
Win   = 3 points
Draw  = 1 point
Loss  = 0 points
```

Default tiebreakers:

```text
1. Points
2. Goal Difference
3. Goals Scored
4. Head-to-Head
5. Organizer-defined final tiebreaker
```

The implementation must keep the rules configurable enough to support different games later.

Do not hardcode football-specific assumptions into generic tournament logic.

---

# 27. GROUP GENERATION

Groups must be competitively balanced using the rules in:

```text
TOURNAMENT_ENGINE_SPEC.md
DRAW_ALGORITHM_SPEC.md
```

Preferred approach:

```text
Seed
↓
Serpentine Distribution
↓
Controlled Randomization
↓
Fairness Evaluation
↓
Best Valid Structure
```

Do not simply shuffle participants into groups.

---

# 28. GROUPS → KNOCKOUT

Qualification must remain unresolved until group results are known.

The engine may represent:

```text
Group A Winner
Group B Runner-up
```

as placeholders.

Do not prematurely assign participant IDs.

If rematch restrictions are enabled, enforce them during knockout mapping.

---

# 29. BACKEND IMPLEMENTATION

After the domain engine is stable, implement Supabase integration.

Required backend concepts:

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

Follow:

```text
BACKEND_SCHEMA.md
```

as the authoritative schema reference.

Use PostgreSQL constraints where appropriate rather than relying exclusively on application validation.

---

# 30. DATABASE INTEGRITY

Important constraints include:

```text
Unique tournament/user registration
Unique tournament membership
Unique group membership
Valid participant references
Non-negative scores
Valid tournament state
Valid role
```

Database constraints should protect critical invariants even if a client is compromised.

---

# 31. SECURE SERVER OPERATIONS

Critical operations should be executed through trusted backend logic.

Recommended operations:

```text
generate_draw()
lock_draw()
submit_match_result()
recalculate_standings()
advance_knockout_winner()
complete_tournament()
```

The exact implementation may use:

```text
Postgres functions
Supabase Edge Functions
secure transactional operations
```

according to the architecture established in `TRD.md`.

---

# 32. CLIENT ARCHITECTURE

Use:

```text
UI
 ↓
ViewModel
 ↓
Use Case / Domain Logic
 ↓
Repository
 ↓
Supabase / Local Data Source
```

Do not place tournament business rules directly in:

```text
Composable functions
ViewModels
UI event handlers
```

Business logic belongs in the domain layer.

---

# 33. LOCAL DATA

Room/local caching may be used for:

- Previously loaded tournaments
- Bracket viewing
- Standings
- Participant data
- Read-only offline resilience

However:

```text
Official score submission
Draw generation
Draw locking
Critical tournament mutations
```

require confirmed connectivity and authoritative backend validation.

Offline UI must never imply that an official operation succeeded when it has not reached the server.

---

# 34. UI IMPLEMENTATION ORDER

Only after the core engine and backend foundation are stable should full UI implementation proceed.

Recommended order:

```text
Authentication
      ↓
Participant Home
      ↓
Tournament Discovery
      ↓
Tournament Detail
      ↓
Registration
      ↓
Participant Tournament View
      ↓
Host Tournament Creation
      ↓
Host Dashboard
      ↓
Participant Management
      ↓
Draw Preview
      ↓
Draw Lock
      ↓
Bracket
      ↓
Match Management
      ↓
Score Entry
      ↓
Standings
      ↓
Tournament Completion
```

---

# 35. PARTICIPANT UX

The participant experience should prioritize:

```text
Tournament status
Your next match
Opponent
Match information
Bracket
Standings
Progression
```

Participants are primarily consumers of official tournament state.

They cannot modify:

```text
Official scores
Bracket
Seeds
Standings
Match assignments
```

---

# 36. HOST UX

The host experience should prioritize operations.

The host must be able to:

```text
Create tournament
Configure tournament
Open registration
Close registration
Review participants
Generate draw
Regenerate draw
Lock draw
Manage matches
Submit scores
Handle exceptional cases
Complete tournament
```

The host console must not become a generic social dashboard.

---

# 37. UI DESIGN RULES

Follow `UI_UX.md`.

The design should be:

```text
Dark-first
Minimal
Professional
Competitive
Utility-focused
Readable
Touch-friendly
```

Preferred visual language:

```text
Background: #0B0D10
Surface: #12151A
Elevated: #181C22
Primary text: #F5F7FA
Secondary text: #9AA2AD
Accent: #2F80ED
Success: #27AE60
Warning: #F2C94C
Error: #EB5757
```

Avoid:

```text
Excessive neon
Esports clichés
Unnecessary gradients
Overly decorative cards
Gaming-style visual noise
```

The application should feel like a serious competition utility, not an esports poster.

---

# 38. MOBILE BRACKET

Do not compress a bracket until it becomes unreadable.

Use:

```text
Horizontal scrolling
Clear round labels
Large enough match cards
Visible participant names
Clear progression
```

Prioritize information hierarchy over fitting the entire bracket on one screen.

---

# 39. SCORE ENTRY

Score entry must use large touch targets.

Recommended flow:

```text
Enter Score
      ↓
Review
      ↓
Confirm Result
      ↓
Server Validation
      ↓
Official Result
```

Do not make a score official immediately after the first accidental tap.

---

# 40. REALTIME

Use Supabase Realtime for appropriate tournament updates.

Important realtime events include:

```text
Match result updated
Bracket progression updated
Standings updated
Tournament status changed
Participant status changed
Draw locked
```

The client must reconcile realtime data with authoritative backend state.

Do not treat local optimistic state as authoritative for critical tournament data.

---

# 41. ERROR HANDLING

Errors must be explicit.

Never silently:

```text
Ignore failed score submission
Ignore draw-generation failure
Ignore synchronization failure
Ignore authorization failure
```

Provide useful user-facing states:

```text
Loading
Empty
Error
Retry
Offline
Unauthorized
Locked
Completed
```

Internal logs should contain enough context to debug the failure.

Do not expose sensitive backend information to users.

---

# 42. TESTING REQUIREMENTS

Testing is mandatory.

Minimum layers:

```text
Domain unit tests
Algorithm unit tests
Property-based tests
Repository tests
Backend integration tests
UI tests for critical flows
```

---

# 43. ENGINE TESTING

Before UI integration, verify:

```text
Metric normalization
Competitive bands
Seed generation
Bracket generation
Bye allocation
Group generation
Knockout mapping
Fairness scoring
Hard constraints
Candidate optimization
Randomness
Reproducibility
```

Test participant counts:

```text
1
2
3
4
5
7
8
9
16
17
32
33
64
```

---

# 44. DETERMINISTIC TEST

The following must always produce the same result:

```text
participants = X
configuration = Y
seed = Z
algorithmVersion = V
```

Repeated executions must return structurally identical results.

This is a hard requirement.

---

# 45. PROPERTY TESTING

For arbitrary valid participant sets:

```text
Every participant appears exactly once.
No participant is duplicated.
No participant is missing.
Bracket size is valid.
All match references are valid.
Progression is valid.
Fairness score remains within 0–100.
```

For groups:

```text
Every participant belongs to exactly one group.
Group assignments are valid.
Generated matches are valid.
```

---

# 46. PERFORMANCE TESTING

Target:

```text
8–64 participants
```

Default candidate generation should be performant enough for interactive host use.

Target:

```text
Typical draw generation < 1 second
```

on a modern device.

Measure before optimizing.

Do not prematurely sacrifice algorithm quality for performance.

---

# 47. SECURITY RULES

Never:

```text
Store privileged backend keys in APK
Trust client-side role checks
Trust client-submitted tournament state
Trust client-submitted winner advancement
Allow participants to modify official scores
Bypass RLS for convenience
```

Security must exist at the backend boundary.

---

# 48. DATA OWNERSHIP

The backend is authoritative for:

```text
Tournament state
Participants
Registrations
Draw
Seeds
Matches
Official scores
Standings
Progression
Audit records
```

The client is responsible for:

```text
Displaying state
Collecting input
Requesting operations
Handling presentation state
```

Never reverse this relationship.

---

# 49. AUDITABILITY

Important administrative operations should generate audit records.

At minimum:

```text
Draw generated
Draw regenerated
Draw locked
Score submitted
Score overridden
Participant modified
Participant withdrawn
Participant disqualified
Tournament state changed
```

Each record should identify:

```text
Actor
Action
Entity
Timestamp
Relevant metadata
Reason where applicable
```

---

# 50. IMPLEMENTATION DISCIPLINE

For every feature:

```text
Read relevant specification
        ↓
Identify dependencies
        ↓
Define domain behavior
        ↓
Implement smallest correct version
        ↓
Compile
        ↓
Run tests
        ↓
Review against specification
        ↓
Fix issues
        ↓
Mark task complete
```

Do not mark a feature complete merely because:

```text
the screen exists
```

or:

```text
the code compiles
```

It is complete only when the underlying behavior works.

---

# 51. NO FAKE IMPLEMENTATIONS

Do not use fake:

```text
Brackets
Participants
Scores
Standings
Tournament states
Draw results
Backend responses
```

as the final implementation.

Mock data is acceptable only for:

```text
UI previews
unit tests
temporary development scaffolding
```

and must be clearly isolated.

---

# 52. NO HARDCODED BUSINESS DATA

Never hardcode:

```text
Participant names
Bracket assignments
Winner IDs
Tournament IDs
Scores
Group membership
Seeds
```

Business data must come from:

```text
database
domain engine
configuration
user input
```

---

# 53. NO DUPLICATED BUSINESS LOGIC

Do not independently implement tournament rules in:

```text
Compose
ViewModel
Repository
Backend
```

without a deliberate synchronization strategy.

There must be one authoritative definition of each business rule.

Especially avoid duplicate implementations of:

```text
Winner calculation
Standings calculation
Fairness scoring
Bracket progression
Tournament state transitions
```

---

# 54. DEVELOPMENT CHECKPOINTS

The project must reach these checkpoints in order.

### CHECKPOINT 1

```text
Android project compiles.
```

### CHECKPOINT 2

```text
Domain models compile.
```

### CHECKPOINT 3

```text
Tournament state machine passes tests.
```

### CHECKPOINT 4

```text
Tournament engine generates valid structures.
```

### CHECKPOINT 5

```text
Fair draw algorithm passes deterministic/property tests.
```

### CHECKPOINT 6

```text
Supabase schema and authorization work.
```

### CHECKPOINT 7

```text
Host can create and configure tournament.
```

### CHECKPOINT 8

```text
Participants can register.
```

### CHECKPOINT 9

```text
Host can generate and lock draw.
```

### CHECKPOINT 10

```text
Host can submit official match results.
```

### CHECKPOINT 11

```text
Winner progression and standings work.
```

### CHECKPOINT 12

```text
Tournament can complete correctly.
```

### CHECKPOINT 13

```text
Realtime synchronization works.
```

### CHECKPOINT 14

```text
End-to-end MVP flow works.
```

---

# 55. END-TO-END ACCEPTANCE TEST

The final MVP must successfully execute:

```text
1. User creates account.

2. User creates tournament.

3. Host selects:
   - Game
   - Format
   - Participant limit
   - Tournament settings

4. Registration opens.

5. Multiple participants register with:
   - Name
   - In-game ID
   - OVR / Team Strength

6. Host closes registration.

7. System validates participants.

8. System generates competitive classifications.

9. System generates multiple draw candidates.

10. System validates candidates.

11. System calculates fairness scores.

12. System selects the best valid candidate.

13. Host previews draw.

14. Host optionally regenerates.

15. Host locks draw.

16. Participants see official structure.

17. Host enters match result.

18. Backend validates host authorization.

19. Result becomes official.

20. Winner automatically advances.

21. Standings update where applicable.

22. Participants receive updated tournament state.

23. Matches continue.

24. Final match completes.

25. Champion is determined.

26. Tournament becomes COMPLETED.
```

Every step must work using real application state.

---

# 56. AGENT WORKING RULES

When working on a task:

### BEFORE CODING

Read:

```text
MASTER_INSTRUCTION.md
```

and the relevant specification documents.

Determine:

```text
What is being implemented?
What depends on it?
What must not change?
What tests are required?
```

### DURING CODING

Prefer:

```text
Small
Testable
Composable
Maintainable
Explicit
```

implementation.

Avoid speculative abstractions.

### AFTER CODING

Always:

```text
Compile
Test
Inspect errors
Review architecture
Review specification compliance
```

Do not stop at compilation.

---

# 57. WHEN SOMETHING FAILS

Do not hide failures.

If compilation fails:

```text
Fix compilation.
```

If a test fails:

```text
Determine whether:
1. Implementation is wrong.
2. Test is wrong.
3. Specification is contradictory.
```

If backend behavior fails:

```text
Inspect authorization,
database constraints,
transaction behavior,
and data flow.
```

If UI behavior fails:

```text
Trace:
UI
→ ViewModel
→ Use Case
→ Repository
→ Backend
```

Do not patch symptoms blindly.

---

# 58. AGENT SELF-REVIEW

Before declaring any milestone complete, verify:

```text
[ ] Does the implementation follow MASTER_INSTRUCTION.md?
[ ] Does it follow DRAW_ALGORITHM_SPEC.md?
[ ] Does it follow TOURNAMENT_ENGINE_SPEC.md?
[ ] Does it follow BACKEND_SCHEMA.md?
[ ] Does it follow TRD.md?
[ ] Does it follow UI_UX.md?
[ ] Is business logic outside UI?
[ ] Are permissions enforced server-side?
[ ] Are critical operations auditable?
[ ] Are hard constraints enforced?
[ ] Is the draw reproducible?
[ ] Are locked structures protected?
[ ] Are tests passing?
[ ] Are there any fake/hardcoded business values?
[ ] Did the implementation add out-of-scope features?
[ ] Does the end-to-end flow still work?
```

---

# 59. TASK MANAGEMENT

Use an explicit implementation checklist.

Recommended structure:

```text
## FOUNDATION
- [ ] Project setup
- [ ] Dependencies
- [ ] Architecture

## DOMAIN
- [ ] Domain models
- [ ] State machine
- [ ] Tournament engine

## DRAW ENGINE
- [ ] Metric normalization
- [ ] Competitive bands
- [ ] Seed generation
- [ ] Bracket generation
- [ ] Group generation
- [ ] Fairness scorer
- [ ] Candidate generation
- [ ] Constraint validation
- [ ] Bye handling
- [ ] Reproducibility
- [ ] Draw locking

## TESTING
- [ ] Unit tests
- [ ] Property tests
- [ ] Deterministic tests
- [ ] Edge cases
- [ ] Performance tests

## BACKEND
- [ ] Supabase project
- [ ] Database schema
- [ ] RLS
- [ ] Secure operations
- [ ] Audit logs
- [ ] Realtime

## APPLICATION
- [ ] Authentication
- [ ] Participant flow
- [ ] Host flow
- [ ] Registration
- [ ] Draw
- [ ] Bracket
- [ ] Match result
- [ ] Standings
- [ ] Completion

## POLISH
- [ ] Loading states
- [ ] Empty states
- [ ] Error states
- [ ] Offline states
- [ ] Accessibility
- [ ] Performance
- [ ] Visual consistency

## FINAL
- [ ] End-to-end test
- [ ] Security review
- [ ] Architecture review
- [ ] Scope review
- [ ] Release build
```

Mark tasks complete only after verification.

---

# 60. FINAL IMPLEMENTATION ORDER

The complete build sequence is:

```text
                    ┌─────────────────────┐
                    │  PROJECT FOUNDATION │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │    DOMAIN MODELS    │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │  STATE MACHINE      │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ TOURNAMENT ENGINE   │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ FAIR DRAW ALGORITHM │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ ENGINE TESTING      │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ SUPABASE + DATABASE │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ SECURITY / RLS      │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ BACKEND OPERATIONS  │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ REPOSITORIES / APP  │
                    └──────────┬──────────┘
                               ↓
              ┌────────────────┴────────────────┐
              ↓                                 ↓
    ┌────────────────────┐           ┌────────────────────┐
    │    HOST CONSOLE    │           │ PARTICIPANT CONSOLE│
    └──────────┬─────────┘           └──────────┬─────────┘
               └────────────────┬───────────────┘
                                ↓
                    ┌─────────────────────┐
                    │ REALTIME / SYNC     │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ POLISH / HARDENING  │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │ FINAL E2E VALIDATION│
                    └─────────────────────┘
```

This order is mandatory unless a genuine technical dependency requires a minor deviation.

If a deviation is necessary, document it before proceeding.

---

# 61. FINAL PRODUCT PRINCIPLE

The application is not merely a bracket generator.

It is a tournament operating system.

The system should allow the host to say:

```text
Set the rules.
```

and then allow the platform to handle:

```text
Registration
Classification
Fair draw
Bracket
Matches
Results
Standings
Progression
Completion
```

The core engine must remain:

```text
Transparent
Fair
Randomized
Reproducible
Secure
Testable
```

The system should never decide who wins.

It should ensure that the **structure of the competition does not unfairly decide who gets the chance to win.**

> **Better structure. Better competition. More meaningful upsets.**

---

# 62. BUILD PHASE COMMAND

From this point onward:

```text
STOP SPECIFICATION DESIGN.
START IMPLEMENTATION.
```

Do not create additional product-specification documents unless a genuine implementation blocker exposes a contradiction or missing requirement.

The specifications are now considered **frozen**.

Any change to product behavior must be deliberate, documented, and justified against the existing architecture.

Begin with:

```text
PHASE 0 — PROJECT FOUNDATION
```

and proceed strictly through the implementation order defined above.