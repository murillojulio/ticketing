import React, { createContext, useContext, useMemo, useState } from 'react';

import type { AuthResponse } from './types';
import { clearAuth, loadAuth, saveAuth, type StoredAuth } from '../lib/storage';

type AuthState = StoredAuth | null;

type AuthContextValue = {
  auth: AuthState;
  isAuthenticated: boolean;
  setFromAuthResponse: (res: AuthResponse) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(() => loadAuth());

  const value = useMemo<AuthContextValue>(() => {
    return {
      auth,
      isAuthenticated: Boolean(auth?.token),
      setFromAuthResponse: (res: AuthResponse) => {
        const stored: StoredAuth = {
          token: res.token,
          email: res.email,
          roles: res.roles,
          userId: res.userId
        };
        saveAuth(stored);
        setAuth(stored);
      },
      logout: () => {
        clearAuth();
        setAuth(null);
      }
    };
  }, [auth]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

