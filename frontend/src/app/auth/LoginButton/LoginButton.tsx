type LoginButtonProps = {
  onClick: () => void
  waiting?: boolean
}

/** The Log in button. The login itself is started by useLogin(). */
export default function LoginButton({ onClick, waiting = false }: LoginButtonProps) {
  return (
    <button type="button" className="login-button" onClick={onClick} aria-busy={waiting}>
      {waiting ? 'Waiting for login…' : 'Log in'}
    </button>
  )
}
