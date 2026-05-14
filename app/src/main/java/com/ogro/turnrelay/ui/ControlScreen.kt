package com.ogro.turnrelay.ui

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ogro.turnrelay.R
import com.ogro.turnrelay.net.TunnelProcess
import com.ogro.turnrelay.viewmodels.MainViewModel
import com.ogro.turnrelay.viewmodels.StartVpnServerError

/**
 * Change the state of the VPN service
 * Enable/disable the service
 */
private fun toggleService(
    context: Context,
    viewModel: MainViewModel,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {

    try {
        if (!viewModel.serviceEnabled) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                launcher.launch(intent)
            } else {
                viewModel.startService()
            }
        } else {
            viewModel.stopService()
        }
    } catch (e: StartVpnServerError) {
        val errMessage = e.message
        Toast.makeText(context, errMessage, Toast.LENGTH_LONG)
            .show()
    }
}

@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context  = LocalContext.current

    val buttonEnabled = viewModel.serviceEnabled

    val labelEnable = stringResource(id = R.string.button_enable)
    val labelDisable = stringResource(id = R.string.button_disable)

    val buttonLabelText = if(buttonEnabled) labelDisable else labelEnable

    val logLines by viewModel.logLines.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    val connectionStateLabel = when(connectionState) {
        TunnelProcess.ConnectionState.DISCONNECT -> stringResource(id = R.string.disconnected)
        TunnelProcess.ConnectionState.CONNECTED -> stringResource(id = R.string.connected)
        TunnelProcess.ConnectionState.ERROR -> stringResource(id = R.string.error)
    }

    val vpnServicePrepareLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {}
        )
    ControlScreenContent(
        modifier = modifier,
        buttonLabelText = buttonLabelText,
        logLines = logLines,
        connectionStateLabel = connectionStateLabel,
        buttonOnClick = {
            toggleService(
                context,
                viewModel,
                vpnServicePrepareLauncher
            )
        }
    )
}

@Composable
fun LogView(
    modifier: Modifier = Modifier,
    logLines: List<String>
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(logLines) { logLine ->
            Text(text = logLine)
        }
    }
}

@Composable
fun ControlScreenContent(
    modifier: Modifier = Modifier,
    buttonLabelText: String,
    logLines: List<String>,
    connectionStateLabel: String,

    buttonOnClick: () -> Unit,
) {
    Column(
        modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = buttonOnClick
            ) {
                Text(
                    text = buttonLabelText,
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.inversePrimary
        )
        Text(
            text = connectionStateLabel,
            style = MaterialTheme.typography.displaySmall,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.inversePrimary
        )

        LogView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            logLines = logLines
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ControlScreenPreview() {
    ControlScreenContent(
        modifier = Modifier,
        buttonLabelText = "Enable",
        logLines = listOf("INFO: App started", "DEBUG: UI initialized", "ERROR: Network failed"),
        connectionStateLabel = "Connected",
        buttonOnClick = {}
    )
}