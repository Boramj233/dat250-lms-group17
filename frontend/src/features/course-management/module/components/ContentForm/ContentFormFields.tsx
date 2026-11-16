import RequiredMark from '@shared/ui/RequiredMark'
import type { ContentType } from '@domain/content/types'
import { Field, type SetFormValue } from '@shared/ui/Form'
import type { ContentFormValues } from './contentFormValues'
import { combineExtensions, FILE_TYPE_PRESETS, formatExtensions } from './fileTypes'
import QuizQuestionsEditor from './QuizQuestionsEditor'

type FieldsProps = {
  values: ContentFormValues
  set: SetFormValue<ContentFormValues>
}

/** Coding exercises: a starter repository, starter code, or both (required as a pair). */
export function StarterFields({ values, set }: FieldsProps) {
  return (
    <fieldset className="form-field starter-fields" aria-describedby="starter-hint">
      <legend className="form-label">
        Where students start
        <RequiredMark />
      </legend>
      <p id="starter-hint" className="form-hint">
        At least one: a starter repository, starter code, or both.
      </p>
      <Field label="Starter repository URL">
        {props => (
          <input
            {...props}
            type="url"
            maxLength={2048}
            value={values.starterUrl}
            onChange={event => set('starterUrl')(event.target.value)}
            placeholder="https://github.com/…"
          />
        )}
      </Field>
      <Field label="Starter code">
        {props => (
          <textarea
            {...props}
            className="code-input"
            spellCheck={false}
            value={values.starterCode}
            onChange={event => set('starterCode')(event.target.value)}
          />
        )}
      </Field>
    </fieldset>
  )
}

/**
 * Assignments and coding exercises: deadline and late submissions; for assignments also
 * mandatory, group assignment and the accepted file types.
 */
export function SubmissionFields({ type, values, set }: FieldsProps & { type: ContentType }) {
  return (
    <>
      <Field label="Deadline" hint="Leave empty for no deadline. Your local time.">
        {props => (
          <input
            {...props}
            type="datetime-local"
            value={values.deadline}
            onChange={event => set('deadline')(event.target.value)}
          />
        )}
      </Field>
      <label className="form-check">
        <input type="checkbox" checked={values.allowLate} onChange={event => set('allowLate')(event.target.checked)} />
        Allow late submissions
      </label>
      {type === 'ASSIGNMENT' && (
        <>
          <label className="form-check">
            <input
              type="checkbox"
              checked={values.mandatory}
              onChange={event => set('mandatory')(event.target.checked)}
            />
            Mandatory
          </label>
          <label className="form-check">
            <input
              type="checkbox"
              checked={values.groupAssignment}
              onChange={event => set('groupAssignment')(event.target.checked)}
            />
            Group assignment (one submission per group)
          </label>
          <FileTypeFields values={values} set={set} />
        </>
      )}
    </>
  )
}

/** Which file types students may hand in: ticked presets plus typed extensions. */
function FileTypeFields({ values, set }: FieldsProps) {
  const { fileTypePresets, otherFileTypes } = values
  return (
    <fieldset className="form-field file-types">
      <legend className="form-label">Accepted file types</legend>
      <div className="form-row">
        {FILE_TYPE_PRESETS.map(preset => (
          <label key={preset.label} className="form-check" title={preset.extensions.join(', ')}>
            <input
              type="checkbox"
              checked={fileTypePresets.includes(preset.label)}
              onChange={event =>
                set('fileTypePresets')(
                  event.target.checked
                    ? [...fileTypePresets, preset.label]
                    : fileTypePresets.filter(label => label !== preset.label),
                )
              }
            />
            {preset.label}
          </label>
        ))}
      </div>
      <label htmlFor="file-types-other">Other extensions</label>
      <input
        id="file-types-other"
        value={otherFileTypes}
        placeholder="e.g. .py, .java"
        onChange={event => set('otherFileTypes')(event.target.value)}
      />
      <p className="form-hint">
        Accepted: {formatExtensions(combineExtensions(fileTypePresets, otherFileTypes))}. Tick nothing to accept any
        file type.
      </p>
    </fieldset>
  )
}

/** Quizzes: attempts allowed, the pass requirement and the questions. */
export function QuizFields({ values, set }: FieldsProps) {
  const count = values.questions.length
  return (
    <>
      <Field label="Attempts allowed" required>
        {props => (
          <input
            {...props}
            type="number"
            min={1}
            value={values.maxAttempts}
            onChange={event => set('maxAttempts')(Number(event.target.value))}
          />
        )}
      </Field>
      <Field
        label="Correct answers to pass"
        hint={
          <>
            An attempt is passed automatically when at least this many of the {count}
            {count === 1 ? ' question is' : ' questions are'} correct. Leave it empty for no pass requirement.
          </>
        }
      >
        {props => (
          <input
            {...props}
            type="number"
            min={1}
            max={count}
            step={1}
            inputMode="numeric"
            value={values.requiredCorrect}
            placeholder="No pass requirement"
            onChange={event => set('requiredCorrect')(event.target.value)}
          />
        )}
      </Field>
      <QuizQuestionsEditor questions={values.questions} onChange={set('questions')} />
    </>
  )
}
