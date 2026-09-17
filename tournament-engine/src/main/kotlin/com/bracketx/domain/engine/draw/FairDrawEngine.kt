package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.DrawCandidate
import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.FairnessScore
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentSettings
import com.bracketx.domain.model.TournamentStructure
import java.util.UUID
import kotlin.random.Random

class DrawGenerationException(message: String) : RuntimeException(message)

object FairDrawEngine {

    fun generateDraw(
        tournamentId: String,
        participants: List<TournamentParticipant>,
        format: TournamentFormat,
        settings: TournamentSettings = TournamentSettings(),
        config: DrawConfiguration = DrawConfiguration(),
        seed: Long = System.currentTimeMillis()
    ): TournamentStructure {
        if (participants.isEmpty()) {
            throw DrawGenerationException("Cannot generate draw for empty participant list")
        }

        val rng = Random(seed)

        // 1. Normalize metrics across participants
        val normalizedParticipants = if (participants.all { it.normalizedScore in 0.0..100.0 && it.seed != null }) {
            participants
        } else {
            val rawMetrics = participants.map { it.metricValue }
            val normalizedScores = MetricNormalizer.normalizeTournamentRelative(rawMetrics)
            participants.mapIndexed { index, p ->
                p.copy(normalizedScore = normalizedScores[index])
            }
        }

        // 2. Assign initial seeds and bands deterministically using seed
        val seededParticipants = SeedGenerator.generateSeeds(normalizedParticipants, rng)

        return when (format) {
            TournamentFormat.SINGLE_ELIMINATION -> {
                generateSingleElimination(tournamentId, seededParticipants, config, seed)
            }
            TournamentFormat.LEAGUE_GROUPS -> {
                generateLeagueGroups(tournamentId, seededParticipants, settings, config, seed)
            }
            TournamentFormat.GROUPS_KNOCKOUT -> {
                generateGroupsKnockout(tournamentId, seededParticipants, settings, config, seed)
            }
        }
    }

    private fun generateSingleElimination(
        tournamentId: String,
        seededParticipants: List<TournamentParticipant>,
        config: DrawConfiguration,
        seed: Long
    ): TournamentStructure {
        val count = seededParticipants.size
        val bracketSize = BracketGenerator.nextPowerOfTwo(count)
        val canonicalSlots = BracketGenerator.canonicalSeedOrder(bracketSize)
        val numByes = bracketSize - count

        val rng = Random(seed)
        var bestCandidate: DrawCandidate? = null
        var bestScore = Double.NEGATIVE_INFINITY
        val topCandidates = mutableListOf<Pair<DrawCandidate, FairnessScore>>()

        repeat(config.candidateCount) {
            // Generate randomized candidate assignments to canonical slots
            val slotAssignments = generateCandidateSlots(
                seededParticipants = seededParticipants,
                bracketSize = bracketSize,
                canonicalSlots = canonicalSlots,
                numByes = numByes,
                rng = rng
            )

            // Build knockout matches
            val matches = BracketGenerator.buildKnockoutTree(
                tournamentId = tournamentId,
                bracketSize = bracketSize,
                orderedParticipants = slotAssignments
            )

            // Validate hard constraints
            val validation = ConstraintValidator.validateKnockoutDraw(
                participants = seededParticipants,
                bracketSize = bracketSize,
                matches = matches
            )

            if (validation.isValid) {
                val score = FairnessScorer.scoreKnockout(
                    orderedParticipants = slotAssignments,
                    matches = matches,
                    participants = seededParticipants,
                    canonicalSeedSlots = canonicalSlots,
                    config = config
                )

                val candidate = DrawCandidate(
                    id = UUID.randomUUID().toString(),
                    participants = seededParticipants,
                    matches = matches,
                    bracketSize = bracketSize,
                    fairnessScore = score,
                    randomSeed = seed
                )

                if (score.total > bestScore) {
                    bestScore = score.total
                    bestCandidate = candidate
                }

                topCandidates.add(candidate to score)
            }
        }

        if (bestCandidate == null) {
            throw DrawGenerationException("Failed to generate a valid tournament candidate satisfying all hard constraints")
        }

        // Check near-ties within tolerance
        val eligibleCandidates = topCandidates.filter {
            bestScore - it.second.total <= config.tieTolerance
        }
        val selected = eligibleCandidates.random(rng).first

        return TournamentStructure(
            participants = selected.participants,
            bracketSize = selected.bracketSize,
            matches = selected.matches,
            fairnessScore = selected.fairnessScore ?: FairnessScore(85.0, 85.0, 85.0, 85.0, 85.0, 85.0),
            algorithmVersion = config.algorithmVersion,
            randomSeed = seed
        )
    }

    private fun generateCandidateSlots(
        seededParticipants: List<TournamentParticipant>,
        bracketSize: Int,
        canonicalSlots: List<Int>,
        numByes: Int,
        rng: Random
    ): List<TournamentParticipant?> {
        val participantMap = seededParticipants.associateBy { it.seed!! }
        val slots = arrayOfNulls<TournamentParticipant>(bracketSize)

        // Seed placement with controlled permutation in compatible tiers
        // For seeds 1-4: top 4 remain in their designated quadrants, but can permute within [1, 2] opposite halves and [3, 4]
        // Seeds 5-8: can permute within their tier
        // Seeds 9-16: can permute within their tier
        val permutedSeeds = mutableMapOf<Int, Int>()
        val tiers = listOf(
            1..2,
            3..4,
            5..8,
            9..16,
            17..32,
            33..64
        )

        for (tier in tiers) {
            val available = tier.filter { it <= bracketSize }.toList()
            val shuffled = available.shuffled(rng)
            for (i in available.indices) {
                permutedSeeds[available[i]] = shuffled[i]
            }
        }

        // Place participants or BYEs
        for (i in 0 until bracketSize) {
            val canonicalSeed = canonicalSlots[i]
            val effectiveSeed = permutedSeeds[canonicalSeed] ?: canonicalSeed

            if (effectiveSeed <= seededParticipants.size) {
                slots[i] = participantMap[effectiveSeed]
            } else {
                slots[i] = null // BYE
            }
        }

        return slots.toList()
    }

    private fun generateLeagueGroups(
        tournamentId: String,
        seededParticipants: List<TournamentParticipant>,
        settings: TournamentSettings,
        config: DrawConfiguration,
        seed: Long
    ): TournamentStructure {
        val groupCount = settings.groupCount ?: when {
            seededParticipants.size >= 32 -> 8
            seededParticipants.size >= 16 -> 4
            seededParticipants.size >= 8 -> 2
            else -> 1
        }

        val rng = Random(seed)
        var bestGroups = GroupGenerator.distributeSerpentine(tournamentId, seededParticipants, groupCount)
        var minSpread = calculateGroupSpread(bestGroups, seededParticipants)

        // Generate alternative permutations to optimize group balance
        repeat(config.candidateCount) {
            val candidateGroups = GroupGenerator.distributeSerpentine(
                tournamentId,
                seededParticipants.shuffled(rng),
                groupCount
            )
            val spread = calculateGroupSpread(candidateGroups, seededParticipants)
            if (spread < minSpread) {
                minSpread = spread
                bestGroups = candidateGroups
            }
        }

        val matches = GroupGenerator.generateGroupMatches(tournamentId, bestGroups)
        val standings = GroupGenerator.initializeStandings(tournamentId, bestGroups)

        return TournamentStructure(
            participants = seededParticipants,
            bracketSize = null,
            groups = bestGroups,
            matches = matches,
            standings = standings,
            fairnessScore = FairnessScore(
                total = (100.0 - minSpread).coerceIn(50.0, 100.0),
                strengthDistribution = (100.0 - minSpread).coerceIn(50.0, 100.0),
                bracketBalance = (100.0 - minSpread).coerceIn(50.0, 100.0),
                competitiveDiversity = 90.0,
                opportunity = 90.0,
                randomness = 85.0
            ),
            algorithmVersion = config.algorithmVersion,
            randomSeed = seed
        )
    }

    private fun generateGroupsKnockout(
        tournamentId: String,
        seededParticipants: List<TournamentParticipant>,
        settings: TournamentSettings,
        config: DrawConfiguration,
        seed: Long
    ): TournamentStructure {
        val groupStructure = generateLeagueGroups(tournamentId, seededParticipants, settings, config, seed)
        return groupStructure
    }

    private fun calculateGroupSpread(
        groups: List<com.bracketx.domain.model.Group>,
        participants: List<TournamentParticipant>
    ): Double {
        val participantMap = participants.associateBy { it.id }
        val groupStrengths = groups.map { group ->
            group.participantIds.sumOf { participantMap[it]?.normalizedScore ?: 0.0 }
        }
        val minStr = groupStrengths.minOrNull() ?: 0.0
        val maxStr = groupStrengths.maxOrNull() ?: 0.0
        return maxStr - minStr
    }
}
