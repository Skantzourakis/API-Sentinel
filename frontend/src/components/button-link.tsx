import type { VariantProps } from 'class-variance-authority'
import { Link, type LinkProps } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'

/** A router link styled as a button; cn() merges the base and variant border classes correctly. */
export function ButtonLink({
  variant,
  size,
  className,
  ...props
}: LinkProps & VariantProps<typeof buttonVariants>) {
  return <Link className={cn(buttonVariants({ variant, size }), className)} {...props} />
}
