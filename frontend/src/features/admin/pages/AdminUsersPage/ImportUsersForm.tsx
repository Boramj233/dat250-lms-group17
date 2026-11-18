import { useRef, useState, type FormEvent } from 'react'
import { useImportUsers } from '@domain/users/hooks'
import type { UserImportRow, UserImportStatus } from '@domain/users/types'
import { errorMessage } from '@shared/api/errorMessage'
import DataTable from '@shared/ui/DataTable'
import { Field, FormError, FormSuccess, Panel, SubmitButton } from '@shared/ui/Form'

const STATUS_LABEL: Record<UserImportStatus, string> = {
  CREATED: 'Added',
  ALREADY_EXISTS: 'Already exists (unchanged)',
  DUPLICATE_IN_FILE: 'Repeats an earlier row',
  INVALID: 'Invalid',
}

/**
 * Adds users from a CSV file (POST /api/admin/users/import). The backend reports every
 * row; emails that already have a profile are skipped, so the same file can be uploaded
 * again after fixing the rows that failed.
 */
export default function ImportUsersForm() {
  const importUsers = useImportUsers()
  const [file, setFile] = useState<File | null>(null)
  const input = useRef<HTMLInputElement>(null)
  const result = importUsers.data

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!file) return
    importUsers.mutate(file, {
      onSuccess: () => {
        setFile(null)
        if (input.current) input.current.value = ''
      },
    })
  }

  // Rows that need attention first; the added ones after.
  const rows = result
    ? [...result.rows].sort((a, b) => Number(a.status === 'CREATED') - Number(b.status === 'CREATED'))
    : []

  return (
    <Panel title="Import users from a CSV file">
      <form className="form" onSubmit={submit}>
        <Field
          label="CSV file"
          required
          hint={
            <>
              A header row with <code>email</code>, <code>firstName</code>, <code>lastName</code> and optionally{' '}
              <code>access</code> (STANDARD, COURSE_MANAGER or ADMIN), separated by commas or semicolons. At most 5000
              rows.
            </>
          }
        >
          {props => (
            <input
              {...props}
              ref={input}
              type="file"
              accept=".csv,text/csv"
              onChange={event => {
                setFile(event.target.files?.[0] ?? null)
                importUsers.reset()
              }}
            />
          )}
        </Field>
        <FormError>
          {importUsers.error &&
            errorMessage(
              importUsers.error,
              { 400: 'The file could not be read as a user list. Check the header row and the format.' },
              'Could not import the file. Try again.',
            )}
        </FormError>
        <FormSuccess>
          {result &&
            `${result.created} added, ${result.skipped} already existed, ${result.failed} failed.` +
              (result.failed > 0 ? ' Fix the failed rows and upload the file again.' : '')}
        </FormSuccess>
        <div className="form-actions">
          <SubmitButton pending={importUsers.isPending} pendingLabel="Importing…" disabled={!file}>
            Import
          </SubmitButton>
        </div>
      </form>

      {result && rows.length > 0 && (
        <DataTable<UserImportRow>
          caption="Rows of the imported file"
          rows={rows}
          rowKey={row => `${row.line}`}
          empty="The file had no rows."
          columns={[
            { header: 'Line', cell: row => row.line },
            { header: 'Email', cell: row => row.email || '–' },
            {
              header: 'Result',
              cell: row => (
                <span
                  className={
                    row.status === 'INVALID' || row.status === 'DUPLICATE_IN_FILE' ? 'tag tag-danger' : undefined
                  }
                >
                  {STATUS_LABEL[row.status]}
                </span>
              ),
            },
            { header: 'Details', cell: row => (row.errors.length > 0 ? row.errors.join('; ') : (row.userId ?? '–')) },
          ]}
        />
      )}
    </Panel>
  )
}
