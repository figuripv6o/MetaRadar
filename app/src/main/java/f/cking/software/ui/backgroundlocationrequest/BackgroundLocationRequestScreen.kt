package f.cking.software.ui.backgroundlocationrequest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import f.cking.software.R
import f.cking.software.utils.graphic.GlassBottomSpace
import f.cking.software.utils.graphic.RoundedBox
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
object BackgroundLocationRequestScreen {

    @Composable
    fun Screen(viewModel: BackgroundLocationRequestViewModel = koinViewModel()) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { AppBar(scrollBehavior) { viewModel.onBack() } },
            content = { paddings ->
                GlassBottomSpace(
                    modifier = Modifier.fillMaxSize(),
                    globalContent = {
                        Content(Modifier.padding(top = paddings.calculateTopPadding()), viewModel)
                    },
                    bottomContent = {
                        Button(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            onClick = { viewModel.grantPermission() },
                            content = { Text(stringResource(R.string.background_location_request_button)) }
                        )
                    }
                )
            }
        )
    }

    @Composable
    private fun Content(modifier: Modifier, viewModel: BackgroundLocationRequestViewModel) {
        Column(modifier.fillMaxSize()) {
            RoundedBox {
                Text(stringResource(R.string.background_location_request_title))
            }
        }
    }

    @Composable
    private fun AppBar(scrollState: TopAppBarScrollBehavior, onBackClick: () -> Unit) {
        TopAppBar(
            scrollBehavior = scrollState,
            colors = TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            title = { Text(text = stringResource(R.string.shader_test_title)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }
}