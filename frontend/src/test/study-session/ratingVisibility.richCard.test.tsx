import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { StudySessionPage } from '../../features/study-session/StudySessionPage'

vi.mock('../../services/studySessionApi', () => ({
  getNextSessionCards: vi.fn(async () => [
    {
      cardId: 'c1',
      deckId: 'd1',
      term: 'Abstraction',
      answer: 'Generalized representation',
      imageUrl: 'https://images.unsplash.com/photo-1',
      partOfSpeech: 'noun',
      phonetic: 'ab-strak-shun',
      examples: ['Example one'],
      frontText: 'Abstraction',
      backText: 'Generalized representation',
      state: 'NEW',
      nextReviewDate: null,
      sourceTier: 'NEW',
    },
  ]),
  previewSessionCardRatings: vi.fn(async () => ({
    again: { nextReviewAt: new Date().toISOString(), scheduledDays: 0, nextState: 'LEARNING' },
    hard: { nextReviewAt: new Date().toISOString(), scheduledDays: 1, nextState: 'REVIEW' },
    good: { nextReviewAt: new Date().toISOString(), scheduledDays: 2, nextState: 'REVIEW' },
    easy: { nextReviewAt: new Date().toISOString(), scheduledDays: 4, nextState: 'REVIEW' },
  })),
  rateSessionCard: vi.fn(),
}))

describe('rating visibility with rich cards', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows rating buttons only after back-face reveal', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/flashcard/study/session/d1']}>
        <Routes>
          <Route path="/flashcard/study/session/:deckId" element={<StudySessionPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('Abstraction')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Good' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Flashcard front side' }))
    expect(screen.getByRole('button', { name: 'Good' })).toBeInTheDocument()
  })
})
