export type ActionType = 'direct_play' | 'open_submenu' | 'external_link';

// Web-specific player types
export type WebPlayerType = 'default' | 'custom' | 'iframe';

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
  servers?: Array<{ name: string; url: string }>; // Multi-server support
}

export interface SubChannel {
  id: string;
  name: string;
  imageUrl: string;
  stream: StreamConfig;
  sortOrder: number;
  preferredPlayer?: WebPlayerType;
  
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
  
  // === Web Settings ===
  stream?: StreamConfig;
  sideMenuId?: string;
  externalUrl?: string; // For external link redirection
  preferredPlayer?: WebPlayerType; // Per-channel web player selection
  
  // === Android Settings ===
  androidStream?: AndroidStreamConfig;
  androidActionType?: AndroidActionType; // 'native', 'webview', 'intent'
}

export interface Category {
  id: string;
  name: string;
  sortOrder: number;
  channels: Record<string, Channel>;
}

export interface AdminData {
  categories: Record<string, Category>;
  sideMenus: Record<string, SideMenu>;
}

// Legacy type alias for backward compatibility
export type PlayerType = WebPlayerType;
