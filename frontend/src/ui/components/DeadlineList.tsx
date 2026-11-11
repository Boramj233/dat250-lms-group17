import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { paths } from '../../app/paths'
import type { DeadlineContentType, UpcomingDeadline } from '../../data/deadlines/types'
import { formatDateTime, formatDayMonth, formatTimeLeft, isSoon } from './dateFormat'
import './DeadlineList.css'

const TYPE_LABEL: Record<DeadlineContentType, string> = {
  ASSIGNMENT: 'Assignment',
  CODING_EXERCISE: 'Coding exercise',
}

type DeadlineListProps = {
  deadlines: UpcomingDeadline[]
}

/** The current time, updated every minute so "in 30 minutes" stays correct. */
function useNow(): Date {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60_000)
    return () => window.clearInterval(timer)
  }, [])
  return now
}

/** Upcoming deadlines, earliest first, with the course each one belongs to. */
export default function DeadlineList({ deadlines }: DeadlineListProps) {
  const now = useNow()
  const sorted = [...deadlines].sort((a, b) => Date.parse(a.deadline) - Date.parse(b.deadline))

  return (
    <ol className="deadline-list">
      {sorted.map(item => {
        const due = new Date(item.deadline)
        const [day, month] = formatDayMonth(due)
        const soon = isSoon(due, now)
        return (
          <li key={`${item.type}-${item.contentId}`} className={`deadline${soon ? ' is-soon' : ''}`}>
            <div className="deadline-date" aria-hidden="true">
              <span className="deadline-day">{day}</span>
              <span className="deadline-month">{month}</span>
            </div>
            <div className="deadline-body">
              {/* TODO: Link the title to the assignment / coding exercise page once it
                  exists (route by item.type and item.contentId). */}
              {/* TODO: Show whether the user has submitted, once UpcomingDeadline has
                  `submitted` (see data/deadlines/types.ts). */}
              <p className="deadline-title">{item.title}</p>
              <p className="deadline-meta">
                <span>{TYPE_LABEL[item.type]}</span>
                <span aria-hidden="true"> · </span>
                <Link to={paths.course(item.courseId)}>{item.courseTitle}</Link>
              </p>
              <p className="deadline-time">
                <time dateTime={item.deadline}>{formatDateTime(due)}</time>
                <span className="deadline-left"> ({formatTimeLeft(due, now)})</span>
              </p>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
