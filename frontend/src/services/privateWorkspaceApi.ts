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

type PagedResponse<T> = {
  content: T[]
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
