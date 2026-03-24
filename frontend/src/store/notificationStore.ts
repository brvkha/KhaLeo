import { create } from 'zustand'

export type NotificationType = 'success' | 'error'

type NotificationItem = {
  id: number
  type: NotificationType
  message: string
}

type NotificationState = {
  items: NotificationItem[]
  pushSuccess: (message: string) => void
  pushError: (message: string) => void
  dismiss: (id: number) => void
}

let counter = 0

export const useNotificationStore = create<NotificationState>((set) => ({
  items: [],
  pushSuccess: (message) => {
    const id = ++counter
    set((state) => ({ items: [...state.items, { id, type: 'success', message }] }))
    setTimeout(() => {
      set((state) => ({ items: state.items.filter((item) => item.id !== id) }))
    }, 3500)
  },
  pushError: (message) => {
    const id = ++counter
    set((state) => ({ items: [...state.items, { id, type: 'error', message }] }))
    setTimeout(() => {
      set((state) => ({ items: state.items.filter((item) => item.id !== id) }))
    }, 6000)
  },
  dismiss: (id) => set((state) => ({ items: state.items.filter((item) => item.id !== id) })),
}))
