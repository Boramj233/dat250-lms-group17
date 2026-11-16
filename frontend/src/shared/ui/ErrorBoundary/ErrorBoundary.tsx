import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Link } from 'react-router'
import { paths } from '../../paths'

type ErrorBoundaryProps = {
  children: ReactNode
  /** When it changes (e.g. the page's path), the content is shown again. */
  resetKey?: string
}

/** `resetKey`: the one the error happened at; a new one shows the content again. */
type ErrorBoundaryState = { failed: boolean; resetKey?: string }

/**
 * Catches an error while showing its content, so only that part of the screen shows a
 * message instead of the whole app going blank. Errors from loading data are shown by the
 * pages themselves (QueryState); this is for bugs.
 */
export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { failed: false, resetKey: this.props.resetKey }

  static getDerivedStateFromError(): Partial<ErrorBoundaryState> {
    return { failed: true }
  }

  // Another page (a new resetKey): show the content again, in the same render.
  static getDerivedStateFromProps(props: ErrorBoundaryProps, state: ErrorBoundaryState): ErrorBoundaryState | null {
    return props.resetKey !== state.resetKey ? { failed: false, resetKey: props.resetKey } : null
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('A page could not be shown:', error, info.componentStack)
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <div role="alert">
        <h1>Something went wrong</h1>
        <p>This page could not be shown. Try again, or go to another page.</p>
        <p>
          <button type="button" className="button-secondary" onClick={() => this.setState({ failed: false })}>
            Try again
          </button>{' '}
          <Link to={paths.home}>Go to home</Link>
        </p>
      </div>
    )
  }
}
