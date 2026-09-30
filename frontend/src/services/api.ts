export const API_BASE_URL = 'http://localhost:8080'

let refreshPromise: Promise<boolean> | null = null

function shouldAttemptRefresh(path: string) {
  return ![
    '/api/auth/login',
    '/api/auth/logout',
    '/api/auth/refresh',
    '/api/auth/forgot-password',
    '/api/auth/reset-password',
  ].includes(path)
}

async function refreshAuthentication() {
  if (!refreshPromise) {
    refreshPromise = fetch(API_BASE_URL + '/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
      .then((response) => response.ok)
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

function notifySessionExpired() {
  window.dispatchEvent(new Event('auth:session-expired'))
}

export async function apiFetch(
  path: string,
  options: RequestInit = {},
  retryOnUnauthorized = true,
): Promise<Response> {
  const headers = new Headers(options.headers)

  const response = await fetch(API_BASE_URL + path, {
    ...options,
    headers,
    credentials: 'include',
  })

  if (response.status !== 401 || !retryOnUnauthorized || !shouldAttemptRefresh(path)) {
    return response
  }

  const refreshed = await refreshAuthentication()

  if (!refreshed) {
    notifySessionExpired()
    return response
  }

  return apiFetch(path, options, false)
}
