package com.tx.carrecord.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.datastore.AppNavigationContext
import com.tx.carrecord.core.datastore.RootTabRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val appNavigationContext: AppNavigationContext,
) : ViewModel() {
    private val _selectedRoute = MutableStateFlow(RootTabRoute.REMINDER)
    val selectedRoute: StateFlow<RootTabRoute> = _selectedRoute.asStateFlow()

    init {
        viewModelScope.launch {
            appNavigationContext.navigationRequestFlow.collect { request ->
                if (request == null) return@collect
                _selectedRoute.value = request.route
                appNavigationContext.clearNavigationRequest()
            }
        }
    }

    fun selectRoute(route: RootTabRoute) {
        _selectedRoute.value = route
    }
}
