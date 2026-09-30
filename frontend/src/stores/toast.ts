import { defineStore } from 'pinia'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: number
  type: ToastType
  message: string
}

const durations: Record<ToastType, number> = {
  success: 4000,
  info: 4000,
  warning: 5000,
  error: 6000,
}

const maxToasts = 4
const duplicateWindowMs = 1000
const timers = new Map<number, ReturnType<typeof window.setTimeout>>()
const recentToasts = new Map<string, number>()

let nextToastId = 1

export const useToastStore = defineStore('toast', {
  state: () => ({
    toasts: [] as Toast[],
  }),
  actions: {
    show(type: ToastType, message: string) {
      const trimmedMessage = message.trim()

      if (!trimmedMessage) {
        return null
      }

      const now = Date.now()
      const duplicateKey = `${type}:${trimmedMessage}`
      const lastShownAt = recentToasts.get(duplicateKey)
      const activeDuplicate = this.toasts.find(
        (toast) => toast.type === type && toast.message === trimmedMessage,
      )

      if (lastShownAt && now - lastShownAt < duplicateWindowMs && activeDuplicate) {
        return activeDuplicate.id
      }

      recentToasts.set(duplicateKey, now)

      const toast: Toast = {
        id: nextToastId,
        type,
        message: trimmedMessage,
      }

      nextToastId += 1

      if (this.toasts.length >= maxToasts) {
        const oldestToast = this.toasts[0]

        if (oldestToast) {
          this.dismiss(oldestToast.id)
        }
      }

      this.toasts.push(toast)

      const timer = window.setTimeout(() => {
        this.dismiss(toast.id)
      }, durations[type])

      timers.set(toast.id, timer)

      return toast.id
    },
    success(message: string) {
      return this.show('success', message)
    },
    error(message: string) {
      return this.show('error', message)
    },
    warning(message: string) {
      return this.show('warning', message)
    },
    info(message: string) {
      return this.show('info', message)
    },
    dismiss(id: number) {
      const timer = timers.get(id)

      if (timer) {
        window.clearTimeout(timer)
        timers.delete(id)
      }

      this.toasts = this.toasts.filter((toast) => toast.id !== id)
    },
  },
})
