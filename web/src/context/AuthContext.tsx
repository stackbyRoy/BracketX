import React, { createContext, useContext, useEffect, useState } from 'react';
import { supabase } from '../api/supabase';

export interface AppUser {
  id: string;
  email?: string;
  displayName: string;
}

interface AuthContextType {
  user: AppUser | null;
  loading: boolean;
  signUp: (email: string, password: string, displayName: string) => Promise<void>;
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

  const signUp = async (email: string, password: string, displayName: string) => {
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

    if (data.user) {
      // Ensure profile row exists
      try {
        await supabase.from('profiles').upsert({
          id: data.user.id,
          display_name: displayName,
        });
      } catch (_) {}

      setUser({
        id: data.user.id,
        email: data.user.email,
        displayName,
      });
    }
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
