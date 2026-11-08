import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { paths } from '../../app/paths'
import type { CurrentUser } from '../../data/auth/auth'
import { apiClient, AuthenticationRequiredError } from '../../data/shared/apiClient'

export default function HomePage() {
  const profile = useQuery({
    queryKey: ['currentUser'],
    queryFn: () => apiClient<CurrentUser>('/api/users/me'),
    retry: false,
  })
  return (
    <>
      <h1>Welcome to DAT250 LMS</h1>
      {profile.isPending && <p>Loading profile...</p>}
      {profile.data && <p>Logged in as {profile.data.firstName} {profile.data.lastName} ({profile.data.email})</p>}
      {profile.error instanceof AuthenticationRequiredError
        ? <Link to={paths.login}>Log in to view your profile</Link>
        : profile.error && <p role="alert">Could not load your profile. Try again later.</p>}
    </>
  )
}
