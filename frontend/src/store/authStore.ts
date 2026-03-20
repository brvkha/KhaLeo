import { create } from 'zustand'
import type { User } from '../types'
import { getClaimsFromAccessToken, loginWithPassword, logoutWithRefreshToken, registerWithPassword } from '../services/authApi'
import { persistAuthSession, readAuthSession } from '../services/authSession'

type AuthState = {
  currentUser: User | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  bootstrap: () => void
  banUser: (email: string) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  currentUser: null,
  login: async (email, password) => {
    const loginResponse = await loginWithPassword(email, password)
    const claims = getClaimsFromAccessToken(loginResponse.accessToken)
    const user: User = {
      id: claims.userId,
      email: email.trim().toLowerCase(),
      role: claims.role,
      verified: true,
      banned: false,
    }

    persistAuthSession({
      currentUser: user,
      accessToken: loginResponse.accessToken,
      refreshToken: loginResponse.refreshToken,
    })

    set({ currentUser: user })
  },
  register: async (email, password) => {
    await registerWithPassword(email, password)
  },
  logout: async () => {
    const session = readAuthSession()
    if (session?.refreshToken) {
      try {
        await logoutWithRefreshToken(session.refreshToken)
      } catch {
        // Local logout should still proceed if remote token revocation fails.
      }
    }

    persistAuthSession(null)
    set({ currentUser: null })
  },
  bootstrap: () => {
    set({ currentUser: readAuthSession()?.currentUser ?? null })
  },
  banUser: (email) => {
    set((state) => {
      if (!state.currentUser || state.currentUser.email !== email) {
        return state
      }
      const next = { ...state.currentUser, banned: true }
      const session = readAuthSession()
      if (session) {
        persistAuthSession({
          ...session,
          currentUser: next,
        })
      }
      return { currentUser: next }
    })
  },
}))
