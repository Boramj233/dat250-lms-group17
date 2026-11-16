import { useEffect, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { useConfirm } from '../ConfirmDialog'

/**
 * While `dirty`, asks before the user leaves the page and loses their unsaved changes:
 * a link inside the app (menu, breadcrumbs, Edit) opens "Discard unsaved changes?", and
 * closing or reloading the tab gets the browser's own warning. Render the returned
 * dialog on the page.
 *
 * The app uses <BrowserRouter>, which cannot block navigation itself, so links are
 * caught before React Router handles them.
 */
export function useLeaveGuard(dirty: boolean): ReactNode {
  const navigate = useNavigate()
  const [confirm, dialog] = useConfirm()

  useEffect(() => {
    if (!dirty) return

    function beforeUnload(event: BeforeUnloadEvent) {
      event.preventDefault()
    }

    function click(event: MouseEvent) {
      if (
        event.defaultPrevented ||
        event.button !== 0 ||
        event.metaKey ||
        event.ctrlKey ||
        event.shiftKey ||
        event.altKey
      )
        return
      const link = (event.target as Element | null)?.closest?.('a[href]') as HTMLAnchorElement | null
      if (!link || link.target === '_blank' || link.hasAttribute('download')) return
      const url = new URL(link.href, window.location.href)
      if (url.origin !== window.location.origin) return
      const to = url.pathname + url.search + url.hash
      if (to === window.location.pathname + window.location.search + window.location.hash) return
      // Stop the link (React Router skips a prevented click) and ask first.
      event.preventDefault()
      void confirm({
        title: 'Discard unsaved changes?',
        message: 'You have changes that are not saved. If you leave this page, they are lost.',
        confirmLabel: 'Discard and leave',
        cancelLabel: 'Stay on this page',
        danger: true,
      }).then(leave => {
        if (leave) navigate(to)
      })
    }

    window.addEventListener('beforeunload', beforeUnload)
    // Capture: before React Router's own click handler on the link.
    document.addEventListener('click', click, true)
    return () => {
      window.removeEventListener('beforeunload', beforeUnload)
      document.removeEventListener('click', click, true)
    }
  }, [dirty, confirm, navigate])

  return dialog
}
