export type ActionType = 'direct_play' | 'open_submenu' | 'external_link';

export type ClearKeyMode = 'separate' | 'combined' | 'url';

export interface DRMConfig {
  clearKeyId?: string;
  clearKeyKey?: string;
  clearKeyCombined?: string; // Format: "KeyID:Key"
  clearKeyUrl?: string; // URL that returns keys
  clearKeyMode?: ClearKeyMode;
}

export interface StreamConfig {
  url: string;
  userAgent?: string;
  referrer?: string;
  cookies?: string;
  drm?: DRMConfig;
}

export interface SubChannel {
  id: string;
  name: string;
  imageUrl: string;
  stream: StreamConfig;
  sortOrder: number;
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
  stream?: StreamConfig;
  sideMenuId?: string;
  externalUrl?: string; // For external link redirection
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
