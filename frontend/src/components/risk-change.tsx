import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react'
import { cn } from '@/lib/utils'

export function RiskChange({ change, baseScanId }: { change: number; baseScanId: number }) {
  const Icon = change > 0 ? ArrowUpRight : change < 0 ? ArrowDownRight : Minus
  const tone = change > 0 ? 'text-critical-strong' : change < 0 ? 'text-success' : 'text-muted-foreground'
  return (
    <span className="inline-flex items-center gap-1 text-sm">
      <Icon className={cn('size-4', tone)} />
      <span className={cn('tabular font-medium', tone)}>{change === 0 ? 'No change' : `${change > 0 ? '+' : ''}${change}`}</span>
      <span className="text-muted-foreground">since scan #{baseScanId}</span>
    </span>
  )
}
