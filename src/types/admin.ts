export type ActionType = 'direct_play' | 'open_submenu' | 'external_link';

// Web-specific player types
export type WebPlayerType = 'default' | 'custom' | 'iframe' | 'secure' | 'external_ios';

// iOS external player apps
export type iOSPlayerApp = 'vlc' | 'nplayer' | 'infuse' | 'outplayer';

export interface iOSPlayerConfig {
  app: iOSPlayerApp;
  fallbackToWeb?: boolean; // If app not installed, play in secure player
}

// Android-specific action types
export type AndroidActionType = 'native' | 'webview' | 'intent';

// DRM schemes for Android
export type DrmScheme = 'widevine' | 'clearkey' | 'playready';

export type ClearKeyMode = 'separate' | 'combined' | 'url';

export interface DRMConfig {
  clearKeyId?: string;
  clearKeyKey?: string;
  clearKeyCombined?: string; // Format: "KeyID:Key"
  clearKeyUrl?: string; // URL that returns keys
  clearKeyMode?: ClearKeyMode;
}

// Headers configuration (shared structure)
export interface StreamHeaders {
  userAgent?: string;
  referrer?: string;
  cookie?: string;
  origin?: string;
}

export interface StreamConfig {
  url: string;
  userAgent?: string;
  referrer?: string;
  cookies?: string;
  drm?: DRMConfig;
}

// Web-specific stream configuration
export interface WebStreamConfig {
  url: string;
  headers?: StreamHeaders;
  drm?: DRMConfig;
}

// Android-specific stream configuration
export interface AndroidStreamConfig {
  url: string;
  headers?: StreamHeaders;
  intentUri?: string; // For launching external apps
  drmLicenseUrl?: string;
  drmScheme?: DrmScheme;
  drmKeyId?: string; // ClearKey Key ID
  drmKey?: string; // ClearKey Key
  drmClearKeyCombined?: string; // Combined format: KeyID:Key
  drmClearKeyMode?: ClearKeyMode; // Which input mode is used
  servers?: Array<{ name: string; url: string }>; // Multi-server support
}

export interface SubChannel {
  id: string;
  name: string;
  imageUrl: string;
  stream: StreamConfig;
  sortOrder: number;
  preferredPlayer?: WebPlayerType;
  iosPlayerApp?: iOSPlayerApp;
  hidden?: boolean;
  
  // Android-specific
  androidStream?: AndroidStreamConfig;
  androidActionType?: AndroidActionType;
}

export interface SideMenu {
  id: string;
  name: string;
  // Stored in Realtime Database as an object keyed by channel id
  channels: Record<string, SubChannel>;
}

export interface Channel {
  id: string;
  name: string;
  imageUrl: string;
  sortOrder: number;
  actionType: ActionType;
  hidden?: boolean;
  
  // === Web Settings ===
  stream?: StreamConfig;
  sideMenuId?: string;
  externalUrl?: string;
  preferredPlayer?: WebPlayerType;
  iosPlayerApp?: iOSPlayerApp;
  
  // === Android Settings ===
  androidStream?: AndroidStreamConfig;
  androidActionType?: AndroidActionType;
}

export interface Category {
  id: string;
  name: string;
  sortOrder: number;
  channels: Record<string, Channel>;
  hidden?: boolean;
  adGateEnabled?: boolean; // Require watching ad before accessing this section
}

// AdMob configuration stored in Firebase
export interface AdConfig {
  admobBannerId?: string;
  admobInterstitialId?: string;
  admobRewardedId?: string;
  adsEnabled?: boolean;
}

export interface AdminData {
  categories: Record<string, Category>;
  sideMenus: Record<string, SideMenu>;
}

// Legacy type alias for backward compatibility
export type PlayerType = WebPlayerType;
