/**
 * Android Bridge - Communication layer between Web App and Android Native Shell
 * Supports the split Web/Android architecture
 */

interface AndroidInterface {
  playVideo: (jsonConfig: string) => void;
  showToast: (message: string) => void;
  isAndroidApp: () => boolean;
  getAppVersion?: () => string;
}

interface AndroidHeaders {
  'User-Agent'?: string;
  'Referer'?: string;
  'Cookie'?: string;
  'Origin'?: string;
}

interface AndroidDrmConfig {
  licenseUrl?: string;
  scheme?: 'widevine' | 'clearkey' | 'playready';
  keyId?: string;
  key?: string;
}

interface AndroidStreamData {
  url: string;
  title: string;
  actionType?: 'native' | 'webview' | 'intent';
  headers?: AndroidHeaders;
  drm?: AndroidDrmConfig;
  intentUri?: string;
  servers?: Array<{ name: string; url: string }>;
}

declare global {
  interface Window {
    Android?: AndroidInterface;
  }
}

/**
 * Check if running inside Android WebView
 */
export function isAndroidApp(): boolean {
  return typeof window !== 'undefined' && 
         typeof window.Android !== 'undefined' && 
         typeof window.Android.isAndroidApp === 'function' &&
         window.Android.isAndroidApp();
}

/**
 * Get Android app version
 */
export function getAndroidVersion(): string | null {
  if (isAndroidApp() && window.Android?.getAppVersion) {
    try {
      return window.Android.getAppVersion();
    } catch {
      return null;
    }
  }
  return null;
}

/**
 * Send stream data to Android native player
 * Falls back to web player if not in Android app
 */
export function sendToAndroid(streamData: AndroidStreamData): boolean {
  if (isAndroidApp() && window.Android) {
    try {
      const jsonConfig = JSON.stringify(streamData);
      console.log('[AndroidBridge] Sending to native player:', jsonConfig);
      window.Android.playVideo(jsonConfig);
      return true;
    } catch (error) {
      console.error('[AndroidBridge] Error sending to Android:', error);
      return false;
    }
  }
  return false;
}

/**
 * Show a toast message on Android
 */
export function showAndroidToast(message: string): boolean {
  if (isAndroidApp() && window.Android) {
    try {
      window.Android.showToast(message);
      return true;
    } catch (error) {
      console.error('[AndroidBridge] Error showing Android toast:', error);
      return false;
    }
  }
  return false;
}

/**
 * Build Android stream config from channel data
 */
export function buildAndroidStreamConfig(
  title: string,
  androidConfig: {
    url?: string;
    actionType?: 'native' | 'webview' | 'intent';
    headers?: {
      userAgent?: string;
      referrer?: string;
      cookie?: string;
      origin?: string;
    };
    intentUri?: string;
    drmLicenseUrl?: string;
    drmScheme?: 'widevine' | 'clearkey' | 'playready';
    servers?: Array<{ name: string; url: string }>;
  }
): AndroidStreamData {
  const config: AndroidStreamData = {
    url: androidConfig.url || '',
    title,
    actionType: androidConfig.actionType || 'native',
  };

  // Add headers if any are provided
  if (androidConfig.headers) {
    config.headers = {};
    if (androidConfig.headers.userAgent) {
      config.headers['User-Agent'] = androidConfig.headers.userAgent;
    }
    if (androidConfig.headers.referrer) {
      config.headers['Referer'] = androidConfig.headers.referrer;
    }
    if (androidConfig.headers.cookie) {
      config.headers['Cookie'] = androidConfig.headers.cookie;
    }
    if (androidConfig.headers.origin) {
      config.headers['Origin'] = androidConfig.headers.origin;
    }
  }

  // Add DRM if provided
  if (androidConfig.drmLicenseUrl || androidConfig.drmScheme) {
    config.drm = {
      licenseUrl: androidConfig.drmLicenseUrl,
      scheme: androidConfig.drmScheme || 'clearkey',
    };
  }

  // Add intent URI if provided
  if (androidConfig.intentUri) {
    config.intentUri = androidConfig.intentUri;
  }

  // Add servers if provided
  if (androidConfig.servers && androidConfig.servers.length > 0) {
    config.servers = androidConfig.servers;
  }

  return config;
}

/**
 * Play a channel using the appropriate method (Android native or web player)
 */
export function playChannel(
  title: string,
  webConfig: {
    url: string;
    playerType?: 'default' | 'custom' | 'iframe';
    drm?: string;
    headers?: string;
  },
  androidConfig?: {
    url?: string;
    actionType?: 'native' | 'webview' | 'intent';
    headers?: {
      userAgent?: string;
      referrer?: string;
      cookie?: string;
      origin?: string;
    };
    intentUri?: string;
    drmLicenseUrl?: string;
    drmScheme?: 'widevine' | 'clearkey' | 'playready';
    servers?: Array<{ name: string; url: string }>;
  }
): boolean {
  // If running in Android app and we have Android config, use native player
  if (isAndroidApp() && androidConfig?.url) {
    const streamData = buildAndroidStreamConfig(title, androidConfig);
    return sendToAndroid(streamData);
  }

  // Fallback to web player
  if (typeof (window as any).openProPlayer === 'function') {
    (window as any).openProPlayer(
      webConfig.url,
      title,
      webConfig.drm,
      webConfig.headers,
      webConfig.playerType
    );
    return true;
  }

  return false;
}
