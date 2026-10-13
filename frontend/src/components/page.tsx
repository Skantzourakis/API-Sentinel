import { Fragment, type ReactNode } from 'react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export function Page({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('mx-auto flex w-full max-w-6xl flex-col gap-10 px-5 py-7 md:px-12 md:py-10', className)}>{children}</div>
}

export interface Crumb {
  label: string
  to?: string
}

export function PageHeader({
  crumbs = [],
  title,
  description,
  meta,
  actions,
}: {
  crumbs?: Crumb[]
  title: ReactNode
  description?: ReactNode
  meta?: ReactNode
  actions?: ReactNode
}) {
  return (
    <header className="flex flex-col gap-4">
      {crumbs.length > 0 && (
        <nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
          {crumbs.map((crumb, index) => (
            <Fragment key={index}>
              {index > 0 && <span aria-hidden>/</span>}
              {crumb.to ? (
                <Link to={crumb.to} className="hover:text-foreground hover:underline">
                  {crumb.label}
                </Link>
              ) : (
                <span className="text-foreground">{crumb.label}</span>
              )}
            </Fragment>
          ))}
        </nav>
      )}
      <div className="flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
        <div className="flex min-w-0 flex-col gap-2">
          <h1 className="text-[1.75rem] leading-tight font-semibold tracking-tight text-balance">{title}</h1>
          {description && <p className="max-w-[65ch] text-muted-foreground">{description}</p>}
          {meta && <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1 text-sm text-muted-foreground">{meta}</div>}
        </div>
        {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  )
}

/** A titled region separated from the one above by a hairline rule, not a box. */
export function Section({
  title,
  description,
  actions,
  children,
  className,
}: {
  title: ReactNode
  description?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={cn('flex flex-col gap-4 border-t border-foreground/15 pt-5', className)}>
      <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
        <div className="flex flex-col gap-1">
          <h2 className="font-semibold">{title}</h2>
          {description && <p className="text-sm text-muted-foreground">{description}</p>}
        </div>
        {actions}
      </div>
      {children}
    </section>
  )
}

export function EmptyState({ title, children, action }: { title: string; children: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex max-w-lg flex-col items-start gap-3 py-6">
      <p className="text-lg font-medium">{title}</p>
      <p className="text-muted-foreground">{children}</p>
      {action && <div className="mt-1">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
      <span className="font-medium text-destructive">Something went wrong.</span>
      <span className="text-muted-foreground">{message}</span>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
