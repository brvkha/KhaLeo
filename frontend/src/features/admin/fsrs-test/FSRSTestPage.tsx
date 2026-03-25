import { useEffect, useState } from 'react'
import { testFSRSAlgorithm } from '../../../services/adminApi'
import type { FSRSTestRequest, FSRSTestResponse } from '../../../services/adminApi'
import { getAlgorithmWeights } from '../../../services/studySettingsApi'

const DEFAULT_FSRS_WEIGHTS = [
  1.2682, 1.2682, 0.7310, 1.7540,
  7.9650, 0.6470, 2.5935, 0.0010,
  1.2670, 0.1510, 1.5040, 2.0287,
  0.0767, 0.4215, 2.5117, 0.2713,
  1.3240, 0.4372, 0.0468,
]
const INIT_STABILITY_WEIGHT_INDEX = 0
const INIT_DIFFICULTY_WEIGHT_INDEX = 4

function createInitialStateFromWeights(weights: number[]): FSRSTestRequest {
  const fallbackStability = DEFAULT_FSRS_WEIGHTS[INIT_STABILITY_WEIGHT_INDEX]
  const fallbackDifficulty = DEFAULT_FSRS_WEIGHTS[INIT_DIFFICULTY_WEIGHT_INDEX]
  const stability = Number.isFinite(weights[INIT_STABILITY_WEIGHT_INDEX])
    ? weights[INIT_STABILITY_WEIGHT_INDEX]
    : fallbackStability
  const difficulty = Number.isFinite(weights[INIT_DIFFICULTY_WEIGHT_INDEX])
    ? weights[INIT_DIFFICULTY_WEIGHT_INDEX]
    : fallbackDifficulty

  return {
    stability,
    difficulty,
    reps: 0,
    lapses: 0,
    elapsedDays: 0,
    state: 'NEW',
    learningStepGoodCount: 0,
  }
}

type StudyRecord = {
  step: number
  elapsedDays: number
  action: 'Good' | 'Hard' | 'Again' | 'Easy'
  reps: number
  lapses: number
  stability: number
  difficulty: number
  state: string  // NEW, LEARNING, REVIEW, RELEARNING
  nextState: string  // What state will be after this action
  nextIntervals: {
    again: string // ISO timestamp
    hard: string
    good: string
    easy: string
  }
  nextReviewAt: string // Track last selected nextReviewAt for elapsed calculation
}

export function FSRSTestPage() {
  const [currentState, setCurrentState] = useState<FSRSTestRequest>(
    createInitialStateFromWeights(DEFAULT_FSRS_WEIGHTS),
  )
  const [initialState, setInitialState] = useState<FSRSTestRequest>(
    createInitialStateFromWeights(DEFAULT_FSRS_WEIGHTS),
  )
  const [predictions, setPredictions] = useState<FSRSTestResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [history, setHistory] = useState<StudyRecord[]>([])
  const [simulationStarted, setSimulationStarted] = useState(false)

  const loadInitialStateFromSettings = async () => {
    const response = await getAlgorithmWeights()
    return createInitialStateFromWeights(response.weights)
  }

  useEffect(() => {
    void loadInitialStateFromSettings()
      .then((state) => {
        setInitialState(state)
        if (!simulationStarted) {
          setCurrentState(state)
        }
      })
      .catch(() => {
        // Keep in-memory defaults if settings API is temporarily unavailable.
      })
  }, [simulationStarted])

  // Start simulation from new card
  const startSimulation = async () => {
    setSimulationStarted(true)
    setHistory([])
    const newState = await loadInitialStateFromSettings().catch(() => initialState)
    setInitialState(newState)
    setCurrentState(newState)
    await loadPredictions(newState)
  }

  const loadPredictions = async (state: FSRSTestRequest) => {
    setLoading(true)
    try {
      const response = await testFSRSAlgorithm(state)
      setPredictions(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch predictions')
      console.error('Error loading predictions', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRating = async (rating: 'AGAIN' | 'HARD' | 'GOOD' | 'EASY') => {
    if (!predictions) return

    const ratingResult =
      rating === 'AGAIN'
        ? predictions.againResult
        : rating === 'HARD'
          ? predictions.hardResult
          : rating === 'GOOD'
            ? predictions.goodResult
            : predictions.easyResult

    // Get the next review timestamp for elapsed calculation on NEXT click
    const selectedNextReviewAt = ratingResult.nextReviewAt
    const appliedElapsedDays = currentState.elapsedDays
    const nextElapsedDays = Math.max(0, calculateIntervalFromTimestamp(selectedNextReviewAt))

    // Record elapsed used for this review step, not the future elapsed for next step.
    const newRecord: StudyRecord = {
      step: history.length + 1,
      elapsedDays: appliedElapsedDays,
      action: rating as 'Good' | 'Hard' | 'Again' | 'Easy',
      reps: ratingResult.reps,
      lapses: ratingResult.lapses,
      stability: ratingResult.stability,
      difficulty: ratingResult.difficulty,
      state: currentState.state || 'NEW',  // Record current state before action
      nextState: ratingResult.nextState,  // Record state after action
      nextIntervals: {
        again: predictions.againResult.nextReviewAt,
        hard: predictions.hardResult.nextReviewAt,
        good: predictions.goodResult.nextReviewAt,
        easy: predictions.easyResult.nextReviewAt,
      },
      nextReviewAt: selectedNextReviewAt,
    }

    setHistory([...history, newRecord])

    // Update state with new values and evolved elapsed days
    // Use nextState from the rating result to track proper state progression
    const newState: FSRSTestRequest = {
      stability: ratingResult.stability,
      difficulty: ratingResult.difficulty,
      reps: ratingResult.reps,
      lapses: ratingResult.lapses,
      elapsedDays: nextElapsedDays,
      state: ratingResult.nextState,  // Use nextState for future requests
      learningStepGoodCount: ratingResult.learningStepGoodCount,
    }

    setCurrentState(newState)
    await loadPredictions(newState)
  }

  const formatPhrase = (days: number) => {
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

  const getCardStatus = () => {
    if (!simulationStarted) return 'Not started'
    if (currentState.reps === 0 && currentState.lapses === 0) return 'New'
    if (currentState.lapses > 0 && history.length > 0) {
      const lastAction = history[history.length - 1]?.action
      return lastAction === 'Again' ? 'Relearning' : 'Learning'
    }
    return 'Learning'
  }

  // Convert ISO timestamp to actual interval duration in days
  const calculateIntervalFromTimestamp = (isoString: string): number => {
    const reviewDate = new Date(isoString)
    const now = new Date()
    const diffMs = reviewDate.getTime() - now.getTime()
    const diffDays = diffMs / (1000 * 60 * 60 * 24)
    return Math.max(0, diffDays)
  }

  const resetSimulation = () => {
    setSimulationStarted(false)
    setHistory([])
    setPredictions(null)
    setCurrentState(initialState)
  }

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">FSRS Algorithm Simulation</h1>
        <p className="mt-2 text-sm text-slate-600">
          Watch how FSRS calculates review intervals based on your ratings. Click rating buttons and track the evolution of intervals over time.
        </p>
      </div>

      {!simulationStarted ? (
        <div className="rounded-lg border-2 border-dashed border-blue-300 bg-blue-50 p-8 text-center">
          <p className="text-slate-700">Ready to test FSRS algorithm?</p>
          <button
            onClick={startSimulation}
            className="mt-4 rounded bg-blue-600 px-6 py-2 text-white hover:bg-blue-700"
          >
            Start Simulation
          </button>
        </div>
      ) : (
        <>
          {/* Current Card State */}
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between mb-4">
              <div>
                <p className="text-sm text-slate-600">Current State</p>
                <p className="text-lg font-semibold text-slate-900">{getCardStatus()}</p>
              </div>
              <div className="text-right text-sm text-slate-600">
                <p>Reps: <span className="font-semibold">{currentState.reps}</span></p>
                <p>Lapses: <span className="font-semibold">{currentState.lapses}</span></p>
                <p>Stability: <span className="font-semibold">{currentState.stability.toFixed(2)}</span></p>
                <p>Difficulty: <span className="font-semibold">{currentState.difficulty.toFixed(2)}</span></p>
              </div>
            </div>

            {error && <p className="text-sm text-red-600 mb-3">{error}</p>}

            {/* Rating Buttons */}
            {predictions ? (
              <div className="grid grid-cols-4 gap-2">
                {[
                  { key: 'AGAIN' as const, label: 'Again', color: 'bg-red-500 hover:bg-red-600', timestamp: predictions.againResult.nextReviewAt },
                  { key: 'HARD' as const, label: 'Hard', color: 'bg-orange-500 hover:bg-orange-600', timestamp: predictions.hardResult.nextReviewAt },
                  { key: 'GOOD' as const, label: 'Good', color: 'bg-green-500 hover:bg-green-600', timestamp: predictions.goodResult.nextReviewAt },
                  { key: 'EASY' as const, label: 'Easy', color: 'bg-blue-500 hover:bg-blue-600', timestamp: predictions.easyResult.nextReviewAt },
                ].map((btn) => (
                  <button
                    key={btn.key}
                    onClick={() => handleRating(btn.key)}
                    disabled={loading}
                    className={`${btn.color} flex flex-col items-center justify-center rounded py-3 px-2 text-white font-semibold transition-colors disabled:opacity-50`}
                  >
                    <span className="text-sm">{btn.label}</span>
                    <span className="text-lg mt-1">{formatPhrase(calculateIntervalFromTimestamp(btn.timestamp))}</span>
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-center text-slate-500">Loading predictions...</p>
            )}

            <button
              onClick={resetSimulation}
              className="mt-4 text-sm text-slate-600 hover:text-slate-900 underline"
            >
              Reset Simulation
            </button>
          </div>

          {/* History Table */}
          {history.length > 0 && (
            <div className="rounded border border-slate-200 bg-white overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-100 border-b border-slate-200">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold">Lần</th>
                      <th className="px-3 py-2 text-left font-semibold">Elapsed</th>
                      <th className="px-3 py-2 text-left font-semibold">State</th>
                      <th className="px-3 py-2 text-left font-semibold">Action</th>
                      <th className="px-3 py-2 text-right font-semibold">Reps</th>
                      <th className="px-3 py-2 text-right font-semibold">Lapses</th>
                      <th className="px-3 py-2 text-right font-semibold">Stability</th>
                      <th className="px-3 py-2 text-right font-semibold">Difficulty</th>
                      <th className="px-3 py-2 text-center font-semibold">Again</th>
                      <th className="px-3 py-2 text-center font-semibold">Hard</th>
                      <th className="px-3 py-2 text-center font-semibold">Good</th>
                      <th className="px-3 py-2 text-center font-semibold">Easy</th>
                      <th className="px-3 py-2 text-left font-semibold">→ State</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((record, idx) => (
                      <tr key={idx} className={idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                        <td className="px-3 py-2 text-slate-900 font-medium">{record.step}</td>
                        <td className="px-3 py-2 text-slate-600">{record.elapsedDays > 0 ? `+${record.elapsedDays.toFixed(2)}d` : '0d'}</td>
                        <td className="px-3 py-2 text-slate-600 text-xs bg-blue-50 rounded px-2 py-1">{record.state}</td>
                        <td className={`px-3 py-2 font-semibold ${
                          record.action === 'Good' ? 'text-green-600' :
                          record.action === 'Hard' ? 'text-orange-600' :
                          record.action === 'Again' ? 'text-red-600' :
                          record.action === 'Easy' ? 'text-blue-600' : ''
                        }`}>
                          {record.action}
                        </td>
                        <td className="px-3 py-2 text-right text-slate-600">{record.reps}</td>
                        <td className="px-3 py-2 text-right text-slate-600">{record.lapses}</td>
                        <td className="px-3 py-2 text-right text-slate-600">{record.stability.toFixed(2)}</td>
                        <td className="px-3 py-2 text-right text-slate-600">{record.difficulty.toFixed(2)}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatPhrase(calculateIntervalFromTimestamp(record.nextIntervals.again))}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatPhrase(calculateIntervalFromTimestamp(record.nextIntervals.hard))}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatPhrase(calculateIntervalFromTimestamp(record.nextIntervals.good))}</td>
                        <td className="px-3 py-2 text-center text-slate-600">{formatPhrase(calculateIntervalFromTimestamp(record.nextIntervals.easy))}</td>
                        <td className="px-3 py-2 text-left text-xs bg-green-50 rounded px-2 py-1 font-semibold text-green-700">{record.nextState}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  )
}
