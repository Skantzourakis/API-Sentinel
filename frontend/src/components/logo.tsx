import { cn } from '@/lib/utils'

// The mark is a miniature severity spectrum: the same element the whole product is built around.
const MARK = ['bg-critical', 'bg-high', 'bg-medium', 'bg-low']

export function LogoMark({ className }: { className?: string }) {
  return (
    <span aria-hidden className={cn('flex h-3.5 items-end gap-[2px]', className)}>
      {MARK.map((fill, index) => (
        <span key={fill} className={cn('w-[5px]', fill)} style={{ height: `${100 - index * 22}%` }} />
      ))}
    </span>
  )
}

export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn('flex items-center gap-2.5', className)}>
      <LogoMark />
      <span className="text-[0.9375rem] font-semibold tracking-tight">API Sentinel</span>
    </span>
  )
}
