-- ==========================================================
-- BRACKETX — SUPABASE DATABASE SCHEMA
-- Authoritative schema per BACKEND_SCHEMA.md & MASTER_INSTRUCTION.md
-- ==========================================================

-- 1. Profiles
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    avatar_url TEXT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Trigger to create public profile on user signup
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
    public_id TEXT NOT NULL UNIQUE,
    host_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    game TEXT NOT NULL CHECK (game IN ('fc_mobile', 'efootball')),
    format TEXT NOT NULL CHECK (format IN ('knockout', 'league', 'groups_knockout')),
    status TEXT NOT NULL DEFAULT 'draft' CHECK (
        status IN ('draft', 'registration_open', 'registration_closed', 'draw_pending', 'draw_generated', 'draw_locked', 'in_progress', 'completed')
    ),
    visibility TEXT NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'private')),
    private_access_code_hash TEXT NULL,
    max_participants INTEGER DEFAULT 16,
    registration_open BOOLEAN DEFAULT false,
    draw_locked BOOLEAN DEFAULT false,
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Function to generate readable, unique public_id (BRX-XXXXXX)
CREATE OR REPLACE FUNCTION public.generate_tournament_public_id()
RETURNS TEXT AS $$
DECLARE
    v_chars TEXT := '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    v_result TEXT;
    v_length INT := 6;
    v_i INT;
    v_exists BOOLEAN;
BEGIN
    LOOP
        v_result := 'BRX-';
        FOR v_i IN 1..v_length LOOP
            v_result := v_result || substr(v_chars, floor(random() * length(v_chars) + 1)::int, 1);
        END LOOP;

        SELECT EXISTS (
            SELECT 1 FROM public.tournaments WHERE upper(public_id) = upper(v_result)
        ) INTO v_exists;

        EXIT WHEN NOT v_exists;
    END LOOP;

    RETURN v_result;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- Trigger to assign public_id if not supplied on insert
CREATE OR REPLACE FUNCTION public.trg_assign_tournament_public_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.public_id IS NULL OR trim(NEW.public_id) = '' THEN
        NEW.public_id := public.generate_tournament_public_id();
    ELSE
        NEW.public_id := upper(trim(NEW.public_id));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_before_insert_tournaments_public_id ON public.tournaments;
CREATE TRIGGER trg_before_insert_tournaments_public_id
    BEFORE INSERT ON public.tournaments
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_assign_tournament_public_id();

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

-- 12. Tournament Access Grants (for private tournaments)
CREATE TABLE IF NOT EXISTS public.tournament_access_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_tournament_user_grant UNIQUE (tournament_id, user_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_tournaments_host ON public.tournaments(host_id);
CREATE INDEX IF NOT EXISTS idx_tournaments_status ON public.tournaments(status);
CREATE INDEX IF NOT EXISTS idx_tournaments_public_id ON public.tournaments(upper(public_id));
CREATE INDEX IF NOT EXISTS idx_tournaments_visibility ON public.tournaments(visibility);
CREATE INDEX IF NOT EXISTS idx_tournament_access_grants_user_tournament ON public.tournament_access_grants (user_id, tournament_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tournament ON public.registrations(tournament_id);
CREATE INDEX IF NOT EXISTS idx_participants_tournament ON public.participants(tournament_id);
CREATE INDEX IF NOT EXISTS idx_matches_tournament ON public.matches(tournament_id);
CREATE INDEX IF NOT EXISTS idx_matches_group ON public.matches(group_id);
CREATE INDEX IF NOT EXISTS idx_standings_group ON public.standings(group_id);
