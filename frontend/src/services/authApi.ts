type LoginResponse = {
  accessToken: string
  refreshToken: string | null
  expiresIn: number
}

type RegisterResponse = {
  userId: string
  email: string
  verificationRequired: boolean
}

export type AuthClaims = {
  userId: string
  role: 'USER' | 'ADMIN'
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function decodeJwtPayload(token: string): Record<string, unknown> {
  const [, payload] = token.split('.')
  if (!payload) {
    throw new Error('Invalid token payload')
  }
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  const pad = normalized.length % 4
  const padded = normalized + (pad > 0 ? '='.repeat(4 - pad) : '')
  const json = atob(padded)
  return JSON.parse(json) as Record<string, unknown>
}

function parseErrorMessage(payload: unknown, fallbackStatus: number): string {
  if (payload && typeof payload === 'object' && 'message' in payload && typeof payload.message === 'string') {
    return payload.message
  }
  return `Request failed: ${fallbackStatus}`
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    let payload: unknown = null
    try {
      payload = await response.json()
    } catch {
      payload = null
    }
    throw new Error(parseErrorMessage(payload, response.status))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export async function loginWithPassword(email: string, password: string): Promise<LoginResponse> {
  return requestJson<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export async function registerWithPassword(email: string, password: string): Promise<RegisterResponse> {
  return requestJson<RegisterResponse>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export async function logoutWithRefreshToken(refreshToken: string): Promise<void> {
  await requestJson('/api/v1/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
}

export async function refreshAccessToken(refreshToken: string): Promise<LoginResponse> {
  return requestJson<LoginResponse>('/api/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
}

export function getClaimsFromAccessToken(accessToken: string): AuthClaims {
  const payload = decodeJwtPayload(accessToken)
  const sub = typeof payload.sub === 'string' ? payload.sub : ''
  const roleValue = typeof payload.role === 'string' ? payload.role : 'ROLE_USER'
  const role = roleValue === 'ROLE_ADMIN' ? 'ADMIN' : 'USER'

  if (!sub) {
    throw new Error('Token does not include subject')
  }

  return {
    userId: sub,
    role,
  }
}