import type { ReactNode } from 'react'

type IconProps = {
  /** Width and height in pixels. */
  size?: number
  strokeWidth?: number
  className?: string
}

/**
 * A 24×24 icon in the current text colour, hidden from screen readers: the button or link
 * around it says what it does. Outlined unless `filled`.
 */
function Icon({
  size = 16,
  strokeWidth = 2,
  className,
  filled = false,
  children,
}: IconProps & { filled?: boolean; children: ReactNode }) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
      {...(filled
        ? { fill: 'currentColor' }
        : { fill: 'none', stroke: 'currentColor', strokeWidth, strokeLinecap: 'round', strokeLinejoin: 'round' })}
    >
      {children}
    </svg>
  )
}

/** ›, e.g. a link to more, or (with .disclosure-chevron) a box that folds open. */
export function ChevronRightIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M9 6l6 6-6 6" />
    </Icon>
  )
}

/** ‹, e.g. "‹ All modules". */
export function ChevronLeftIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M15 6l-6 6 6 6" />
    </Icon>
  )
}

/** ✕: close, or "wrong" / "not passed". */
export function CloseIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M6 6l12 12M6 18L18 6" />
    </Icon>
  )
}

/** ☰: opens a menu. */
export function MenuIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M4 6h16M4 12h16M4 18h16" />
    </Icon>
  )
}

/** ✓: "correct" / "passed". */
export function CheckIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M5 12.5l4.5 4.5L19 7.5" />
    </Icon>
  )
}

/** ✓ in a circle: done, e.g. a completed quiz. */
export function CheckCircleIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="12" r="10" />
      <path d="M7.5 12.5l3 3 6-6.5" />
    </Icon>
  )
}

/** ⠿: a handle to drag a row by. */
export function DragHandleIcon(props: IconProps) {
  return (
    <Icon {...props} filled>
      <circle cx="9" cy="6" r="1.6" />
      <circle cx="15" cy="6" r="1.6" />
      <circle cx="9" cy="12" r="1.6" />
      <circle cx="15" cy="12" r="1.6" />
      <circle cx="9" cy="18" r="1.6" />
      <circle cx="15" cy="18" r="1.6" />
    </Icon>
  )
}

/** ⋯: opens a menu of actions. */
export function MoreIcon(props: IconProps) {
  return (
    <Icon {...props} filled>
      <circle cx="5" cy="12" r="2" />
      <circle cx="12" cy="12" r="2" />
      <circle cx="19" cy="12" r="2" />
    </Icon>
  )
}
