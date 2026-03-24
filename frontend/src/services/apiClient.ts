import { refreshAccessToken } from './authApi'
import { getAccessToken, persistAuthSession, readAuthSession } from './authSession'
import { useNotificationStore } from '../store/notificationStore'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type RequestOptions = RequestInit & {
  useAuth?: boolean
}

function parseErrorMessage(payload: unknown, fallbackStatus: number): string {
  if (payload && typeof payload === 'object' && 'message' in payload && typeof payload.message === 'string') {
    return payload.message
  }
  return `Request failed: ${fallbackStatus}`
}

export async function requestJson<T>(path: string, init?: RequestOptions): Promise<T> {
  const useAuth = init?.useAuth !== false
  const requestWithToken = (token: string | null) =>
    fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(init?.headers ?? {}),
      },
    })

  const tryRefreshAccessToken = async (): Promise<string | null> => {
    const session = readAuthSession()
    if (!session?.refreshToken) {
      return null
    }

    try {
      const refreshResponse = await refreshAccessToken(session.refreshToken)
      const nextAccessToken = refreshResponse.accessToken
      persistAuthSession({
        ...session,
        accessToken: nextAccessToken,
        refreshToken: refreshResponse.refreshToken ?? session.refreshToken,
      })
      return nextAccessToken
    } catch {
      persistAuthSession(null)
      return null
    }
  }

  const token = useAuth ? getAccessToken() : null
  let response = await requestWithToken(token)

  if (response.status === 401 && useAuth) {
    const refreshedAccessToken = await tryRefreshAccessToken()
    if (refreshedAccessToken) {
      response = await requestWithToken(refreshedAccessToken)
    }
  }

  if (!response.ok) {
    let payload: unknown = null
    try {
      payload = await response.json()
    } catch {
      payload = null
    }
    const message = parseErrorMessage(payload, response.status)
    console.error('api_request_failed', {
      path,
      status: response.status,
      payload,
      message,
    })
    useNotificationStore.getState().pushError(message)
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}