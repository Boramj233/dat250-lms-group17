// Node resolve hooks for the tests: the same imports as Vite and TypeScript accept.
// - the path aliases (@app/, @shared/, @domain/, @features/; see tsconfig.app.json)
// - imports without an extension, or of a folder with an index.ts
const srcUrl = new URL('../../src/', import.meta.url)
const ALIASES = ['app', 'shared', 'domain', 'features']

async function firstThatResolves(baseHref, context, nextResolve) {
  for (const suffix of ['', '.ts', '.tsx', '/index.ts']) {
    try {
      return await nextResolve(baseHref + suffix, context)
    } catch {
      // try the next suffix
    }
  }
  return undefined
}

export async function resolve(specifier, context, nextResolve) {
  const alias = ALIASES.find(name => specifier.startsWith(`@${name}/`))
  if (alias) {
    const target = new URL(`${alias}/${specifier.slice(alias.length + 2)}`, srcUrl)
    const found = await firstThatResolves(target.href, context, nextResolve)
    if (found) return found
  }
  const relative = specifier.startsWith('./') || specifier.startsWith('../')
  if (relative && context.parentURL?.startsWith(srcUrl.href) && !/\.[cm]?[jt]sx?$/.test(specifier)) {
    const found = await firstThatResolves(new URL(specifier, context.parentURL).href, context, nextResolve)
    if (found) return found
  }
  return nextResolve(specifier, context)
}
