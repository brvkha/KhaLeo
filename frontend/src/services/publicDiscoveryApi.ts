export type PublicDeckSummaryDto = {
  id: string
  name: string
  ownerName: string
  description: string | null
  tags: string[]
  cardCount: number
  updatedAt: string
}

type PublicDeckPageResponse = {
  items: PublicDeckSummaryDto[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }

  return (await response.json()) as T
}

export async function listPublicDecks(query = ''): Promise<PublicDeckSummaryDto[]> {
  const encoded = encodeURIComponent(query)
  const page = await requestJson<PublicDeckPageResponse>(
    `/api/v1/public/decks?q=${encoded}&page=0&size=50`,
  )
  return page.items
}

export async function importPublicDeck(deckId: string): Promise<void> {
  await requestJson(`/api/v1/public/decks/${deckId}/import`, {
    method: 'POST',
  })
}
