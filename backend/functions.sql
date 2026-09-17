-- ==========================================================
-- BRACKETX — SECURE BACKEND OPERATIONS & RPCS
-- Atomic operations per Section 31 of MASTER_INSTRUCTION.md
-- ==========================================================

-- 1. Submit Match Result (Host score submission with auto-advancement & standings update)
CREATE OR REPLACE FUNCTION public.submit_match_result(
    p_tournament_id UUID,
    p_match_id UUID,
    p_score_a INTEGER,
    p_score_b INTEGER,
    p_is_override BOOLEAN DEFAULT false,
    p_reason TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_host_id UUID;
    v_match public.matches%ROWTYPE;
    v_winner_id UUID;
    v_next_match public.matches%ROWTYPE;
    v_tournament_status TEXT;
BEGIN
    -- 1. Check host authorization
    SELECT host_id, status INTO v_host_id, v_tournament_status
    FROM public.tournaments
    WHERE id = p_tournament_id;

    IF v_host_id IS NULL THEN
        RAISE EXCEPTION 'Tournament not found: %', p_tournament_id;
    END IF;

    IF v_host_id != auth.uid() THEN
        RAISE EXCEPTION 'Unauthorized: Only the tournament host can submit official scores.';
    END IF;

    -- 2. Validate scores
    IF p_score_a < 0 OR p_score_b < 0 THEN
        RAISE EXCEPTION 'Invalid score: Scores cannot be negative.';
    END IF;

    -- 3. Fetch match
    SELECT * INTO v_match
    FROM public.matches
    WHERE id = p_match_id AND tournament_id = p_tournament_id;

    IF v_match.id IS NULL THEN
        RAISE EXCEPTION 'Match not found: % in tournament %', p_match_id, p_tournament_id;
    END IF;

    -- 4. Check completed match protection
    IF v_match.status = 'completed' AND NOT p_is_override THEN
        RAISE EXCEPTION 'Match % is already completed. Host override with confirmation required.', p_match_id;
    END IF;

    -- 5. Knockout draws are prohibited
    IF v_match.stage != 'group' AND p_score_a = p_score_b THEN
        RAISE EXCEPTION 'Knockout matches cannot end in a draw.';
    END IF;

    -- 6. Determine winner
    IF p_score_a > p_score_b THEN
        v_winner_id := v_match.participant_a;
    ELSIF p_score_b > p_score_a THEN
        v_winner_id := v_match.participant_b;
    ELSE
        v_winner_id := NULL; -- Group draw
    END IF;

    -- 7. Update match record
    UPDATE public.matches
    SET score_a = p_score_a,
        score_b = p_score_b,
        winner_id = v_winner_id,
        status = 'completed',
        completed_at = now(),
        updated_at = now()
    WHERE id = p_match_id;

    -- 8. Auto-advance knockout winner to next match
    IF v_winner_id IS NOT NULL AND v_match.next_match_id IS NOT NULL THEN
        IF v_match.next_match_slot = 'slot_a' THEN
            UPDATE public.matches
            SET participant_a = v_winner_id, updated_at = now()
            WHERE id = v_match.next_match_id;
        ELSIF v_match.next_match_slot = 'slot_b' THEN
            UPDATE public.matches
            SET participant_b = v_winner_id, updated_at = now()
            WHERE id = v_match.next_match_id;
        END IF;
    END IF;

    -- 9. Update group standings if group match
    IF v_match.stage = 'group' AND v_match.group_id IS NOT NULL THEN
        PERFORM public.recalculate_group_standings(p_tournament_id, v_match.group_id);
    END IF;

    -- 10. Update tournament status to in_progress if currently draw_locked
    IF v_tournament_status = 'draw_locked' THEN
        UPDATE public.tournaments
        SET status = 'in_progress', updated_at = now()
        WHERE id = p_tournament_id;
    END IF;

    -- 11. Write audit log
    INSERT INTO public.audit_logs (tournament_id, actor_id, action, entity_type, entity_id, metadata)
    VALUES (
        p_tournament_id,
        auth.uid(),
        CASE WHEN p_is_override THEN 'MATCH_RESULT_OVERRIDDEN' ELSE 'MATCH_RESULT_UPDATED' END,
        'match',
        p_match_id,
        jsonb_build_object(
            'score_a', p_score_a,
            'score_b', p_score_b,
            'winner_id', v_winner_id,
            'is_override', p_is_override,
            'reason', p_reason
        )
    );

    RETURN jsonb_build_object(
        'success', true,
        'match_id', p_match_id,
        'winner_id', v_winner_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Recalculate Group Standings
CREATE OR REPLACE FUNCTION public.recalculate_group_standings(
    p_tournament_id UUID,
    p_group_id UUID
)
RETURNS VOID AS $$
DECLARE
    v_member RECORD;
    v_played INT;
    v_wins INT;
    v_draws INT;
    v_losses INT;
    v_gf INT;
    v_ga INT;
    v_gd INT;
    v_points INT;
BEGIN
    FOR v_member IN (
        SELECT participant_id FROM public.group_members WHERE group_id = p_group_id
    ) LOOP
        -- Calculate stats for this participant in completed matches
        SELECT
            COUNT(*),
            COALESCE(SUM(CASE WHEN (participant_a = v_member.participant_id AND score_a > score_b) OR (participant_b = v_member.participant_id AND score_b > score_a) THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN score_a = score_b THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN (participant_a = v_member.participant_id AND score_a < score_b) OR (participant_b = v_member.participant_id AND score_b < score_a) THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN participant_a = v_member.participant_id THEN score_a ELSE score_b END), 0),
            COALESCE(SUM(CASE WHEN participant_a = v_member.participant_id THEN score_b ELSE score_a END), 0)
        INTO v_played, v_wins, v_draws, v_losses, v_gf, v_ga
        FROM public.matches
        WHERE tournament_id = p_tournament_id
          AND group_id = p_group_id
          AND status = 'completed'
          AND (participant_a = v_member.participant_id OR participant_b = v_member.participant_id);

        v_gd := v_gf - v_ga;
        v_points := (v_wins * 3) + (v_draws * 1);

        -- Upsert standings record
        INSERT INTO public.standings (
            tournament_id, group_id, participant_id,
            played, wins, draws, losses, goals_for, goals_against, goal_difference, points, updated_at
        ) VALUES (
            p_tournament_id, p_group_id, v_member.participant_id,
            v_played, v_wins, v_draws, v_losses, v_gf, v_ga, v_gd, v_points, now()
        )
        ON CONFLICT (group_id, participant_id) DO UPDATE
        SET played = EXCLUDED.played,
            wins = EXCLUDED.wins,
            draws = EXCLUDED.draws,
            losses = EXCLUDED.losses,
            goals_for = EXCLUDED.goals_for,
            goals_against = EXCLUDED.goals_against,
            goal_difference = EXCLUDED.goal_difference,
            points = EXCLUDED.points,
            updated_at = now();
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Lock Tournament Draw
CREATE OR REPLACE FUNCTION public.lock_tournament_draw(p_tournament_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_host_id UUID;
    v_status TEXT;
BEGIN
    SELECT host_id, status INTO v_host_id, v_status
    FROM public.tournaments
    WHERE id = p_tournament_id;

    IF v_host_id != auth.uid() THEN
        RAISE EXCEPTION 'Unauthorized: Only tournament host can lock draw.';
    END IF;

    IF v_status NOT IN ('draw_generated', 'draw_pending') THEN
        RAISE EXCEPTION 'Cannot lock draw in status: %', v_status;
    END IF;

    UPDATE public.tournaments
    SET draw_locked = true,
        status = 'draw_locked',
        updated_at = now()
    WHERE id = p_tournament_id;

    -- Update locked_at in draw_generations
    UPDATE public.draw_generations
    SET locked_at = now()
    WHERE tournament_id = p_tournament_id AND locked_at IS NULL;

    INSERT INTO public.audit_logs (tournament_id, actor_id, action, entity_type, entity_id)
    VALUES (p_tournament_id, auth.uid(), 'DRAW_LOCKED', 'tournament', p_tournament_id);

    RETURN jsonb_build_object('success', true, 'tournament_id', p_tournament_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
