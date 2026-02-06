/**
 * Android Bridge - Communication layer between Web App and Android Native Shell
 */

interface AndroidInterface {
  playVideo: (jsonConfig: string) => void;
  showToast: (message: string) => void;
  isAndroidApp: () => boolean;
}

interface StreamData {
  url: string;
  title: string;
  headers?: {
    'User-Agent'?: string;
    'Referer'?: string;
    'Cookie'?: string;
    'Origin'?: string;
  };
  drm?: string; // Format: "keyId:key" or license URL
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
 * Send stream data to Android native player
 * Falls back to web player if not in Android app
 */
export function sendToAndroid(streamData: StreamData): boolean {
  if (isAndroidApp() && window.Android) {
    try {
      const jsonConfig = JSON.stringify(streamData);
      window.Android.playVideo(jsonConfig);
      return true;
    } catch (error) {
      console.error('Error sending to Android:', error);
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
      console.error('Error showing Android toast:', error);
      return false;
    }
  }
  return false;
}

/**
 * Build stream config for Android player
 */
export function buildAndroidStreamConfig(
  url: string,
  title: string,
  options?: {
    userAgent?: string;
    referrer?: string;
    cookies?: string;
    drm?: string;
  }
): StreamData {
  const config: StreamData = {
    url,
    title,
  };

  // Add headers if any are provided
  if (options?.userAgent || options?.referrer || options?.cookies) {
    config.headers = {};
    if (options.userAgent) config.headers['User-Agent'] = options.userAgent;
    if (options.referrer) config.headers['Referer'] = options.referrer;
    if (options.cookies) config.headers['Cookie'] = options.cookies;
  }

  // Add DRM if provided
  if (options?.drm) {
    config.drm = options.drm;
  }

  return config;
}
