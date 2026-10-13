import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, getToken, setToken } from '@/lib/api'
import type { TokenResponse } from '@/lib/types'

interface Session {
  token: string
  email: string
}

interface AuthContextValue {
  session: Session | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

// Reads the email and expiry from the JWT payload; the signature is checked by the backend.
function sessionFromToken(token: string | null): Session | null {
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()) {
      setToken(null)
      return null
    }
    return { token, email: String(payload.email ?? '') }
  } catch {
    setToken(null)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(() => sessionFromToken(getToken()))

  const login = useCallback(async (email: string, password: string) => {
    const response = await api<TokenResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    setToken(response.accessToken)
    setSession(sessionFromToken(response.accessToken))
  }, [])

  const register = useCallback(
    async (email: string, password: string) => {
      await api('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) })
      await login(email, password)
    },
    [login],
  )

  const logout = useCallback(() => {
    setToken(null)
    setSession(null)
    queryClient.clear()
  }, [queryClient])

  const value = useMemo(() => ({ session, login, register, logout }), [session, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
