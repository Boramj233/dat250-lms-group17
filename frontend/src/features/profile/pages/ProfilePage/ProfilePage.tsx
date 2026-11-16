import { useCurrentUser } from '@shared/auth/useAuth'
import { TrailPage } from '@shared/navigation/Trail'
import PageHeader from '@shared/ui/PageHeader'
import './ProfilePage.css'

/** The logged-in user's account details, as the backend knows them. */
export default function ProfilePage() {
  const user = useCurrentUser().data!

  return (
    <TrailPage label="Profile">
      <PageHeader title="Profile" />
      <dl className="profile-facts">
        <dt>User ID</dt>
        <dd>{user.userId}</dd>
        <dt>First name</dt>
        <dd>{user.firstName}</dd>
        <dt>Last name</dt>
        <dd>{user.lastName}</dd>
        <dt>Email</dt>
        <dd>{user.email}</dd>
      </dl>
    </TrailPage>
  )
}
