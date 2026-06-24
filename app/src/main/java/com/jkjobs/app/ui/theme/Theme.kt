package com.jkjobs.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// JKJobs+ brand palette
val BrandPurple = Color(0xFF4B2EDB)
val BrandPurpleDark = Color(0xFF3A21B0)
val BrandPurpleLightContainer = Color(0xFFE9E4FB)
val BrandAccentGreen = Color(0xFF2E9E5B)   // "New" / success-style pills
val BrandAccentOrange = Color(0xFFE08A1E)  // "Urgent" pills
val BrandAccentBlue = Color(0xFF2E6FE0)    // "Apply" pills

val JKLightColorScheme = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleLightContainer,
    onPrimaryContainer = BrandPurpleDark,
    secondary = BrandAccentBlue,
    secondaryContainer = Color(0xFFE3ECFC),
    background = Color(0xFFF7F6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1EEFB),
    onSurfaceVariant = Color(0xFF5B5670),
    outline = Color(0xFFDAD6EA)
)

val JKDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBBA9F7),
    onPrimary = Color(0xFF1E0F66),
    primaryContainer = BrandPurpleDark,
    onPrimaryContainer = BrandPurpleLightContainer,
    secondary = Color(0xFF9DB8F2),
    background = Color(0xFF13111A),
    surface = Color(0xFF1C1926),
    surfaceVariant = Color(0xFF2A2636),
    onSurfaceVariant = Color(0xFFC8C2DC)
)
