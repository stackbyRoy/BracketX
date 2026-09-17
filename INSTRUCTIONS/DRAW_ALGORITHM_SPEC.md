# DRAW ALGORITHM SPECIFICATION

## Fair Tournament Draw Engine

**Document Status:** Final  
**Implementation Phase:** Build Phase  
**Priority:** Critical  
**Applies To:** Single Elimination, League/Groups, Groups → Knockout  
**Primary Backend:** Supabase + PostgreSQL  
**Primary Client:** Android / Kotlin  
**Algorithm Type:** Constrained randomized optimization  
**ML/AI Required:** No

---

# 1. Purpose

This document defines the exact algorithmic behavior of the tournament draw system.

The draw engine must produce tournament structures that are:

- Fair
- Randomized
- Reproducible
- Explainable
- Deterministic given a supplied random seed
- Resistant to obvious unfair clustering
- Compatible with tournament progression rules
- Independent of UI and backend implementation

The engine must **not** attempt to predict match outcomes.

Its purpose is to construct a competition structure that gives participants legitimate opportunities while preserving appropriate seeding for stronger participants.

> **Core principle: Fair opportunity, not artificial equality.**

A weaker participant should have a genuine chance of progressing.

A stronger participant should still be appropriately recognized as stronger.

The algorithm must never guarantee outcomes or intentionally manipulate matches to produce an upset.

---

# 2. Scope

This specification covers:

1. Participant normalization
2. Competitive band calculation
3. Seed generation
4. Single-elimination bracket generation
5. Bye allocation
6. Group generation
7. Groups → knockout generation
8. Candidate draw generation
9. Hard constraint validation
10. Fairness scoring
11. Candidate comparison
12. Randomness
13. Reproducibility
14. Draw regeneration
15. Draw locking
16. Algorithm versioning
17. Testing requirements

This specification does **not** cover:

- Match result verification
- Anti-cheat systems
- Computer vision
- Gameplay analysis
- Player skill prediction
- AI opponent modeling
- Global player rankings
- Betting or gambling
- Tournament scheduling optimization beyond structural match generation

---

# 3. Design Philosophy

The engine must avoid two extremes.

### Extreme A — Pure Randomness

Every participant is treated identically.

Problems:

- Strong participants may cluster together.
- Weak participants may receive systematically difficult paths.
- Tournament structure may become unnecessarily unbalanced.
- Seeding information becomes meaningless.

### Extreme B — Pure Seeding

Participants are placed almost entirely according to competitive rating.

Problems:

- Draw becomes predictable.
- Randomness disappears.
- Participants repeatedly face expected opponents.
- Lower-rated players receive no meaningful upset opportunities.

### Required Approach

Use:

```text
Competitive Information
        +
Controlled Randomness
        +
Structural Constraints
        +
Fairness Optimization
```

The engine generates multiple valid candidate draws and selects the candidate with the strongest fairness score.

---

# 4. Algorithm Pipeline

The complete pipeline is:

```text
Raw Participants
      ↓
Validate Input
      ↓
Normalize Competitive Metrics
      ↓
Calculate Competitive Bands
      ↓
Generate Seeds
      ↓
Generate Candidate Draw
      ↓
Apply Hard Constraints
      ↓
Calculate Fairness Score
      ↓
Repeat N Times
      ↓
Compare Valid Candidates
      ↓
Select Best Candidate
      ↓
Persist Random Seed + Algorithm Version
      ↓
Preview
      ↓
Optional Regeneration
      ↓
Lock
```

---

# 5. Input Model

The engine receives a normalized participant list.

```kotlin
data class TournamentParticipant(
    val id: String,
    val displayName: String,
    val inGameId: String,
    val competitiveMetric: Double,
    val organizerSeed: Int? = null
)
```

The engine must not depend on Android-specific classes.

---

# 6. Competitive Metric

Different games expose different competitive indicators.

Examples:

| Game | Registration Metric |
|---|---|
| FC Mobile | OVR |
| eFootball | Team Strength |

The engine internally converts the game-specific metric into:

```text
competitiveScore ∈ [0, 100]
```

Higher value means stronger competitive rating.

Example:

```text
FC Mobile OVR  = 115
→ competitiveScore = 92

FC Mobile OVR  = 110
→ competitiveScore = 74
```

The exact normalization function is game-specific and must be configurable.

---

# 7. Metric Normalization

## 7.1 Preferred Method

If reliable game-specific minimum and maximum values are known:

```text
score =
((metric - minimum) / (maximum - minimum)) × 100
```

Clamp:

```text
score = max(0, min(100, score))
```

## 7.2 Tournament-relative Normalization

If global bounds are unavailable or unsuitable, normalize relative to the tournament:

```text
relativeScore =
((metric - tournamentMin) /
 (tournamentMax - tournamentMin)) × 100
```

If:

```text
tournamentMax == tournamentMin
```

then every participant receives:

```text
competitiveScore = 50
```

This prevents division-by-zero and correctly treats the tournament as competitively uniform.

---

# 8. Competitive Bands

Participants should be assigned to relative competitive bands.

Recommended bands:

```text
S — Elite
A — Strong
B — Mid
C — Developing
D — Underdog
```

The bands should preferably be calculated relative to the tournament population rather than using rigid global thresholds.

Example for 32 participants:

```text
Top ~10%      → S
Next ~20%     → A
Middle ~40%   → B
Next ~20%     → C
Bottom ~10%   → D
```

Exact percentile boundaries may be configured.

For very small tournaments, bands may collapse naturally.

Example:

```text
8 participants:

S → top 1
A → next 2
B → middle 2
C → next 2
D → bottom 1
```

If there are too few participants for meaningful five-band classification, the engine must reduce the number of active bands rather than fabricate distinctions.

---

# 9. Seed Generation

Primary ordering:

```text
competitiveScore DESC
```

Tie resolution:

```text
1. organizerSeed, if provided
2. deterministic randomized ordering using draw seed
```

The engine must not use participant IDs as the only tie-breaker because that can create hidden deterministic bias.

---

# 10. Seed Philosophy

Seeding exists to:

- Distribute stronger participants.
- Reduce accidental clustering.
- Provide structural balance.
- Preserve competitive hierarchy.

Seeding does **not** mean:

> Seed #1 must receive the easiest possible route.

Instead:

> Strong participants should be distributed across the tournament structure so they do not unnecessarily eliminate one another in the opening round.

---

# 11. Single-Elimination Bracket Size

For `N` participants:

```text
bracketSize = smallest power of 2 >= N
```

Examples:

```text
5 participants  → 8-slot bracket
8 participants  → 8-slot bracket
13 participants → 16-slot bracket
20 participants → 32-slot bracket
32 participants → 32-slot bracket
```

---

# 12. Byes

When:

```text
N < bracketSize
```

the number of byes is:

```text
byes = bracketSize - N
```

Example:

```text
13 participants
16 bracket slots
3 byes
```

Byes must be distributed using a combination of:

- Seed position
- Bracket balance
- Competitive distribution
- Controlled randomness

The engine must NOT simply:

```text
Give all byes to highest seeds
```

or:

```text
Give all byes to lowest seeds
```

The objective is structural fairness.

Higher seeds may receive preference for byes when necessary for conventional seeding, but this must be balanced against the overall draw score.

---

# 13. Bracket Positioning

The engine should initially establish an idealized seed structure.

For a 16-slot bracket:

```text
Seed 1  ─┐
         ├─
Seed 16 ─┘

Seed 8  ─┐
         ├─
Seed 9  ─┘

Seed 5  ─┐
         ├─
Seed 12 ─┘

Seed 4  ─┐
         ├─
Seed 13 ─┘

Seed 3  ─┐
         ├─
Seed 14 ─┘

Seed 6  ─┐
         ├─
Seed 11 ─┘

Seed 7  ─┐
         ├─
Seed 10 ─┘

Seed 2  ─┐
         ├─
Seed 15 ─┘
```

This is a conceptual starting structure, not the final mandatory placement.

Controlled randomization may swap compatible positions.

---

# 14. Draw Candidate Generation

A candidate is one complete possible tournament structure.

The engine should generate candidates using:

```text
seed structure
+
controlled randomization
+
fairness constraints
```

Recommended process:

```text
1. Generate ideal seed slots.
2. Identify equivalent/compatible slot groups.
3. Randomly permute participants within compatible groups.
4. Insert byes.
5. Build all matches.
6. Validate.
7. Score.
```

---

# 15. Hard Constraints

Hard constraints are non-negotiable.

A candidate is invalid if any of the following occur.

## 15.1 Duplicate Participant

A participant cannot appear in multiple initial slots.

```text
count(participantId) <= 1
```

---

## 15.2 Missing Participant

Every eligible participant must appear exactly once.

```text
assignedParticipants == eligibleParticipants
```

---

## 15.3 Invalid Bracket Size

Bracket size must be a power of two for standard single elimination.

---

## 15.4 Invalid Match Reference

Every match must reference either:

```text
participant
```

or:

```text
winner of previous match
```

Never reference a nonexistent entity.

---

## 15.5 Invalid Progression

A winner must have exactly one valid next destination.

---

## 15.6 Duplicate Match Assignment

A participant cannot be scheduled into two simultaneous matches.

---

## 15.7 Locked Draw Mutation

Once locked:

```text
participant placement
seed
match relationship
bracket structure
```

must not change through ordinary operations.

---

# 16. Soft Fairness Constraints

Soft constraints affect the candidate score.

They do not automatically invalidate a candidate.

Examples:

- Strongest participants clustered in one quarter.
- Too many high-rated vs high-rated opening matches.
- Too many low-rated vs low-rated opening matches.
- Uneven competitive strength between bracket sections.
- Excessive same-band pairings.
- Poor underdog distribution.
- Excessive structural advantage caused by byes.

The engine should optimize these rather than enforce unrealistic perfection.

---

# 17. Fairness Score

Each valid candidate receives a score:

```text
fairnessScore ∈ [0, 100]
```

Recommended weighted model:

```text
Fairness Score =
    30% Strength Distribution
  + 25% Bracket Balance
  + 20% Competitive Diversity
  + 15% Opportunity
  + 10% Randomness
```

Weights must be constants/configuration values rather than scattered magic numbers.

```kotlin
data class FairnessWeights(
    val strengthDistribution: Double = 0.30,
    val bracketBalance: Double = 0.25,
    val competitiveDiversity: Double = 0.20,
    val opportunity: Double = 0.15,
    val randomness: Double = 0.10
)
```

Weights must sum to:

```text
1.0
```

---

# 18. Component 1 — Strength Distribution

### Weight: 30%

Purpose:

Prevent high-strength participants from being concentrated into the same portion of the bracket.

For a bracket divided into `Q` major sections, calculate:

```text
strength(section) =
sum(competitiveScore of participants in section)
```

Then calculate deviation from the ideal average:

```text
idealStrength =
totalStrength / numberOfSections
```

For each section:

```text
deviation =
abs(sectionStrength - idealStrength)
```

Aggregate:

```text
totalDeviation =
sum(section deviations)
```

Normalize into `[0,100]`.

A candidate with evenly distributed strength receives a higher score.

---

# 19. Component 2 — Bracket Balance

### Weight: 25%

The bracket should not contain a disproportionately difficult section.

For each major bracket section calculate:

```text
sectionStrength
sectionAverageStrength
eliteCount
strongCount
underdogCount
```

Calculate the spread:

```text
max(sectionStrength) - min(sectionStrength)
```

Lower spread = better balance.

The score should be inversely proportional to the normalized spread.

Example:

```text
Candidate A:
Quarter strength:
[410, 405, 412, 408]

Candidate B:
Quarter strength:
[500, 360, 390, 385]
```

Candidate A receives a significantly higher bracket-balance score.

---

# 20. Component 3 — Competitive Diversity

### Weight: 20%

Opening rounds should not systematically consist of participants from the same competitive band.

For every first-round match calculate:

```text
bandPair =
(S,S)
(S,A)
(A,B)
(C,D)
etc.
```

The engine should favor a healthy mixture of:

- Strong vs mid
- Strong vs developing
- Mid vs mid
- Developing vs underdog

However, same-band matches must remain possible.

Do NOT implement:

```text
S can never play S
A can never play A
```

That would make the draw artificial.

Instead, penalize excessive same-band clustering.

---

# 21. Competitive Distance

For a match:

```text
distance =
abs(scoreA - scoreB)
```

Very high distance indicates a likely mismatch.

Very low distance indicates a closely matched pairing.

The engine must not minimize distance globally.

If it did, the result would become:

```text
strong vs strong
medium vs medium
weak vs weak
```

That defeats the purpose of meaningful upset opportunities.

Instead, the algorithm should penalize **extreme clustering and repeated mismatches**, while preserving randomness.

---

# 22. Component 4 — Opportunity Score

### Weight: 15%

This component evaluates whether lower-rated participants have reasonable access to the tournament.

Important:

> The algorithm must not guarantee an easy opponent to any participant.

Instead, calculate whether weaker participants are disproportionately exposed to the strongest participants.

For example:

```text
Bad:
D1 → S1
D2 → S2
D3 → S3
D4 → S4
```

This creates systematic disadvantage.

Better:

```text
D1 → B
D2 → C
D3 → A
D4 → B
```

with appropriate randomness.

The opportunity score should therefore penalize:

```text
underdog → elite
```

pairings when they occur disproportionately across the draw.

One such pairing is acceptable.

Systematic concentration is not.

---

# 23. Component 5 — Randomness Score

### Weight: 10%

The draw must remain a draw.

A mathematically optimal arrangement that always produces the same structural result is undesirable.

The engine should reward candidates that preserve controlled randomness.

This score should consider:

- Number of randomized placements.
- Deviation from the canonical seed arrangement.
- Random slot selections.
- Participant permutation.

Do not reward randomness blindly.

A completely random candidate that produces a structurally poor bracket must still score badly.

---

# 24. Fairness Objective

The optimization target is:

```text
maximize(FairnessScore)
```

subject to:

```text
all hard constraints = true
```

Formally:

```text
D* = argmax D∈ValidDraws F(D)
```

where:

```text
D = candidate draw
F(D) = fairness score
ValidDraws = all candidates satisfying hard constraints
```

---

# 25. Candidate Search

The engine must not attempt to evaluate every mathematically possible bracket.

That becomes computationally expensive as participant count increases.

Instead use randomized sampling.

Recommended default:

```text
N = 500 candidates
```

For typical tournaments:

```text
8–16 participants  → 250–500
17–32 participants → 500–1000
33–64 participants → 750–1500
```

The exact value should be configurable.

Example:

```kotlin
data class DrawConfig(
    val candidateCount: Int = 500
)
```

---

# 26. Candidate Selection

Pseudo-code:

```text
bestCandidate = null
bestScore = -∞

repeat(candidateCount):

    candidate = generateCandidate(random)

    if !validateHardConstraints(candidate):
        continue

    score = calculateFairnessScore(candidate)

    if score > bestScore:
        bestCandidate = candidate
        bestScore = score

return bestCandidate
```

If no valid candidate is produced:

```text
throw DrawGenerationException
```

Do not silently fall back to an invalid or unscored bracket.

---

# 27. Random Seed

Every draw generation must use an explicit random seed.

Example:

```text
randomSeed = 839174625
```

The seed must be stored with:

```text
draw_generation
```

alongside:

```text
algorithm_version
fairness_score
generated_at
generated_by
```

---

# 28. Reproducibility

Given identical:

```text
participant input
+
tournament configuration
+
algorithm version
+
random seed
```

the engine must generate the same candidate sequence and final result.

This allows:

- Debugging
- Auditing
- Reproduction
- Dispute investigation
- Automated testing

---

# 29. Important Randomness Rule

Two independent draw generations should NOT always produce the same bracket.

Therefore:

```text
same participants
+
new random seed
```

should normally produce a different valid draw.

However:

```text
same participants
+
same configuration
+
same algorithm version
+
same seed
```

must reproduce the same draw.

---

# 30. Regeneration

Before draw lock:

```text
Host → Regenerate
```

must:

1. Generate a new random seed.
2. Run the algorithm again.
3. Generate candidate draws.
4. Select the highest-scoring candidate.
5. Store a new draw-generation record.
6. Replace the current preview.
7. Preserve previous generation history.

Example:

```text
Generation 1
Seed: 381920
Score: 86.4

Generation 2
Seed: 920183
Score: 89.1

Generation 3
Seed: 713624
Score: 88.7
```

Generation 2 may become the selected preview.

---

# 31. Draw Lock

After host selects:

```text
LOCK DRAW
```

the system must persist:

```text
draw_locked = true
```

and freeze:

- Participant positions
- Seeds
- Bracket structure
- Initial matches
- Group assignments
- Match relationships

Normal participant or host actions must not mutate these structures.

If an exceptional override is later required, it must use an explicit override operation and generate an audit record.

---

# 32. Draw Audit Record

Each generation should store:

```text
draw_generation
----------------
id
tournament_id
algorithm_version
random_seed
candidate_count
fairness_score
generated_by
generated_at
locked_at
configuration_snapshot
```

Recommended:

```text
configuration_snapshot JSONB
```

This ensures the exact conditions under which a draw was created can be reconstructed.

---

# 33. Group Generation

For league/group formats, the same principles apply.

Participants should first be ordered by competitive strength.

Then distribute them across groups using a serpentine strategy.

Example:

```text
Group A ← Seed 1
Group B ← Seed 2
Group C ← Seed 3
Group D ← Seed 4

Group D ← Seed 5
Group C ← Seed 6
Group B ← Seed 7
Group A ← Seed 8

Group A ← Seed 9
Group B ← Seed 10
...
```

This creates balanced aggregate strength.

---

# 34. Group Randomization

After initial strength balancing, controlled randomization may swap participants between compatible positions.

The same fairness scoring model should be used where applicable.

The engine should avoid:

```text
Group A = all strongest participants
Group B = all weakest participants
```

It should also avoid making every group mathematically identical.

Some variation is expected and desirable.

---

# 35. Group Match Generation

For a group containing `N` participants:

```text
matches = N × (N - 1) / 2
```

Example:

```text
4 participants
= 4 × 3 / 2
= 6 matches
```

For double round-robin:

```text
matches = N × (N - 1)
```

Example:

```text
4 participants
= 4 × 3
= 12 matches
```

---

# 36. Group Strength Balance

For each group:

```text
groupStrength =
sum(competitiveScore)
```

The objective is to minimize:

```text
max(groupStrength) - min(groupStrength)
```

while maintaining randomness.

The group assignment algorithm should therefore:

```text
seed
→ serpentine distribution
→ controlled swaps
→ validate
→ score
→ repeat
→ select best
```

---

# 37. Groups → Knockout

The engine must support configurable qualification rules.

Example:

```text
Group A → top 2
Group B → top 2
Group C → top 2
Group D → top 2
```

Result:

```text
8 knockout participants
```

Qualification slots must not be resolved before group results exist.

The bracket can contain placeholders such as:

```text
Winner Group A
Runner-up Group B
```

but the engine must not prematurely convert these into participant IDs.

---

# 38. Qualification Constraints

If rematches are prohibited:

```text
Group A Winner
```

must not be paired against:

```text
Group A Runner-up
```

if both participants came from the same group.

If such restrictions make the bracket impossible, the engine must:

1. Attempt alternative valid mappings.
2. Generate additional candidates.
3. Return an explicit failure if no valid mapping exists.

Never silently violate the rule.

---

# 39. Knockout Mapping

Qualification mapping should be represented explicitly.

Example:

```text
Quarterfinal 1:
Group A Winner vs Group B Runner-up

Quarterfinal 2:
Group B Winner vs Group A Runner-up
```

The mapping itself must be part of the generated tournament structure.

---

# 40. Draw Fairness Explanation

The system should be able to expose a simplified explanation to the host.

Example:

```text
Fairness Score: 88.7

✓ Strong participants distributed across bracket sections
✓ Competitive bands reasonably mixed
✓ No major strength imbalance
✓ Underdog exposure is balanced
✓ Randomized draw preserved
```

The user should not see raw mathematical internals unless an advanced/debug view is later implemented.

---

# 41. Fairness Score Interpretation

Recommended interpretation:

```text
90–100 → Excellent
80–89  → Strong
70–79  → Acceptable
60–69  → Weak
<60    → Poor
```

These thresholds are informational only.

A candidate with a lower score may still be selected if:

- It is valid.
- The score difference between candidates is negligible.
- Randomness preservation is preferable.

---

# 42. Near-Tie Randomization

To prevent the algorithm from becoming overly deterministic, candidates with nearly identical scores may be selected probabilistically.

Example:

```text
Candidate A = 89.31
Candidate B = 89.27
Candidate C = 89.24
```

Instead of always choosing A, the engine may use a small score tolerance:

```text
tieTolerance = 0.10
```

Candidates within the tolerance of the best score become eligible.

Then select one using the draw RNG.

This preserves randomness without sacrificing fairness.

For MVP, deterministic highest-score selection is acceptable if implementing probabilistic near-tie selection would materially complicate the engine.

---

# 43. Avoiding Algorithmic Bias

The engine must not use:

- Participant name alphabetical order
- In-game ID alphabetical order
- Database insertion order
- Account creation date
- User ID lexical order

as hidden competitive factors.

Such values may be used only as deterministic fallback mechanisms when absolutely necessary, never as a fairness factor.

---

# 44. Host Organizer Seed

An optional organizer seed may exist.

Example:

```text
organizerSeed = 1
```

This may be used for:

- Officially seeded players
- Defending champion
- Tournament-specific qualification
- Organizer-defined ranking

However:

```text
organizerSeed
```

must never silently override all fairness calculations.

Organizer seeding should be explicit and visible in the tournament configuration.

---

# 45. Overrides

Host overrides are allowed only for exceptional cases.

Examples:

- Participant withdrawal
- Administrative correction
- Disqualification
- Verified registration error
- Tournament rule enforcement

An override must require:

```text
confirmation
+
reason
+
audit log
```

Example:

```text
Override:
Participant A moved from Match 3 to Match 7.

Reason:
Duplicate registration detected.

Actor:
Host ID

Timestamp:
...
```

---

# 46. Withdrawal Handling

Before draw lock:

```text
withdraw participant
→ remove from candidate pool
→ regenerate draw
```

After draw lock:

Do not silently regenerate the entire tournament.

The engine should apply the tournament's configured withdrawal policy.

Possible behavior:

```text
withdrawn participant → opponent receives advancement
```

or:

```text
host replacement policy
```

The chosen behavior must be explicit.

---

# 47. Disqualification

A disqualification after a match has started must not modify historical match results.

Instead:

```text
participant.status = DISQUALIFIED
```

and the engine determines the next valid progression according to tournament rules.

Historical audit data must remain intact.

---

# 48. Match Integrity

The engine must maintain these invariants:

```text
A participant cannot play two active matches simultaneously.
```

```text
A completed match cannot be completed twice.
```

```text
A winner cannot advance twice.
```

```text
A match cannot receive a second winner.
```

```text
A participant cannot be inserted into an already occupied progression slot.
```

```text
A locked match structure cannot be silently replaced.
```

---

# 49. Score Validation

For a completed match:

```text
scoreA >= 0
scoreB >= 0
```

Knockout match:

```text
scoreA != scoreB
```

unless the tournament configuration explicitly supports:

```text
draw → extra time
draw → penalties
```

Group match:

```text
scoreA == scoreB
```

is allowed.

The engine must not assume every game uses football-style scoring rules forever.

Scoring rules should therefore be configurable at the tournament/game-rule level.

---

# 50. Architecture

The algorithm must be implemented as a standalone domain module.

Recommended structure:

```text
domain/
└── tournament/
    ├── engine/
    │   ├── TournamentEngine.kt
    │   ├── DrawEngine.kt
    │   ├── DrawCandidateGenerator.kt
    │   ├── FairnessScorer.kt
    │   ├── ConstraintValidator.kt
    │   ├── SeedGenerator.kt
    │   ├── GroupGenerator.kt
    │   ├── BracketGenerator.kt
    │   └── models/
    │       ├── TournamentStructure.kt
    │       ├── DrawCandidate.kt
    │       ├── FairnessScore.kt
    │       └── DrawConfiguration.kt
    └── ...
```

The engine must not directly depend on:

```text
Compose
ViewModel
Activity
Fragment
Supabase SDK
Android Context
UI state
```

---

# 51. Core Interfaces

Recommended abstraction:

```kotlin
interface DrawEngine {
    fun generateDraw(
        participants: List<TournamentParticipant>,
        config: DrawConfiguration,
        seed: Long
    ): TournamentStructure
}
```

Fairness scoring:

```kotlin
interface FairnessScorer {
    fun score(
        structure: TournamentStructure,
        participants: List<TournamentParticipant>,
        config: DrawConfiguration
    ): FairnessScore
}
```

Constraint validation:

```kotlin
interface DrawValidator {
    fun validate(
        structure: TournamentStructure,
        participants: List<TournamentParticipant>
    ): ValidationResult
}
```

---

# 52. Fairness Score Model

Use component-level scores.

```kotlin
data class FairnessScore(
    val total: Double,
    val strengthDistribution: Double,
    val bracketBalance: Double,
    val competitiveDiversity: Double,
    val opportunity: Double,
    val randomness: Double
)
```

All component scores:

```text
0.0–100.0
```

Total:

```text
total =
    strengthDistribution * 0.30 +
    bracketBalance * 0.25 +
    competitiveDiversity * 0.20 +
    opportunity * 0.15 +
    randomness * 0.10
```

Round only for presentation.

Internal calculations should retain sufficient precision.

---

# 53. Pseudocode — Complete Single-Elimination Algorithm

```text
function generateSingleElimination(participants, config, seed):

    validateInput(participants)

    normalized =
        normalizeCompetitiveMetrics(participants)

    bands =
        assignCompetitiveBands(normalized)

    seededParticipants =
        generateSeeds(normalized, bands, seed)

    bracketSize =
        nextPowerOfTwo(participants.count)

    bestCandidate = null
    bestScore = NEGATIVE_INFINITY

    rng = Random(seed)

    repeat config.candidateCount times:

        candidate =
            generateCandidate(
                seededParticipants,
                bracketSize,
                rng,
                config
            )

        validation =
            validateHardConstraints(candidate)

        if validation.isInvalid:
            continue

        fairness =
            calculateFairnessScore(
                candidate,
                normalized,
                config
            )

        candidate.fairnessScore = fairness

        if fairness.total > bestScore:
            bestScore = fairness.total
            bestCandidate = candidate

    if bestCandidate == null:
        throw DrawGenerationException

    return bestCandidate
```

---

# 54. Pseudocode — Candidate Generation

```text
function generateCandidate(participants, bracketSize, rng, config):

    slots =
        generateSeedAwareSlots(
            bracketSize,
            participants
        )

    randomizedSlots =
        controlledRandomize(
            slots,
            rng,
            config
        )

    slotsWithByes =
        allocateByes(
            randomizedSlots,
            participants,
            rng
        )

    matches =
        buildBracket(
            slotsWithByes
        )

    return TournamentStructure(
        participants = participants,
        matches = matches,
        seeds = extractSeeds(slotsWithByes),
        bracketSize = bracketSize
    )
```

---

# 55. Pseudocode — Candidate Validation

```text
function validateHardConstraints(candidate):

    if duplicateParticipants(candidate):
        return INVALID

    if missingParticipants(candidate):
        return INVALID

    if invalidBracketSize(candidate):
        return INVALID

    if invalidMatchReferences(candidate):
        return INVALID

    if invalidProgression(candidate):
        return INVALID

    if duplicateMatchAssignments(candidate):
        return INVALID

    return VALID
```

---

# 56. Pseudocode — Fairness Scoring

```text
function calculateFairnessScore(candidate, participants, config):

    strength =
        calculateStrengthDistribution(candidate)

    balance =
        calculateBracketBalance(candidate)

    diversity =
        calculateCompetitiveDiversity(candidate)

    opportunity =
        calculateOpportunity(candidate)

    randomness =
        calculateRandomness(candidate)

    total =
        strength * config.weights.strengthDistribution +
        balance * config.weights.bracketBalance +
        diversity * config.weights.competitiveDiversity +
        opportunity * config.weights.opportunity +
        randomness * config.weights.randomness

    return FairnessScore(
        total = total,
        strengthDistribution = strength,
        bracketBalance = balance,
        competitiveDiversity = diversity,
        opportunity = opportunity,
        randomness = randomness
    )
```

---

# 57. Testing Strategy

The draw algorithm is business-critical.

Testing must occur before UI integration.

## Unit Tests

Required:

```text
nextPowerOfTwo()
normalizeMetric()
assignCompetitiveBands()
generateSeeds()
allocateByes()
generateBracket()
calculateStrengthDistribution()
calculateBracketBalance()
calculateCompetitiveDiversity()
calculateOpportunity()
calculateRandomness()
calculateFairnessScore()
validateHardConstraints()
```

---

# 58. Determinism Tests

Given:

```text
participants = X
config = Y
seed = Z
algorithmVersion = V
```

the result must always equal:

```text
expectedStructure
```

across repeated executions.

Test:

```text
generate(X,Y,Z)
generate(X,Y,Z)
```

must produce identical output.

---

# 59. Different Seed Tests

Test:

```text
generate(X,Y,100)
generate(X,Y,200)
```

Expected:

```text
valid structures
```

and normally:

```text
different structures
```

Do not require every seed to produce a different structure because collisions are mathematically possible.

---

# 60. Property-Based Tests

For random participant sets:

```text
Every participant appears exactly once.
```

```text
No invalid match references exist.
```

```text
Every generated bracket is structurally valid.
```

```text
Bracket size is always a power of two.
```

```text
All participants eventually have a valid progression path.
```

```text
Fairness score is always within [0,100].
```

---

# 61. Edge Cases

The engine must explicitly test:

```text
1 participant
2 participants
3 participants
4 participants
5 participants
7 participants
8 participants
9 participants
16 participants
17 participants
32 participants
33 participants
64 participants
```

Also test:

```text
all participants have identical ratings
```

```text
one participant is dramatically stronger than everyone else
```

```text
ratings contain many ties
```

```text
many organizer seeds
```

```text
no organizer seeds
```

```text
large number of byes
```

---

# 62. Expected Behavior for Identical Ratings

If every participant has the same competitive metric:

```text
competitiveScore = 50
```

for all participants.

The algorithm should then rely primarily on:

```text
randomness
+
structural balance
```

No participant should receive artificial priority.

---

# 63. Expected Behavior for Extreme Skill Gap

Example:

```text
Player A = 99
Players B–H = 50
```

The algorithm should:

- Recognize Player A as the strongest.
- Avoid unnecessarily placing A against another strong player because none exists.
- Avoid manipulating every other participant around A.
- Preserve randomness for all remaining positions.

The engine cannot manufacture fairness where the participant pool itself is highly uneven.

---

# 64. Fairness Is Not Outcome Equality

The engine must never attempt to produce:

```text
50% win probability for everyone
```

That is impossible without manipulating the competition.

Instead it should produce:

```text
reasonable structural opportunity
```

The actual winner must remain determined by gameplay.

---

# 65. No Machine Learning in MVP

Do not implement:

```text
neural networks
machine learning
LLM ranking
AI skill prediction
reinforcement learning
```

for draw generation.

The available tournament data will not justify this complexity during MVP.

A transparent algorithm is preferable because the host should be able to understand why the draw was generated.

---

# 66. Algorithm Versioning

Every generated draw must contain:

```text
algorithmVersion
```

Example:

```text
"fair-draw-v1"
```

If the algorithm changes materially:

```text
fair-draw-v2
```

must be introduced.

Old tournament draws must remain reproducible using their original algorithm version.

Do not silently change the behavior of an algorithm used by an active tournament.

---

# 67. Performance Requirements

Target tournament sizes:

```text
8–64 participants
```

The engine should generate a draw quickly enough for interactive host usage.

Target:

```text
< 1 second
```

for typical tournaments on a modern Android device for the default candidate count.

Performance should be measured rather than assumed.

If candidate generation becomes expensive:

1. Optimize scoring.
2. Reduce unnecessary allocations.
3. Cache participant metrics.
4. Optimize bracket calculations.
5. Adjust candidate count based on tournament size.

Do not remove fairness components merely to improve performance.

---

# 68. Backend Execution

The algorithm may initially execute locally for development and testing.

However, the final authoritative draw-generation operation should be protected against client manipulation.

Recommended architecture:

```text
Android Host Console
        ↓
Secure Backend Function
        ↓
Tournament Engine
        ↓
Validated Tournament Structure
        ↓
PostgreSQL Transaction
        ↓
Realtime Update
```

The Android client must not be trusted to enforce draw locking or tournament integrity.

---

# 69. Transactional Draw Creation

When a draw is finalized, the backend should perform the following atomically where practical:

```text
BEGIN TRANSACTION

validate tournament state
validate host authorization
validate participant registrations
generate/finalize structure
create participant assignments
create groups
create matches
create draw_generation record
update tournament status
write audit log

COMMIT
```

If any critical operation fails:

```text
ROLLBACK
```

No partially generated tournament should remain.

---

# 70. Tournament State Requirements

Draw generation is allowed only when:

```text
REGISTRATION_CLOSED
```

or:

```text
DRAW_PENDING
```

depending on the exact lifecycle implementation.

After:

```text
DRAW_LOCKED
```

normal regeneration must be rejected.

After:

```text
IN_PROGRESS
```

the structural draw must not be regenerated.

---

# 71. Required Engine Output

The engine must return a structured object similar to:

```kotlin
data class TournamentStructure(
    val participants: List<SeededParticipant>,
    val bracketSize: Int?,
    val rounds: List<Round>,
    val groups: List<Group>,
    val matches: List<Match>,
    val progressionMap: Map<String, String>,
    val standings: List<Standing>,
    val fairnessScore: FairnessScore,
    val algorithmVersion: String,
    val randomSeed: Long
)
```

Fields not applicable to a tournament format may be empty/null as appropriate.

---

# 72. Example Output

For an 8-player tournament:

```text
Tournament
└── Single Elimination

Seed 1  → Player A
Seed 2  → Player B
Seed 3  → Player C
Seed 4  → Player D
Seed 5  → Player E
Seed 6  → Player F
Seed 7  → Player G
Seed 8  → Player H

Quarterfinals
├── A vs F
├── D vs G
├── C vs H
└── B vs E

Fairness Score
├── Strength Distribution: 91
├── Bracket Balance: 88
├── Competitive Diversity: 85
├── Opportunity: 90
├── Randomness: 87
└── Total: 88.7
```

The exact bracket above is illustrative only.

The algorithm must determine the actual structure.

---

# 73. Non-Goals

Do not add:

```text
AI-generated match predictions
```

```text
AI-generated winner predictions
```

```text
forced upset mechanics
```

```text
hidden handicaps
```

```text
dynamic difficulty adjustment
```

```text
skill-based score manipulation
```

```text
secret organizer favoritism
```

```text
player popularity weighting
```

```text
social engagement weighting
```

These violate the fairness philosophy.

---

# 74. Implementation Rules

The implementation agent must follow these rules.

### Rule 1

Do not replace the algorithm with simple random shuffle.

### Rule 2

Do not replace it with pure tournament seeding.

### Rule 3

Do not hardcode participant counts.

### Rule 4

Do not hardcode bracket positions for one tournament size only.

### Rule 5

Do not place fairness calculations inside Compose UI code.

### Rule 6

Do not duplicate fairness logic between Android and backend implementations.

### Rule 7

Do not hide failed draw generation.

### Rule 8

Do not silently modify a locked draw.

### Rule 9

Do not introduce ML/AI into MVP.

### Rule 10

Every generated draw must be reproducible from its stored seed and algorithm version.

---

# 75. Build Priority

The implementation order should be:

```text
1. Core data models
2. Metric normalization
3. Competitive bands
4. Seed generator
5. Bracket generator
6. Hard constraint validator
7. Fairness scorer
8. Candidate generation
9. Candidate optimization
10. Bye handling
11. Group generation
12. Groups → knockout mapping
13. Deterministic tests
14. Property-based tests
15. Performance tests
16. Supabase integration
17. Host UI
18. Participant UI
19. Realtime synchronization
```

The engine should be considered independently usable before the complete UI is built.

---

# 76. Definition of Done

The fair draw system is complete only when:

```text
[ ] Participant metrics are normalized.
[ ] Competitive bands are calculated.
[ ] Seeds are generated correctly.
[ ] Single-elimination brackets support arbitrary participant counts.
[ ] Byes are handled.
[ ] Group generation works.
[ ] Groups → knockout mapping works.
[ ] Hard constraints are enforced.
[ ] Fairness score is calculated.
[ ] Candidate generation is randomized.
[ ] Multiple candidates are evaluated.
[ ] Best valid candidate is selected.
[ ] Random seed is stored.
[ ] Algorithm version is stored.
[ ] Same seed reproduces the same draw.
[ ] Different seeds can produce different draws.
[ ] Draw regeneration works before lock.
[ ] Draw locking prevents normal structural mutation.
[ ] Host overrides are audited.
[ ] Edge cases are tested.
[ ] Property-based tests pass.
[ ] Performance is acceptable for 8–64 participants.
[ ] Backend authorization prevents client-side manipulation.
```

---

# 77. Final Engineering Principle

The tournament engine is not designed to make every participant equally likely to win.

It is designed to prevent the tournament structure itself from becoming unnecessarily unfair.

The desired system is:

```text
Strong players
      ↓
appropriately seeded

Average players
      ↓
meaningfully distributed

Lower-rated players
      ↓
legitimate opportunities

Everyone
      ↓
meaningful randomness

Actual winner
      ↓
determined by gameplay
```

The algorithm must therefore optimize for:

> **Fair opportunity without artificial equality.**

The final product should make a host confident that the tournament was not simply shuffled randomly, while also ensuring that no participant can reasonably claim that the system secretly engineered their path.

**Better structure. Better competition. More meaningful upsets.**