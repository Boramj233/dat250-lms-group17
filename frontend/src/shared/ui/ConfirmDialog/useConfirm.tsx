import { useCallback, useState, type ReactNode } from 'react'
import ConfirmDialog from './ConfirmDialog'

export type ConfirmOptions = {
  title: string
  /** Text (shown as a paragraph) or your own paragraphs. */
  message: ReactNode
  /** The confirm button's text, e.g. "Delete group". */
  confirmLabel: string
  cancelLabel?: string
}

type Request = ConfirmOptions & { resolve: (confirmed: boolean) => void }

/**
 * Like window.confirm, but with ConfirmDialog. Render `dialog` somewhere on the page, then
 * `if (!(await confirm({ ... }))) return`. The promise is true for the confirm button,
 * false for Cancel, Esc or a click outside.
 */
export function useConfirm(): [(options: ConfirmOptions) => Promise<boolean>, ReactNode] {
  const [request, setRequest] = useState<Request | null>(null)

  const confirm = useCallback((options: ConfirmOptions) =>
    new Promise<boolean>(resolve => setRequest({ ...options, resolve })), [])

  function answer(confirmed: boolean) {
    request?.resolve(confirmed)
    setRequest(null)
  }

  const dialog = (
    <ConfirmDialog
      open={request !== null}
      title={request?.title ?? ''}
      confirmLabel={request?.confirmLabel ?? ''}
      cancelLabel={request?.cancelLabel}
      onConfirm={() => answer(true)}
      onCancel={() => answer(false)}
    >
      {typeof request?.message === 'string' ? <p>{request.message}</p> : request?.message}
    </ConfirmDialog>
  )

  return [confirm, dialog]
}
