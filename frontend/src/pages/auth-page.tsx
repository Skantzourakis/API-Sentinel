import { LoaderCircle } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router'
import { Logo } from '@/components/logo'
import { SeverityMark } from '@/components/severity'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import type { Severity } from '@/lib/severity'

const CHECKS: { severity: Severity; label: string }[] = [
  { severity: 'CRITICAL', label: 'Admin endpoints without authentication' },
  { severity: 'HIGH', label: 'Writes open to anonymous callers' },
  { severity: 'HIGH', label: 'Object ids without access control (BOLA)' },
  { severity: 'HIGH', label: 'Secrets in query strings, plain-HTTP servers' },
  { severity: 'MEDIUM', label: 'Unbounded list endpoints' },
  { severity: 'LOW', label: 'Undocumented error responses' },
]

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { session, login, register } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string[] | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (session) return <Navigate to="/projects" replace />

  const isLogin = mode === 'login'

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await (isLogin ? login(email, password) : register(email, password))
      navigate('/projects', { replace: true })
    } catch (e) {
      // Validation errors arrive as "field: message"; show only the message.
      const details = e instanceof ApiError && e.details.length > 0 ? e.details : [(e as Error).message]
      setError(details.map((d) => d.replace(/^\w+: /, '')))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
      <aside className="hidden flex-col justify-between bg-sidebar px-12 py-10 text-sidebar-foreground lg:flex">
        <Logo className="text-sidebar-accent-foreground" />
        <div className="flex max-w-md flex-col gap-10">
          <div className="flex flex-col gap-4">
            <h2 className="text-[2.125rem] leading-[1.15] font-semibold tracking-tight text-balance text-sidebar-accent-foreground">
              Catch API security gaps before they ship.
            </h2>
            <p className="text-sidebar-foreground/75">
              Upload an OpenAPI specification. Get findings mapped to the OWASP API Security Top&nbsp;10, a risk score,
              and what changed since the last version.
            </p>
          </div>
          <ul className="flex flex-col border-t border-sidebar-border text-sm">
            {CHECKS.map((check) => (
              <li key={check.label} className="flex items-center gap-3 border-b border-sidebar-border py-2.5 text-sidebar-foreground/85">
                <SeverityMark severity={check.severity} />
                {check.label}
              </li>
            ))}
          </ul>
        </div>
        <p className="text-sm text-sidebar-muted">Static analysis only. Your API never receives a request.</p>
      </aside>

      <main className="flex flex-col justify-center px-6 py-12 sm:px-16">
        <div className="flex w-full max-w-sm flex-col gap-8">
          <Logo className="lg:hidden" />
          <div className="flex flex-col gap-2">
            <h1 className="text-[1.75rem] leading-tight font-semibold tracking-tight">{isLogin ? 'Sign in' : 'Create your account'}</h1>
            <p className="text-muted-foreground">
              {isLogin ? 'Welcome back. Sign in to see your projects.' : 'Start scanning your API specifications.'}
            </p>
          </div>

          {params.get('expired') && isLogin && !error && (
            <p className="text-sm text-accent-foreground">Your session expired. Sign in again to continue.</p>
          )}

          <form onSubmit={onSubmit} className="flex flex-col gap-5" noValidate>
            <div className="flex flex-col gap-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
                className="h-10 bg-card"
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete={isLogin ? 'current-password' : 'new-password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="h-10 bg-card"
                aria-describedby={isLogin ? undefined : 'password-hint'}
              />
              {!isLogin && (
                <p id="password-hint" className="text-xs text-muted-foreground">
                  At least 8 characters.
                </p>
              )}
            </div>

            {error && (
              <ul role="alert" className="flex flex-col gap-1 text-sm text-destructive">
                {error.map((message) => (
                  <li key={message}>{message}</li>
                ))}
              </ul>
            )}

            <Button type="submit" disabled={submitting} className="h-10 w-full">
              {submitting && <LoaderCircle className="animate-spin" />}
              {isLogin ? 'Sign in' : 'Create account'}
            </Button>
          </form>

          <p className="text-sm text-muted-foreground">
            {isLogin ? 'New to API Sentinel? ' : 'Already have an account? '}
            <Link to={isLogin ? '/register' : '/login'} className="font-medium text-foreground underline underline-offset-4 hover:text-primary">
              {isLogin ? 'Create an account' : 'Sign in'}
            </Link>
          </p>
        </div>
      </main>
    </div>
  )
}
