import type { ReactNode } from 'react'
import './DataTable.css'

export type Column<T> = {
  header: string
  cell: (row: T) => ReactNode
}

type DataTableProps<T> = {
  /** For screen readers: what the table lists. */
  caption: string
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string
  /** Shown in the table when there are no rows. */
  empty: ReactNode
}

/** A table with a header row, used for the course's lists (people, grades, content). */
export default function DataTable<T>({ caption, columns, rows, rowKey, empty }: DataTableProps<T>) {
  return (
    <div className="data-table-scroll">
      <table className="data-table">
        <caption className="visually-hidden">{caption}</caption>
        <thead>
          <tr>
            {columns.map(column => (
              <th key={column.header} scope="col">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td className="data-table-empty" colSpan={columns.length}>
                {empty}
              </td>
            </tr>
          ) : (
            rows.map(row => (
              <tr key={rowKey(row)}>
                {columns.map(column => (
                  <td key={column.header}>{column.cell(row)}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
