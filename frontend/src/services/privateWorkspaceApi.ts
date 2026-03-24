import { requestJson } from './apiClient'

export type PrivateDeckDto = {
  id: string
  name: string
  description: string | null
  isPublic?: boolean
}

export type PrivateCardDto = {
  id: string
  deckId: string
  frontText: string
  backText: string
}

export type DeckStatsDto = {
  deckId: string
  learning: number
  review: number
  new_cards: number
}

type PagedResponse<T> = {
  content: T[]
}

export async function listPrivateDecks(query = ''): Promise<PrivateDeckDto[]> {
  const encoded = encodeURIComponent(query)
  const page = await requestJson<PagedResponse<PrivateDeckDto>>(
    `/api/v1/private/decks?q=${encoded}&page=0&size=50`,
  )

  return page.content.filter((deck) => deck.isPublic !== true)
}

export async function createPrivateDeck(payload: {
  name: string
  description: string
}): Promise<void> {
  await requestJson(`/api/v1/private/decks`, {
    method: 'POST',
    body: JSON.stringify({
      name: payload.name,
      description: payload.description,
      isPublic: false,
    }),
  })
}

export async function updatePrivateDeck(payload: {
  deckId: string
  name: string
  description: string
}): Promise<void> {
  await requestJson(`/api/v1/private/decks/${payload.deckId}`, {
    method: 'PUT',
    body: JSON.stringify({
      name: payload.name,
      description: payload.description,
      isPublic: false,
    }),
  })
}

export async function deletePrivateDeck(deckId: string): Promise<void> {
  await requestJson(`/api/v1/private/decks/${deckId}`, {
    method: 'DELETE',
  })
}

export async function searchPrivateDeckCards(deckId: string, query: string): Promise<PrivateCardDto[]> {
  const encoded = encodeURIComponent(query)
  const page = await requestJson<PagedResponse<PrivateCardDto>>(
    `/api/v1/private/decks/${deckId}/cards/search?frontText=${encoded}&page=0&size=50`,
  )
  return page.content
}

export async function createPrivateCard(payload: {
  deckId: string
  frontText: string
  backText: string
}): Promise<void> {
  await requestJson(`/api/v1/decks/${payload.deckId}/cards`, {
    method: 'POST',
    body: JSON.stringify({
      frontText: payload.frontText,
      backText: payload.backText,
    }),
  })
}

export async function deletePrivateCard(cardId: string): Promise<void> {
  await requestJson(`/api/v1/cards/${cardId}`, {
    method: 'DELETE',
  })
}

export async function updatePrivateCard(payload: {
  cardId: string
  frontText: string
  backText: string
}): Promise<void> {
  await requestJson(`/api/v1/cards/${payload.cardId}`, {
    method: 'PUT',
    body: JSON.stringify({
      frontText: payload.frontText,
      backText: payload.backText,
    }),
  })
}

export async function getDeckStats(deckId: string): Promise<DeckStatsDto> {
  return await requestJson<DeckStatsDto>(`/api/v1/private/decks/${deckId}/stats`)
}
