package f.cking.software.ui.shadertest

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.letIf
import f.cking.software.utils.graphic.RefractionMaterial
import f.cking.software.utils.graphic.Shaders
import f.cking.software.utils.graphic.glassPanel
import f.cking.software.utils.graphic.pxToDp
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
object ShaderTestScreen {

    @Composable
    fun Screen(
        router: Router,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Text("This screen requires Android 13+")
            return
        }

        if (!BuildConfig.DEBUG) {
            Text("This is screen is for debugging purposes only. Not supported in release builds.")
            return
        }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { AppBar(scrollBehavior) { router.navigate(BackCommand) } },
            content = { paddings ->
                Content(Modifier.padding(paddings))
            }
        )
    }

    @SuppressLint("NewApi")
    @Composable
    private fun Content(
        parentModifier: Modifier = Modifier,
    ) {

        val scrollState = rememberScrollState()
        val screenSize = remember { mutableStateOf(Size(0.0f, 0.0f)) }
        val settingsBlockSize = remember { mutableStateOf(Size(0.0f, 0.0f)) }

        val sliderSizeState = remember { SliderState(value = 0.5f) }
        val sliderAberrationState = remember { SliderState(value = 0.2f) }
        val glassType = remember { mutableStateOf<GlassType>(GlassType.MOD) }
        val sliderRefractionIndexState = remember {
            SliderState(value = RefractionMaterial.GLASS.refractionIndex, valueRange = RefractionMaterial.VACUUM.refractionIndex..RefractionMaterial.DIAMOND.refractionIndex)
        }

        val offset = max(0f, settingsBlockSize.value.height - scrollState.value)

        val panelSize = Size(screenSize.value.width * sliderSizeState.value, screenSize.value.height * sliderSizeState.value)
        val top = max((screenSize.value.height / 2 - panelSize.height / 2).toInt(), offset.toInt())
        val rect = Rect(
            (screenSize.value.width / 2 - panelSize.width / 2).toInt(),
            top,
            (screenSize.value.width / 2 + panelSize.width / 2).toInt(),
            (top + panelSize.height).toInt(),
        )

        Box(
            parentModifier.fillMaxSize()
                .onSizeChanged {
                    screenSize.value = Size(it.width.toFloat(), it.height.toFloat())
                }
                .letIf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.glassPanel(
                        rect = rect,
                        refractionIndex = sliderRefractionIndexState.value,
                        aberrationIndex = sliderAberrationState.value,
                        curveType = glassType.value.curveType,
                    )
                }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .onSizeChanged { settingsBlockSize.value = Size(it.width.toFloat(), it.height.toFloat()) }
                ) {
                    // Scale
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(R.string.shader_test_panel_size, (sliderSizeState.value * 100).toInt()),
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), state = sliderSizeState)

                    // Aberraion
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(R.string.shader_test_glass_aberration, sliderAberrationState.value),
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), state = sliderAberrationState)

                    // Refraction Index
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(R.string.shader_test_glass_refraction, sliderRefractionIndexState.value),
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), state = sliderRefractionIndexState)

                    // Glass type
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(R.string.shader_test_glass_type),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Absolute.SpaceAround) {
                        GlassType.entries.forEach { type ->
                            FilterChip(
                                onClick = { glassType.value = type },
                                trailingIcon = type.imageRes?.let {
                                    {
                                        Icon(
                                            modifier = Modifier.size(32.dp),
                                            painter = painterResource(id = it),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                selected = glassType.value == type,
                                label = { Text(stringResource(type.nameRes)) },
                            )
                        }
                    }
                }

                // Images
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = painterResource(id = R.drawable.monstera),
                    contentDescription = "",
                    contentScale = ContentScale.FillWidth
                )
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = painterResource(id = R.drawable.appa),
                    contentDescription = stringResource(id = R.string.secret_cat),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(200.dp))
            }

            Box(
                Modifier.size(
                    height = pxToDp(rect.height().toFloat()).dp,
                    width = pxToDp(rect.width().toFloat()).dp
                ).offset(
                    x = pxToDp(rect.left.toFloat()).dp,
                    y = pxToDp(rect.top.toFloat()).dp,
                ).background(Color.Gray.copy(alpha = 0.1f))
            )
        }
    }

    private enum class GlassType(val curveType: Shaders.CurveType, val nameRes: Int, val imageRes: Int?) {
        MOD(Shaders.CurveType.Mod, R.string.shader_test_glass_type_mod, R.drawable.glass_type_fluted),
        SIN(Shaders.CurveType.Sin, R.string.shader_test_glass_type_sin, R.drawable.glass_type_curved),
        FLAT(Shaders.CurveType.Flat, R.string.shader_test_glass_type_flat, null),
    }

    @Composable
    private fun AppBar(scrollState: TopAppBarScrollBehavior, onBackClick: () -> Unit) {
        TopAppBar(
            scrollBehavior = scrollState,
            colors = TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            title = {
                Text(text = stringResource(R.string.shader_test_title))
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }
}