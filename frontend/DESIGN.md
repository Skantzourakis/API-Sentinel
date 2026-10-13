# Design

API Sentinel is a working tool: people open it to check how risky an API
specification is and what to fix. The interface serves that task first, and its
character comes from type, a committed palette and one signature element rather
than decoration.

## Principles

- **Open, not boxed.** Content sits directly on the page. Structure comes from hairline
  rules and space; there are no cards, panels or nested containers.
- **No decorative icons.** No icon tiles, emoji or illustrations. Icons appear only where
  they carry function (expand chevrons, direction of change).
- **Familiar controls.** Standard buttons, inputs, tables and text tabs.
- **Colour has a job.** Severity colours mean severity; the teal accent marks primary
  actions, selection and focus only.
- **Every state is designed.** Skeletons while loading, empty states that say what to do
  next, errors that name the problem and the recovery.
- **Inline before modal.** Creating a project, confirming a delete and uploading a file
  happen in place; files can be dropped anywhere on a project page.

## Visual language

- **Paper and ink:** warm paper working surface, ink-navy sidebar.
- **Type:** IBM Plex Sans for the interface, IBM Plex Mono only for code-like data
  (HTTP methods, paths, file names, rule ids). Numbers use tabular figures.
- **Shape:** small radii (4px) and square severity markers; nothing pill-shaped.
- **Severity:** Critical red, High orange, Medium amber, Low slate-blue. Text labels use
  the strong tone of each colour to keep contrast above 4.5:1.
- **Motion:** 150–200 ms transitions that communicate state only.

## Signature: the severity spectrum

Every scan is drawn as one squared bar of its findings, most severe first.

- **Logo:** a miniature spectrum.
- **Scan page:** a large spectrum labelled directly under each segment.
- **Comparison:** the previous and current scan as two spectra on one scale.
- **History and project lists:** bars share one scale, so their length is the number of
  findings. A project improving over time reads as bars getting shorter.

Tokens live in `src/index.css`; severity helpers in `src/lib/severity.ts`; the spectrum
components in `src/components/severity.tsx`.
