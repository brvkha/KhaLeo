import { expect, test } from '@playwright/test'

test('guest browse, login import, and study access flow', async ({ page }) => {
  await page.route('**/api/v1/public/decks**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            id: 'deck-public-1',
            name: 'Public Biology',
            ownerName: 'owner@khaleo.app',
            description: 'Cells and genes',
            tags: ['bio'],
            cardCount: 20,
            updatedAt: new Date().toISOString(),
          },
        ],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      }),
    })
  })

  await page.route('**/api/v1/public/decks/*/import', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        importLinkId: 'link-1',
        sourceDeckId: 'deck-public-1',
        targetPrivateDeckId: 'deck-private-1',
        status: 'SUCCESS',
      }),
    })
  })

  await page.route('**/api/v1/private/decks**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'deck-private-1',
            name: 'Imported Biology',
            description: 'Private imported copy',
            isPublic: false,
          },
        ],
      }),
    })
  })

  await page.route('**/api/v1/study-session/decks/*/next-cards**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            cardId: 'card-1',
            deckId: 'deck-private-1',
            frontText: 'What is DNA?',
            backText: 'Genetic material',
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

  await page.route('**/api/v1/study-session/cards/*/rate', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        cardId: 'card-1',
        state: 'LEARNING',
        nextReviewAt: new Date().toISOString(),
        newInterval: 0,
        newEaseFactor: 2.5,
      }),
    })
  })

  await page.goto('/decks')
  await expect(page.getByRole('heading', { name: 'Public Deck Discovery' })).toBeVisible()
  await expect(page.getByText('Public Biology')).toBeVisible()

  await page.getByRole('button', { name: 'Import to Study' }).click()
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible()

  await page.getByLabel('Email').fill('learner@khaleo.app')
  await page.getByLabel('Password').fill('123456')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await page.goto('/decks')
  await page.getByRole('button', { name: 'Import to Study' }).click()

  await page.goto('/study')
  await expect(page.getByText('Imported Biology')).toBeVisible()
  await page.getByRole('link', { name: 'Start session' }).click()

  await expect(page.getByRole('heading', { name: 'Study Session' })).toBeVisible()
  await expect(page.getByText('What is DNA?')).toBeVisible()
  await page.getByRole('button', { name: 'Reveal answer' }).click()
  await expect(page.getByText('Genetic material')).toBeVisible()
  await page.getByRole('button', { name: 'Good' }).click()
  await expect(page.getByText('Session complete. No due cards right now.')).toBeVisible()
})
