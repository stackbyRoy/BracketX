package com.bracketx.domain.state

import com.bracketx.domain.model.TournamentStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TournamentStateMachineTest {

    @Test
    fun `valid standard lifecycle transitions succeed`() {
        var status = TournamentStatus.DRAFT

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.OpenRegistration)
        assertEquals(TournamentStatus.REGISTRATION_OPEN, status)

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.CloseRegistration)
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, status)

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.PrepareDraw)
        assertEquals(TournamentStatus.DRAW_PENDING, status)

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.GenerateDraw)
        assertEquals(TournamentStatus.DRAW_GENERATED, status)

        // Multiple regenerations before lock should succeed
        status = TournamentStateMachine.handleEvent(status, TournamentEvent.RegenerateDraw)
        assertEquals(TournamentStatus.DRAW_GENERATED, status)

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.LockDraw)
        assertEquals(TournamentStatus.DRAW_LOCKED, status)

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.StartTournament)
        assertEquals(TournamentStatus.IN_PROGRESS, status)

        status = TournamentStateMachine.handleEvent(status, TournamentEvent.CompleteTournament)
        assertEquals(TournamentStatus.COMPLETED, status)
    }

    @Test
    fun `direct draw generation from registration closed succeeds`() {
        val status = TournamentStateMachine.transition(
            TournamentStatus.REGISTRATION_CLOSED,
            TournamentStatus.DRAW_GENERATED
        )
        assertEquals(TournamentStatus.DRAW_GENERATED, status)
    }

    @Test
    fun `reopening registration from registration closed succeeds`() {
        val status = TournamentStateMachine.handleEvent(
            TournamentStatus.REGISTRATION_CLOSED,
            TournamentEvent.ReopenRegistration
        )
        assertEquals(TournamentStatus.REGISTRATION_OPEN, status)
    }

    @Test
    fun `reopening registration from draw generated succeeds`() {
        val status = TournamentStateMachine.handleEvent(
            TournamentStatus.DRAW_GENERATED,
            TournamentEvent.ReopenRegistration
        )
        assertEquals(TournamentStatus.REGISTRATION_OPEN, status)
    }

    @Test
    fun `invalid transitions are rejected`() {
        // DRAFT -> IN_PROGRESS must fail
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.DRAFT, TournamentStatus.IN_PROGRESS)
        }

        // DRAFT -> DRAW_LOCKED must fail
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.DRAFT, TournamentStatus.DRAW_LOCKED)
        }

        // REGISTRATION_OPEN -> DRAW_LOCKED must fail
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.REGISTRATION_OPEN, TournamentStatus.DRAW_LOCKED)
        }

        // DRAW_LOCKED -> DRAW_GENERATED must fail (cannot regenerate once locked without explicit host override)
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.DRAW_LOCKED, TournamentStatus.DRAW_GENERATED)
        }

        // DRAW_LOCKED -> REGISTRATION_OPEN must fail
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.DRAW_LOCKED, TournamentStatus.REGISTRATION_OPEN)
        }

        // IN_PROGRESS -> DRAFT must fail
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.IN_PROGRESS, TournamentStatus.DRAFT)
        }

        // COMPLETED -> IN_PROGRESS must fail (terminal state)
        assertThrows<InvalidStateTransitionException> {
            TournamentStateMachine.transition(TournamentStatus.COMPLETED, TournamentStatus.IN_PROGRESS)
        }
    }
}
