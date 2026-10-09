package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// TraceHarvest Dark Green & White Theme
// Inspired by Nigerian national colors (Green-White-Green) & agricultural vitality
// ============================================================================

// Primary: Authoritative Deep Forest / Emerald Dark Green
val DarkGreenPrimary = Color(0xFF0F5132)      // Deep Nigerian Emerald / Forest Green
val DarkGreenDark = Color(0xFF0A3622)         // Deep Pine / Canopy Green (Headers & TopBars)
val DarkGreenLight = Color(0xFF198754)        // Vibrant Fresh Green (Accents & Active States)
val DarkGreenContainer = Color(0xFFE8F5E9)    // Crisp Pale Mint-White Container
val DarkGreenOnContainer = Color(0xFF0A3622)  // High-contrast deep green text on container

// Secondary: Fresh Foliage Green
val ForestGreenSecondary = Color(0xFF146C43)
val ForestGreenDark = Color(0xFF0A3622)
val ForestGreenLight = Color(0xFF198754)
val ForestGreenContainer = Color(0xFFE8F5E9)

// Accent: Clean Harvest Leaf Green / Mint
val WarmOchreAccent = Color(0xFF198754)       // Vibrant Leaf Green
val WarmOchreDark = Color(0xFF0F5132)
val WarmOchreLight = Color(0xFF20C997)
val WarmOchreContainer = Color(0xFFE8F5E9)

// Background & Surfaces: Crisp White & Porcelain
val WhiteBackground = Color(0xFFF7FAF8)       // Ultra-clean subtle porcelain white background
val PureWhiteSurface = Color(0xFFFFFFFF)      // 100% Crisp White for cards, dialogs & sheets
val WhiteSurfaceVariant = Color(0xFFF0F5F1)   // Soft Mint-tinted white for inputs and chips
val PureWhiteSurfaceVariant = WhiteSurfaceVariant
val OutlineGreen = Color(0xFFD6E3D8)          // Subtle clean sage outline

// Typography & Text: Deep Slate-Charcoal (Never harsh stark black, exceptional contrast)
val CharcoalDarkText = Color(0xFF111D15)      // Deepest forest charcoal text
val MutedDarkText = Color(0xFF415648)         // Muted slate text for captions & subtitles
val LightSlateText = Color(0xFF708577)        // Light helper text

// Status Indicators (Natural, High-Contrast)
val NaturalStatusGreen = Color(0xFF0F5132)    // Compliant / Verified
val NaturalStatusAmber = Color(0xFFB78103)    // Caution / Pending
val NaturalStatusRed = Color(0xFFC02626)      // Violation / Blocked
val NaturalStatusBlue = Color(0xFF1E6091)     // Blockchain / Info

// ----------------------------------------------------------------------------
// Aliases for seamless backward compatibility across all screens & components
// ----------------------------------------------------------------------------
val LateriteRedPrimary = DarkGreenPrimary
val LateriteRedDark = DarkGreenDark
val LateriteRedLight = DarkGreenLight
val LateriteRedContainer = DarkGreenContainer

val WarmOffWhiteBackground = WhiteBackground
val SoftCreamSurface = PureWhiteSurface
val SurfaceVariantCream = WhiteSurfaceVariant
val OutlineWarm = OutlineGreen

val CharcoalBrownText = CharcoalDarkText
val MutedBrownText = MutedDarkText
val LightBrownText = LightSlateText

val BlockchainBlue = NaturalStatusBlue
val HarvestGreenPrimary = DarkGreenPrimary
val HarvestGreenLight = DarkGreenLight
val HarvestGreenContainer = DarkGreenContainer
val HarvestGreenDark = DarkGreenDark

val SesameAmberSecondary = WarmOchreAccent
val SesameAmberLight = WarmOchreLight
val SesameAmberContainer = WarmOchreContainer

val ComplianceGreen = NaturalStatusGreen
val WarningAmber = NaturalStatusAmber
val ViolationRed = NaturalStatusRed

val TextPrimary = CharcoalDarkText
val TextSecondary = MutedDarkText
val OutlineBorder = OutlineGreen
val SurfaceWarm = WhiteBackground
val SurfaceVariantWarm = WhiteSurfaceVariant

// UI convenience color aliases
val SoftGreenBg = DarkGreenContainer
val TextMutedSubtle = MutedDarkText
val ForestGreenAccent = ForestGreenLight
val GoldenAmber = NaturalStatusAmber
val AlertRed = NaturalStatusRed

