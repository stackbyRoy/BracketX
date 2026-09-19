-- ==========================================================
-- BRACKETX MIGRATION: 20260919_fix_tournament_insert_rls.sql
-- 1. Harden public.tournaments INSERT RLS policy:
--    - Target TO authenticated role explicitly
--    - Enforce scalar subquery caching with (select auth.uid()) = host_id
-- 2. Update generate_tournament_public_id() to SECURITY DEFINER
--    to prevent collision check blindness under caller RLS
-- ==========================================================

-- 1. Recreate INSERT policy with TO authenticated and cached auth.uid()
DROP POLICY IF EXISTS "Authenticated users can create tournaments" ON public.tournaments;

CREATE POLICY "Authenticated users can create tournaments"
    ON public.tournaments FOR INSERT
    TO authenticated
    WITH CHECK (
        (select auth.uid()) = host_id
    );

-- 2. Ensure generate_tournament_public_id() runs as SECURITY DEFINER
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
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Grant execute permissions to anon, authenticated, and service_role
GRANT EXECUTE ON FUNCTION public.generate_tournament_public_id() TO anon, authenticated, service_role;
