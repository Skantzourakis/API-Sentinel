import type { ScanStatus } from '@/lib/types'
import { cn } from '@/lib/utils'

const STATUS = {
  COMPLETED: { label: 'Completed', className: 'text-muted-foreground' },
  FAILED: { label: 'Failed', className: 'font-medium text-destructive' },
  PENDING: { label: 'Pending', className: 'text-muted-foreground' },
} as const

export function ScanStatusLabel({ status, className }: { status: ScanStatus; className?: string }) {
  const { label, className: tone } = STATUS[status]
  return <span className={cn('text-sm', tone, className)}>{label}</span>
}
