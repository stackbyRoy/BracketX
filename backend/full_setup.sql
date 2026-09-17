-- ==========================================================
-- BRACKETX — COMPLETE SUPABASE DATABASE SETUP SCRIPT
-- Run this in the Supabase SQL Editor (Dashboard -> SQL Editor -> New Query)
-- ==========================================================

-- ==========================================
-- PART 1: TABLES & TRIGGERS
-- ==========================================

-- 1. Profiles
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    avatar_url TEXT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, display_name)
    VALUES (new.id, COALESCE(new.raw_user_meta_data->>'display_name', 'Player'));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 2. Tournaments
CREATE TABLE IF NOT EXISTS public.tournaments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    game TEXT NOT NULL CHECK (game IN ('fc_mobile', 'efootball')),
    format TEXT NOT NULL CHECK (format IN ('knockout', 'league', 'groups_knockout')),
    status TEXT NOT NULL DEFAULT 'draft' CHECK (
        status IN ('draft', 'registration_open', 'registration_closed', 'draw_pending', 'draw_generated', 'draw_locked', 'in_progress', 'completed')
    ),
    max_participants INTEGER DEFAULT 16,
    registration_open BOOLEAN DEFAULT false,
    draw_locked BOOLEAN DEFAULT false,
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Tournament Members
CREATE TABLE IF NOT EXISTS public.tournament_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('host', 'participant')),
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_tournament_user UNIQUE (tournament_id, user_id)
);

-- 4. Registrations
CREATE TABLE IF NOT EXISTS public.registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    in_game_id TEXT NOT NULL,
    game_metric_type TEXT NOT NULL,
    game_metric_value NUMERIC NOT NULL,
    status TEXT NOT NULL DEFAULT 'confirmed' CHECK (
        status IN ('pending', 'approved', 'rejected', 'confirmed')
    ),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_tournament_registration UNIQUE (tournament_id, user_id)
);

-- 5. Participants
CREATE TABLE IF NOT EXISTS public.participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    in_game_id TEXT NOT NULL,
    game_metric_type TEXT NOT NULL,
    game_metric_value NUMERIC NOT NULL,
    normalized_score NUMERIC DEFAULT 50.0,
    seed INTEGER NULL,
    competitive_band TEXT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'withdrawn', 'disqualified')),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_tournament_participant UNIQUE (tournament_id, user_id)
);

-- 6. Groups
CREATE TABLE IF NOT EXISTS public.groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    group_order INTEGER NOT NULL,
    CONSTRAINT unique_tournament_group_name UNIQUE (tournament_id, name)
);

-- 7. Group Members
CREATE TABLE IF NOT EXISTS public.group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES public.participants(id) ON DELETE CASCADE,
    CONSTRAINT unique_group_member UNIQUE (group_id, participant_id)
);

-- 8. Matches
CREATE TABLE IF NOT EXISTS public.matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    stage TEXT NOT NULL,
    round_number INTEGER NOT NULL DEFAULT 1,
    match_number INTEGER NOT NULL,
    group_id UUID NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    participant_a UUID NULL REFERENCES public.participants(id) ON DELETE SET NULL,
    participant_b UUID NULL REFERENCES public.participants(id) ON DELETE SET NULL,
    score_a INTEGER NULL CHECK (score_a IS NULL OR score_a >= 0),
    score_b INTEGER NULL CHECK (score_b IS NULL OR score_b >= 0),
    winner_id UUID NULL REFERENCES public.participants(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'live', 'completed', 'disputed', 'cancelled')),
    next_match_id UUID NULL REFERENCES public.matches(id) ON DELETE SET NULL,
    next_match_slot TEXT NULL CHECK (next_match_slot IS NULL OR next_match_slot IN ('slot_a', 'slot_b')),
    is_bye BOOLEAN DEFAULT false,
    scheduled_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 9. Standings
CREATE TABLE IF NOT EXISTS public.standings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES public.participants(id) ON DELETE CASCADE,
    played INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    draws INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    goals_for INTEGER DEFAULT 0,
    goals_against INTEGER DEFAULT 0,
    goal_difference INTEGER DEFAULT 0,
    points INTEGER DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_standing_entry UNIQUE (group_id, participant_id)
);

-- 10. Draw Generations
CREATE TABLE IF NOT EXISTS public.draw_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    algorithm_version TEXT NOT NULL,
    random_seed TEXT NOT NULL,
    fairness_score NUMERIC NULL,
    candidate_count INTEGER DEFAULT 500,
    generated_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    generated_at TIMESTAMPTZ DEFAULT now(),
    locked_at TIMESTAMPTZ NULL,
    snapshot JSONB NULL
);

-- 11. Audit Logs
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID NULL,
    metadata JSONB NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_tournaments_host ON public.tournaments(host_id);
CREATE INDEX IF NOT EXISTS idx_tournaments_status ON public.tournaments(status);
CREATE INDEX IF NOT EXISTS idx_registrations_tournament ON public.registrations(tournament_id);
CREATE INDEX IF NOT EXISTS idx_participants_tournament ON public.participants(tournament_id);
CREATE INDEX IF NOT EXISTS idx_matches_tournament ON public.matches(tournament_id);
CREATE INDEX IF NOT EXISTS idx_matches_group ON public.matches(group_id);
CREATE INDEX IF NOT EXISTS idx_standings_group ON public.standings(group_id);

-- Enable Realtime publications on key tournament tables
ALTER PUBLICATION supabase_realtime ADD TABLE public.tournaments;
ALTER PUBLICATION supabase_realtime ADD TABLE public.matches;
ALTER PUBLICATION supabase_realtime ADD TABLE public.standings;
ALTER PUBLICATION supabase_realtime ADD TABLE public.participants;

-- ==========================================
-- PART 2: ROW LEVEL SECURITY (RLS)
-- ==========================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.standings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.draw_generations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.is_tournament_host(p_tournament_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.tournaments
        WHERE id = p_tournament_id AND host_id = auth.uid()
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Profiles Policies
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Public profiles are viewable by everyone" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
CREATE POLICY "Users can insert their own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Tournaments Policies
DROP POLICY IF EXISTS "Tournaments are viewable by everyone" ON public.tournaments;
CREATE POLICY "Tournaments are viewable by everyone" ON public.tournaments FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can create tournaments" ON public.tournaments;
CREATE POLICY "Authenticated users can create tournaments" ON public.tournaments FOR INSERT WITH CHECK (auth.uid() = host_id);

DROP POLICY IF EXISTS "Hosts can update their own tournaments" ON public.tournaments;
CREATE POLICY "Hosts can update their own tournaments" ON public.tournaments FOR UPDATE USING (auth.uid() = host_id);

DROP POLICY IF EXISTS "Hosts can delete their own tournaments" ON public.tournaments;
CREATE POLICY "Hosts can delete their own tournaments" ON public.tournaments FOR DELETE USING (auth.uid() = host_id);

-- Registrations Policies
DROP POLICY IF EXISTS "Participants can view own registration" ON public.registrations;
CREATE POLICY "Participants can view own registration" ON public.registrations FOR SELECT USING (auth.uid() = user_id OR public.is_tournament_host(tournament_id));

DROP POLICY IF EXISTS "Users can register themselves for tournaments" ON public.registrations;
CREATE POLICY "Users can register themselves for tournaments" ON public.registrations FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Hosts can update registrations" ON public.registrations;
CREATE POLICY "Hosts can update registrations" ON public.registrations FOR UPDATE USING (public.is_tournament_host(tournament_id));

-- Participants Policies
DROP POLICY IF EXISTS "Participants are viewable by everyone" ON public.participants;
CREATE POLICY "Participants are viewable by everyone" ON public.participants FOR SELECT USING (true);

DROP POLICY IF EXISTS "Hosts can manage participants" ON public.participants;
CREATE POLICY "Hosts can manage participants" ON public.participants FOR ALL USING (public.is_tournament_host(tournament_id));

-- Groups & Members Policies
DROP POLICY IF EXISTS "Groups viewable by everyone" ON public.groups;
CREATE POLICY "Groups viewable by everyone" ON public.groups FOR SELECT USING (true);

DROP POLICY IF EXISTS "Hosts can manage groups" ON public.groups;
CREATE POLICY "Hosts can manage groups" ON public.groups FOR ALL USING (public.is_tournament_host(tournament_id));

DROP POLICY IF EXISTS "Group members viewable by everyone" ON public.group_members;
CREATE POLICY "Group members viewable by everyone" ON public.group_members FOR SELECT USING (true);

-- Matches Policies
DROP POLICY IF EXISTS "Matches viewable by everyone" ON public.matches;
CREATE POLICY "Matches viewable by everyone" ON public.matches FOR SELECT USING (true);

DROP POLICY IF EXISTS "Only hosts can manage matches directly" ON public.matches;
CREATE POLICY "Only hosts can manage matches directly" ON public.matches FOR ALL USING (public.is_tournament_host(tournament_id));

-- Standings Policies
DROP POLICY IF EXISTS "Standings viewable by everyone" ON public.standings;
CREATE POLICY "Standings viewable by everyone" ON public.standings FOR SELECT USING (true);

-- ==========================================
-- PART 3: SECURE FUNCTIONS & RPCS
-- ==========================================

-- Submit Match Result (Host score submission with auto-advancement & standings update)
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
    v_tournament_status TEXT;
BEGIN
    SELECT host_id, status INTO v_host_id, v_tournament_status
    FROM public.tournaments
    WHERE id = p_tournament_id;

    IF v_host_id IS NULL THEN
        RAISE EXCEPTION 'Tournament not found: %', p_tournament_id;
    END IF;

    IF v_host_id != auth.uid() THEN
        RAISE EXCEPTION 'Unauthorized: Only the tournament host can submit official scores.';
    END IF;

    IF p_score_a < 0 OR p_score_b < 0 THEN
        RAISE EXCEPTION 'Invalid score: Scores cannot be negative.';
    END IF;

    SELECT * INTO v_match
    FROM public.matches
    WHERE id = p_match_id AND tournament_id = p_tournament_id;

    IF v_match.id IS NULL THEN
        RAISE EXCEPTION 'Match not found: % in tournament %', p_match_id, p_tournament_id;
    END IF;

    IF v_match.status = 'completed' AND NOT p_is_override THEN
        RAISE EXCEPTION 'Match % is already completed. Host override with confirmation required.', p_match_id;
    END IF;

    IF v_match.stage != 'group' AND p_score_a = p_score_b THEN
        RAISE EXCEPTION 'Knockout matches cannot end in a draw.';
    END IF;

    IF p_score_a > p_score_b THEN
        v_winner_id := v_match.participant_a;
    ELSIF p_score_b > p_score_a THEN
        v_winner_id := v_match.participant_b;
    ELSE
        v_winner_id := NULL;
    END IF;

    UPDATE public.matches
    SET score_a = p_score_a,
        score_b = p_score_b,
        winner_id = v_winner_id,
        status = 'completed',
        completed_at = now(),
        updated_at = now()
    WHERE id = p_match_id;

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

    IF v_match.stage = 'group' AND v_match.group_id IS NOT NULL THEN
        PERFORM public.recalculate_group_standings(p_tournament_id, v_match.group_id);
    END IF;

    IF v_tournament_status = 'draw_locked' THEN
        UPDATE public.tournaments
        SET status = 'in_progress', updated_at = now()
        WHERE id = p_tournament_id;
    END IF;

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

    RETURN jsonb_build_object('success', true, 'match_id', p_match_id, 'winner_id', v_winner_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Recalculate Group Standings
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

-- Lock Tournament Draw
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

    UPDATE public.tournaments
    SET draw_locked = true,
        status = 'draw_locked',
        updated_at = now()
    WHERE id = p_tournament_id;

    UPDATE public.draw_generations
    SET locked_at = now()
    WHERE tournament_id = p_tournament_id AND locked_at IS NULL;

    INSERT INTO public.audit_logs (tournament_id, actor_id, action, entity_type, entity_id)
    VALUES (p_tournament_id, auth.uid(), 'DRAW_LOCKED', 'tournament', p_tournament_id);

    RETURN jsonb_build_object('success', true, 'tournament_id', p_tournament_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
