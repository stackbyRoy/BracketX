-- ==========================================================
-- BRACKETX MIGRATION: 20260918_visibility_and_public_id.sql
-- Tournament Visibility, Public IDs, Private Access Authorization
-- ==========================================================

-- 1. Add columns to public.tournaments if not already present
ALTER TABLE public.tournaments
    ADD COLUMN IF NOT EXISTS public_id TEXT,
    ADD COLUMN IF NOT EXISTS visibility TEXT NOT NULL DEFAULT 'public',
    ADD COLUMN IF NOT EXISTS private_access_code_hash TEXT NULL;

-- 2. Enforce check constraints on visibility
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'check_tournament_visibility'
    ) THEN
        ALTER TABLE public.tournaments
            ADD CONSTRAINT check_tournament_visibility
            CHECK (visibility IN ('public', 'private'));
    END IF;
END $$;

-- 3. Function to generate secure, readable, non-sequential Tournament Public IDs (Format: BRX-XXXXXX)
CREATE OR REPLACE FUNCTION public.generate_tournament_public_id()
RETURNS TEXT AS $$
DECLARE
    -- Crockford / unambiguous alphanumeric chars (excluding 0, O, 1, I for ease of verbal/typing communication)
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

-- 4. Backfill existing tournaments with public_id if any exist without one
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT id FROM public.tournaments WHERE public_id IS NULL LOOP
        UPDATE public.tournaments
        SET public_id = public.generate_tournament_public_id()
        WHERE id = r.id;
    END LOOP;
END $$;

-- 5. Add NOT NULL and UNIQUE constraint on public_id
ALTER TABLE public.tournaments
    ALTER COLUMN public_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_tournament_public_id'
    ) THEN
        ALTER TABLE public.tournaments
            ADD CONSTRAINT unique_tournament_public_id UNIQUE (public_id);
    END IF;
END $$;

-- 6. Trigger to automatically generate public_id on tournament creation if omitted
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

-- 7. Tournament Access Grants table for tracking verified private tournament authorizations per user
CREATE TABLE IF NOT EXISTS public.tournament_access_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_tournament_user_grant UNIQUE (tournament_id, user_id)
);

ALTER TABLE public.tournament_access_grants ENABLE ROW LEVEL SECURITY;

-- 8. Indexes for fast, case-insensitive lookups and RLS joins
CREATE INDEX IF NOT EXISTS idx_tournaments_public_id ON public.tournaments (upper(public_id));
CREATE INDEX IF NOT EXISTS idx_tournaments_visibility ON public.tournaments (visibility);
CREATE INDEX IF NOT EXISTS idx_tournament_access_grants_user_tournament ON public.tournament_access_grants (user_id, tournament_id);

-- 9. RPC: Resolve Tournament by Public ID (Deterministic, Case-Insensitive, Server-Safe)
-- Returns tournament information without exposing private_access_code_hash
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

-- 10. RPC: Verify Private Access & Issue Authorization Grant
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

    -- Host always has access
    IF v_t.host_id = v_caller_id THEN
        RETURN jsonb_build_object('success', true, 'message', 'Host authorized.');
    END IF;

    -- Check if registration is open
    IF NOT v_t.registration_open OR v_t.status != 'registration_open' THEN
        RAISE EXCEPTION 'Registration is not currently open for this tournament.';
    END IF;

    -- Check capacity
    SELECT COUNT(*) INTO v_count FROM public.participants WHERE tournament_id = p_tournament_id;
    IF v_count >= v_t.max_participants THEN
        RAISE EXCEPTION 'Tournament capacity reached.';
    END IF;

    -- Check duplicate registration
    SELECT EXISTS (
        SELECT 1 FROM public.registrations WHERE tournament_id = p_tournament_id AND user_id = v_caller_id
    ) INTO v_already_registered;

    IF v_already_registered THEN
        RAISE EXCEPTION 'User is already registered for this tournament.';
    END IF;

    -- Compare SHA-256 hash of normalized code
    v_code_hash := encode(sha256(v_code_clean::bytea), 'hex');
    IF v_t.private_access_code_hash IS NULL OR v_t.private_access_code_hash != v_code_hash THEN
        RAISE EXCEPTION 'Invalid access code.';
    END IF;

    -- Upsert tournament access grant
    INSERT INTO public.tournament_access_grants (tournament_id, user_id)
    VALUES (p_tournament_id, v_caller_id)
    ON CONFLICT (tournament_id, user_id) DO UPDATE
    SET granted_at = now();

    -- Audit log
    INSERT INTO public.audit_logs (tournament_id, actor_id, action, entity_type, entity_id)
    VALUES (p_tournament_id, v_caller_id, 'PRIVATE_ACCESS_GRANTED', 'tournament', p_tournament_id);

    RETURN jsonb_build_object(
        'success', true,
        'tournament_id', p_tournament_id,
        'message', 'Access granted.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 11. RPC: Update Tournament Visibility (Host only, restricted lifecycle)
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

    -- Visibility changes allowed only in DRAFT or REGISTRATION_OPEN
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

-- 12. Update RLS Policies
-- Tournaments: Public discovery only returns public tournaments, or user's own/authorized tournaments
DROP POLICY IF EXISTS "Tournaments are viewable by everyone" ON public.tournaments;
CREATE POLICY "Tournaments are viewable by everyone"
    ON public.tournaments FOR SELECT
    USING (
        visibility = 'public'
        OR host_id = (SELECT auth.uid())
        OR id IN (SELECT tournament_id FROM public.tournament_members WHERE user_id = (SELECT auth.uid()))
        OR id IN (SELECT tournament_id FROM public.tournament_access_grants WHERE user_id = (SELECT auth.uid()))
    );

-- Tournament Access Grants RLS
DROP POLICY IF EXISTS "Users can view own access grants" ON public.tournament_access_grants;
CREATE POLICY "Users can view own access grants"
    ON public.tournament_access_grants FOR SELECT
    USING (user_id = (SELECT auth.uid()) OR public.is_tournament_host(tournament_id));

-- Registrations: For private tournaments, registration requires an access grant or host status
DROP POLICY IF EXISTS "Users can register themselves for tournaments" ON public.registrations;
CREATE POLICY "Users can register themselves for tournaments"
    ON public.registrations FOR INSERT
    WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.tournaments t
            WHERE t.id = tournament_id
            AND t.registration_open = true
            AND (
                t.visibility = 'public'
                OR t.host_id = (SELECT auth.uid())
                OR EXISTS (
                    SELECT 1 FROM public.tournament_access_grants g
                    WHERE g.tournament_id = t.id AND g.user_id = (SELECT auth.uid())
                )
            )
        )
    );
