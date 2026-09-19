-- ==========================================================
-- BRACKETX — ROW LEVEL SECURITY POLICIES
-- Strict server-side authorization per MASTER_INSTRUCTION.md
-- ==========================================================

-- Enable RLS on all tables
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
ALTER TABLE public.tournament_access_grants ENABLE ROW LEVEL SECURITY;

-- Helper function to check if current user is host of a tournament
CREATE OR REPLACE FUNCTION public.is_tournament_host(p_tournament_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.tournaments
        WHERE id = p_tournament_id AND host_id = auth.uid()
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 1. Profiles
CREATE POLICY "Public profiles are viewable by everyone"
    ON public.profiles FOR SELECT
    USING (true);

CREATE POLICY "Users can insert their own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- 2. Tournaments
CREATE POLICY "Tournaments are viewable by everyone"
    ON public.tournaments FOR SELECT
    USING (
        visibility = 'public'
        OR host_id = (SELECT auth.uid())
        OR id IN (SELECT tournament_id FROM public.tournament_members WHERE user_id = (SELECT auth.uid()))
        OR id IN (SELECT tournament_id FROM public.tournament_access_grants WHERE user_id = (SELECT auth.uid()))
    );

CREATE POLICY "Authenticated users can create tournaments"
    ON public.tournaments FOR INSERT
    TO authenticated
    WITH CHECK ((select auth.uid()) = host_id);

CREATE POLICY "Hosts can update their own tournaments"
    ON public.tournaments FOR UPDATE
    USING (auth.uid() = host_id);

CREATE POLICY "Hosts can delete their own tournaments"
    ON public.tournaments FOR DELETE
    USING (auth.uid() = host_id);

-- 3. Tournament Members
CREATE POLICY "Members viewable by tournament viewers"
    ON public.tournament_members FOR SELECT
    USING (true);

CREATE POLICY "Host can manage members"
    ON public.tournament_members FOR ALL
    USING (public.is_tournament_host(tournament_id));

-- 4. Registrations
CREATE POLICY "Participants can view own registration"
    ON public.registrations FOR SELECT
    USING (auth.uid() = user_id OR public.is_tournament_host(tournament_id));

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

CREATE POLICY "Hosts can update registrations"
    ON public.registrations FOR UPDATE
    USING (public.is_tournament_host(tournament_id));

-- 4b. Tournament Access Grants
CREATE POLICY "Users can view own access grants"
    ON public.tournament_access_grants FOR SELECT
    USING (user_id = (SELECT auth.uid()) OR public.is_tournament_host(tournament_id));

-- 5. Participants
CREATE POLICY "Participants are viewable by everyone"
    ON public.participants FOR SELECT
    USING (true);

CREATE POLICY "Hosts can manage participants"
    ON public.participants FOR ALL
    USING (public.is_tournament_host(tournament_id));

-- 6. Groups & Group Members
CREATE POLICY "Groups viewable by everyone"
    ON public.groups FOR SELECT
    USING (true);

CREATE POLICY "Hosts can manage groups"
    ON public.groups FOR ALL
    USING (public.is_tournament_host(tournament_id));

CREATE POLICY "Group members viewable by everyone"
    ON public.group_members FOR SELECT
    USING (true);

CREATE POLICY "Hosts can manage group members"
    ON public.group_members FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM public.groups g
            WHERE g.id = group_members.group_id
            AND public.is_tournament_host(g.tournament_id)
        )
    );

-- 7. Matches
CREATE POLICY "Matches viewable by everyone"
    ON public.matches FOR SELECT
    USING (true);

CREATE POLICY "Only hosts can manage matches directly"
    ON public.matches FOR ALL
    USING (public.is_tournament_host(tournament_id));

-- 8. Standings
CREATE POLICY "Standings viewable by everyone"
    ON public.standings FOR SELECT
    USING (true);

CREATE POLICY "Only hosts can manage standings"
    ON public.standings FOR ALL
    USING (public.is_tournament_host(tournament_id));

-- 9. Draw Generations
CREATE POLICY "Draw generations viewable by everyone"
    ON public.draw_generations FOR SELECT
    USING (true);

CREATE POLICY "Hosts can insert draw generations"
    ON public.draw_generations FOR INSERT
    WITH CHECK (public.is_tournament_host(tournament_id));

-- 10. Audit Logs
CREATE POLICY "Audit logs viewable by tournament viewers"
    ON public.audit_logs FOR SELECT
    USING (true);

CREATE POLICY "Hosts and system can insert audit logs"
    ON public.audit_logs FOR INSERT
    WITH CHECK (public.is_tournament_host(tournament_id));
