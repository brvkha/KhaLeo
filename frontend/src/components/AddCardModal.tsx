import { useState } from 'react'

interface AddCardModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (front: string, back: string) => void
  deckName: string
}

export function AddCardModal({ isOpen, onClose, onSubmit, deckName }: AddCardModalProps) {
  const [front, setFront] = useState('')
  const [back, setBack] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!front.trim() || !back.trim()) return

    setIsLoading(true)
    try {
      await Promise.resolve(onSubmit(front.trim(), back.trim()))
      setFront('')
      setBack('')
      onClose()
    } finally {
      setIsLoading(false)
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-3xl rounded-lg bg-white shadow-lg">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-xl font-semibold text-slate-900">Add Card to {deckName}</h2>
          <p className="mt-1 text-sm text-slate-600">Create a new flashcard with front and back content</p>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              Front (one concept)
            </label>
            <input
              type="text"
              className="w-full rounded border border-slate-300 px-4 py-3 text-sm focus:border-blue-500 focus:outline-none"
              placeholder="e.g., What is React?"
              value={front}
              onChange={(e) => setFront(e.target.value)}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              Back (explanation, examples, multiline supported)
            </label>
            <textarea
              className="w-full rounded border border-slate-300 px-4 py-3 text-sm focus:border-blue-500 focus:outline-none"
              placeholder="Enter detailed explanation here. You can use multiple lines to format your answer. Add examples or important notes."
              rows={10}
              value={back}
              onChange={(e) => setBack(e.target.value)}
            />
            <p className="mt-1 text-xs text-slate-500">
              {back.length} characters | Supports multiline content
            </p>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-200">
            <button
              type="button"
              onClick={onClose}
              className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!front.trim() || !back.trim() || isLoading}
              className="rounded bg-emerald-600 px-6 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Adding...' : 'Add Card'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
