import '../../styles/forms.css'

/**
 * The "*" after the label of a field that must be filled in. Only for the eye: the field
 * itself says it is required (required / aria-required), which screen readers announce.
 */
export default function RequiredMark() {
  return (
    <span className="form-required" aria-hidden="true">
      *
    </span>
  )
}
