---
name: Praja Disha Governance System
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434655'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#784b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#996100'
  on-tertiary-container: '#ffeedd'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
  mono-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  container-padding: 24px
  gutter: 16px
  row-height-dense: 32px
  row-height-standard: 48px
---

## Brand & Style

The design system is engineered for high-stakes governance and enterprise resource management. The brand personality is **authoritative, transparent, and technologically advanced**, prioritizing data integrity over decorative flair. 

The aesthetic leans into **Modern Corporate Minimalism** with a focus on high information density and cognitive efficiency. It utilizes a structured "utility-first" visual language characterized by crisp geometry, a restrained color palette, and clear semantic signaling. The goal is to evoke a sense of absolute reliability and systemic order, ensuring that AI-generated insights are presented with the gravitas required for public sector decision-making.

## Colors

The palette is anchored in a professional **Slate Gray** spectrum to provide a neutral, low-strain backdrop for complex data. 

- **Primary (Blue):** Used exclusively for primary actions and active states.
- **Secondary (Slate):** Handles metadata, borders, and non-critical navigation elements.
- **Semantic Highlights:** These are strictly reserved for status communication. **Red (P0/Critical)** and **Amber (Review)** use high-chroma variants to ensure they stand out against the neutral base, while **Green (Resolved)** provides a calm, distinct confirmation of success.
- **Borders:** Subtle `slate-200` for light mode and `slate-700` for dark mode provide structural definition without adding visual noise.

## Typography

This design system utilizes **Inter** for all UI elements to maximize legibility at small sizes. The scale is intentionally tight to support high-density layouts.

- **Data Density:** Use `body-sm` (13px) for primary table content and `label-md` for headers to maximize visible rows.
- **Hierarchy:** Bold weights (600-700) are used sparingly to highlight critical status or navigation headings.
- **Monospaced Content:** AI-generated IDs, timestamps, or technical logs should use a monospaced font for character differentiation and alignment.

## Layout & Spacing

The layout follows a **Fixed-Fluid Hybrid** model. Navigation and sidebars are fixed-width to maintain consistent control areas, while the main content area is a fluid grid that maximizes the use of wide-screen monitors common in governance centers.

- **8pt Grid System:** All margins and paddings are multiples of 4px/8px to ensure mathematical alignment.
- **Information Density:** For data-heavy views, use a "compact" mode where vertical padding is reduced by 50%.
- **Structure:** Use a 12-column grid for dashboard layouts. In nested tree views, use a consistent 16px indentation per level.

## Elevation & Depth

Depth in this design system is primarily conveyed through **Tonal Layering and Low-Contrast Outlines** rather than heavy shadows.

- **Z-Axis Hierarchy:**
  - **Level 0 (Background):** `slate-50` or `slate-100`.
  - **Level 1 (Cards/Surface):** White background with a 1px border of `slate-200`.
  - **Level 2 (Popovers/Dropdowns):** White background with a subtle, tight shadow (`0 4px 6px -1px rgb(0 0 0 / 0.1)`) and a `slate-300` border.
- **Active State:** Elements being interacted with (e.g., dragged tree nodes) should use a subtle blue tint (`blue-50`) to indicate elevation rather than a large shadow.

## Shapes

The shape language is **Professional and Structured**. 

- **Components:** Buttons, input fields, and cards use a `4px` (soft) radius. This provides a modern feel while maintaining the serious, "blocked" look of a professional tool.
- **Badges:** Status badges use a slightly higher radius (6px) to distinguish them from interactive buttons.
- **Interactive States:** Focus states must use a 2px offset ring in the primary color to ensure accessibility and clear user orientation.

## Components

### Buttons & Inputs
- **Primary Action:** Solid `blue-600` with white text.
- **Secondary Action:** Ghost style with `slate-200` border and `slate-700` text.
- **Inputs:** `1px` border in `slate-300`. On focus, border changes to `blue-500` with a `blue-100` outer glow.

### Tables & Tree Views
- **Tables:** Use alternating row stripes (`slate-50`) for readability. Headers should be sticky with a slightly darker border-bottom.
- **Tree Views:** Use chevron icons for expansion. Active nodes should have a vertical `2px` blue bar on the left edge.

### Status Indicators
- **Badges:** Small, pill-shaped, using a light background tint of the semantic color (e.g., `red-50`) with high-contrast text (`red-700`).
- **P0/Critical:** Include a "pulse" icon or high-contrast solid background to ensure immediate visibility.

### Interactive Cards
- Cards used for AI insights should feature a subtle left-accent border in the primary color to denote "AI-generated" status, separating them from standard manual data entries.