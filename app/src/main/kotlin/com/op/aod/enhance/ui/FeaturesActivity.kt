package com.op.aod.enhance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.op.aod.enhance.data.AodConfigStore
import com.op.aod.enhance.data.AodUiConfig
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

class FeaturesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                FeaturesScreen(
                    initial = AodConfigStore.read(contentResolver),
                    onSave = { cfg -> AodConfigStore.write(contentResolver, cfg) }
                )
            }
        }
    }

}

@OptIn(FlowPreview::class)
@Composable
private fun FeaturesScreen(
    initial: AodUiConfig,
    onSave: (AodUiConfig) -> Unit
) {
    var enablePanoramic by remember { mutableStateOf(initial.enablePanoramic) }
    var enableSettingsSupport by remember { mutableStateOf(initial.enableSettingsSupport) }
    var blockSingleClick by remember { mutableStateOf(initial.blockSingleClick) }
    val currentOnSave by rememberUpdatedState(onSave)
    val resolver = LocalContext.current.contentResolver

    LaunchedEffect(Unit) {
        snapshotFlow { Triple(enablePanoramic, enableSettingsSupport, blockSingleClick) }
            .drop(1) // skip initial emission on Activity creation
            .debounce(300)
            .distinctUntilChanged()
            .collect { (panoramic, settingsSupport, singleClick) ->
                // Read the full config from cache before overwriting, so settings from other pages aren't reset to defaults
                val base = AodConfigStore.read(resolver)
                currentOnSave(
                    base.copy(
                        enablePanoramic = panoramic,
                        enableSettingsSupport = settingsSupport,
                        blockSingleClick = singleClick,
                    )
                )
            }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "AOD Feature Settings",
                color = MiuixTheme.colorScheme.secondaryContainer,
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = paddingValues,
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.background,
                    ),
                ) {
                    SwitchPreference(
                        title = "System UI - All-day Panoramic AOD support",
                        summary = "Unlock all-day panoramic AOD capabilities in System UI",
                        checked = enablePanoramic,
                        onCheckedChange = { enablePanoramic = it },
                    )
                    SwitchPreference(
                        title = "AOD - All-day Panoramic AOD toggle",
                        summary = "Show the all-day panoramic AOD toggle in AOD settings",
                        checked = enableSettingsSupport,
                        onCheckedChange = { enableSettingsSupport = it },
                    )
                    SwitchPreference(
                        title = "Block AOD single-tap wake",
                        summary = "Prevent accidental single taps on AOD from waking the screen",
                        checked = blockSingleClick,
                        onCheckedChange = { blockSingleClick = it },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}
