# FairFold — Design System

Version 1.0 · 12 November 2026 · Applies to all 36 pages in the Architecture file

## 1. Design brief

A modern, calm, dark interface for a product where trust matters. It should feel precise and engineered: flat surfaces, sharp edges, thin lines, strong type.

### Hard rules

| Rule | What it means |
|---|---|
| **Dark mode only** | One theme, on a dark gray background. No light theme and no theme switcher. |
| **No rounded corners** | Every radius is `0`: buttons, inputs, cards, dialogs, badges, tags, switches, avatars, tooltips, progress bars, chart bars. |
| **No gradients** | Solid colours only: no gradient backgrounds, borders, text, charts, skeleton shimmers or image overlays. |
| **No shadows** | Depth comes from surface colour steps and 1 px borders, not blur. |
| **Palette only** | The five palette colours, dark neutrals, and tints derived from them (section 2). No other hues. |
| **Colour is never alone** | Every status has a text label and an icon as well as a colour. |

### Principles

1. **Flat and structured.** A visible grid of 1 px lines and clear surface levels does the work that shadows and rounding do elsewhere.
2. **One loud thing per screen.** One purple primary action; everything else is quiet.
3. **Type carries the hierarchy.** Big, bold headings; small, spaced, uppercase labels; plain body text.
4. **Fairness is visible.** Advisory labels, rule-based labels, quote highlights and reveal warnings get clear visual treatment (section 11).
5. **Fast to build.** Few tokens, few components, one set of rules that an AI assistant can follow without guessing.

## 2. Colour

### 2.1 Source palette ("Classy Palette")

| Swatch | Hex | Name in this document | Main job |
|---|---|---|---|
| Purple | `#845EC2` | `primary` | Primary actions, selection, brand |
| Dark plum gray | `#4B4453` | `border` | Borders, dividers, track of bars, table header lines |
| Light lavender gray | `#B0A8B9` | `text-secondary` | Secondary text, icons |
| Brick red | `#C34A36` | `danger` | Destructive actions, failed and rejected states |
| Coral | `#FF8066` | `accent` | Warnings, flags, evidence highlight, error text, chart accent |

### 2.2 Tokens

Neutrals are dark grays with a very slight purple tint so they sit well with the palette. Tints are the palette colour mixed into the surface at about 18%, flattened to a solid colour (so there is no transparency and no gradient).

| Token | Hex | Use |
|---|---|---|
| `bg` | `#1C1B1F` | Page background (the dark gray) |
| `surface` | `#26242A` | Cards, tables, sidebar, top bar |
| `raised` | `#302D36` | Dialogs, menus, hovered rows, inputs on cards |
| `border` | `#4B4453` | Dividers and card borders (decorative lines) |
| `border-strong` | `#8B8396` | Input, checkbox and button outlines (these must be easy to see) |
| `text` | `#F4F2F7` | Main text (near white, never pure white on dark) |
| `text-secondary` | `#B0A8B9` | Captions, labels, helper text |
| `text-subtle` | `#9A92A5` | Placeholders, disabled text |
| `primary` | `#845EC2` | Primary button fill, active indicators |
| `primary-hover` | `#6F4BAA` | Primary button hover (darker, to keep white text readable) |
| `primary-pressed` | `#5C3D91` | Primary button pressed |
| `primary-text` | `#B79CF0` | Links, active nav text, focus outline, success text |
| `primary-subtle` | `#372E45` | Selected row, active nav background, success badge background |
| `danger` | `#C34A36` | Destructive button fill, failed or rejected badge fill |
| `danger-subtle` | `#422B2C` | Error banner background |
| `accent` | `#FF8066` | Warning text and icons, error text, flagged outline |
| `accent-subtle` | `#4D3535` | Warning badge and banner background |
| `highlight` | `#563837` | Evidence quote highlight background |
| `scrim` | `#000000` at 60% | Behind dialogs (one flat dark layer, not a gradient) |

Notes:

- The palette has no green. **Success states use `primary-text` on `primary-subtle` with a check icon.** If you later want green, add one derived token; do not change the rest.
- `primary` on `bg` is only about 3.6:1, so purple is for fills, bars and borders, never for small text. Use `primary-text` for text.

### 2.3 Contrast (checked by hand, confirm with a contrast tool)

| Pair | Ratio | Passes |
|---|---|---|
| `text` on `bg` | about 15 : 1 | AA and AAA |
| `text-secondary` on `bg` / `surface` / `raised` | about 7.6 / 6.6 / 5.9 : 1 | AA |
| `text-subtle` on `surface` | about 5.1 : 1 | AA |
| `primary-text` on `bg` / `surface` / `primary-subtle` | about 7.4 / 6.6 / 5.6 : 1 | AA |
| White on `primary` | about 4.8 : 1 | AA |
| White on `primary-hover` | about 6.5 : 1 | AA |
| White on `danger` | about 4.8 : 1 | AA |
| `accent` on `bg` / `surface` / `accent-subtle` | about 7.0 / 6.2 / 4.7 : 1 | AA |
| `bg` (dark text) on `accent` | about 7.0 : 1 | AA |
| `border-strong` on `bg` (for UI outlines) | about 4.7 : 1 | needs 3 : 1 |

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
| Corner radius | `0` everywhere. Tailwind `rounded-*` is disabled in the config (section 13). |
| Borders | 1 px. `border` colour for cards and dividers, `border-strong` for form controls and secondary buttons. |
| Depth | Three surface levels: `bg` → `surface` → `raised`. Dialogs and menus are `raised` with a 1 px `border-strong` outline on top of the `scrim`. |
| Shadows | None. |
| Spacing | 4 px base: 4, 8, 12, 16, 24, 32, 48, 64. |
| Control height | 36 (small), 40 (default), 44 (large and all touch targets on mobile). |
| Dividers | A 1 px line is preferred to extra whitespace between sections of a card. |
| Logo mark | A solid purple square with one corner cut off diagonally in coral (a "fold"). The cut is a straight line, not a curve. |

## 5. Layout

### 5.1 Shells

| Shell | Structure |
|---|---|
| **PublicShell** | Top bar (56 px, `surface`, 1 px bottom border): logo, Jobs, Log in, Register. Content up to 1100 px wide. |
| **PortalShell** | Top bar (56 px) with notification bell and account menu. Left sidebar 248 px, `surface`, 1 px right border, from 1024 px up. Below 1024 px the sidebar becomes a drawer opened by a menu button. Content up to 1280 px wide, 24 px padding (16 px on mobile). |
| **FocusShell** | For the assessment attempt (P18): no sidebar, a slim top bar with the title, the countdown timer and the question navigator. |
| **AuthShell** | Two columns from 900 px up: a left panel in `primary-subtle` with the wordmark and three short statements (Anonymised screening, Explained scores, People decide), and the form on the right. Below 900 px only the form. No illustration, no gradient. |

### 5.2 Page header

```text
Page title (h1)                                   [Secondary]  [Primary action]
Short description in text-secondary
------------------------------------------------------------------------------
```

### 5.3 Sidebar

- Item height 40 px, icon plus label, label in `text-secondary`.
- Hover: `raised` background.
- Active: `primary-subtle` background, `primary-text` label and icon, and a **3 px `primary` bar** on the left edge.
- Group labels in the 12 px uppercase style.

### 5.4 Page types

| Type | Pages | Pattern |
|---|---|---|
| Browse | P01 | Search bar and filter row on `surface`, then a list of job rows separated by 1 px lines (not cards in a grid). |
| Public detail | P02 | Two columns: description left, sticky summary and Apply button right. |
| Auth | P03 to P08 | AuthShell. |
| Dashboard | P12, P20, P31 | Row of stat blocks (4 across, 2 on tablet, 1 on mobile), each a bordered `surface` block with a big tabular number, a label and a link; below them lists. |
| List | P09, P15, P17, P22, P26, P32, P33, P34 | Page header, filter bar, table, pagination. |
| Form | P13, P21, P23, P35 | Sections as bordered blocks, each with its own Save; sticky action bar on long forms. |
| Review (split) | P25 and P16 | Content left, sticky decision panel right (stacked on mobile, actions in a sticky bottom bar). |
| Workspace | P24 | Header with lifecycle actions, tabs below. |
| Charts | P28, P36 | Filter row and bordered chart blocks, each with a "View as table" toggle. |
| Settings | P10, P29, P30 | Stacked bordered blocks. Destructive actions in a block with a `danger` top border. |
| Practice | P19 | Question on top, text area, feedback blocks, history list. |
| Upload | P14 | Dashed-border drop zone (a straight dashed line), state steps, two tabs. |
| Focus | P18 | FocusShell, one question at a time. |
| Error | P11 | Centred large code (`404`, `403`) in the large stat style, one sentence, one button. |

## 6. Components

All components have square corners. States listed are required for each.

| Component | Specification |
|---|---|
| **Button, primary** | Fill `primary`, white text, 40 px high, 16 px side padding, 600 weight. Hover `primary-hover`, pressed `primary-pressed`. Disabled: `raised` fill, `text-subtle` text. Loading: spinner replaces the icon and the width stays fixed. |
| **Button, secondary** | Transparent, 1 px `border-strong`, `text`. Hover: `raised`. |
| **Button, danger** | Fill `danger`, white text. Used only for destructive confirmations. |
| **Button, ghost** | No border, `primary-text` text, underline on hover. For tertiary actions. |
| **Input, select, textarea** | Fill `bg` (so it looks inset on `surface`), 1 px `border-strong`, `text`, placeholder `text-subtle`. Focus: border `primary-text` plus the focus outline. Error: border `accent`, message below in `accent` with an X icon. Disabled: `surface` fill, `text-subtle`. |
| **Checkbox** | 18 px square, 1 px `border-strong`; checked: `primary` fill with a white check. |
| **Radio** | 18 px square outline, with a solid `primary` inner square when selected. Radios are always inside a labelled group, so they are not confused with checkboxes. |
| **Switch** | Rectangular track 40 by 20 px, square thumb 14 px. On: `primary` track. Off: `border` track. Label text beside it says On or Off. |
| **Badge** | 24 px high, 8 px side padding, 12 px uppercase 600, icon plus label (section 7). |
| **Tabs** | Text tabs with a 2 px `primary` underline on the active tab, a 1 px `border` line under the whole row. |
| **Card / block** | `surface`, 1 px `border`, 24 px padding (16 on mobile). No shadow. |
| **Table** | Header row on `bg`, 12 px uppercase labels in `text-secondary`, 1 px lines between rows, row height 48, hover row `raised`, selected row `primary-subtle`. Numbers right-aligned with tabular figures. Sticky header. |
| **Pagination** | Square buttons, current page with `primary` fill. |
| **Dialog** | `raised`, 1 px `border-strong`, max width 520 px (720 for large forms), 24 px padding, on the `scrim`. Title h2, actions right-aligned (primary on the right). Focus is trapped. |
| **Toast** | `raised`, 1 px `border-strong`, 4 px left bar in the status colour, icon plus text, bottom right on desktop and top on mobile. Auto-dismiss after 6 seconds except errors. |
| **Banner** | Full-width block with a 4 px left bar: info (`primary` bar on `surface`), warning (`accent` bar on `accent-subtle`), error (`danger` bar on `danger-subtle`). Always icon plus text. |
| **Tooltip** | `raised`, 1 px `border-strong`, 12 px text. Never the only place for essential information. |
| **Score meter** | A number with tabular figures (for example `82`) and a horizontal bar 8 px high: track `border`, fill `primary`, no rounding, no ring or dial. A marker line shows the job's cut-off score when one exists. |
| **Progress bar** | Same as the score bar, with counts as text. |
| **Skeleton** | Flat `raised` blocks with a slow opacity pulse (1.2 s). No shimmer sweep, because a sweep is a gradient. |
| **Empty and error states** | A square 48 px icon block in `raised`, one sentence, one action. |
| **Avatar / initials** | Square, 32 px, `primary-subtle` fill, `primary-text` initials. |
| **File drop zone** | `surface`, 1 px dashed `border-strong`. Drag-over: `primary-subtle` fill and `primary-text` border. |
| **Evidence quote** | Inside the resume text: `highlight` background, 2 px `accent` underline. In the claim list: a block with a 3 px `accent` left bar. |
| **Chip / tag** | Same as badge but neutral: `raised` fill, 1 px `border`, `text-secondary`. |

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

### 13.1 CSS variables

```css
/* styles/tokens.css */
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
}
html { background: var(--bg); color: var(--text); font-family: var(--font-sans); font-variant-numeric: tabular-nums; }
:focus-visible { outline: 2px solid var(--primary-text); outline-offset: 2px; }
@media (prefers-reduced-motion: reduce) { * { transition: none !important; animation: none !important; } }
```

### 13.2 Tailwind config

```ts
// tailwind.config.ts
import type { Config } from 'tailwindcss';
const zero = '0px';
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    borderRadius: { none: zero, DEFAULT: zero, sm: zero, md: zero, lg: zero, xl: zero, '2xl': zero, '3xl': zero, full: zero },
    boxShadow: { none: 'none' },
    backgroundImage: {},                       // removes every bg-gradient-* utility
    colors: {
      transparent: 'transparent', current: 'currentColor',
      bg: 'var(--bg)', surface: 'var(--surface)', raised: 'var(--raised)',
      border: 'var(--border)', 'border-strong': 'var(--border-strong)',
      text: 'var(--text)', 'text-secondary': 'var(--text-secondary)', 'text-subtle': 'var(--text-subtle)',
      primary: 'var(--primary)', 'primary-hover': 'var(--primary-hover)', 'primary-pressed': 'var(--primary-pressed)',
      'primary-text': 'var(--primary-text)', 'primary-subtle': 'var(--primary-subtle)',
      danger: 'var(--danger)', 'danger-subtle': 'var(--danger-subtle)',
      accent: 'var(--accent)', 'accent-subtle': 'var(--accent-subtle)', highlight: 'var(--highlight)',
    },
    fontFamily: { sans: ['var(--font-sans)'] },   // the only font family
  },
} satisfies Config;
```

Because the `colors` key is replaced (not extended), only the tokens above exist, so an off-palette colour class simply does not compile.

### 13.3 Enforcement script

AI assistants often add `rounded-lg`, `shadow-md` or a gradient out of habit. This script fails the commit when that happens.

```bash
#!/usr/bin/env bash
# scripts/check-design.sh  (run before every commit and in CI)
set -u
cd "$(dirname "$0")/../frontend/src" || exit 1

bad=$(grep -rnE "rounded|gradient|shadow|border-radius|box-shadow" . \
        --include=*.ts --include=*.tsx --include=*.css \
      | grep -vE "rounded-none|border-radius: *0|borderRadius *(=|:) *0" || true)

hex=$(grep -rnE "#[0-9A-Fa-f]{6}\b" . --include=*.ts --include=*.tsx --include=*.css \
      | grep -vE "styles/tokens.css|charts/defaults.ts" || true)

if [ -n "$bad" ] || [ -n "$hex" ]; then
  [ -n "$bad" ] && { echo "Forbidden radius, gradient or shadow:"; echo "$bad"; }
  [ -n "$hex" ] && { echo "Colours must come from tokens:"; echo "$hex"; }
  exit 1
fi
echo "design check passed"
```

### 13.4 Component folder

```text
src/components/ui/
  Button.tsx  Input.tsx  Select.tsx  Textarea.tsx  Checkbox.tsx  Radio.tsx  Switch.tsx
  Badge.tsx   Tabs.tsx   Card.tsx    Table.tsx     Pagination.tsx
  Dialog.tsx  Toast.tsx  Banner.tsx  Tooltip.tsx   Skeleton.tsx  ProgressBar.tsx
  Icon.tsx    (wraps lucide with square caps and miter joins)
  statusMap.ts (section 7 as data: status -> style, icon, label)
```

Build order for chunk 1: Button, Input, Select, Table, Badge, Dialog, Toast, then the rest as pages need them.

### 13.5 Rules for AI-written UI code

Give the AI these lines together with the page spec:

1. Use only classes from the Tailwind config in this file. No `rounded-*`, `shadow-*`, gradient or arbitrary colour classes.
2. Every status uses `statusMap.ts` with icon and label.
3. Every button, input and dialog is the shared component, never a raw element with custom styles.
4. Every list, table and form has loading, empty and error states.
5. Run `scripts/check-design.sh` and fix everything it reports.

## 14. Checklist before a page is done

- [ ] No rounded corners, gradients or shadows (the script passes)
- [ ] Only token colours
- [ ] Heading hierarchy is h1, then h2
- [ ] All statuses have icon and text
- [ ] Keyboard path works and focus is visible
- [ ] Loading, empty and error states exist
- [ ] Works at 360 px width and at 1280 px width
- [ ] Tables have a mobile layout or a scroll container
- [ ] Fairness patterns from section 11 are present where the page needs them
