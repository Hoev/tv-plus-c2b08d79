package com.apix.app;

import java.util.List;
import java.util.Map;

/**
 * Data models matching Firebase Realtime Database structure
 */
public class FirebaseModels {

    public static class Category {
        public String id;
        public String name;
        public int sortOrder;
        public Map<String, Channel> channels;
        public boolean hidden;
    }

    public static class Channel {
        public String id;
        public String name;
        public String imageUrl;
        public int sortOrder;
        public String actionType; // "direct_play", "open_submenu", "external_link"
        public boolean hidden;

        // Web settings
        public StreamConfig stream;
        public String sideMenuId;
        public String externalUrl;
        public String preferredPlayer;

        // Android settings
        public AndroidStreamConfig androidStream;
        public String androidActionType; // "native", "webview", "intent"
    }

    public static class StreamConfig {
        public String url;
        public String userAgent;
        public String referrer;
        public String cookies;
        public DRMConfig drm;
    }

    public static class AndroidStreamConfig {
        public String url;
        public Map<String, String> headers;
        public String intentUri;
        public String drmLicenseUrl;
        public String drmScheme;
        public String drmKeyId;
        public String drmKey;
        public String drmClearKeyCombined;
        public String drmClearKeyMode;
        public List<Server> servers;
    }

    public static class DRMConfig {
        public String clearKeyId;
        public String clearKeyKey;
        public String clearKeyCombined;
        public String clearKeyUrl;
        public String clearKeyMode;
    }

    public static class Server {
        public String name;
        public String url;
    }

    public static class SideMenu {
        public String id;
        public String name;
        public Map<String, SubChannel> channels;
    }

    public static class SubChannel {
        public String id;
        public String name;
        public String imageUrl;
        public StreamConfig stream;
        public int sortOrder;
        public String preferredPlayer;
        public boolean hidden;
        public AndroidStreamConfig androidStream;
        public String androidActionType;
    }
}
