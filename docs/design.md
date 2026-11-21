# FairFold — Design System

Version 1.1 · Updated November 2026 · Applies across the entire application

## 1. Design brief

A modern, clean, user-friendly interface for an evidence-based hiring platform. Designed for both Dark Mode and Light Mode with refined typography, accessible contrast, and smooth rounded corners.

### Hard rules

| Rule | What it means |
|---|---|
| **Dark & Light mode support** | Seamless dual-theme support with instant toggle and system preference detection via CSS tokens. |
| **Rounded corners across the UI** | Modern rounded corners: 6 px (`rounded-sm`) on inputs/buttons, 8-12 px (`rounded`/`rounded-lg`) on controls and chips, 12-16 px (`rounded-xl`) on cards, panels and dialogs, and `rounded-full` for badges/pills. |
| **No heavy gradients** | Solid, clean colours only: no aggressive gradient backgrounds, borders, or flashy image overlays. |
| **Subtle depth & elevation** | Depth comes from surface contrast steps, refined border lines, and subtle shadows. |
| **Classy Palette** | The core palette colours, clean neutrals for dark & light modes, and tints derived from them (section 2). |
| **Colour is never alone** | Every status has a text label and an icon as well as a colour. |

### Principles

1. **Clean and user-friendly.** Refined surface contrast and rounded containers make information easy to digest.
2. **One loud thing per screen.** One purple primary action; everything else is quiet.
3. **Type carries the hierarchy.** Big, bold headings; small, spaced, uppercase labels; plain body text.
4. **Fairness is visible.** Advisory labels, rule-based labels, quote highlights and reveal warnings get clear visual treatment (section 11).
5. **Fast and consistent.** Tokens, atomic components, and theme switcher working seamlessly together.

## 2. Colour

### 2.1 Source palette ("Classy Palette")

| Swatch | Hex | Name in this document | Main job |
|---|---|---|---|
| Purple | `#845EC2` | `primary` | Primary actions, selection, brand |
| Dark plum gray | `#4B4453` | `border` | Borders, dividers, track of bars |
| Light lavender gray | `#B0A8B9` | `text-secondary` | Secondary text, icons |
| Brick red | `#C34A36` | `danger` | Destructive actions, failed and rejected states |
| Coral | `#FF8066` | `accent` | Warnings, flags, evidence highlight, error text |

### 2.2 Tokens (Dark & Light Mode)

| Token | Dark Mode | Light Mode | Use |
|---|---|---|---|
| `bg` | `#1C1B1F` | `#F7F6F9` | Page background |
| `surface` | `#26242A` | `#FFFFFF` | Cards, tables, sidebar, top bar |
| `raised` | `#302D36` | `#F0EDF5` | Dialogs, menus, hovered rows, inputs on cards |
| `border` | `#4B4453` | `#E3DFE8` | Dividers and card borders |
| `border-strong` | `#8B8396` | `#BCB4C6` | Input, checkbox and button outlines |
| `text` | `#F4F2F7` | `#191620` | Main text (high contrast) |
| `text-secondary` | `#B0A8B9` | `#5B5365` | Captions, labels, helper text |
| `text-subtle` | `#9A92A5` | `#898094` | Placeholders, disabled text |
| `primary` | `#845EC2` | `#845EC2` | Primary button fill, active indicators |
| `primary-hover` | `#6F4BAA` | `#714BB0` | Primary button hover |
| `primary-pressed` | `#5C3D91` | `#5C3996` | Primary button pressed |
| `primary-text` | `#B79CF0` | `#6E3CAE` | Links, active nav text, focus outline (>5:1 contrast) |
| `primary-subtle` | `#372E45` | `#F1ECF9` | Selected row, active nav background, badge background |
| `danger` | `#C34A36` | `#C34A36` | Destructive button fill, failed or rejected badge fill |
| `danger-subtle` | `#422B2C` | `#FCEEED` | Error banner background |
| `accent` | `#FF8066` | `#D65135` | Warning text and icons, error text, flagged outline |
| `accent-subtle` | `#4D3535` | `#FDF0ED` | Warning badge and banner background |
| `highlight` | `#563837` | `#FEEAE4` | Evidence quote highlight background |
| `scrim` | `rgba(0,0,0,0.6)` | `rgba(25,22,32,0.45)`| Behind dialogs and drawers |

Notes:

- The palette has no green. **Success states use `primary-text` on `primary-subtle` with a check icon.** If you later want green, add one derived token; do not change the rest.
- `primary` on dark `bg` is about 3.6:1, so purple is for fills, bars and borders, never for small text. Use `primary-text` (`#B79CF0` in dark mode, `#6E3CAE` in light mode) for text and links.
- `white` is fixed to `#FFFFFF` in Tailwind so `text-white` on `bg-primary` and `bg-danger` buttons/badges remains crisp white in both themes.

### 2.3 Contrast (checked for both Dark & Light modes)

| Pair | Dark Mode Ratio | Light Mode Ratio | Passes |
|---|---|---|---|
| `text` on `bg` / `surface` | ~15 : 1 | ~15.5 : 1 | AA and AAA |
| `text-secondary` on `bg` / `surface` / `raised` | ~7.6 / 6.6 / 5.9 : 1 | ~7.1 / 7.5 / 6.6 : 1 | AA |
| `text-subtle` on `surface` | ~5.1 : 1 | ~3.9 : 1 | AA (placeholders/captions) |
| `primary-text` on `bg` / `surface` / `primary-subtle` | ~7.4 / 6.6 / 5.6 : 1 | ~5.8 / 6.2 / 5.4 : 1 | AA |
| White (`#FFFFFF`) on `primary` | ~4.8 : 1 | ~4.8 : 1 | AA |
| White (`#FFFFFF`) on `primary-hover` | ~6.5 : 1 | ~6.1 : 1 | AA |
| White (`#FFFFFF`) on `danger` | ~4.8 : 1 | ~4.8 : 1 | AA |
| `accent` on `bg` / `surface` / `accent-subtle` | ~7.0 / 6.2 / 4.7 : 1 | ~4.6 / 4.9 / 4.5 : 1 | AA |
| `border-strong` on `bg` / `surface` (for UI outlines) | ~4.7 : 1 | ~3.2 : 1 | Passes 3 : 1 UI contrast |

## 3. Typography

**One font family for everything: Inter**, a clean, modern sans-serif that stays sharp and readable on dark backgrounds. No serif and no monospace fonts anywhere. Numbers, scores and aliases use the same font with tabular figures, so digits line up in columns.

| Use | Weight | Size / line height | Notes |
|---|---|---|---|
| Headings h1 | 700 | 28/36 | Letter-spacing -0.01em |
| Headings h2, h3 | 600 | h2 20/28, h3 16/24 | |
| Wordmark | 700 | 20/24 | |
| Body, forms, tables | 400 (500 for emphasis) | 14/22 | 16/24 for inputs on mobile |
| Labels and table headers | 600 | 12/16 | Uppercase, letter-spacing 0.06em |
| Numbers (scores, counts, table figures) and aliases (`C-7H2X9`) | 500 | 14/22 | `font-variant-numeric: tabular-nums` |
| Large stat numbers (dashboards, cost estimate, error code) | 700 | 32/40 | Tabular figures |

- Font: `@fontsource-variable/inter`, self-hosted (no external font requests, which also helps the Docker build). Fallback: `system-ui, sans-serif`.
- Want a different look later? Change the single `--font-sans` variable (section 13.1) and the package. Good alternatives with the same dark-mode behaviour: Manrope, DM Sans, Plus Jakarta Sans.
- Never go below 12 px. In dark mode avoid weights under 400.
- Maximum line length for reading text (resume, descriptions, messages): about 70 characters.

## 4. Shape, borders, depth, spacing

| Item | Rule |
|---|---|
| Corner radius | Smooth rounded corners across the UI: `xs: 4px`, `sm: 6px`, `DEFAULT: 8px`, `md: 10px` (chips/tags), `lg: 12px` (buttons, inputs, selects, nav items), `xl: 16px` (cards, tables, panels, banners, markdown editor), `2xl: 20px` (dialogs, large avatars), `full: 9999px` (status badges, notification count pills). |
| Borders | 1 px. `border` colour for cards and section dividers (`divide-y divide-border`), `border-strong` for secondary buttons and dialog outlines. Avoid nested bordered boxes inside cards — use clean spacing and horizontal dividers instead. |
| Depth | Three surface levels (`bg` → `surface` → `raised`) paired with subtle elevation (`shadow-sm` on cards/tables, `shadow-lg`/`shadow-xl` on toasts, drawers and dialogs). |
| Spacing | 4 px base: 4, 8, 12, 16, 24, 32, 48, 64. |
| Control height | 36 (small), 40 (default), 44 (large and all touch targets on mobile). |
| Dividers | Use `divide-y divide-border` inside a single rounded `surface` container rather than wrapping every field or section in its own box. |
| Logo mark | A rounded purple square (`rx="7"`) with a folded coral top-right corner (`fill-accent` + `fill-primary-pressed` crease) and a crisp geometric `F` monogram (`components/ui/Logo.tsx` and `public/favicon.svg`). |

## 5. Layout

### 5.1 Shells

| Shell | Structure |
|---|---|
| **PublicShell** | For guests: sticky top bar (56 px, `surface`, 1 px bottom border) with Logo, Browse jobs link, `ThemeToggle`, Log in, and Register buttons. **When a user is signed in, `PublicShell` delegates to `PortalShell`** so the left sidebar and top bar remain visible on public routes like Browse Jobs (`/`). |
| **PortalShell** | Sticky top bar (56 px) with `ThemeToggle`, notification bell (with rounded pill unread badge) and account profile chip. Sticky left sidebar (`240 px`, `lg:sticky lg:top-0 lg:h-screen`, `surface`, 1 px right border) from 1024 px up so it never scrolls out of view; drawer below 1024 px. Sign-out and Change Password live inside the Account Profile page (`/account` and `/account/password`), keeping the navigation uncluttered. |
| **FocusShell** | For the assessment attempt (P18): no sidebar, a slim top bar with the title, the countdown timer and the question navigator. |
| **AuthShell** | Two columns from 768 px up: a left panel in `primary-subtle` with the wordmark and three rounded pillar cards (Anonymised screening, Explained scores, People decide), and a `rounded-2xl` form card on the right with a floating `ThemeToggle` in the top-right corner. |

### 5.2 Page header and block structure

```text
Page title (h1)                                   [Secondary]  [Primary action]
Short description in text-secondary
```

- **Minimalist containers:** Avoid excessive boxes and heavy borders. Group related content into a single `rounded-xl border border-border bg-surface p-6 sm:p-8 shadow-sm` card divided by `divide-y divide-border`.
- **Clean definition grids:** Read-only profile and company views use a clean multi-column `<dl>` grid (`grid gap-y-5 gap-x-8 sm:grid-cols-2 lg:grid-cols-3`) with a 12 px `text-secondary` label and a 14 px `font-medium text-text` value (`Not specified` in `text-subtle` when empty).

### 5.3 Sidebar

- Sticky full-height (`lg:sticky lg:top-0 lg:h-screen`) with independent `overflow-y-auto` on the nav list and a pinned account footer linking to `/account`.
- Item height 40 px (`h-10`), `rounded-lg`, icon plus label, label in `text-secondary`.
- Hover: `raised` background, `text` colour.
- Active: `primary-subtle` background, `primary-text` font-semibold label and icon.

### 5.4 Page types

| Type | Pages | Pattern |
|---|---|---|
| Browse | P01 | Two-column layout (`lg:grid-cols-[minmax(0,1fr)_280px]`): job results list on the left inside a `rounded-xl` `divide-y` card, and a sticky search & filter sidebar (`lg:sticky lg:top-20`) on the right. Active filter chips appear above results with a quick "Clear filters" action. |
| Public detail | P02 | Two columns: Markdown job description/requirements and skill chips on the left, sticky summary and Apply card on the right. |
| Auth | P03 to P08 | AuthShell with `ThemeToggle`. |
| Dashboard | P12, P20, P31 | Row of stat blocks (4 across, 2 on tablet, 1 on mobile), each a rounded `surface` block with a big tabular number, a label and a link; below them lists. |
| List | P09, P15, P17, P22, P26, P32, P33, P34 | Page header, filter bar, `rounded-xl` table, pagination. |
| Form | P13, P21, P23, P35 | Overview tab + Edit tab on profile/company pages; MarkdownEditor with formatting toolbar and live preview for rich text fields. |
| Review (split) | P25 and P16 | Content left, sticky decision panel right (stacked on mobile, actions in a sticky bottom bar). |
| Workspace | P24 | Header with lifecycle actions, Overview tab (Markdown description/requirements + summary sidebar) and Applicants tab with count badge. |
| Charts | P28, P36 | Filter row and bordered chart blocks, each with a "View as table" toggle. |
| Settings | P10, P29, P30 | `/account` shows full user details, role snapshot (Candidate or Employer Company), Appearance & Theme selector (Light / Dark / System), Sign Out button, and link to `/account/password` (dedicated Change Password page). |
| Practice | P19 | Question on top, text area, feedback blocks, history list. |
| Upload | P14 | `rounded-xl` dashed-border drop zone, current resume card with inline PDF iframe preview (`Preview` / `Hide preview`) and `New tab` viewer (`GET /api/candidate/resume/file`). |
| Focus | P18 | FocusShell, one question at a time. |
| Error | P11 | Centred large code (`404`, `403`) in the large stat style, one sentence, one button. |

## 6. Components

All components use the rounded corner scale and adapt automatically to Dark and Light modes via CSS variables.

| Component | Specification |
|---|---|
| **Button, primary** | `rounded-lg`, fill `primary`, white text, 40 px high, 16 px side padding, 600 weight. Hover `primary-hover`, pressed `primary-pressed`. Disabled: `raised` fill, `text-subtle` text. Loading: spinner. |
| **Button, secondary** | `rounded-lg`, transparent, 1 px `border-strong`, `text`. Hover: `raised`. |
| **Button, danger** | `rounded-lg`, fill `danger`, white text. Used only for destructive confirmations. |
| **Button, ghost** | `rounded-lg`, no border, `primary-text` text, underline on hover. For tertiary actions. |
| **ThemeToggle** | `rounded-lg`, 36 px high (`h-9`), 1 px `border`, `surface` fill, `Sun` / `Moon` icon (optional label). Toggles between Dark and Light mode and persists to `localStorage` (`fairfold_theme`). |
| **Input, select, textarea** | `rounded-lg`, fill `bg`, 1 px `border`, `text`, placeholder `text-subtle`. Focus: border `primary`. Error: border `accent`. Disabled: `surface` fill, `text-subtle`. |
| **MarkdownEditor** | `rounded-xl`, toolbar on `bg` with quick formatting buttons (`rounded-md`: Heading, Bold, Italic, Code, Link, Bullet list, Numbered list, Quote, Horizontal rule) + side-by-side textarea and live `Markdown` preview on `surface`. |
| **Checkbox** | 18 px `rounded` square, 1 px `border-strong`; checked: `primary` fill with a white check. |
| **Radio** | 18 px circle/rounded outline, with a solid `primary` inner dot when selected. |
| **Switch** | Rounded track 40 by 20 px, thumb 14 px. On: `primary` track. Off: `border` track. |
| **Badge** | `rounded-full` pill, 24 px high, 10 px side padding (`px-2.5`), 12 px uppercase 600, icon plus label (section 7). |
| **Tabs** | Text tabs with a 2 px `primary` underline on the active tab, a 1 px `border` line under the whole row. |
| **Card / block** | `rounded-xl`, `surface`, 1 px `border`, 24 px padding, `shadow-sm`. |
| **Table** | `rounded-xl` container with `shadow-sm`, header row on `bg`, 12 px uppercase labels in `text-secondary`, 1 px lines between rows, row height 48, hover row `raised`, selected row `primary-subtle`. |
| **Pagination** | `rounded-lg` buttons, current page with `primary` fill. |
| **Dialog** | `rounded-2xl`, `raised`, 1 px `border-strong`, `shadow-xl`, max width 520 px (720 for large forms), 24 px padding, on the `scrim`. |
| **Toast** | `rounded-xl`, `raised`, 1 px `border-strong`, 4 px left bar in the status colour, `shadow-lg`, icon plus text, bottom right. |
| **Banner** | `rounded-xl`, full-width block with a 4 px left bar and `shadow-sm`: info (`primary` bar on `surface`), warning (`accent` bar on `accent-subtle`), error (`danger` bar on `danger-subtle`). Always icon plus text. |
| **Avatar / initials** | `rounded-lg` (small, 32-36 px) or `rounded-2xl` (profile headers, 56 px), `primary-subtle` fill, `primary-text` bold initials. |
| **File drop zone** | `rounded-xl`, `surface`, 1 px dashed `border`, `shadow-sm`. Drag-over: `primary-subtle` fill and `primary-text` border. |
| **Chip / tag** | `rounded-md`, `raised` or `bg` fill, `text-secondary` or `text`. |

### Focus

Every interactive element shows a **2 px solid `primary-text` outline with a 2 px offset** on keyboard focus. The outline is square, like everything else. Never remove it.

## 7. Status colours and icons

Badges are square. "Subtle" means the tint background with the text colour; "solid" means the solid fill with white text.

| Meaning | Style | Icon (lucide) | Used for |
|---|---|---|---|
| Neutral | `raised` fill, 1 px `border`, `text-secondary` | `Minus` | Draft, Applied, Not required (None), Not matched |
| Active / in progress | `primary-subtle` fill, `primary-text` | `Clock` or `Loader` | Screening, Processing, Interview, Pending (gate) |
| Success | `primary-subtle` fill, `primary-text`, check icon | `Check` | Active (job), Screened, Shortlisted, Passed, Ready, Hired, Clear, Met, Verified |
| Warning | `accent-subtle` fill, `accent` text | `TriangleAlert` | Paused, Partial, Flagged, Pending retake, Rule-based |
| Failure | `danger` fill, white text | `X` | Failed, Rejected, Broken link, Not met |
| Closed | `raised` fill, `text-subtle`, 1 px `border` | `Lock` | Closed |

Because success and "in progress" share the purple family, the **icon and text are what tell them apart**; never rely on the colour.

## 8. Data visualisation

- Charts use `react-chartjs-2` with solid fills only, no gradient fills, no rounded bars.
- Series order: `#845EC2`, `#FF8066`, `#B0A8B9`, `#C34A36`. With more than one series, also vary the point shape (`rect`, `triangle`, `rectRot`, `cross`) and the line dash, so charts work without colour.
- Gridlines `#4B4453`, tick and legend text `#B0A8B9`, tooltip `raised` with a 1 px border.
- Lines are straight (no curve smoothing), 2 px wide, with square points.
- Every chart has the "View as table" toggle (the table is the accessible version).
- The career timeline (P36) is drawn with square bars on a time axis.
- The funnel (P28) is a horizontal bar chart: one bar per stage with the count at the end.

```ts
// charts/defaults.ts
import { Chart } from 'chart.js';
Chart.defaults.color = '#B0A8B9';
Chart.defaults.borderColor = '#4B4453';
Chart.defaults.font.family = "'Inter Variable', Inter, system-ui, sans-serif";
Chart.defaults.elements.bar.borderRadius = 0;
Chart.defaults.elements.line.tension = 0;
Chart.defaults.elements.line.borderWidth = 2;
Chart.defaults.elements.point.pointStyle = 'rect';
Chart.defaults.elements.point.radius = 4;
export const SERIES = ['#845EC2', '#FF8066', '#B0A8B9', '#C34A36'];
```

## 9. Motion

- Only colour, opacity and position changes, 120 to 160 ms, `ease-out`. No scaling, bouncing or sliding more than 8 px.
- Drawers and dialogs fade in; the drawer may slide 16 px.
- Respect `prefers-reduced-motion: reduce` by turning all transitions off.
- The assessment timer updates every second but is announced to screen readers only at 5 minutes, 1 minute and zero.

## 10. Icons and imagery

- `lucide-react`, 20 px in controls, 16 px inside badges. Set `strokeLinecap="square"` and `strokeLinejoin="miter"` globally so icon corners are sharp too (wrap the icons in one `Icon` component that sets this).
- Icons are `text-secondary` by default and take the text colour of their context.
- No stock photos, illustrations or avatars from images. Candidates have no photos anywhere (this is part of the product's fairness).
- Company logos (if added later) show in a square frame.

## 11. Fairness patterns

| Pattern | Visual treatment |
|---|---|
| Advisory banner ("Scores are advisory, you decide") | Info banner at the top of every ranked list, always visible. |
| Rule-based result | Warning badge "Rule-based" next to the score, with a banner on the page. |
| Alias | Tabular-figure text (for example `C-7H2X9`) in a neutral chip. Never a name or initials. |
| Evidence | Quotes with the `highlight` background and the coral underline; each claim in its own block with the quote and a verdict badge. |
| Bias flagged | Warning badge and a banner saying which claim was removed and that manual review is advised. |
| Identity reveal dialog | `raised` dialog with a warning banner: "The candidate's name and contact details will be shown and this will be logged." Primary button "Shortlist and reveal". |
| Override reason | Dialog with the score and cut-off shown as the score meter with its marker, and a required text area. The button stays disabled until the reason has enough text. |
| Cost estimate | A dialog with the number of applications and the cost as large tabular numbers, and the mode (AI or rule-based) as a badge. |
| Proctoring signals | Neutral chips ("Left the tab 3 times, 1 min 20 s") on `raised`, never in warning or failure colours, with the label "Does not affect the score". |
| Private practice | A neutral note at the top of P19: "Practice is private and never affects your applications." |

## 12. Accessibility

Target WCAG 2.1 AA.

- Contrast as in section 2.3. Check any new colour pair before use.
- The focus outline is always visible (section 6).
- Status is always text plus icon plus colour.
- Touch targets at least 44 by 44 px on mobile.
- Dark mode specifics: no pure white text (`text` is `#F4F2F7`), no pure black backgrounds, set `color-scheme: dark` so scrollbars and native controls match.
- Respect `prefers-reduced-motion`.
- Forms: visible labels, errors in text under the field, focus moves to the first invalid field.
- Charts have a table version. Meaningful icons have text labels nearby.

## 13. Implementation

### 13.1 CSS variables (`styles/tokens.css`)

```css
:root {
  color-scheme: dark;
  --font-sans: 'Inter Variable', Inter, system-ui, sans-serif;
  --bg: #1C1B1F;           --surface: #26242A;       --raised: #302D36;
  --border: #4B4453;       --border-strong: #8B8396;
  --text: #F4F2F7;         --text-secondary: #B0A8B9; --text-subtle: #9A92A5;
  --primary: #845EC2;      --primary-hover: #6F4BAA;  --primary-pressed: #5C3D91;
  --primary-text: #B79CF0; --primary-subtle: #372E45;
  --danger: #C34A36;       --danger-subtle: #422B2C;
  --accent: #FF8066;       --accent-subtle: #4D3535;  --highlight: #563837;
  --scrim: rgba(0, 0, 0, 0.6);
  --white: #FFFFFF;        --black: #000000;
  --radius-xs: 4px;        --radius-sm: 6px;         --radius: 10px;
  --radius-lg: 14px;       --radius-xl: 18px;        --radius-full: 9999px;
}

:root.light,
html.light,
[data-theme="light"] {
  color-scheme: light;
  --bg: #F7F6F9;           --surface: #FFFFFF;       --raised: #F0EDF5;
  --border: #E3DFE8;       --border-strong: #BCB4C6;
  --text: #191620;         --text-secondary: #5B5365; --text-subtle: #898094;
  --primary: #845EC2;      --primary-hover: #714BB0;  --primary-pressed: #5C3996;
  --primary-text: #6E3CAE; --primary-subtle: #F1ECF9;
  --danger: #C34A36;       --danger-subtle: #FCEEED;
  --accent: #D65135;       --accent-subtle: #FDF0ED;  --highlight: #FEEAE4;
  --scrim: rgba(25, 22, 32, 0.45);
}
```

### 13.2 Tailwind config (`tailwind.config.ts`)

```ts
import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: ['class'],
  theme: {
    borderRadius: {
      none: '0px', xs: '4px', sm: '6px', DEFAULT: '8px', md: '10px',
      lg: '12px', xl: '16px', '2xl': '20px', '3xl': '24px', full: '9999px',
    },
    boxShadow: {
      none: 'none',
      sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
      DEFAULT: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
      md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
      lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
    },
    backgroundImage: {},
    colors: {
      transparent: 'transparent', current: 'currentColor',
      white: '#FFFFFF', black: '#000000',
      bg: 'var(--bg)', surface: 'var(--surface)', raised: 'var(--raised)', scrim: 'var(--scrim)',
      border: 'var(--border)', 'border-strong': 'var(--border-strong)',
      text: 'var(--text)', 'text-secondary': 'var(--text-secondary)', 'text-subtle': 'var(--text-subtle)',
      primary: 'var(--primary)', 'primary-hover': 'var(--primary-hover)', 'primary-pressed': 'var(--primary-pressed)',
      'primary-text': 'var(--primary-text)', 'primary-subtle': 'var(--primary-subtle)',
      danger: 'var(--danger)', 'danger-subtle': 'var(--danger-subtle)',
      accent: 'var(--accent)', 'accent-subtle': 'var(--accent-subtle)', highlight: 'var(--highlight)',
    },
    fontFamily: { sans: ['var(--font-sans)'] },
  },
} satisfies Config;
```

### 13.3 Enforcement script (`scripts/check-design.sh`)

Ensures no gradient utilities or raw off-token hex colours are introduced in components.

```bash
#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/../frontend/src" || exit 1

bad=$(grep -rnE "bg-gradient-" . --include=*.ts --include=*.tsx --include=*.css || true)

hex=$(grep -rnE "#[0-9A-Fa-f]{6}\b" . --include=*.ts --include=*.tsx --include=*.css \
      | grep -vE "styles/tokens.css|charts/defaults.ts" || true)

if [ -n "$bad" ] || [ -n "$hex" ]; then
  [ -n "$bad" ] && { echo "Forbidden gradient:"; echo "$bad"; }
  [ -n "$hex" ] && { echo "Colours must come from tokens:"; echo "$hex"; }
  exit 1
fi
echo "design check passed"
```

### 13.4 Component folder

```text
src/components/ui/
  Button.tsx  Input.tsx  Select.tsx  Textarea.tsx  Checkbox.tsx  ThemeToggle.tsx
  Badge.tsx   Tabs.tsx   Card.tsx    Table.tsx     Logo.tsx
  Dialog.tsx  Toast.tsx  Banner.tsx  Skeleton.tsx
  Markdown.tsx  MarkdownEditor.tsx   Icon.tsx
```

## 14. Checklist before a page is done

- [ ] Works seamlessly in both Dark Mode and Light Mode
- [ ] Consistent rounded corners (`rounded-lg` controls, `rounded-xl` cards/tables, `rounded-full` badges)
- [ ] Only token colours (no raw hex in components; `scripts/check-design.sh` passes)
- [ ] Heading hierarchy is h1, then h2
- [ ] All statuses have icon and text
- [ ] Keyboard path works and focus is visible
- [ ] Loading, empty and error states exist
- [ ] Works at 360 px width and at 1280 px width
- [ ] Tables have a mobile layout or a scroll container
- [ ] Fairness patterns from section 11 are present where the page needs them
