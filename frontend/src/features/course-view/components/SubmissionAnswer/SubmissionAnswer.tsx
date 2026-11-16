import { useState } from 'react'
import { apiDownload } from '../../../../shared/api/apiClient'
import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { safeHttpUrl } from '../../../../shared/lib/safeUrl'
import type { MySubmission } from '../../../assignments/types'

/** "1.2 MB", "340 kB". */
function formatSize(bytes: number): string {
  if (bytes >= 1_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`
  return `${Math.max(1, Math.round(bytes / 1000))} kB`
}

/**
 * What was handed in, in its own box: the text, the repository as a link, or the PDF
 * with a download button (the download needs the token, so it goes through the API).
 * Styled by the submittable-* classes in pages/ContentView/SubmittableContent.css,
 * which the pages using it load.
 */
export default function SubmissionAnswer({ submission }: { submission: MySubmission }) {
  const [downloading, setDownloading] = useState(false)
  const [failed, setFailed] = useState(false)
  const url = submission.type === 'GIT_URL' ? safeHttpUrl(submission.payload) : undefined
  const label = submission.type === 'GIT_URL' ? 'Repository' : submission.type === 'FILE' ? 'PDF file' : 'Answer'

  async function download() {
    if (!submission.file) return
    setDownloading(true)
    setFailed(false)
    try {
      await apiDownload(submission.file.downloadPath, submission.file.name)
    } catch {
      setFailed(true)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="submittable-answer">
      <p className="submittable-label">
        {label}
        <span className="submittable-muted"> · {formatDateTime(new Date(submission.submittedAt))}</span>
      </p>
      {submission.type === 'FILE' && submission.file && (
        <div className="submittable-file">
          <span>
            <strong>{submission.file.name}</strong>
            <span className="submittable-muted"> · {formatSize(submission.file.sizeBytes)}</span>
          </span>
          <button type="button" className="button-secondary" onClick={() => void download()} disabled={downloading}>
            {downloading ? 'Downloading…' : 'Download'}
          </button>
          {failed && <p className="form-error" role="alert">Could not download the file. Try again.</p>}
        </div>
      )}
      {submission.type === 'GIT_URL' && (
        url ? <a href={url} target="_blank" rel="noreferrer">{url}</a> : <span>{submission.payload}</span>
      )}
      {submission.type === 'TEXT' && <div className="submittable-text">{submission.payload}</div>}
    </div>
  )
}
