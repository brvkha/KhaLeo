import { useState } from 'react'

type ConflictItem = {
  conflictId: string
  fieldPath: string
  localValue: string | null
  cloudValue: string | null
}

type Props = {
  conflicts: ConflictItem[]
  onResolve: (conflictId: string, choice: 'LOCAL' | 'CLOUD') => Promise<void>
  onClose: () => void
}

export function ReimportConflictDialog({ conflicts, onResolve, onClose }: Props) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-900/50 p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-2xl rounded border border-slate-300 bg-white p-4">
        <h2 className="text-lg font-semibold">Resolve Re-import Conflicts</h2>
        <p className="mt-1 text-sm text-slate-600">Choose local or cloud value for each conflict to finalize merge.</p>

        <ul className="mt-4 space-y-3">
          {conflicts.map((conflict) => (
            <li className="rounded border border-slate-200 p-3" key={conflict.conflictId}>
              <p className="text-sm font-medium">Field: {conflict.fieldPath}</p>
              <p className="mt-1 text-xs text-slate-600">Local: {conflict.localValue ?? '(empty)'}</p>
              <p className="text-xs text-slate-600">Cloud: {conflict.cloudValue ?? '(empty)'}</p>
              <div className="mt-2 flex gap-2">
                <button
                  className="rounded bg-slate-800 px-3 py-1 text-white"
                  disabled={saving}
                  onClick={() => {
                    setSaving(true)
                    setError('')
                    void onResolve(conflict.conflictId, 'LOCAL')
                      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to resolve conflict'))
                      .finally(() => setSaving(false))
                  }}
                >
                  Keep Local
                </button>
                <button
                  className="rounded bg-emerald-700 px-3 py-1 text-white"
                  disabled={saving}
                  onClick={() => {
                    setSaving(true)
                    setError('')
                    void onResolve(conflict.conflictId, 'CLOUD')
                      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to resolve conflict'))
                      .finally(() => setSaving(false))
                  }}
                >
                  Use Cloud
                </button>
              </div>
            </li>
          ))}
        </ul>

        {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}

        <div className="mt-4 text-right">
          <button className="rounded border border-slate-300 px-3 py-1" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
