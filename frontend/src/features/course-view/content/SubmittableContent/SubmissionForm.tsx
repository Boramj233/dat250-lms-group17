import { useState, type FormEvent } from 'react'
import { errorMessage } from '@shared/api/errorMessage'
import { safeHttpUrl } from '@shared/lib/safeUrl'
import type { GroupId } from '@shared/types'
import { useSubmitFile, useSubmitMySubmission } from '@domain/assignments/hooks'
import type { AssignmentContent, CodingExerciseContent } from '@domain/content/types'
import { Field, FormError, SubmitButton } from '@shared/ui/Form'

/** The backend accepts PDF files up to this size (lms.submission.files.max-size). */
const MAX_FILE_BYTES = 10 * 1024 * 1024

type SubmissionFormProps = {
  content: AssignmentContent | CodingExerciseContent
  /**
   * The user's group in the course, if any. The backend lets a member hand in for their
   * group on any assignment or coding exercise, so the form asks whether this answer is
   * for the group or only for the user.
   */
  group?: { id: GroupId; label: string }
  /** Whether "for the group" is chosen at first (e.g. the last answer was a group one). */
  forGroupAtFirst?: boolean
  onDone: () => void
  onCancel: () => void
}

/**
 * The answer in a format the backend accepts: for an assignment a text or a PDF file,
 * for a coding exercise a link to a Git repository.
 */
export default function SubmissionForm({
  content,
  group,
  forGroupAtFirst = false,
  onDone,
  onCancel,
}: SubmissionFormProps) {
  const submitText = useSubmitMySubmission()
  const submitPdf = useSubmitFile()
  const isCode = content.type === 'CODING_EXERCISE'
  const [kind, setKind] = useState<'TEXT' | 'FILE'>('TEXT')
  const [value, setValue] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [forGroup, setForGroup] = useState(group !== undefined && forGroupAtFirst)
  const groupId = group && forGroup ? group.id : undefined
  const pending = submitText.isPending || submitPdf.isPending
  const error = submitText.error ?? submitPdf.error

  const answer = value.trim()
  const fileProblem =
    file &&
    (!file.name.toLowerCase().endsWith('.pdf')
      ? 'Choose a PDF file.'
      : file.size > MAX_FILE_BYTES
        ? 'The file is too large (at most 10 MB).'
        : null)
  const valid = isCode
    ? safeHttpUrl(answer) !== undefined
    : kind === 'FILE'
      ? file !== null && !fileProblem
      : answer !== ''

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!valid) return
    if (!isCode && kind === 'FILE') {
      submitPdf.mutate({ contentId: content.id, file: file!, groupId }, { onSuccess: onDone })
    } else {
      submitText.mutate(
        { contentId: content.id, type: isCode ? 'GIT_URL' : 'TEXT', payload: answer, groupId },
        { onSuccess: onDone },
      )
    }
  }

  return (
    <form className="form submittable-form" onSubmit={handleSubmit}>
      {group && (
        <fieldset className="submittable-kind">
          <legend className="form-label">Hand in for</legend>
          <label>
            <input type="radio" name="submission-for" checked={!forGroup} onChange={() => setForGroup(false)} />
            Only me
          </label>
          <label>
            <input type="radio" name="submission-for" checked={forGroup} onChange={() => setForGroup(true)} />
            {group.label} (it counts for everyone in the group)
          </label>
        </fieldset>
      )}
      {!isCode && (
        <fieldset className="submittable-kind">
          <legend className="visually-hidden">How do you hand in?</legend>
          <label>
            <input type="radio" name="submission-kind" checked={kind === 'TEXT'} onChange={() => setKind('TEXT')} />
            Write an answer
          </label>
          <label>
            <input type="radio" name="submission-kind" checked={kind === 'FILE'} onChange={() => setKind('FILE')} />
            Upload a PDF file
          </label>
        </fieldset>
      )}

      {!isCode && kind === 'FILE' ? (
        <Field label="PDF file" hint="One PDF file, at most 10 MB." hintAbove error={fileProblem}>
          {props => (
            <input
              {...props}
              type="file"
              accept="application/pdf,.pdf"
              onChange={event => setFile(event.target.files?.[0] ?? null)}
            />
          )}
        </Field>
      ) : (
        <Field
          label={isCode ? 'Repository URL' : 'Your answer'}
          hintAbove
          hint={
            isCode
              ? 'A link to your Git repository, starting with https://, e.g. https://github.com/you/project.'
              : 'Write your answer as text (at most 10 000 characters).'
          }
        >
          {props =>
            isCode ? (
              <input {...props} type="url" value={value} onChange={event => setValue(event.target.value)} autoFocus />
            ) : (
              <textarea
                {...props}
                rows={10}
                maxLength={10000}
                value={value}
                onChange={event => setValue(event.target.value)}
                autoFocus
              />
            )
          }
        </Field>
      )}

      <FormError>{error && submitError(error)}</FormError>
      <div className="form-actions">
        <SubmitButton pending={pending} pendingLabel="Submitting…" disabled={!valid}>
          Submit
        </SubmitButton>
        <button type="button" className="button-secondary" onClick={onCancel} disabled={pending}>
          Cancel
        </button>
      </div>
    </form>
  )
}

/** A message for a failed hand-in, from the backend's status code. */
function submitError(error: unknown): string {
  return errorMessage(
    error,
    {
      409: 'The deadline has passed, so it could not be handed in.',
      415: 'Only PDF files are accepted.',
      413: 'The file is too large (at most 10 MB).',
      403: 'You cannot hand in here (or for this group).',
      503: 'Handing in files is not available right now. Try again later.',
    },
    'Could not submit your answer. Try again.',
  )
}
