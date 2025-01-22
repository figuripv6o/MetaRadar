package f.cking.software.utils.graphic

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

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.glassPanel(
    rect: Rect,
    material: RefractionMaterial = RefractionMaterial.GLASS,
    aberrationIndex: Float = 0.1f,
    curveType: Shaders.CurveType = Shaders.CurveType.Mod,
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
    refractionIndex: Float = 1.44f, // glass refraction
    aberrationIndex: Float = 0.1f,
    curveType: Shaders.CurveType = Shaders.CurveType.Mod,
    elevationPx: Float = LocalContext.current.dpToPx(8f).toFloat(),
): Modifier = composed {

    val glassShader = remember { RuntimeShader(Shaders.GLASS_SHADER_ADVANCED) }
    val contentSize = remember { mutableStateOf(Size(0.0f, 0.0f)) }

    glassShader.setFloatUniform(Shaders.ARG_ELEVATION, elevationPx)
    glassShader.setFloatUniform(Shaders.ARG_REFRACTION_INDEX, refractionIndex)
    glassShader.setIntUniform(Shaders.ARG_CURVE_TYPE, curveType.type)
    glassShader.setFloatUniform(Shaders.ARG_CURVE_PARAM_A, curveType.A)
    glassShader.setFloatUniform(Shaders.ARG_CURVE_PARAM_K, curveType.k)
    glassShader.setFloatUniform(Shaders.ARG_ABERRATION_INDEX, aberrationIndex)

    glassShader.setFloatUniform(Shaders.ARG_RESOLUTION, contentSize.value.width.toFloat(), contentSize.value.height.toFloat())
    glassShader.setFloatUniform(Shaders.ARG_PANEL_HEIGHT, rect.height().toFloat())
    glassShader.setFloatUniform(Shaders.ARG_PANEL_WIDTH, rect.width().toFloat())
    glassShader.setFloatUniform(Shaders.ARG_PANEL_X, rect.left.toFloat())
    glassShader.setFloatUniform(Shaders.ARG_PANEL_Y, rect.top.toFloat())

    this
        .onSizeChanged {
            contentSize.value = Size(it.width.toFloat(), it.height.toFloat())
        }
        .then(
            graphicsLayer {
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(glassShader, Shaders.ARG_CONTENT)
                    .asComposeRenderEffect()
            }
        )
}

enum class RefractionMaterial(val refractionIndex: Float) {
    AIR(1.0003f),
    WATER(1.33f),
    GLASS(1.44f),
    SAPPHIRE(1.77f),
    DIAMOND(2.42f),
    ICE(1.31f),
    PLASTIC(1.5f),
    VACUUM(1.0f),
}