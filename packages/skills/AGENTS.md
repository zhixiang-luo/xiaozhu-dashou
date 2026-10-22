# Skills Package Rules

- Keep this package platform-neutral.
- Do not import Android, network, model provider, or App-specific code.
- Schema changes require JSON Schema and Zod updates in the same change.
- Reject unknown fields and require at least one verification step.
- Every new behavior needs a failing Vitest test first.
