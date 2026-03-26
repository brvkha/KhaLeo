import { expect, test } from '@playwright/test'

test('mobile study rating bar is fixed at bottom after reveal', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem(
      'khaleo-auth-session',
      JSON.stringify({
        currentUser: {
          id: 'user-1',
          email: 'learner@khaleo.app',
          role: 'USER',
          verified: true,
          banned: false,
        },
        accessToken: 'mock-token',
        refreshToken: 'mock-refresh',
      }),
    )
  })

  await page.route('**/api/v1/study-session/decks/*/next-cards**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            cardId: 'card-1',
            deckId: 'test-deck',
            term: 'Abstraction',
            answer: 'Generalized representation',
            imageUrl: 'https://images.unsplash.com/photo-1',
            partOfSpeech: 'noun',
            phonetic: 'ab-strak-shun',
            examples: ['Example one', 'Example two'],
            frontText: 'Abstraction',
            backText: 'Generalized representation',
            state: 'NEW',
            nextReviewDate: null,
            sourceTier: 'NEW',
          },
        ],
        nextContinuationToken: null,
        hasMore: false,
      }),
    })
  })

  await page.route('**/api/v1/study-session/cards/*/preview-ratings', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        again: { nextReviewAt: new Date().toISOString(), scheduledDays: 0, nextState: 'LEARNING' },
        hard: { nextReviewAt: new Date().toISOString(), scheduledDays: 1, nextState: 'REVIEW' },
        good: { nextReviewAt: new Date().toISOString(), scheduledDays: 2, nextState: 'REVIEW' },
        easy: { nextReviewAt: new Date().toISOString(), scheduledDays: 4, nextState: 'REVIEW' },
      }),
    })
  })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/flashcard/study/session/test-deck')

  await expect(page.getByText('Abstraction')).toBeVisible()

  await page.getByRole('button', { name: 'Flashcard front side' }).click()

  const ratingBar = page.locator('.study-rating-bar')
  await expect(ratingBar).toBeVisible()
  await expect(ratingBar).toHaveCSS('position', 'fixed')
})
