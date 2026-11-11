import { useCurrentUser } from '../../data/auth/useAuth'

/** Only shown to logged-in users (see AuthGate). */
export default function HomePage() {
  const profile = useCurrentUser()
  return (
    <>
      <h1>Welcome to DAT250 LMS</h1>
      {profile.data && (
        <p>Logged in as {profile.data.firstName} {profile.data.lastName} ({profile.data.email})</p>
      )}
    </>
  )
}
