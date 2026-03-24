import { requestJson } from './apiClient'
import { getAccessToken } from './authSession'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type PagedResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

type PagingParams = {
  query?: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export type AdminStatsDto = {
  totalUsers: number
  totalDecks: number
  totalCards: number
  reviewsLast24Hours: number
  generatedAt: string
}

export type AdminUserModerationItemDto = {
  id: string
  email: string
  role: string
  verified: boolean
  banned: boolean
  createdAt: string
}

export type AdminDeckModerationItemDto = {
  id: string
  name: string
  ownerEmail: string
  isPublic: boolean
  banned: boolean
  cardCount: number
  createdAt: string
}

export type AdminModerationActionDto = {
  id: string
  adminUserId: string
  adminEmail: string
  actionType: string
  targetType: string
  targetId: string
  targetDisplayName: string
  status: string
  reasonCode: string | null
  createdAt: string
}

function buildPagingQuery(params?: PagingParams): string {
  const query = params?.query ?? ''
  const page = params?.page ?? 0
  const size = params?.size ?? 20
  const sortBy = params?.sortBy ?? 'createdAt'
  const sortDir = params?.sortDir ?? 'desc'
  return `q=${encodeURIComponent(query)}&page=${page}&size=${size}&sortBy=${encodeURIComponent(sortBy)}&sortDir=${sortDir}`
}

export async function getAdminStats(): Promise<AdminStatsDto> {
  return requestJson<AdminStatsDto>('/api/v1/admin/stats')
}

export async function listAdminUsers(params?: PagingParams): Promise<PagedResponse<AdminUserModerationItemDto>> {
  return requestJson<PagedResponse<AdminUserModerationItemDto>>(`/api/v1/admin/users?${buildPagingQuery(params)}`)
}

export async function listAdminDecks(params?: PagingParams): Promise<PagedResponse<AdminDeckModerationItemDto>> {
  return requestJson<PagedResponse<AdminDeckModerationItemDto>>(`/api/v1/admin/decks?${buildPagingQuery(params)}`)
}

export async function banAdminUser(userId: string): Promise<void> {
  await requestJson(`/api/v1/admin/users/${userId}/ban`, {
    method: 'POST',
  })
}

export async function unbanAdminUser(userId: string): Promise<void> {
  await requestJson(`/api/v1/admin/users/${userId}/unban`, {
    method: 'POST',
  })
}

export async function banAdminDeck(deckId: string): Promise<void> {
  await requestJson(`/api/v1/admin/decks/${deckId}/ban`, {
    method: 'POST',
  })
}

export async function unbanAdminDeck(deckId: string): Promise<void> {
  await requestJson(`/api/v1/admin/decks/${deckId}/unban`, {
    method: 'POST',
  })
}

type ModerationActionParams = {
  adminUserId?: string
  adminEmail?: string
  targetType?: '' | 'USER' | 'DECK' | 'CARD'
  status?: '' | 'SUCCESS' | 'FAILURE'
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export async function listAdminModerationActions(params?: ModerationActionParams): Promise<PagedResponse<AdminModerationActionDto>> {
  const adminUserId = params?.adminUserId ?? ''
  const adminEmail = params?.adminEmail ?? ''
  const targetType = params?.targetType ?? ''
  const status = params?.status ?? ''
  const page = params?.page ?? 0
  const size = params?.size ?? 20
  const sortBy = params?.sortBy ?? 'createdAt'
  const sortDir = params?.sortDir ?? 'desc'

  const query =
    `adminUserId=${encodeURIComponent(adminUserId)}` +
    `&adminEmail=${encodeURIComponent(adminEmail)}` +
    `&targetType=${encodeURIComponent(targetType)}` +
    `&status=${encodeURIComponent(status)}` +
    `&page=${page}` +
    `&size=${size}` +
    `&sortBy=${encodeURIComponent(sortBy)}` +
    `&sortDir=${sortDir}`

  return requestJson<PagedResponse<AdminModerationActionDto>>(`/api/v1/admin/moderation-actions?${query}`)
}

export async function exportAdminModerationActionsCsv(params?: {
  adminUserId?: string
  adminEmail?: string
  targetType?: '' | 'USER' | 'DECK' | 'CARD'
  status?: '' | 'SUCCESS' | 'FAILURE'
  size?: number
}): Promise<string> {
  const adminUserId = params?.adminUserId ?? ''
  const adminEmail = params?.adminEmail ?? ''
  const targetType = params?.targetType ?? ''
  const status = params?.status ?? ''
  const size = params?.size ?? 1000

  const query =
    `adminUserId=${encodeURIComponent(adminUserId)}` +
    `&adminEmail=${encodeURIComponent(adminEmail)}` +
    `&targetType=${encodeURIComponent(targetType)}` +
    `&status=${encodeURIComponent(status)}` +
    `&size=${size}`

  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/moderation-actions/export?${query}`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(`Export failed: ${response.status}`)
  }

  return response.text()
}
