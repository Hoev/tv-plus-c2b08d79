package com.apix.app.data

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Category(
    var id: String = "",
    var name: String = "",
    var sortOrder: Int = 0,
    var channels: Map<String, Channel>? = null,
    var hidden: Boolean = false
)

@IgnoreExtraProperties
data class Channel(
    var id: String = "",
    var name: String = "",
    var imageUrl: String = "",
    var sortOrder: Int = 0,
    var actionType: String = "direct_play",
    var hidden: Boolean = false,
    var stream: StreamConfig? = null,
    var sideMenuId: String? = null,
    var externalUrl: String? = null,
    var preferredPlayer: String? = null,
    var androidStream: AndroidStreamConfig? = null,
    var androidActionType: String? = null
)

@IgnoreExtraProperties
data class StreamConfig(
    var url: String? = null,
    var userAgent: String? = null,
    var referrer: String? = null,
    var cookies: String? = null,
    var drm: DRMConfig? = null
)

@IgnoreExtraProperties
data class AndroidStreamConfig(
    var url: String? = null,
    var headers: Map<String, String>? = null,
    var intentUri: String? = null,
    var drmLicenseUrl: String? = null,
    var drmScheme: String? = null,
    var drmKeyId: String? = null,
    var drmKey: String? = null,
    var drmClearKeyCombined: String? = null,
    var drmClearKeyMode: String? = null,
    var servers: List<Server>? = null
)

@IgnoreExtraProperties
data class DRMConfig(
    var clearKeyId: String? = null,
    var clearKeyKey: String? = null,
    var clearKeyCombined: String? = null,
    var clearKeyUrl: String? = null,
    var clearKeyMode: String? = null
)

@IgnoreExtraProperties
data class Server(
    var name: String? = null,
    var url: String? = null
)

@IgnoreExtraProperties
data class SideMenu(
    var id: String = "",
    var name: String = "",
    var channels: Map<String, SubChannel>? = null
)

@IgnoreExtraProperties
data class AppSettings(
    @get:PropertyName("showSettingsSection")
    @set:PropertyName("showSettingsSection")
    var showSettingsSection: Boolean = true
)

@IgnoreExtraProperties
data class SubChannel(
    var id: String = "",
    var name: String = "",
    var imageUrl: String = "",
    var sortOrder: Int = 0,
    var stream: StreamConfig? = null,
    var preferredPlayer: String? = null,
    var hidden: Boolean = false,
    var androidStream: AndroidStreamConfig? = null,
    var androidActionType: String? = null
)

/**
 * Player config passed between screens via JSON
 */
data class PlayerConfig(
    var url: String = "",
    var title: String = "",
    var actionType: String? = null,
    var headers: PlayerHeaders? = null,
    var drm: PlayerDrm? = null,
    var servers: List<Server>? = null
)

data class PlayerHeaders(
    var userAgent: String? = null,
    var referer: String? = null,
    var cookie: String? = null,
    var origin: String? = null
)

data class PlayerDrm(
    var licenseUrl: String? = null,
    var scheme: String? = null,
    var keyId: String? = null,
    var key: String? = null
)
