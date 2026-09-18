-- ==========================================================
-- BRACKETX MIGRATION: 20260918_delete_and_dashboard_tournaments.sql
-- 1. Host-only tournament deletion RPC
-- 2. Dashboard tournaments RPC (ensuring private tournaments are visible to host)
-- ==========================================================

-- 1. Host-Only Tournament Deletion Function
CREATE OR REPLACE FUNCTION public.delete_tournament_as_host(
    p_tournament_id UUID,
    p_host_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_host_id UUID;
BEGIN
    SELECT host_id INTO v_host_id
    FROM public.tournaments
    WHERE id = p_tournament_id;

    IF v_host_id IS NULL THEN
        RETURN FALSE;
    END IF;

    -- Verify caller is the actual host of the tournament
    IF (v_host_id = p_host_id) OR (v_host_id = auth.uid()) THEN
        DELETE FROM public.tournaments WHERE id = p_tournament_id;
        RETURN TRUE;
    ELSE
        RAISE EXCEPTION 'Unauthorized: Only the tournament host can delete this tournament.';
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Dashboard Tournaments Function (Returns public tournaments + host private tournaments)
CREATE OR REPLACE FUNCTION public.get_dashboard_tournaments(
    p_user_id UUID DEFAULT NULL
)
RETURNS SETOF public.tournaments AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM public.tournaments
    WHERE visibility = 'public'
       OR (p_user_id IS NOT NULL AND host_id = p_user_id)
       OR (auth.uid() IS NOT NULL AND host_id = auth.uid())
       OR (p_user_id IS NOT NULL AND id IN (SELECT tournament_id FROM public.tournament_members WHERE user_id = p_user_id))
       OR (auth.uid() IS NOT NULL AND id IN (SELECT tournament_id FROM public.tournament_members WHERE user_id = auth.uid()))
       OR (p_user_id IS NOT NULL AND id IN (SELECT tournament_id FROM public.tournament_access_grants WHERE user_id = p_user_id))
       OR (auth.uid() IS NOT NULL AND id IN (SELECT tournament_id FROM public.tournament_access_grants WHERE user_id = auth.uid()))
    ORDER BY created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permissions to anon and authenticated roles
GRANT EXECUTE ON FUNCTION public.delete_tournament_as_host(UUID, UUID) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.get_dashboard_tournaments(UUID) TO anon, authenticated, service_role;
