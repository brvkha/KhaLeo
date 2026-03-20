export type StudySessionCardDto = {
  cardId: string
  deckId: string
  frontText: string
  backText: string
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'MASTERED'
  nextReviewDate: string | null
  sourceTier: string
}

type NextCardsResponseDto = {
  items: StudySessionCardDto[]
  nextContinuationToken: string | null
  hasMore: boolean
}

type RateCardResponseDto = {
  cardId: string
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'MASTERED'
  nextReviewAt: string
  newInterval: number
  newEaseFactor: number
}

type RatingValue = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'

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

export async function getNextSessionCards(deckId: string): Promise<StudySessionCardDto[]> {
  const response = await requestJson<NextCardsResponseDto>(
    `/api/v1/study-session/decks/${deckId}/next-cards?size=20`,
  )
  return response.items
}

export async function rateSessionCard(cardId: string, rating: RatingValue, timeSpentMs: number): Promise<RateCardResponseDto> {
  return requestJson<RateCardResponseDto>(`/api/v1/study-session/cards/${cardId}/rate`, {
    method: 'POST',
    body: JSON.stringify({
      rating,
      timeSpentMs,
    }),
  })
}