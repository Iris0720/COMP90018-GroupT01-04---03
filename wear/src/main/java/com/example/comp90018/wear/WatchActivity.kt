package com.example.comp90018.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: WatchViewModel = viewModel()
            val state by vm.state.collectAsState()
            val demo by vm.demo.collectAsState()
            LaunchedEffect(vm) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    vm.refresh()
                    while (true) {
                        delay(3000)
                        if (!vm.state.value.stale && !vm.state.value.canRetry &&
                            vm.state.value.session?.phase in setOf(com.example.comp90018.watch.WatchPhase.LIVE,
                                com.example.comp90018.watch.WatchPhase.PAUSED)) vm.refresh()
                    }
                }
            }
            WatchScreen(state, demo, vm::refresh, vm::send, vm::retry, vm::enableDemo,
                vm::demoConnection, vm::demoSignIn)
        }
    }
}
