package ca.lght.mindfulmail.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/**
 * No-op Indication that draws no ripple or press feedback.
 * E Ink displays should not show ripple effects — they cause ghosting artifacts.
 */
object NoIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance =
        remember {
            object : IndicationInstance {
                override fun ContentDrawScope.drawIndication() {
                    drawContent()
                }
            }
        }
}

private val EInkColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = LightGray,
    onPrimaryContainer = Black,
    secondary = DarkGray,
    onSecondary = White,
    secondaryContainer = LightGray,
    onSecondaryContainer = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,
    outline = Gray,
    error = Black,
    onError = White,
)

@Composable
fun MindfulMailTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIndication provides NoIndication) {
        MaterialTheme(
            colorScheme = EInkColorScheme,
            typography = MindfulMailTypography,
            content = content,
        )
    }
}
