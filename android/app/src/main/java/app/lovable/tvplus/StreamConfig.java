package app.lovable.tvplus;

import com.google.gson.annotations.SerializedName;

/**
 * Data class representing stream configuration passed from WebView
 */
public class StreamConfig {
    
    @SerializedName("url")
    public String url;
    
    @SerializedName("title")
    public String title;
    
    @SerializedName("headers")
    public Headers headers;
    
    @SerializedName("drm")
    public String drm; // Format: "keyId:key" or URL
    
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
    
    // Helper methods
    public String getUserAgent() {
        return headers != null && headers.userAgent != null ? headers.userAgent : "";
    }
    
    public String getReferer() {
        return headers != null && headers.referer != null ? headers.referer : "";
    }
    
    public String getCookie() {
        return headers != null && headers.cookie != null ? headers.cookie : "";
    }
    
    public String getOrigin() {
        return headers != null && headers.origin != null ? headers.origin : "";
    }
    
    public boolean hasDrm() {
        return drm != null && !drm.isEmpty();
    }
    
    public boolean hasHeaders() {
        return headers != null && (
            (headers.userAgent != null && !headers.userAgent.isEmpty()) ||
            (headers.referer != null && !headers.referer.isEmpty()) ||
            (headers.cookie != null && !headers.cookie.isEmpty()) ||
            (headers.origin != null && !headers.origin.isEmpty())
        );
    }
}
