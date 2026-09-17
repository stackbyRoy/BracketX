package com.bracketx.domain.state

import com.bracketx.domain.model.TournamentStatus

sealed class TournamentEvent {
    object OpenRegistration : TournamentEvent()
    object CloseRegistration : TournamentEvent()
    object ReopenRegistration : TournamentEvent()
    object PrepareDraw : TournamentEvent()
    object GenerateDraw : TournamentEvent()
    object RegenerateDraw : TournamentEvent()
    object LockDraw : TournamentEvent()
    object StartTournament : TournamentEvent()
    object CompleteTournament : TournamentEvent()
}

class InvalidStateTransitionException(
    val currentStatus: TournamentStatus,
    val targetStatus: TournamentStatus,
    message: String = "Cannot transition tournament state from $currentStatus to $targetStatus"
) : IllegalStateException(message)

object TournamentStateMachine {

    private val allowedTransitions: Map<TournamentStatus, Set<TournamentStatus>> = mapOf(
        TournamentStatus.DRAFT to setOf(
            TournamentStatus.REGISTRATION_OPEN
        ),
        TournamentStatus.REGISTRATION_OPEN to setOf(
            TournamentStatus.REGISTRATION_CLOSED
        ),
        TournamentStatus.REGISTRATION_CLOSED to setOf(
            TournamentStatus.REGISTRATION_OPEN, // Host can reopen registration
            TournamentStatus.DRAW_PENDING,
            TournamentStatus.DRAW_GENERATED     // Direct generation from closed
        ),
        TournamentStatus.DRAW_PENDING to setOf(
            TournamentStatus.DRAW_GENERATED,
            TournamentStatus.REGISTRATION_OPEN  // Cancel draw setup, reopen registration
        ),
        TournamentStatus.DRAW_GENERATED to setOf(
            TournamentStatus.DRAW_GENERATED,    // Regenerate draw
            TournamentStatus.DRAW_LOCKED,       // Lock draw
            TournamentStatus.REGISTRATION_OPEN  // Discard draw and reopen registration
        ),
        TournamentStatus.DRAW_LOCKED to setOf(
            TournamentStatus.IN_PROGRESS        // Start tournament matches
        ),
        TournamentStatus.IN_PROGRESS to setOf(
            TournamentStatus.COMPLETED          // All matches finished and champion crowned
        ),
        TournamentStatus.COMPLETED to emptySet() // Terminal state
    )

    fun canTransition(current: TournamentStatus, target: TournamentStatus): Boolean {
        return allowedTransitions[current]?.contains(target) == true
    }

    fun transition(current: TournamentStatus, target: TournamentStatus): TournamentStatus {
        if (!canTransition(current, target)) {
            throw InvalidStateTransitionException(current, target)
        }
        return target
    }

    fun handleEvent(current: TournamentStatus, event: TournamentEvent): TournamentStatus {
        val target = when (event) {
            is TournamentEvent.OpenRegistration -> TournamentStatus.REGISTRATION_OPEN
            is TournamentEvent.CloseRegistration -> TournamentStatus.REGISTRATION_CLOSED
            is TournamentEvent.ReopenRegistration -> TournamentStatus.REGISTRATION_OPEN
            is TournamentEvent.PrepareDraw -> TournamentStatus.DRAW_PENDING
            is TournamentEvent.GenerateDraw -> TournamentStatus.DRAW_GENERATED
            is TournamentEvent.RegenerateDraw -> TournamentStatus.DRAW_GENERATED
            is TournamentEvent.LockDraw -> TournamentStatus.DRAW_LOCKED
            is TournamentEvent.StartTournament -> TournamentStatus.IN_PROGRESS
            is TournamentEvent.CompleteTournament -> TournamentStatus.COMPLETED
        }
        return transition(current, target)
    }
}
