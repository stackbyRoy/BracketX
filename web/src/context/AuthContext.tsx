import React, { createContext, useContext, useEffect, useState } from 'react';
import { supabase } from '../api/supabase';

export interface AppUser {
  id: string;
  email?: string;
  displayName: string;
}

export interface SignUpResult {
  needsEmailConfirmation?: boolean;
}

interface AuthContextType {
  user: AppUser | null;
  loading: boolean;
  signUp: (email: string, password: string, displayName: string) => Promise<SignUpResult>;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AppUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check active session
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session?.user) {
        syncProfile(session.user.id, session.user.email, session.user.user_metadata?.display_name);
      } else {
        setUser(null);
        setLoading(false);
      }
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user) {
        syncProfile(session.user.id, session.user.email, session.user.user_metadata?.display_name);
      } else {
        setUser(null);
        setLoading(false);
      }
    });

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  const syncProfile = async (id: string, email?: string, metaName?: string) => {
    try {
      const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', id)
        .single();

      const displayName = profile?.display_name || metaName || email?.split('@')[0] || 'Competitor';
      setUser({
        id,
        email,
        displayName,
      });
    } catch (_) {
      setUser({
        id,
        email,
        displayName: metaName || email?.split('@')[0] || 'Competitor',
      });
    } finally {
      setLoading(false);
    }
  };

  const signUp = async (email: string, password: string, displayName: string): Promise<SignUpResult> => {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          display_name: displayName,
        },
      },
    });

    if (error) throw error;

    if (data.session && data.user) {
      // Active session established (autoconfirm enabled or immediate token)
      try {
        await supabase.from('profiles').upsert({
          id: data.user.id,
          display_name: displayName,
        });
      } catch (_) {}

      await syncProfile(data.user.id, data.user.email, displayName);
      return { needsEmailConfirmation: false };
    } else if (data.user) {
      // Email confirmation required by Supabase; no active session token was issued.
      // Do NOT set in-memory user without a valid session!
      setUser(null);
      return { needsEmailConfirmation: true };
    }

    return { needsEmailConfirmation: false };
  };

  const signIn = async (email: string, password: string) => {
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (error) throw error;

    if (data.user) {
      await syncProfile(data.user.id, data.user.email, data.user.user_metadata?.display_name);
    }
  };

  const signOut = async () => {
    await supabase.auth.signOut();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, signUp, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
