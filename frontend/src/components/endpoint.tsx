import { cn } from '@/lib/utils'

/** An HTTP method and path, or a server URL for specification-level findings (method "ALL"). */
export function Endpoint({ method, path, className }: { method: string; path: string; className?: string }) {
  const isServer = method === 'ALL'
  return (
    <span className={cn('flex min-w-0 items-baseline gap-2 font-mono text-[0.8125rem]', className)}>
      <span className="w-[3.25rem] shrink-0 text-[0.6875rem] font-semibold tracking-wide text-muted-foreground">
        {isServer ? 'SERVER' : method}
      </span>
      <span className="truncate" title={path}>
        {path}
      </span>
    </span>
  )
}
