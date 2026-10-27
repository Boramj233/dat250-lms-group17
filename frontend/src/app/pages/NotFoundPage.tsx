import { Link } from 'react-router'
import { paths } from '../paths'

export default function NotFoundPage() {
  return (
    <>
      <h1>Page not found</h1>
      <Link to={paths.courses}>Go to my courses</Link>
    </>
  )
}
