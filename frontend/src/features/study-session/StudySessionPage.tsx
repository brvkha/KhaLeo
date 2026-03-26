import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  getNextSessionCards,
  previewSessionCardRatings,
  rateSessionCard,
  type StudyRatingPreviewsDto,
  type StudySessionCardDto,
} from '../../services/studySessionApi'
import { RichCardContent } from './RichCardContent'

type UiRating = 'Again' | 'Hard' | 'Good' | 'Easy'

const uiRatings: UiRating[] = ['Again', 'Hard', 'Good', 'Easy']

function toApiRating(rating: UiRating): 'AGAIN' | 'HARD' | 'GOOD' | 'EASY' {
  return rating.toUpperCase() as 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'
}

function formatPhrase(days: number) {
  if (days === 0) return 'Today'
  if (days < 1) {
    const mins = Math.round(days * 24 * 60)
    if (mins >= 60) {
      const hours = Math.round(mins / 60)
      return `${hours}h`
    }
    return `${Math.max(1, mins)}m`
  }
  if (days >= 30) {
    const months = Math.round(days / 30)
    return `${months}mon`
  }
  return `${Math.round(days)}d`
}

function calculateIntervalFromTimestamp(isoString: string): number {
  const reviewDate = new Date(isoString)
  const now = new Date()
  const diffMs = reviewDate.getTime() - now.getTime()
  const diffDays = diffMs / (1000 * 60 * 60 * 24)
  return Math.max(0, diffDays)
}

export function StudySessionPage() {
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const deckName = (location.state as { deckName?: string } | null)?.deckName

  const [cards, setCards] = useState<StudySessionCardDto[]>([])
  const [loading, setLoading] = useState(false)
  const [rating, setRating] = useState(false)
  const [revealed, setRevealed] = useState(false)
  const [error, setError] = useState('')
  const [shownAt, setShownAt] = useState<number>(Date.now())
  const [ratingPreview, setRatingPreview] = useState<StudyRatingPreviewsDto | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)

  const current = useMemo(() => cards[0], [cards])

  const refresh = useCallback(async () => {
    if (!deckId) {
      setError('Missing deck id for study session')
      return
    }
    setLoading(true)
    setError('')
    try {
      const nextCards = await getNextSessionCards(deckId)
      setCards(nextCards)
      setRevealed(false)
      setShownAt(Date.now())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load session cards')
    } finally {
      setLoading(false)
    }
  }, [deckId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    if (loading || cards.length > 0) {
      return
    }

    const timer = window.setInterval(() => {
      void refresh()
    }, 15_000)

    return () => window.clearInterval(timer)
  }, [cards.length, loading, refresh])

  useEffect(() => {
    if (!current?.cardId) {
      setRatingPreview(null)
      return
    }

    setPreviewLoading(true)
    void previewSessionCardRatings(current.cardId)
      .then((response) => setRatingPreview(response))
      .catch(() => setRatingPreview(null))
      .finally(() => setPreviewLoading(false))
  }, [current?.cardId])

  const getPreviewLabel = (value: UiRating): string => {
    if (!ratingPreview) {
      return previewLoading ? '...' : '-'
    }

    const nextReviewAt =
      value === 'Again'
        ? ratingPreview.again.nextReviewAt
        : value === 'Hard'
          ? ratingPreview.hard.nextReviewAt
          : value === 'Good'
            ? ratingPreview.good.nextReviewAt
            : ratingPreview.easy.nextReviewAt

    return formatPhrase(calculateIntervalFromTimestamp(nextReviewAt))
  }

  const onRate = async (value: UiRating) => {
    if (!current || rating) {
      return
    }
    setRating(true)
    setError('')
    try {
      const elapsed = Math.max(0, Date.now() - shownAt)
      await rateSessionCard(current.cardId, toApiRating(value), elapsed)
      setCards((prev) => {
        const next = prev.slice(1)
        if (next.length === 0) {
          void refresh()
        }
        return next
      })
      setRatingPreview(null)
      setRevealed(false)
      setShownAt(Date.now())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit rating')
    } finally {
      setRating(false)
    }
  }

  return (
    <section className="h-[calc(100vh-11rem)] min-h-[500px] overflow-hidden">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Study Session</h1>
        <button className="rounded border border-slate-300 px-3 py-2" onClick={() => navigate('/flashcard/study')}>
          Back to study workspace
        </button>
      </div>

      {deckName ? <p className="mt-2 text-sm text-slate-600">Name: {deckName}</p> : null}

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}
      {loading ? <p className="mt-3 text-sm text-slate-500">Loading session...</p> : null}

      {!loading && !current ? (
        <p className="mt-4 rounded border border-emerald-200 bg-emerald-50 p-4">
          Session complete. No due cards right now.
        </p>
      ) : null}

      {current ? (
        <article className="mt-3 h-[calc(100%-4rem)] rounded border border-slate-200 bg-white p-4">

          <div className="mt-3">
            <button
              aria-label={revealed ? 'Flashcard back side' : 'Flashcard front side'}
              className="w-full rounded-2xl text-left"
              onClick={() => setRevealed(true)}
              type="button"
            >
              <div className="rounded-3xl border border-slate-300 bg-gradient-to-br from-white to-slate-100 p-6 shadow-sm">
                <RichCardContent card={current} revealed={revealed} />
              </div>
            </button>
          </div>

          {revealed ? (
            <div className="study-rating-bar mt-5 flex flex-wrap gap-3">
              {uiRatings.map((value) => (
                <div key={value} className="flex flex-col items-center">
                  <button
                    className="min-w-20 rounded border border-slate-300 px-3 py-2 text-center disabled:opacity-50"
                    onClick={() => void onRate(value)}
                    disabled={rating}
                  >
                    {value}
                  </button>
                  <p className="mt-1 text-xs font-medium text-slate-500">
                    {getPreviewLabel(value)}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-500">Tap the card to reveal answer and rating buttons.</p>
          )}
        </article>
      ) : null}
    </section>
  )
}