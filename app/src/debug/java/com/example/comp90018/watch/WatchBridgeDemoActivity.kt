package com.example.comp90018.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comp90018.ui.theme.TrailwiseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchBridgeDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: PhoneDemoViewModel = viewModel()
            val state by vm.state.collectAsState()
            TrailwiseTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Watch bridge demo", style = MaterialTheme.typography.headlineMedium)
                    Text("DEMO ONLY · synthetic activity · no GPS · no database save")
                    Text("This debug activity installs the phone receiver. A paired watch uses real Data Layer RPC to control this demo session.")
                    Text(state.session?.phase?.name ?: "No confirmed state", Modifier.testTag("phone-phase"))
                    Text("${state.session?.durationSeconds ?: 0} s · ${state.session?.distanceMetres ?: 0.0} m")
                    state.failure?.let { Text(it.name, Modifier.testTag("phone-error")) }
                    Button(onClick = { vm.send(WatchAction.START) }, enabled = !state.busy,
                        modifier = Modifier.testTag("phone-start")) { Text("Start demo") }
                    Button(onClick = { vm.send(WatchAction.PAUSE) }, enabled = !state.busy,
                        modifier = Modifier.testTag("phone-pause")) { Text("Pause") }
                    Button(onClick = { vm.send(WatchAction.RESUME) }, enabled = !state.busy,
                        modifier = Modifier.testTag("phone-resume")) { Text("Resume") }
                    Button(onClick = { vm.send(WatchAction.FINISH) }, enabled = !state.busy,
                        modifier = Modifier.testTag("phone-finish")) { Text("Finish demo (not saved)") }
                    Button(onClick = { vm.auth(false) }, modifier = Modifier.testTag("phone-signout")) { Text("Sign out demo") }
                    Button(onClick = { vm.auth(true) }, modifier = Modifier.testTag("phone-signin")) { Text("Sign in demo") }
                }
            }
        }
    }
}

class PhoneDemoViewModel : ViewModel() {
    private val gateway = DemoPhoneGateway()
    private val dispatcher = PhoneWatchEndpoint.install(gateway)
    private val _state = MutableStateFlow(WatchUiState())
    val state = _state.asStateFlow()
    private val controller = WatchController(WatchBridge { dispatcher.sendWatchCommand("phone-demo-ui", it) }) {
        _state.value = it
    }
    init {
        viewModelScope.launch {
            while (true) { controller.refresh(); delay(1000) }
        }
    }
    fun send(action: WatchAction) {
        viewModelScope.launch { controller.command(action, if (action == WatchAction.START) WatchStartOptions() else null) }
    }
    fun auth(signedIn: Boolean) {
        gateway.signedIn = signedIn
        dispatcher.invalidateAccount()
        viewModelScope.launch { controller.refresh() }
    }
    override fun onCleared() { PhoneWatchEndpoint.uninstall(dispatcher) }
}
