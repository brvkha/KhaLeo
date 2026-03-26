export type Role = 'USER' | 'ADMIN'

export type User = {
  id: string
  email: string
  role: Role
  verified: boolean
  banned: boolean
}

export type Deck = {
  id: string
  name: string
  description: string
}

export type Card = {
  id: string
  deckId: string
  term: string
  answer: string
  imageUrl?: string | null
  partOfSpeech?: string | null
  phonetic?: string | null
  examples: string[]
  version: number
  front: string
  back: string
  tags: string[]
  mediaUrl?: string
  due: boolean
}

export type StudyRating = 'Again' | 'Hard' | 'Good' | 'Easy'
