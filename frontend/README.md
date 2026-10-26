# Frontend architecture

React and TypeScript, organized by domain to align with the backend.

- `src/app/`: application entry point, root component, and shared layout. Add routing here when needed.
- `src/features/`: domain-specific code (`auth`, `course`, `module`, `content`, `submission`). Each feature contains:
  - `pages/`: full pages.
  - `components/`: components used within the feature.
  - `api/`: backend calls for the feature.
- `src/shared/`: components and API utilities used across features.
- `src/styles/`: primitive tokens, semantic tokens, and global styles.
- `src/assets/`: images and other imported assets.

Keep component CSS next to its component. Use existing style tokens where possible. Empty folders guide placement; only add files when needed.
