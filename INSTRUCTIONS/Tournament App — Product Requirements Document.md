# Tournament App — Product Requirements Document

**Version:** 1.0  
**Platform:** Android  
**Product Type:** Mobile-first Tournament Management Application  
**Primary Games:** FC Mobile, eFootball Mobile  
**Backend:** Supabase  
**Status:** MVP Specification

---

## 1. Product Overview

The application is a mobile-first tournament management platform designed primarily for college-level gaming tournaments.

The initial target use case is organizing tournaments for games such as:

- EA SPORTS FC Mobile
- eFootball Mobile

The application eliminates the need for organizers to manually manage:

- participant registration
- spreadsheets
- tournament draws
- brackets
- league tables
- match progression
- score updates
- tournament status

The core product philosophy is:

> **Make tournament organization effortless while creating competitive, transparent and reasonably fair paths for participants of different skill levels.**

The application must not simply generate random brackets.

It should use participant competitive metrics such as:

- FC Mobile OVR
- eFootball Team Strength

to create balanced tournament structures while still preserving meaningful upset opportunities for lower-rated participants.

---

# 2. Product Goals

## Primary Goals

1. Allow users to create accounts.
2. Allow users to create and manage tournaments.
3. Allow users to register for tournaments.
4. Automatically classify participants based on their competitive metric.
5. Automatically generate tournament draws.
6. Support knockout tournaments.
7. Support league/group-based tournaments.
8. Support groups leading into knockout stages.
9. Automatically advance winners after results are recorded.
10. Allow only authorized hosts to update match scores.
11. Allow participants to view tournament information in real time.
12. Provide a simple mobile-first experience.

---

# 3. Non-Goals for MVP

The following are explicitly outside MVP scope:

- Chat system
- Social feed
- Payments
- Tournament entry fees
- Sponsorship management
- Streaming integration
- AI chatbot
- Advanced player analytics
- Public global rankings
- Cross-platform web application
- Team marketplace
- In-app messaging
- Automated cheating detection
- Computer vision for result verification
- Native advertising system

These may be considered in future versions.

---

# 4. User Types

The application has two functional user contexts.

## Participant

A normal registered user who joins tournaments.

Capabilities:

- create account
- maintain profile
- discover tournaments
- register for tournaments
- view registered tournaments
- view brackets
- view standings
- view matches
- view results
- view tournament information

Participants cannot directly modify tournament state.

---

## Host

A registered user who creates and manages a tournament.

Capabilities:

- create tournament
- configure tournament
- manage registration
- review participants
- generate draw
- regenerate draw before locking
- lock draw
- manage matches
- update scores
- resolve result issues
- control tournament lifecycle

A user does not need a permanently separate "Host account".

The same account can be:

- participant in Tournament A
- participant in Tournament B
- host of Tournament C

---

# 5. Core User Journey

## Participant Journey

```text
Create Account
      ↓
Discover Tournament
      ↓
Open Tournament
      ↓
Register
      ↓
Enter:
  Name
  In-game ID
  OVR / Team Strength
      ↓
Registration Confirmed
      ↓
Wait for Draw
      ↓
View Draw
      ↓
View Match
      ↓
Play Match
      ↓
Host Records Result
      ↓
Bracket/Table Updates
      ↓
Progress
```

---

## Host Journey

```text
Create Account
      ↓
Create Tournament
      ↓
Configure Rules
      ↓
Open Registration
      ↓
Participants Register
      ↓
Close Registration
      ↓
Review Participants
      ↓
Generate Fair Draw
      ↓
Lock Draw
      ↓
Start Tournament
      ↓
Update Match Scores
      ↓
Automatic Progression
      ↓
Complete Tournament
```

---

# 6. Registration Requirements

The participant registration form must contain only three required fields in MVP.

### Field 1 — Name

The participant's display name.

### Field 2 — In-game ID

The identifier used by the participant inside the selected game.

### Field 3 — Competitive Metric

Game-specific metric.

For FC Mobile:

```text
OVR
```

For eFootball:

```text
Team Strength
```

The UI label must automatically change based on the selected game.

---

# 7. Registration Validation

The application must validate:

- name is not empty
- in-game ID is not empty
- competitive metric is numeric
- competitive metric falls within configurable valid bounds
- duplicate registration is prevented
- a user cannot register twice for the same tournament

The host may edit participant information before the draw is locked.

After the draw is locked, competitive metrics should not be changed without an explicit host override.

---

# 8. Tournament Types

MVP should support three tournament structures.

## Single Elimination

```text
Round of 16
     ↓
Quarter Final
     ↓
Semi Final
     ↓
Final
```

## League / Groups

Participants are divided into groups.

Each participant plays according to the selected group format.

Standings are automatically calculated.

## Groups → Knockout

Example:

```text
32 Players
   ↓
4 Groups × 8
   ↓
Top 2 from each group
   ↓
Quarter Finals
   ↓
Semi Finals
   ↓
Final
```

The exact number of groups and qualification slots must be configurable.

---

# 9. Fair Draw Philosophy

Fairness is a core product feature.

The system should not simply:

- completely randomize participants
- pair highest-rated against lowest-rated
- create a deterministic strongest-vs-weakest bracket

Instead, the draw engine should consider:

- competitive metric
- seed
- distribution of strong participants
- bracket balance
- competitive diversity
- randomness
- upset opportunity

The objective is:

> **Preserve competitive integrity while ensuring that lower-rated participants have a realistic path to progress.**

The algorithm must be explainable.

It should never claim that the system guarantees a fair outcome.

---

# 10. Draw Lifecycle

```text
REGISTRATION OPEN
       ↓
REGISTRATION CLOSED
       ↓
DRAW GENERATION
       ↓
DRAW PREVIEW
       ↓
REGENERATE (optional)
       ↓
LOCK DRAW
       ↓
DRAW FINAL
```

Once locked:

- participant order cannot be casually changed
- matchups cannot be modified by participants
- tournament progression begins

Only host-level override actions may alter locked tournament data.

---

# 11. Match Result System

Only the host can officially update scores.

Participant:

```text
VIEW ONLY
```

Host:

```text
ENTER SCORE
```

Example:

```text
Quarter Final

Player A
[ 3 ]

Player B
[ 1 ]

[ Confirm Result ]
```

After confirmation:

```text
Match = Completed
Winner = Player A
```

The tournament engine automatically determines the next state.

---

# 12. Tournament State

Tournament lifecycle:

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

Invalid state transitions must be rejected.

---

# 13. Participant Experience Principles

Participants should be able to answer these questions immediately:

1. What tournaments am I registered for?
2. What is my next match?
3. Who am I playing?
4. When/where is the match?
5. What is the current score?
6. Where am I in the bracket?
7. What is the current tournament status?

The application must prioritize these over secondary information.

---

# 14. Host Experience Principles

The host should be able to run an entire tournament from a phone.

The host should not need:

- a laptop
- spreadsheet software
- external forms
- external bracket generators
- manual calculations

The application is intended to replace those workflows.

---

# 15. MVP Success Criteria

The MVP is successful if a real college tournament can be conducted entirely through the application.

Minimum complete flow:

```text
Host creates tournament
        ↓
Registration form generated
        ↓
Players register
        ↓
Host closes registration
        ↓
System generates draw
        ↓
Host locks draw
        ↓
Players see bracket
        ↓
Host enters results
        ↓
Bracket automatically progresses
        ↓
Winner is determined
```

If this flow works reliably, the MVP is considered functional.