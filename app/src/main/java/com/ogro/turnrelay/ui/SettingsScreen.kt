package com.ogro.turnrelay.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ogro.turnrelay.R
import com.ogro.turnrelay.util.portIsValid
import com.ogro.turnrelay.viewmodels.MainViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val serverAddress = viewModel.serverAddress
    val serverPort = viewModel.serverPort
    val turnAddress = viewModel.turnAddress
    val turnPort = viewModel.turnPort
    val turnUser = viewModel.turnUsername
    val turnPass = viewModel.turnPass

    SettingsScreenContent(
        modifier = modifier,

        turnAddress = turnAddress,
        turnPort = turnPort,
        turnUser = turnUser,
        turnPass = turnPass,
        serverAddress = serverAddress,
        serverPort = serverPort,

        serverAddressChanged = { s ->
            viewModel.serverAddressChanged(s)
        },
        serverPortChanged = {
            viewModel.serverPortChanged(it)
        },
        turnAddressChanged = {
            viewModel.turnAddressChanged(it)
        },
        turnPortChanged = {
            viewModel.turnPortChanged(it)
        },
        turnUserChanged = {
            viewModel.turnUserChanged(it)
        },
        turnPassChanged = {
            viewModel.turnPassChanged(it)
        }
    )
}

@Composable
fun SettingsScreenContent(
    modifier: Modifier,

    serverAddress: String,
    serverPort: Int,
    turnAddress: String,
    turnPort: Int,
    turnUser: String,
    turnPass: String,

    serverAddressChanged: (String) -> Unit,
    serverPortChanged: (Int) -> Unit,
    turnAddressChanged: (String) -> Unit,
    turnPortChanged: (Int) -> Unit,
    turnUserChanged: (String) -> Unit,
    turnPassChanged: (String) -> Unit
) {
    val serverAddressLabel = stringResource(R.string.server_ip_address)
    val serverPortLabel = stringResource(R.string.server_port)
    val turnAddressLabel = stringResource(R.string.turn_server_address)
    val turnPortLabel = stringResource(R.string.turn_server_port)
    val turnUserLabel = stringResource(R.string.turn_username)
    val turnPassLabel = stringResource(R.string.turn_password)

    var turnPortStr by remember { mutableStateOf(turnPort.toString()) }

    Column(modifier = modifier.fillMaxSize()) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = serverAddress,
            label = { Text(serverAddressLabel) },
            onValueChange = {
                serverAddressChanged(it)
            }
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = serverPort.toString(),
            label = { Text(serverPortLabel) },
            onValueChange = { it ->
                if(it.all{it.isDigit()}) {
                    val value = it.toInt()
                    if(portIsValid(value)) {
                        serverPortChanged(value)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = turnAddress,
            label = { Text(turnAddressLabel) },
            onValueChange = {
                turnAddressChanged(it)
            }
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = turnPortStr,
            isError = !portIsValid(turnPortStr),
            label = { Text(turnPortLabel) },
            onValueChange = {
                turnPortStr = it
                if(portIsValid(turnPortStr)) {
                    val value = turnPortStr.toInt()
                    turnPortChanged(value)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = turnUser,
            label = { Text(turnUserLabel) },
            onValueChange = {
                turnUserChanged(it)
            }
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = turnPass,
            label = { Text(turnPassLabel) },
            onValueChange = {
                turnPassChanged(it)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreenContent(
        modifier = Modifier,

        turnAddress = "127.0.0.1",
        turnPort = 443,
        turnUser = "user",
        turnPass = "pass",
        serverAddress = "127.0.0.1",
        serverPort = 443,

        serverAddressChanged = {},
        serverPortChanged = {},
        turnAddressChanged = {},
        turnPortChanged = {},
        turnUserChanged = {},
        turnPassChanged = {}
    )
}
