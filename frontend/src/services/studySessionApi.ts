import { requestJson } from './apiClient'

export type StudySessionCardDto = {
  cardId: string
  deckId: string
  term?: string
  answer?: string
  imageUrl?: string | null
  partOfSpeech?: string | null
  phonetic?: string | null
  examples?: string[]
  frontText: string
  backText: string
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING' | 'MASTERED'
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
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING' | 'MASTERED'
  nextReviewAt: string
  scheduledDays: number
  newStability: number
  newDifficulty: number
}

export type { RateCardResponseDto }

type RatingValue = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'

export async function getNextSessionCards(deckId: string): Promise<StudySessionCardDto[]> {
  const response = await requestJson<NextCardsResponseDto>(
    `/api/v1/study-session/decks/${deckId}/next-cards?size=20`,
  )
  return response.items.map((item) => ({
    ...item,
    term: item.term ?? item.frontText,
    answer: item.answer ?? item.backText,
    examples: item.examples ?? [],
  }))
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

export type StudyRatingPreviewDto = {
  nextReviewAt: string
  scheduledDays: number
  nextState: 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING' | 'MASTERED'
}

export type StudyRatingPreviewsDto = {
  again: StudyRatingPreviewDto
  hard: StudyRatingPreviewDto
  good: StudyRatingPreviewDto
  easy: StudyRatingPreviewDto
}

export async function previewSessionCardRatings(cardId: string): Promise<StudyRatingPreviewsDto> {
  return requestJson<StudyRatingPreviewsDto>(`/api/v1/study-session/cards/${cardId}/preview-ratings`)
}