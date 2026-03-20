import { requestJson } from './apiClient'

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

export async function listPublicDecks(query = ''): Promise<PublicDeckSummaryDto[]> {
  const encoded = encodeURIComponent(query)
  const page = await requestJson<PublicDeckPageResponse>(
    `/api/v1/public/decks?q=${encoded}&page=0&size=50`,
    { useAuth: false },
  )
  return page.items
}

export async function importPublicDeck(deckId: string): Promise<void> {
  await requestJson(`/api/v1/public/decks/${deckId}/import`, {
    method: 'POST',
  })
}
