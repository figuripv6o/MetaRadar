package f.cking.software.utils.graphic.glass

import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import f.cking.software.dpToPx
import f.cking.software.utils.graphic.glass.GlassShader.CurveType.Companion.getType

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.glassPanel(
    rect: Rect,
    material: RefractionMaterial = RefractionMaterial.GLASS,
    aberrationIndex: Float = 0.1f,
    curveType: GlassShader.CurveType = GlassShader.CurveType.Mod,
    elevationPx: Float = LocalContext.current.dpToPx(8f).toFloat(),
): Modifier = this.glassPanel(
    rect = rect,
    refractionIndex = material.refractionIndex,
    aberrationIndex = aberrationIndex,
    curveType = curveType,
    elevationPx = elevationPx,
)

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.glassPanel(
    rect: Rect,
    refractionIndex: Float = RefractionMaterial.GLASS.refractionIndex,
    aberrationIndex: Float = 0.1f,
    curveType: GlassShader.CurveType = GlassShader.CurveType.Mod,
    elevationPx: Float = LocalContext.current.dpToPx(8f).toFloat(),
): Modifier = composed {

    val glassShader = remember { RuntimeShader(GlassShader.GLASS_SHADER_ADVANCED) }
    val contentSize = remember { mutableStateOf(Size(0.0f, 0.0f)) }

    glassShader.setFloatUniform(GlassShader.ARG_ELEVATION, elevationPx)
    glassShader.setFloatUniform(GlassShader.ARG_REFRACTION_INDEX, refractionIndex)
    glassShader.setIntUniform(GlassShader.ARG_CURVE_TYPE, curveType::class.getType())
    glassShader.setFloatUniform(GlassShader.ARG_CURVE_PARAM_A, curveType.A)
    glassShader.setFloatUniform(GlassShader.ARG_CURVE_PARAM_K, curveType.k)
    glassShader.setFloatUniform(GlassShader.ARG_ABERRATION_INDEX, aberrationIndex)

    glassShader.setFloatUniform(GlassShader.ARG_RESOLUTION, contentSize.value.width.toFloat(), contentSize.value.height.toFloat())
    glassShader.setFloatUniform(GlassShader.ARG_PANEL_HEIGHT, rect.height().toFloat())
    glassShader.setFloatUniform(GlassShader.ARG_PANEL_WIDTH, rect.width().toFloat())
    glassShader.setFloatUniform(GlassShader.ARG_PANEL_X, rect.left.toFloat())
    glassShader.setFloatUniform(GlassShader.ARG_PANEL_Y, rect.top.toFloat())

    this
        .onSizeChanged {
            contentSize.value = Size(it.width.toFloat(), it.height.toFloat())
        }
        .then(
            graphicsLayer {
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(glassShader, GlassShader.ARG_CONTENT)
                    .asComposeRenderEffect()
            }
        )
}

enum class RefractionMaterial(val refractionIndex: Float) {
    VACUUM(1.0f),
    AIR(1.0003f),
    ICE(1.31f),
    WATER(1.33f),
    GLASS(1.5f),
    SAPPHIRE(1.77f),
    PLASTIC(1.5f),
    DIAMOND(2.42f),
}