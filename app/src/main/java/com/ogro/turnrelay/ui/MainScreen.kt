package com.ogro.turnrelay.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ogro.turnrelay.R
import com.ogro.turnrelay.ui.theme.TurnRelayTheme
import com.ogro.turnrelay.viewmodels.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
object Controller
@Serializable
object Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationTopBar(navigateToSettings: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {Text(appName)},
        navigationIcon = {
            IconButton(onClick = { navigateToSettings() }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.bindService()
    }

    TurnRelayTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = { ApplicationTopBar { navController.navigate(Settings) } }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Controller,
                modifier = Modifier.padding(innerPadding)) {
                composable<Controller>{ ControlScreen() }
                composable<Settings>{ SettingsScreen() }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}