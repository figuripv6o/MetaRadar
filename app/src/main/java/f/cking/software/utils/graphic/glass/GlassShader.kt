package f.cking.software.utils.graphic.glass

import f.cking.software.utils.graphic.glass.GlassShader.CurveType.Companion.getType
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

object GlassShader {

    const val ARG_CONTENT = "content"
    const val ARG_ELEVATION = "elevation"
    const val ARG_REFRACTION_INDEX = "refractionIndex"
    const val ARG_RESOLUTION = "iResolution"
    const val ARG_PANEL_HEIGHT = "panelHeigh"
    const val ARG_PANEL_WIDTH = "panelWidth"
    const val ARG_PANEL_X = "panelX"
    const val ARG_PANEL_Y = "panelY"
    const val ARG_CURVE_TYPE = "curveType"
    const val ARG_CURVE_PARAM_A = "curveParamA"
    const val ARG_CURVE_PARAM_K = "curveParamK"
    const val ARG_ABERRATION_INDEX = "aberrationIndex"


    sealed interface CurveType {
        val A: Float
        val k: Float

        data class Sin(override val A: Float = 0.2f, override val k: Float = 0.3f) : CurveType
        data class Mod(override val A: Float = 0.2f, override val k: Float = 0.25f) : CurveType
        object Flat : CurveType {
            override val k: Float = 0.0f
            override val A: Float = 0.0f
        }

        companion object {
            val Sin = Sin()
            val Mod = Mod()

            fun KClass<out CurveType>.getType(): Int {
                return when {
                    this == Sin::class -> 0
                    this == Mod::class -> 1
                    this == Flat::class -> 2
                    else -> throw IllegalStateException("Type matcher is not implemented for CurveType: $this")
                }
            }
        }
    }

    @Language("AGSL")
    val GLASS_SHADER_ADVANCED = """
        uniform shader $ARG_CONTENT;
        uniform float $ARG_ELEVATION;
        uniform float $ARG_REFRACTION_INDEX;
        uniform float2 $ARG_RESOLUTION;
        uniform float $ARG_PANEL_HEIGHT;
        uniform float $ARG_PANEL_WIDTH;
        uniform float $ARG_PANEL_X;
        uniform float $ARG_PANEL_Y;
        uniform int $ARG_CURVE_TYPE;
        uniform float $ARG_CURVE_PARAM_A;
        uniform float $ARG_CURVE_PARAM_K;
        uniform float $ARG_ABERRATION_INDEX;
        
        float curveSin(float2 fCoord, float A, float k) {
            return A * sin(k * fCoord.x);
        }
        
        float curveMod(float2 fCoord, float A, float k) {
            return pow(mod(fCoord.x * k - 2.0, 4.0) - 2.0, 2.0) * A * k - A * k * 2;
        }
        
        float curve(float2 fCoord, float A, float k) {
            switch($ARG_CURVE_TYPE) {
                case ${CurveType.Sin::class.getType()}:
                    return curveSin(fCoord, A, k);
                case ${CurveType.Mod::class.getType()}:
                    return curveMod(fCoord, A, k);
                case ${CurveType.Flat::class.getType()}:
                    return 0.0;
            }
            
            return 0.0;
        }
        
        float height(float x, float y) {
            return curve(float2(x, y), $ARG_CURVE_PARAM_A, $ARG_CURVE_PARAM_K);
        }
        
        float3 calculateNormal(float2 fCoord) {
            float dx = 0.001; // Small step in x
            float dy = 0.001; // Small step in y
        
            // Partial derivatives
            float dz_dx = (height(fCoord.x + dx, fCoord.y) - height(fCoord.x - dx, fCoord.y)) / (2.0 * dx);
            float dz_dy = (height(fCoord.x, fCoord.y + dy) - height(fCoord.x, fCoord.y - dy)) / (2.0 * dy);
        
            // Tangent vectors
            float3 tangent_x = float3(1.0, 0.0, dz_dx);
            float3 tangent_y = float3(0.0, 1.0, dz_dy);
        
            // Normal vector (normalized)
            float3 normal = normalize(cross(tangent_x, tangent_y));
        
            return normal;
        }
        
        bool isInsidePanel(float2 fCoord) {
            return fCoord.x >= $ARG_PANEL_X && fCoord.x < $ARG_PANEL_X + $ARG_PANEL_WIDTH && fCoord.y > $ARG_PANEL_Y && fCoord.y < $ARG_PANEL_Y + $ARG_PANEL_HEIGHT;
        }
        
        float4 main(float2 fragCoord) {
        
            if (!isInsidePanel(fragCoord)) {
                return content.eval(fragCoord);
            }
        
            float2 uv = fragCoord / iResolution; // Normalize screen coordinates
        
            float2 eyeVector = (uv * 2.0) - 1.0;
            float depth = curve(fragCoord, $ARG_CURVE_PARAM_A, $ARG_CURVE_PARAM_K) * ($ARG_CURVE_PARAM_A / 2.0);
            float3 incident = float3(eyeVector.x * 0.02, -eyeVector.y * 0.001 + depth * 0.2, -1.0);
            float3 normal = calculateNormal(fragCoord);
            float ior = 1.0/$ARG_REFRACTION_INDEX;
            
            float aberationIndex = $ARG_ABERRATION_INDEX;
            float iorR = ior - aberationIndex;
            float iorG = ior;
            float iorB = ior + aberationIndex;
            
            float2 refractedR = refract(incident, normal, iorR).xy;
            float2 refractedG = refract(incident, normal, iorG).xy;
            float2 refractedB = refract(incident, normal, iorB).xy;
            
            float r = $ARG_CONTENT.eval((uv + refractedR) * iResolution).r;
            float g = $ARG_CONTENT.eval((uv + refractedG) * iResolution).g;
            float b = $ARG_CONTENT.eval((uv + refractedB) * iResolution).b;
            float a = $ARG_CONTENT.eval((uv + refractedG) * iResolution).a;
            
            return float4(r, g, b, a);
        }
    """
}