# Appearance

**Last verified:** 2026-09-01

Kai has a four-way theme picker — **System**, **Light**, **Dark**, and **OLED** — exposed in Settings on every platform. The default is System, which follows the operating system's dark/light preference. The other three force a specific theme regardless of system state. Dark uses a soft dark background (`#121212`) with slightly lighter surfaces (`#1E1E1E`); OLED flattens the background and the lowest surface tier to pure black (`#000000`) for users who want to save power on OLED panels.

Cards, dialogs, bottom sheets, and menus stay visually lifted in either dark variant because only the lowest surface tiers are affected; container tiers keep their default Material 3 elevation.

## Behavior

- **System**: `isSystemInDarkTheme()` decides between the light and dark schemes.
- **Light**: forces the Material 3 light scheme.
- **Dark**: forces the Material 3 dark scheme. `background` renders `#121212` and `surface` renders `#1E1E1E`. `surfaceContainer*` tiers use their default Material 3 dark values so elevated components remain visible. `onBackground` / `onSurface` stay white.
- **OLED**: forces dark + pure-black override. `background`, `surface`, and `surfaceContainerLowest` render pure black. The elevated `surfaceContainer*` tiers are unchanged so cards and menus stay visible against black.
- **Material You (Android 12+)**: wallpaper-derived accent colors (`primary`, `secondary`, `tertiary`) always apply. When OLED is selected, the black override is layered on top of the dynamic dark scheme so accents and buttons continue to track the wallpaper.
- **Reactivity**: changing the theme picker recomposes the theme immediately without an app restart.
- **UI scale**: desktop applies the user scale on top of a **frozen** first-frame density so opening dialogs or sheets does not re-read fluctuating OS DPI and make the whole window jump in size. Change scale from Settings → General.
- **Empty-chat CUI**: letter size is 176 sp; the animated double-dot that stands in for the `i` tittle matches the standalone 52 dp double-dot icon and sits on the letter stem.
- **Windows title bar**: on Windows the native caption is replaced with an in-app bar that uses the same `background` / `onBackground` as the rest of the theme, so min/max/close sit on Dark, OLED, Light, or Eye Care instead of a forced white OS chrome. Maximize snaps to the **working area** (screen minus the taskbar) instead of covering the taskbar. Mac and Linux keep the system window decorations.

The picker exists on every platform because system theme detection is unreliable on some desktop window systems (notably Linux/Wayland), so users there need an explicit override.

## Component guidance

When adding new surfaces in dark mode, **do not** bind fills to `surface` if the element should stand out from the page background with OLED selected — in OLED `surface` becomes black and the element will be invisible against the background. Use `surfaceContainer` (or higher) for anything that represents a raised card, pill, or control.

## Key Files

| File | Purpose |
|------|---------|
| `composeApp/.../ui/Theme.kt` | `DarkColorScheme` / `LightColorScheme` constants; `withBlackBackground()` extension that flattens a dark scheme to pure black |
| `composeApp/.../data/AppSettings.kt` | `ThemeMode` enum; `themeModeFlow` / `getThemeMode()` / `setThemeMode()` — the persistent setting, with one-time migration from the legacy OLED boolean |
| `composeApp/.../App.kt` | Shared `AppContent` — observes `themeModeFlow`, picks `lightColorScheme` / `darkColorScheme` / `darkColorScheme.withBlackBackground()` based on the selected mode |
| `composeApp/.../ui/settings/SettingsScreen.kt` | Theme mode dropdown in the General tab |
| `androidApp/.../MainActivity.kt` | Android entry — supplies dynamic-color light/dark schemes; the resolved `isDarkTheme` (from `themeMode` + system) drives the system-bar style |
| `androidApp/.../res/values-night/styles.xml` | Pre-Compose window background set to `#FF121212` to match the default dark frame |
| `composeApp/.../iosMain/.../MainViewController.kt` | iOS entry — uses common `App` defaults |
| `composeApp/.../desktopMain/.../main.kt` | Desktop entry — uses common `App` defaults; HiDPI hints; the window opens fitted to the working area (not over the taskbar) |
| `composeApp/.../desktopMain/.../PlatformWindowTheme.jvm.kt` | Windows in-app title bar (same colors as the current theme) plus DWM caption/border attributes; maximize toggles working-area fit |
| `composeApp/.../desktopMain/.../desktop/DesktopWindowTheme.kt` | Native HWND lookup and `DwmSetWindowAttribute` for immersive dark mode / caption color |
| `composeApp/.../ui/components/CuiBranding.kt` | Empty-state CUI wordmark |
