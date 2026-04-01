package com.apix.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apix.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _sideMenus = MutableStateFlow<Map<String, SideMenu>>(emptyMap())
    val sideMenus: StateFlow<Map<String, SideMenu>> = _sideMenus.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                FirebaseRepository.ensureAnonymousAuth()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "فشل المصادقة: ${e.message}") }
                return@launch
            }

            // Observe categories
            launch {
                FirebaseRepository.observeCategories().collect { cats ->
                    _uiState.update { state ->
                        val selected = state.selectedCategory
                            ?: cats.firstOrNull()
                        state.copy(
                            categories = cats,
                            selectedCategory = selected,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }

            // Observe side menus
            launch {
                FirebaseRepository.observeSideMenus().collect { menus ->
                    _sideMenus.value = menus
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun getVisibleChannels(): List<Channel> {
        val cat = _uiState.value.selectedCategory ?: return emptyList()
        return (cat.channels?.values ?: emptyList())
            .filter { !it.hidden }
            .sortedBy { it.sortOrder }
    }

    fun searchChannels(query: String): List<Channel> {
        if (query.isBlank()) return emptyList()
        val filter = query.lowercase().trim()
        val results = mutableListOf<Channel>()

        // Search main channels
        for (cat in _uiState.value.categories) {
            cat.channels?.values?.forEach { ch ->
                if (!ch.hidden && ch.name.lowercase().contains(filter)) {
                    results.add(ch)
                }
            }
        }

        // Search sub-channels
        for (menu in _sideMenus.value.values) {
            menu.channels?.values?.forEach { sc ->
                if (!sc.hidden && sc.name.lowercase().contains(filter)) {
                    results.add(Channel(
                        id = sc.id,
                        name = sc.name,
                        imageUrl = sc.imageUrl,
                        actionType = "direct_play",
                        stream = sc.stream,
                        androidStream = sc.androidStream,
                        androidActionType = sc.androidActionType
                    ))
                }
            }
        }

        return results
    }

    fun buildPlayerConfig(channel: Channel): PlayerConfig? {
        val config = PlayerConfig(title = channel.name)

        if (channel.androidStream?.url != null) {
            config.url = channel.androidStream!!.url!!
            config.actionType = channel.androidActionType

            channel.androidStream!!.headers?.let { h ->
                config.headers = PlayerHeaders(
                    userAgent = h["userAgent"],
                    referer = h["referrer"],
                    cookie = h["cookie"],
                    origin = h["origin"]
                )
            }

            channel.androidStream!!.drmScheme?.let { scheme ->
                var keyId = channel.androidStream!!.drmKeyId
                var key = channel.androidStream!!.drmKey
                if (channel.androidStream!!.drmClearKeyMode == "combined" &&
                    channel.androidStream!!.drmClearKeyCombined != null
                ) {
                    val parts = channel.androidStream!!.drmClearKeyCombined!!.split(":")
                    if (parts.size == 2) {
                        keyId = parts[0]; key = parts[1]
                    }
                }
                config.drm = PlayerDrm(
                    licenseUrl = channel.androidStream!!.drmLicenseUrl,
                    scheme = scheme,
                    keyId = keyId,
                    key = key
                )
            }

            channel.androidStream!!.servers?.let { servers ->
                config.servers = servers
            }
        } else if (channel.stream?.url != null) {
            config.url = channel.stream!!.url!!
            if (channel.stream!!.userAgent != null || channel.stream!!.referrer != null) {
                config.headers = PlayerHeaders(
                    userAgent = channel.stream!!.userAgent,
                    referer = channel.stream!!.referrer,
                    cookie = channel.stream!!.cookies
                )
            }
        }

        return if (config.url.isNotEmpty()) config else null
    }
}

data class UiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
