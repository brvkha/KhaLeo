import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { StudySessionPage } from '../../features/study-session/StudySessionPage'

vi.mock('../../services/studySessionApi', () => ({
  getNextSessionCards: vi.fn(),
  previewSessionCardRatings: vi.fn(async () => ({
    again: { nextReviewAt: new Date().toISOString(), scheduledDays: 0, nextState: 'LEARNING' },
    hard: { nextReviewAt: new Date().toISOString(), scheduledDays: 1, nextState: 'REVIEW' },
    good: { nextReviewAt: new Date().toISOString(), scheduledDays: 2, nextState: 'REVIEW' },
    easy: { nextReviewAt: new Date().toISOString(), scheduledDays: 4, nextState: 'REVIEW' },
  })),
  rateSessionCard: vi.fn(async () => ({
    cardId: 'c1',
    state: 'LEARNING',
    nextReviewAt: new Date().toISOString(),
    scheduledDays: 1,
    newStability: 2.4,
    newDifficulty: 5.87,
  })),
}))

import { getNextSessionCards, rateSessionCard } from '../../services/studySessionApi'

describe('StudySessionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('reveals answer and submits rating in two-sided flow', async () => {
    vi.mocked(getNextSessionCards)
      .mockResolvedValueOnce([
        {
          cardId: 'c1',
          deckId: 'd1',
          frontText: 'Question front',
          backText: 'Answer back',
          state: 'NEW',
          nextReviewDate: null,
          sourceTier: 'NEW',
        },
      ])
      .mockResolvedValueOnce([])

    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/flashcard/study/session/d1']}>
        <Routes>
          <Route path="/flashcard/study/session/:deckId" element={<StudySessionPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('Question front')).toBeInTheDocument())

    expect(screen.queryByRole('button', { name: 'Good' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Flashcard front side' }))
    expect(screen.getByText('Answer back')).toBeInTheDocument()

    const goodButton = screen.getByRole('button', { name: 'Good' })
    expect(goodButton).not.toBeDisabled()

    await user.click(goodButton)

    await waitFor(() => {
      expect(rateSessionCard).toHaveBeenCalledOnce()
      expect(rateSessionCard).toHaveBeenCalledWith('c1', 'GOOD', expect.any(Number))
    })

    await waitFor(() => {
      expect(screen.getByText('Session complete. No due cards right now.')).toBeInTheDocument()
    })
  })
})
