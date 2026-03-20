import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './authStore'
import { persistAuthSession } from '../services/authSession'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    persistAuthSession(null)
    useAuthStore.setState({
      currentUser: null,
      login: useAuthStore.getState().login,
      register: useAuthStore.getState().register,
      logout: useAuthStore.getState().logout,
      bootstrap: useAuthStore.getState().bootstrap,
      banUser: useAuthStore.getState().banUser,
    })
  })

  it('bootstraps persisted auth session', () => {
    persistAuthSession({
      currentUser: {
        id: 'u1',
        email: 'khaleo@khaleo.app',
        role: 'USER',
        verified: true,
        banned: false,
      },
      accessToken: 'token',
      refreshToken: 'refresh',
    })

    useAuthStore.setState({
      ...useAuthStore.getState(),
      currentUser: null,
    })

    useAuthStore.getState().bootstrap()
    expect(useAuthStore.getState().currentUser?.email).toBe('khaleo@khaleo.app')
  })
})
