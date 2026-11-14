import { Link } from 'react-router'
import { paths } from '../../../app/paths'

export default function NotFoundPage() {
  return (
    <>
      <h1>Page not found</h1>
      <Link to={paths.home}>Go to home</Link>
    </>
  )
}
