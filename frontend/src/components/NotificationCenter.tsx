import { useNotificationStore } from '../store/notificationStore'

export function NotificationCenter() {
  const items = useNotificationStore((state) => state.items)
  const dismiss = useNotificationStore((state) => state.dismiss)

  return (
    <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-full max-w-sm flex-col gap-2">
      {items.map((item) => (
        <div
          key={item.id}
          className={`pointer-events-auto rounded border px-4 py-3 text-sm shadow ${
            item.type === 'success'
              ? 'border-emerald-300 bg-emerald-50 text-emerald-800'
              : 'border-rose-300 bg-rose-50 text-rose-800'
          }`}
        >
          <div className="flex items-start justify-between gap-3">
            <p>{item.message}</p>
            <button
              className="rounded px-2 py-0.5 text-xs text-slate-500 hover:bg-white"
              onClick={() => dismiss(item.id)}
              type="button"
            >
              Close
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
