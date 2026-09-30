import { defineStore } from 'pinia'
import { apiFetch } from '@/services/api'
import type { AuthUser, CurrentUserResponse, LoginResponse } from '@/types/auth'

const AUTH_USER_KEY = 'auth_user'

function restoreUser(): AuthUser | null {
  const storedUser = localStorage.getItem(AUTH_USER_KEY)

  if (!storedUser) {
    return null
  }

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

function persistUser(user: AuthUser) {
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function mapCurrentUser(response: CurrentUserResponse): AuthUser {
  return {
    userId: response.id,
    name: response.name,
    username: response.username,
    email: response.email,
    userType: response.userType,
    permissions: response.permissions,
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: restoreUser(),
    sessionChecked: false,
    loading: false,
    error: '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.user),
    isSuperAdmin: (state) => state.user?.userType === 'SUPER_ADMIN',
  },
  actions: {
    async login(identifier: string, password: string) {
      this.loading = true
      this.error = ''

      try {
        const response = await apiFetch('/api/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ identifier, password }),
        })

        if (!response.ok) {
          const message = 'Invalid username/email or password'
          this.error = message
          throw new Error(message)
        }

        const loginResponse = (await response.json()) as LoginResponse
        const user: AuthUser = {
          userId: loginResponse.userId,
          name: loginResponse.name,
          username: loginResponse.username,
          email: loginResponse.email,
          userType: loginResponse.userType,
          permissions: loginResponse.permissions,
        }

        this.user = user
        this.sessionChecked = true
        persistUser(user)
      } finally {
        this.loading = false
      }
    },
    async restoreSession(force = false) {
      if (this.sessionChecked && !force) {
        return this.isAuthenticated
      }

      try {
        const response = await apiFetch('/api/users/me')

        if (!response.ok) {
          this.clearLocalSession()
          return false
        }

        const currentUser = (await response.json()) as CurrentUserResponse
        const user = mapCurrentUser(currentUser)
        this.user = user
        persistUser(user)
        return true
      } catch {
        this.clearLocalSession()
        return false
      } finally {
        this.sessionChecked = true
      }
    },
    async refreshSession() {
      try {
        const response = await apiFetch('/api/auth/refresh', {
          method: 'POST',
        })

        if (!response.ok) {
          this.clearLocalSession()
          return false
        }

        return this.restoreSession(true)
      } catch {
        this.clearLocalSession()
        return false
      }
    },
    async logout() {
      try {
        await apiFetch('/api/auth/logout', {
          method: 'POST',
        })
      } finally {
        this.clearLocalSession()
      }
    },
    clearLocalSession() {
      this.user = null
      this.error = ''
      this.sessionChecked = true
      localStorage.removeItem(AUTH_USER_KEY)
    },
  },
})
