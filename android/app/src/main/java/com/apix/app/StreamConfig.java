package com.apix.app;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Data class representing stream configuration passed from WebView
 */
public class StreamConfig {
    
    @SerializedName("url")
    public String url;
    
    @SerializedName("title")
    public String title;
    
    @SerializedName("actionType")
    public String actionType;
    
    @SerializedName("headers")
    public Headers headers;
    
    @SerializedName("drm")
    public DrmConfig drm;
    
    @SerializedName("intentUri")
    public String intentUri;
    
    @SerializedName("servers")
    public List<Server> servers;
    
    public static class Headers {
        @SerializedName("User-Agent")
        public String userAgent;
        @SerializedName("Referer")
        public String referer;
        @SerializedName("Cookie")
        public String cookie;
        @SerializedName("Origin")
        public String origin;
    }
    
    public static class DrmConfig {
        @SerializedName("licenseUrl")
        public String licenseUrl;
        @SerializedName("scheme")
        public String scheme;
        @SerializedName("keyId")
        public String keyId;
        @SerializedName("key")
        public String key;
    }
    
    public static class Server {
        @SerializedName("name")
        public String name;
        @SerializedName("url")
        public String url;
    }
    
    public String getUserAgent() { return headers != null && headers.userAgent != null ? headers.userAgent : ""; }
    public String getReferer() { return headers != null && headers.referer != null ? headers.referer : ""; }
    public String getCookie() { return headers != null && headers.cookie != null ? headers.cookie : ""; }
    public String getOrigin() { return headers != null && headers.origin != null ? headers.origin : ""; }
    
    public boolean hasDrm() {
        return drm != null && ((drm.licenseUrl != null && !drm.licenseUrl.isEmpty()) || (drm.keyId != null && !drm.keyId.isEmpty()));
    }
    
    public boolean hasHeaders() {
        return headers != null && (
            (headers.userAgent != null && !headers.userAgent.isEmpty()) ||
            (headers.referer != null && !headers.referer.isEmpty()) ||
            (headers.cookie != null && !headers.cookie.isEmpty()) ||
            (headers.origin != null && !headers.origin.isEmpty()));
    }
    
    public boolean hasServers() { return servers != null && !servers.isEmpty(); }
    public boolean isNativeAction() { return actionType == null || actionType.isEmpty() || "native".equals(actionType); }
    public boolean isWebViewAction() { return "webview".equals(actionType); }
    public boolean isIntentAction() { return "intent".equals(actionType); }
}
