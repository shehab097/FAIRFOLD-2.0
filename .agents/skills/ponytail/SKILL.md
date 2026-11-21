---
name: ponytail
description: >-
  Pragmatic, minimalist coding and token-efficiency skill (Ponytail / Phonytail).
  Use when writing, refactoring, reviewing, or planning code to prevent over-engineering,
  minimize token usage and lines of code, and enforce the Ponytail Decision Ladder.
---

# Ponytail — Minimalist Coding & Token Efficiency

> **"The best code is the code you never wrote."**

Think like a pragmatic, minimalist senior developer. Be lazy about writing new code and abstractions, **never** lazy about reading and tracing existing code.

---

## 1. Read First, Code Last

Before proposing or writing any code:
- Trace the existing code flow and conventions in the repository.
- Batch file reads, searches, and commands into parallel tool calls to avoid wasted turns and redundant context tokens.
- Do not re-read unchanged files or re-run searches already in context.

---

## 2. The Decision Ladder

Walk every task through this hierarchy in order and stop at the earliest rung that solves the problem:

1. **Does this need to exist at all? (YAGNI)**
   - Skip speculative features, premature abstractions, single-use wrappers, and unrequested configuration knobs.
2. **Is it already in this codebase?**
   - Reuse existing helpers, services, repositories, DTOs, styles, or patterns instead of creating duplicates.
3. **Does the standard library do it?**
   - Prefer built-in language APIs (`java.time`, `java.util`, standard JS/TS built-ins) over custom utilities.
4. **Does a native platform or framework feature cover it?**
   - Use built-in Spring Boot, JPA, MySQL, HTML/CSS, or browser capabilities instead of hand-rolling logic (e.g., native `<input type="date">`, native SQL/JPA queries, built-in validation annotations).
5. **Does an already-installed dependency solve it?**
   - Never add a new package/dependency for something an existing library or a few lines of code can handle.
6. **Can it be done in one line?**
   - Prefer direct, readable inline expressions over multi-class ceremonial scaffolding.
7. **Write the minimum viable code.**
   - Only when rungs 1–6 do not suffice, write the smallest, clearest code that solves the exact problem requested.

---

## 3. Anti-Bloat Rules

- **No speculative abstractions:** Do not create interfaces with only one implementation, generic factories, or base classes "for future extensibility" unless the project strictly mandates it.
- **No defensive clutter inside trusted boundaries:** Validate at system boundaries (user input, external APIs, DB constraints). Do not litter internal calls with redundant null-checks or impossible catch blocks.
- **Modify in place over creating new files:** Prefer extending an existing cohesive class/module over splintering logic across new files.
- **Concise output & token efficiency:**
  - Keep explanations brief and direct—no filler preamble or restating obvious code changes.
  - Emit only the diff/edits needed; avoid rewriting untouched sections of files.

---

## 4. Non-Negotiable Safety Rails

Minimalism never compromises correctness or safety. **Never skip:**
- Security & authentication/authorization checks
- Trust-boundary input validation & parameterized queries
- Data-loss prevention & transaction integrity
- Accessibility (a11y) & core error handling at boundaries

---

## 5. Strictness Modes

- **`lite`**: Apply the Decision Ladder to new code without challenging the user's premise; keep changes minimal.
- **`full` (Default)**: Enforce the full Decision Ladder, reject single-use abstractions, and briefly note any skipped ceremony or trade-offs at the end of the response.
- **`ultra`**: Actively challenge whether the requested feature/abstraction is needed at all before writing code; propose a zero-code or deletion-based alternative whenever possible.

