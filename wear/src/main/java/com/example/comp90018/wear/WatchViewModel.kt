package com.example.comp90018.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comp90018.watch.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(WatchUiState())
    val state = _state.asStateFlow()
    private val _demo = MutableStateFlow(false)
    val demo = _demo.asStateFlow()
    private val demoGateway = DemoPhoneGateway()
    private val dispatcher = PhoneCommandDispatcher(demoGateway)
    private val realBridge = WearDataLayerBridge(application)
    private val controller = WatchController(WatchBridge { command ->
        if (_demo.value) {
            if (demoGateway.connected) dispatcher.sendWatchCommand("watch-demo", command)
            else WatchAcknowledgement.rejected(command, WatchFailure.DISCONNECTED, true)
        } else realBridge.sendWatchCommand(command)
    }) { _state.value = it }

    fun refresh() { viewModelScope.launch { controller.refresh() } }
    fun send(action: WatchAction, options: WatchStartOptions? = null) {
        viewModelScope.launch { controller.command(action, options) }
    }
    fun retry() { viewModelScope.launch { controller.retry() } }
    fun enableDemo() {
        // Mode switching requires a fresh controller/account handshake; exposed only
        // before a real connection, so a live phone session cannot be hidden by demo mode.
        if (_state.value.busy || _state.value.session != null || _state.value.canRetry) return
        _demo.value = true
        refresh()
    }
    fun demoSignIn(value: Boolean) {
        if (!_demo.value || _state.value.busy) return
        demoGateway.signedIn = value
        dispatcher.invalidateAccount()
        refresh()
    }
    fun demoConnection(value: Boolean) {
        if (!_demo.value || _state.value.busy) return
        demoGateway.connected = value
        refresh()
    }
}
