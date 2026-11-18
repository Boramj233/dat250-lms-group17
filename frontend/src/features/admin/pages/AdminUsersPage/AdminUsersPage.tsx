import { useState } from 'react'
import { useAdminUsers } from '@domain/users/hooks'
import type { UserAccount } from '@domain/users/types'
import { ACCESS_LABEL } from '@shared/auth/access'
import { fullName } from '@shared/lib/fullName'
import { useDebounced } from '@shared/lib/useDebounced'
import { TrailPage } from '@shared/navigation/Trail'
import type { UserId } from '@shared/types'
import DataTable from '@shared/ui/DataTable'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import CreateUserForm from './CreateUserForm'
import ImportUsersForm from './ImportUsersForm'
import UserAccessPanel from './UserAccessPanel'
import './AdminUsersPage.css'

const PAGE_SIZE = 20

/**
 * Administrators: every profile (searchable, 20 per page), changing a user's global
 * access, and adding users one by one or from a CSV file (AdminUserController).
 */
export default function AdminUsersPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<UserId | null>(null)
  const text = useDebounced(search).trim()
  const users = useAdminUsers({ search: text, page, size: PAGE_SIZE })

  return (
    <TrailPage label="Users">
      <PageHeader title="Users">
        <p className="text-meta">
          Profiles for the accounts in the login service. Access decides who may create courses (course manager) and
          manage users (administrator).
        </p>
      </PageHeader>

      <section aria-labelledby="admin-users-list" className="admin-users">
        <h2 id="admin-users-list" className="section-title">
          All users
        </h2>
        <div className="form-field admin-users-search">
          <label htmlFor="admin-users-search">Search by name or email</label>
          <input
            id="admin-users-search"
            type="search"
            autoComplete="off"
            value={search}
            onChange={event => {
              setSearch(event.target.value)
              setPage(0)
            }}
          />
        </div>

        <QueryState query={users} loading="Loading users…" error="Could not load the users. Try again later.">
          {result => (
            <>
              <DataTable<UserAccount>
                caption="Users"
                rows={result.users}
                rowKey={user => String(user.userId)}
                empty={text ? 'No users match the search.' : 'No users yet.'}
                columns={[
                  { header: 'ID', cell: user => user.userId },
                  { header: 'Name', cell: user => fullName(user) },
                  { header: 'Email', cell: user => <span className="admin-users-email">{user.email}</span> },
                  {
                    header: 'Access',
                    cell: user => (
                      <span className={user.access === 'STANDARD' ? undefined : 'tag tag-primary'}>
                        {ACCESS_LABEL[user.access]}
                      </span>
                    ),
                  },
                  {
                    header: 'Actions',
                    cell: user => (
                      <button
                        type="button"
                        className="button-secondary"
                        aria-expanded={String(selected) === String(user.userId)}
                        onClick={() => setSelected(String(selected) === String(user.userId) ? null : user.userId)}
                      >
                        Change access<span className="visually-hidden"> for {fullName(user)}</span>
                      </button>
                    ),
                  },
                ]}
              />
              {result.totalPages > 1 && (
                <nav className="admin-users-pages" aria-label="Pages of users">
                  <button
                    type="button"
                    className="button-secondary"
                    disabled={page === 0}
                    onClick={() => setPage(p => p - 1)}
                  >
                    Previous
                  </button>
                  <span className="text-meta">
                    Page {result.page + 1} of {result.totalPages} · {result.totalElements} users
                  </span>
                  <button
                    type="button"
                    className="button-secondary"
                    disabled={page + 1 >= result.totalPages}
                    onClick={() => setPage(p => p + 1)}
                  >
                    Next
                  </button>
                </nav>
              )}
            </>
          )}
        </QueryState>
      </section>

      {selected !== null && (
        <UserAccessPanel key={String(selected)} userId={selected} onClose={() => setSelected(null)} />
      )}

      <CreateUserForm />
      <ImportUsersForm />
    </TrailPage>
  )
}
