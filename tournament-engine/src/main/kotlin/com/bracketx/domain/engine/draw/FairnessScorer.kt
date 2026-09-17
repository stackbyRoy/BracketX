package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.CompetitiveBand
import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.FairnessScore
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.TournamentParticipant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object FairnessScorer {

    fun scoreKnockout(
        orderedParticipants: List<TournamentParticipant?>,
        matches: List<Match>,
        participants: List<TournamentParticipant>,
        canonicalSeedSlots: List<Int>,
        config: DrawConfiguration
    ): FairnessScore {
        val count = orderedParticipants.size
        if (count <= 2 || participants.isEmpty()) {
            return FairnessScore(
                total = 85.0,
                strengthDistribution = 85.0,
                bracketBalance = 85.0,
                competitiveDiversity = 85.0,
                opportunity = 85.0,
                randomness = 85.0
            )
        }

        val strengthDist = calculateStrengthDistribution(orderedParticipants)
        val balance = calculateBracketBalance(orderedParticipants)
        val diversity = calculateCompetitiveDiversity(matches, participants)
        val opportunity = calculateOpportunityScore(matches, participants)
        val randomness = calculateRandomnessScore(orderedParticipants, canonicalSeedSlots)

        val total = (
            strengthDist * config.weights.strengthDistribution +
            balance * config.weights.bracketBalance +
            diversity * config.weights.competitiveDiversity +
            opportunity * config.weights.opportunity +
            randomness * config.weights.randomness
        ).coerceIn(0.0, 100.0)

        return FairnessScore(
            total = total,
            strengthDistribution = strengthDist,
            bracketBalance = balance,
            competitiveDiversity = diversity,
            opportunity = opportunity,
            randomness = randomness
        )
    }

    /**
     * Component 1: Strength Distribution (Weight: 30%)
     * Evaluates whether total participant strength is evenly divided among bracket sections (quarters / halves).
     */
    fun calculateStrengthDistribution(orderedParticipants: List<TournamentParticipant?>): Double {
        val totalSlots = orderedParticipants.size
        val numSections = when {
            totalSlots >= 8 -> 4
            totalSlots >= 4 -> 2
            else -> 1
        }
        if (numSections <= 1) return 100.0

        val slotsPerSection = totalSlots / numSections
        val sectionStrengths = (0 until numSections).map { secIndex ->
            val slice = orderedParticipants.subList(secIndex * slotsPerSection, (secIndex + 1) * slotsPerSection)
            slice.filterNotNull().sumOf { it.normalizedScore }
        }

        val totalStrength = sectionStrengths.sum()
        if (totalStrength <= 0.0) return 100.0

        val idealStrength = totalStrength / numSections.toDouble()
        val totalDeviation = sectionStrengths.sumOf { abs(it - idealStrength) }
        val maxPossibleDeviation = 2.0 * totalStrength * (1.0 - 1.0 / numSections)

        val ratio = if (maxPossibleDeviation > 0.0) totalDeviation / maxPossibleDeviation else 0.0
        return clamp((1.0 - ratio) * 100.0, 0.0, 100.0)
    }

    /**
     * Component 2: Bracket Balance (Weight: 25%)
     * Evaluates the spread: max(sectionStrength) - min(sectionStrength).
     */
    fun calculateBracketBalance(orderedParticipants: List<TournamentParticipant?>): Double {
        val totalSlots = orderedParticipants.size
        val numSections = when {
            totalSlots >= 8 -> 4
            totalSlots >= 4 -> 2
            else -> 1
        }
        if (numSections <= 1) return 100.0

        val slotsPerSection = totalSlots / numSections
        val sectionStrengths = (0 until numSections).map { secIndex ->
            val slice = orderedParticipants.subList(secIndex * slotsPerSection, (secIndex + 1) * slotsPerSection)
            slice.filterNotNull().sumOf { it.normalizedScore }
        }

        val minStr = sectionStrengths.minOrNull() ?: 0.0
        val maxStr = sectionStrengths.maxOrNull() ?: 0.0
        val spread = maxStr - minStr
        val totalStrength = sectionStrengths.sum()

        if (totalStrength <= 0.0) return 100.0

        val normalizedSpread = spread / (totalStrength / numSections.toDouble() + 1.0)
        return clamp((1.0 - (normalizedSpread / 2.0)) * 100.0, 0.0, 100.0)
    }

    /**
     * Component 3: Competitive Diversity (Weight: 20%)
     * Opening rounds should avoid excessive same-band clustering or excessive extreme mismatches.
     */
    fun calculateCompetitiveDiversity(
        matches: List<Match>,
        participants: List<TournamentParticipant>
    ): Double {
        val participantMap = participants.associateBy { it.id }
        val round1Matches = matches.filter { it.roundNumber == 1 && !it.isBye }
        if (round1Matches.isEmpty()) return 100.0

        var penaltyPoints = 0.0
        for (m in round1Matches) {
            val pA = m.participantAId?.let { participantMap[it] } ?: continue
            val pB = m.participantBId?.let { participantMap[it] } ?: continue

            val bandA = pA.competitiveBand
            val bandB = pB.competitiveBand

            // Penalize excessive same-band matchups at extremes
            if (bandA == CompetitiveBand.S && bandB == CompetitiveBand.S) {
                penaltyPoints += 25.0
            } else if (bandA == CompetitiveBand.D && bandB == CompetitiveBand.D) {
                penaltyPoints += 15.0
            }
        }

        val avgPenalty = penaltyPoints / round1Matches.size.toDouble()
        return clamp(100.0 - avgPenalty * 3.0, 0.0, 100.0)
    }

    /**
     * Component 4: Opportunity Score (Weight: 15%)
     * Evaluates whether weaker participants (D / C) are disproportionately exposed to Elite (S) opponents.
     */
    fun calculateOpportunityScore(
        matches: List<Match>,
        participants: List<TournamentParticipant>
    ): Double {
        val participantMap = participants.associateBy { it.id }
        val round1Matches = matches.filter { it.roundNumber == 1 && !it.isBye }
        if (round1Matches.isEmpty()) return 100.0

        var eliteVsUnderdogCount = 0
        var totalUnderdogs = 0

        for (m in round1Matches) {
            val pA = m.participantAId?.let { participantMap[it] } ?: continue
            val pB = m.participantBId?.let { participantMap[it] } ?: continue

            val isUnderdogA = pA.competitiveBand == CompetitiveBand.D
            val isUnderdogB = pB.competitiveBand == CompetitiveBand.D
            val isEliteA = pA.competitiveBand == CompetitiveBand.S
            val isEliteB = pB.competitiveBand == CompetitiveBand.S

            if (isUnderdogA || isUnderdogB) totalUnderdogs++

            if ((isUnderdogA && isEliteB) || (isUnderdogB && isEliteA)) {
                eliteVsUnderdogCount++
            }
        }

        if (totalUnderdogs == 0) return 95.0

        // If all underdogs are paired against elites, opportunity is poor
        val ratio = eliteVsUnderdogCount.toDouble() / totalUnderdogs.toDouble()
        val penalty = ratio * 50.0
        return clamp(100.0 - penalty, 0.0, 100.0)
    }

    /**
     * Component 5: Randomness Score (Weight: 10%)
     * Evaluates variation from canonical deterministic seed placement.
     */
    fun calculateRandomnessScore(
        orderedParticipants: List<TournamentParticipant?>,
        canonicalSeedSlots: List<Int>
    ): Double {
        if (orderedParticipants.size != canonicalSeedSlots.size || orderedParticipants.isEmpty()) {
            return 80.0
        }

        var matchCount = 0
        var totalChecked = 0

        for (i in orderedParticipants.indices) {
            val p = orderedParticipants[i]
            val canonicalSeed = canonicalSeedSlots[i]
            if (p != null) {
                totalChecked++
                if (p.seed == canonicalSeed) {
                    matchCount++
                }
            }
        }

        if (totalChecked == 0) return 80.0

        // If 100% matches canonical, randomness is low (score ~60)
        // If 0% matches canonical, high randomness (score ~95)
        val canonicalRatio = matchCount.toDouble() / totalChecked.toDouble()
        val score = 95.0 - (canonicalRatio * 35.0)
        return clamp(score, 0.0, 100.0)
    }

    private fun clamp(value: Double, minVal: Double, maxVal: Double): Double {
        return max(minVal, min(maxVal, value))
    }
}
