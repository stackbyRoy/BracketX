# Tournament App — UI/UX Design Specification

**Platform:** Android  
**Design Direction:** Modern competitive utility  
**Primary Principle:** Fast, clear, information-dense without feeling cluttered

---

# 1. Design Philosophy

The application is a **tool**, not a social media platform.

The interface should communicate:

- control
- clarity
- competition
- reliability
- speed

Avoid:

- excessive gradients
- gaming clichés
- esports team aesthetics
- excessive neon
- unnecessary animations
- decorative UI that reduces information density

The application should feel appropriate for a college tournament organizer.

---

# 2. Visual Direction

Use:

- dark-first interface
- neutral surfaces
- strong typography
- restrained accent color
- clear status indicators
- rounded cards
- subtle borders
- minimal shadows

The design should feel closer to:

```text
modern productivity app
+
sports competition dashboard
+
premium mobile utility
```

rather than:

```text
esports betting website
```

---

# 3. Color System

Use a neutral dark foundation.

Recommended:

```text
Background
#0B0D10

Surface
#12151A

Elevated Surface
#181C22

Primary Text
#F5F7FA

Secondary Text
#9AA2AD

Accent
#2F80ED

Success
#27AE60

Warning
#F2C94C

Error
#EB5757
```

The accent should be used sparingly.

Do not turn the entire application blue.

---

# 4. Typography

Use Android system typography / Material 3 typography.

Hierarchy:

```text
Display
Headline
Title
Body
Label
Caption
```

Tournament names and matchups should receive strong visual hierarchy.

---

# 5. Navigation

Use bottom navigation for primary user destinations.

Participant:

```text
Home
My Tournaments
Profile
```

Host:

```text
Home
My Tournaments
Create
Profile
```

Do not add navigation items merely because a screen exists.

---

# 6. Home Screen

The home screen should immediately surface relevant tournaments.

Example:

```text
Good evening, Swarnendu

MY TOURNAMENTS

┌─────────────────────────┐
│ GCETTS eFootball Cup    │
│ LIVE                    │
│ Quarter Final           │
│                         │
│ Your next match         │
│ 7:30 PM                 │
└─────────────────────────┘

DISCOVER

┌─────────────────────────┐
│ FC Mobile Championship  │
│ Registration open       │
│ 24 / 32 players         │
└─────────────────────────┘
```

---

# 7. Tournament Detail

Header:

```text
Tournament Name
Game
Status
```

Tabs:

```text
Overview
Bracket
Matches
Standings
Players
```

For knockout tournaments:

```text
Overview
Bracket
Matches
Players
```

For league tournaments:

```text
Overview
Standings
Matches
Players
```

---

# 8. Participant Tournament View

Prioritize the participant's own status.

Example:

```text
YOUR STATUS

Quarter Final
vs Rahul

Saturday · 7:30 PM

[ VIEW MATCH ]
```

Then:

```text
BRACKET

Your position should be visually identifiable.
```

The participant should never have to search through a giant bracket to find themselves.

---

# 9. Host Tournament Console

The host console is the most operational screen.

Example:

```text
GCETTS EFOOTBALL CUP
LIVE

32 Players

┌─────────┬─────────┐
│ Matches │ Results │
│ 12       │ 8       │
└─────────┴─────────┘

QUICK ACTIONS

[ Update Score ]

[ View Participants ]

[ View Bracket ]

[ Tournament Settings ]
```

---

# 10. Create Tournament Flow

Use a multi-step setup rather than one huge form.

### Step 1

```text
Tournament Name
```

### Step 2

```text
Select Game

○ FC Mobile
○ eFootball
```

### Step 3

```text
Tournament Format

○ Knockout
○ League
○ Groups → Knockout
```

### Step 4

Format-specific configuration.

### Step 5

Review:

```text
Tournament
FC Mobile

Format
Single Elimination

Participants
32

[ CREATE TOURNAMENT ]
```

---

# 11. Registration UI

Participant registration must be extremely short.

```text
JOIN TOURNAMENT

Name
[________________]

In-game ID
[________________]

OVR
[________________]

By registering, you agree to the tournament rules.

[ REGISTER ]
```

For eFootball:

```text
Team Strength
```

instead of:

```text
OVR
```

No unnecessary fields.

---

# 12. Participant Management

Host view:

```text
REGISTERED PLAYERS

32 Participants

┌─────────────────────────┐
│ Swarnendu Roy           │
│ stackbyRoy              │
│ OVR 112                 │
│ Seed —                  │
└─────────────────────────┘
```

Search and filtering should be available.

---

# 13. Draw Screen

Before locking:

```text
DRAW GENERATOR

Participants
32

Draw Philosophy
Balanced

Fairness
87%

[ GENERATE DRAW ]
```

After generation:

```text
DRAW PREVIEW

Round of 16

Match 1
Player A
vs
Player B

...

[ REGENERATE ]

[ LOCK DRAW ]
```

Locking should require confirmation.

```text
Lock this draw?

Once locked, participant placement
cannot be changed normally.

[ CANCEL ] [ LOCK DRAW ]
```

---

# 14. Bracket UI

The bracket should prioritize readability over visual spectacle.

Example:

```text
ROUND OF 16      QUARTERS      SEMIS       FINAL

A ───┐
     ├── A ───┐
B ───┘        │
              ├── A ───┐
C ───┐        │        │
     ├── C ───┘        │
D ───┘                 │
                       ├── CHAMPION
E ───┐                 │
     ├── E ────────────┘
F ───┘
```

On mobile, use horizontal scrolling rather than compressing the bracket until it becomes unreadable.

---

# 15. Match Screen

Participant:

```text
QUARTER FINAL

Swarnendu Roy
vs
Rahul Das

Scheduled
7:30 PM

Venue
College Lab 2

Status
Awaiting Result
```

Host:

```text
QUARTER FINAL

Swarnendu Roy
[ 0 ]

Rahul Das
[ 0 ]

[ UPDATE SCORE ]
```

---

# 16. Score Update

Use large touch targets.

```text
UPDATE RESULT

Swarnendu Roy
[-]   3   [+]

Rahul Das
[-]   1   [+]

Winner
Swarnendu Roy

[ SAVE RESULT ]
```

Confirmation:

```text
Confirm result?

Swarnendu Roy 3
Rahul Das      1

The winner will advance automatically.

[ CONFIRM ]
```

---

# 17. Standings

League table:

```text
GROUP A

TEAM / PLAYER   P  W  D  L  GD  PTS

Swarnendu       3  2  1  0  +4   7
Rahul           3  2  0  1  +2   6
Ayan            3  1  0  2  -1   3
Rohan           3  0  1  2  -5   1
```

The user's row should be visually identifiable.

---

# 18. Loading States

Use skeletons where appropriate.

Avoid full-screen spinners for simple actions.

---

# 19. Empty States

Example:

```text
No tournaments yet.

Create your first tournament
or join one using an invite.

[ CREATE TOURNAMENT ]
```

Empty states should always explain what the user can do next.

---

# 20. Error States

Errors should be understandable.

Bad:

```text
Error 23505
```

Good:

```text
You're already registered for this tournament.
```

---

# 21. Animation

Use subtle motion only.

Appropriate:

- screen transitions
- bracket progression
- score confirmation
- tournament status changes

Avoid:

- flashy particle effects
- constant animated backgrounds
- excessive card animations

The product should feel fast.

---

# 22. Accessibility

Minimum requirements:

- sufficient contrast
- scalable text
- touch targets ≥ 48dp
- don't rely solely on color for status
- meaningful content descriptions
- support system font scaling