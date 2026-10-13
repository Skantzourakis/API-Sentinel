const TOKEN_KEY = 'api-sentinel.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export class ApiError extends Error {
  readonly status: number
  readonly details: string[]

  constructor(status: number, message: string, details: string[]) {
    super(message)
    this.status = status
    this.details = details
  }
}

// Backend errors share one shape: { status, error, details[], timestamp }.
async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json()
    const details: string[] = Array.isArray(body.details) ? body.details : []
    return new ApiError(response.status, details[0] ?? body.error ?? response.statusText, details)
  } catch {
    return new ApiError(response.status, response.statusText || 'Request failed', [])
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`/api${path}`, { ...init, headers })

  // An expired or invalid token ends the session everywhere at once.
  if (response.status === 401 && token && !path.startsWith('/auth/')) {
    setToken(null)
    window.location.assign('/login?expired=1')
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}
