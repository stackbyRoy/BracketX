package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.Group
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.TournamentParticipant

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    val isInvalid: Boolean get() = !isValid

    companion object {
        val Valid = ValidationResult(true)
        fun invalid(vararg errors: String) = ValidationResult(false, errors.toList())
        fun invalid(errors: List<String>) = ValidationResult(false, errors)
    }
}

object ConstraintValidator {

    fun validateKnockoutDraw(
        participants: List<TournamentParticipant>,
        bracketSize: Int,
        matches: List<Match>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Bracket size must be a power of two (or 1)
        if (bracketSize > 1 && (bracketSize and (bracketSize - 1)) != 0) {
            errors.add("Bracket size $bracketSize is not a power of two")
        }

        // 2. Initial round participant checks
        val round1Matches = matches.filter { it.roundNumber == 1 }
        val assignedIds = mutableListOf<String>()

        for (m in round1Matches) {
            m.participantAId?.let { assignedIds.add(it) }
            m.participantBId?.let { assignedIds.add(it) }
        }

        // Duplicate check
        val duplicateIds = assignedIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            errors.add("Duplicate participants in Round 1: $duplicateIds")
        }

        // Missing participant check
        val participantIds = participants.map { it.id }.toSet()
        val missingIds = participantIds - assignedIds.toSet()
        if (missingIds.isNotEmpty()) {
            errors.add("Missing participants from Round 1: $missingIds")
        }

        // Foreign participant check
        val extraIds = assignedIds.toSet() - participantIds
        if (extraIds.isNotEmpty()) {
            errors.add("Unknown participants in Round 1: $extraIds")
        }

        // 3. Match reference and progression validation
        val matchIds = matches.map { it.id }.toSet()
        for (m in matches) {
            if (m.nextMatchId != null) {
                if (!matchIds.contains(m.nextMatchId)) {
                    errors.add("Match ${m.id} references non-existent nextMatchId ${m.nextMatchId}")
                }
                if (m.nextMatchId == m.id) {
                    errors.add("Circular reference in match ${m.id}")
                }
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.invalid(errors)
    }

    fun validateGroupDraw(
        participants: List<TournamentParticipant>,
        groups: List<Group>,
        matches: List<Match>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        val allGroupMembers = groups.flatMap { it.participantIds }
        val duplicateMembers = allGroupMembers.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateMembers.isNotEmpty()) {
            errors.add("Duplicate group membership: $duplicateMembers")
        }

        val participantIds = participants.map { it.id }.toSet()
        val missingMembers = participantIds - allGroupMembers.toSet()
        if (missingMembers.isNotEmpty()) {
            errors.add("Missing participants from groups: $missingMembers")
        }

        val extraMembers = allGroupMembers.toSet() - participantIds
        if (extraMembers.isNotEmpty()) {
            errors.add("Unknown participants in groups: $extraMembers")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.invalid(errors)
    }
}
