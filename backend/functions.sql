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

-- 4. Resolve Tournament by Public ID (Deterministic, Case-Insensitive, Safe)
CREATE OR REPLACE FUNCTION public.resolve_tournament_by_public_id(p_public_id TEXT)
RETURNS JSONB AS $$
DECLARE
    v_norm_id TEXT := upper(trim(p_public_id));
    v_t RECORD;
    v_registered_count INT;
    v_is_registered BOOLEAN := false;
    v_is_authorized BOOLEAN := false;
    v_caller_id UUID := auth.uid();
BEGIN
    SELECT * INTO v_t
    FROM public.tournaments
    WHERE upper(public_id) = v_norm_id;

    IF v_t.id IS NULL THEN
        RETURN jsonb_build_object('found', false);
    END IF;

    SELECT COUNT(*) INTO v_registered_count
    FROM public.participants
    WHERE tournament_id = v_t.id;

    IF v_caller_id IS NOT NULL THEN
        SELECT EXISTS (
            SELECT 1 FROM public.registrations
            WHERE tournament_id = v_t.id AND user_id = v_caller_id
        ) INTO v_is_registered;

        IF v_t.visibility = 'public' OR v_t.host_id = v_caller_id THEN
            v_is_authorized := true;
        ELSE
            SELECT EXISTS (
                SELECT 1 FROM public.tournament_access_grants
                WHERE tournament_id = v_t.id AND user_id = v_caller_id
            ) INTO v_is_authorized;
        END IF;
    END IF;

    RETURN jsonb_build_object(
        'found', true,
        'id', v_t.id,
        'public_id', v_t.public_id,
        'host_id', v_t.host_id,
        'name', v_t.name,
        'game', v_t.game,
        'format', v_t.format,
        'status', v_t.status,
        'max_participants', v_t.max_participants,
        'registration_open', v_t.registration_open,
        'draw_locked', v_t.draw_locked,
        'visibility', v_t.visibility,
        'created_at', v_t.created_at,
        'participant_count', v_registered_count,
        'is_registered', v_is_registered,
        'is_authorized', v_is_authorized
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 5. Verify Private Access Code & Issue Access Grant
CREATE OR REPLACE FUNCTION public.verify_private_access(
    p_tournament_id UUID,
    p_access_code TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_t RECORD;
    v_code_clean TEXT := upper(trim(p_access_code));
    v_code_hash TEXT;
    v_caller_id UUID := auth.uid();
    v_already_registered BOOLEAN;
    v_count INT;
BEGIN
    IF v_caller_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized: Authentication required.';
    END IF;

    SELECT * INTO v_t
    FROM public.tournaments
    WHERE id = p_tournament_id;

    IF v_t.id IS NULL THEN
        RAISE EXCEPTION 'Tournament not found.';
    END IF;

    IF v_t.visibility != 'private' THEN
        RETURN jsonb_build_object('success', true, 'message', 'Tournament is public, no code required.');
    END IF;

    IF v_t.host_id = v_caller_id THEN
        RETURN jsonb_build_object('success', true, 'message', 'Host authorized.');
    END IF;

    IF NOT v_t.registration_open OR v_t.status != 'registration_open' THEN
        RAISE EXCEPTION 'Registration is not currently open for this tournament.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM public.participants WHERE tournament_id = p_tournament_id;
    IF v_count >= v_t.max_participants THEN
        RAISE EXCEPTION 'Tournament capacity reached.';
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.registrations WHERE tournament_id = p_tournament_id AND user_id = v_caller_id
    ) INTO v_already_registered;

    IF v_already_registered THEN
        RAISE EXCEPTION 'User is already registered for this tournament.';
    END IF;

    v_code_hash := encode(sha256(v_code_clean::bytea), 'hex');
    IF v_t.private_access_code_hash IS NULL OR v_t.private_access_code_hash != v_code_hash THEN
        RAISE EXCEPTION 'Invalid access code.';
    END IF;

    INSERT INTO public.tournament_access_grants (tournament_id, user_id)
    VALUES (p_tournament_id, v_caller_id)
    ON CONFLICT (tournament_id, user_id) DO UPDATE
    SET granted_at = now();

    INSERT INTO public.audit_logs (tournament_id, actor_id, action, entity_type, entity_id)
    VALUES (p_tournament_id, v_caller_id, 'PRIVATE_ACCESS_GRANTED', 'tournament', p_tournament_id);

    RETURN jsonb_build_object(
        'success', true,
        'tournament_id', p_tournament_id,
        'message', 'Access granted.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 6. Update Tournament Visibility (Host only, restricted lifecycle)
CREATE OR REPLACE FUNCTION public.update_tournament_visibility(
    p_tournament_id UUID,
    p_visibility TEXT,
    p_access_code TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_t RECORD;
    v_caller_id UUID := auth.uid();
    v_code_hash TEXT := NULL;
BEGIN
    IF v_caller_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized.';
    END IF;

    SELECT * INTO v_t FROM public.tournaments WHERE id = p_tournament_id;
    IF v_t.id IS NULL THEN
        RAISE EXCEPTION 'Tournament not found.';
    END IF;

    IF v_t.host_id != v_caller_id THEN
        RAISE EXCEPTION 'Unauthorized: Only the host can update tournament visibility.';
    END IF;

    IF v_t.status NOT IN ('draft', 'registration_open') THEN
        RAISE EXCEPTION 'Tournament visibility cannot be changed after registration closes.';
    END IF;

    IF p_visibility NOT IN ('public', 'private') THEN
        RAISE EXCEPTION 'Invalid visibility value. Must be public or private.';
    END IF;

    IF p_visibility = 'private' THEN
        IF p_access_code IS NULL OR length(trim(p_access_code)) < 4 THEN
            RAISE EXCEPTION 'Private tournaments require a valid access code (min 4 characters).';
        END IF;
        v_code_hash := encode(sha256(upper(trim(p_access_code))::bytea), 'hex');
    END IF;

    UPDATE public.tournaments
    SET visibility = p_visibility,
        private_access_code_hash = v_code_hash,
        updated_at = now()
    WHERE id = p_tournament_id;

    INSERT INTO public.audit_logs (tournament_id, actor_id, action, entity_type, entity_id, metadata)
    VALUES (
        p_tournament_id,
        v_caller_id,
        'VISIBILITY_UPDATED',
        'tournament',
        p_tournament_id,
        jsonb_build_object('visibility', p_visibility)
    );

    RETURN jsonb_build_object('success', true, 'visibility', p_visibility);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
