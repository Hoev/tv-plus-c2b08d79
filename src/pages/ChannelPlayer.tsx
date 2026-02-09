import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import PlayerWrapper from '@/components/player/PlayerWrapper';
import { useIptvData } from '@/hooks/useIptvData';
import { isAndroidApp, sendToAndroid, buildAndroidStreamConfig } from '@/lib/androidBridge';
import type { Channel, SubChannel, StreamConfig, AndroidStreamConfig, PlayerType } from '@/types/admin';

/**
 * Direct Channel Player Page
 * URL format: /channel/:slug
 * Slug is generated from channel name (spaces -> hyphens, lowercase)
 */
const ChannelPlayer: React.FC = () => {
  const params = useParams<{ slug: string; '*': string }>();
  const navigate = useNavigate();
  const { categories, sideMenus, loading } = useIptvData();
  const [error, setError] = useState<string | null>(null);
  const [playerOpened, setPlayerOpened] = useState(false);
  
  // Extract slug from URL - supports /channel/:slug and /:slug-player formats
  const slug = React.useMemo(() => {
    if (params.slug) {
      // Remove "-player" suffix if present (for /:slug-player route)
      return params.slug.replace(/-player$/, '');
    }
    return '';
  }, [params.slug]);

  // Build DRM string from config
  const buildDrmString = (drm: StreamConfig['drm']): string | undefined => {
    if (!drm) return undefined;
    
    const mode = drm.clearKeyMode || 'separate';
    
    if (mode === 'combined' && drm.clearKeyCombined) {
      return drm.clearKeyCombined;
    }
    
    if (mode === 'url' && drm.clearKeyUrl) {
      return drm.clearKeyUrl;
    }
    
    if (drm.clearKeyId && drm.clearKeyKey) {
      return `${drm.clearKeyId}:${drm.clearKeyKey}`;
    }
    
    return undefined;
  };

  // Build Android DRM config
  const buildAndroidDrmConfig = (androidStream?: AndroidStreamConfig) => {
    if (!androidStream) return {};
    
    let drmKeyId = androidStream.drmKeyId;
    let drmKey = androidStream.drmKey;
    
    if (androidStream.drmClearKeyMode === 'combined' && androidStream.drmClearKeyCombined) {
      const parts = androidStream.drmClearKeyCombined.split(':');
      if (parts.length === 2) {
        drmKeyId = parts[0];
        drmKey = parts[1];
      }
    }
    
    return {
      drmLicenseUrl: androidStream.drmLicenseUrl,
      drmScheme: androidStream.drmScheme,
      drmKeyId,
      drmKey,
    };
  };

  // Convert channel name to slug
  const nameToSlug = (name: string): string => {
    return name
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\w\-]/g, '')
      .replace(/--+/g, '-')
      .replace(/^-+|-+$/g, '');
  };

  // Find channel by slug
  const findChannelBySlug = (): Channel | SubChannel | null => {
    if (!slug) return null;
    
    // Search main channels
    for (const category of Object.values(categories)) {
      for (const channel of Object.values(category.channels || {})) {
        if (nameToSlug(channel.name) === slug) {
          return channel;
        }
      }
    }
    
    // Search side menu channels
    for (const menu of Object.values(sideMenus)) {
      for (const subChannel of Object.values(menu.channels || {})) {
        if (nameToSlug(subChannel.name) === slug) {
          return subChannel;
        }
      }
    }
    
    return null;
  };

  // Open player with channel
  const openPlayer = (channel: Channel | SubChannel) => {
    const isSubChannel = !('actionType' in channel);
    
    // For Android app
    if (isAndroidApp()) {
      const androidStream = channel.androidStream;
      const drmConfig = buildAndroidDrmConfig(androidStream);
      
      const streamData = buildAndroidStreamConfig(channel.name, {
        url: androidStream?.url || (channel as any).stream?.url,
        actionType: (channel as any).androidActionType || 'native',
        headers: androidStream?.headers || {
          userAgent: (channel as any).stream?.userAgent,
          referrer: (channel as any).stream?.referrer,
          cookie: (channel as any).stream?.cookies,
        },
        intentUri: androidStream?.intentUri,
        servers: androidStream?.servers,
        ...drmConfig,
      });
      
      if (sendToAndroid(streamData)) {
        return;
      }
    }

    // Web player
    const stream = (channel as any).stream as StreamConfig | undefined;
    if (!stream?.url) {
      setError('لا يوجد رابط بث لهذه القناة');
      return;
    }

    const drm = buildDrmString(stream.drm);
    const headers: Record<string, string> = {};
    if (stream.userAgent) headers['User-Agent'] = stream.userAgent;
    if (stream.referrer) headers['Referer'] = stream.referrer;
    if (stream.cookies) headers['Cookie'] = stream.cookies;
    const headersStr = Object.keys(headers).length ? JSON.stringify(headers) : undefined;

    const preferredPlayer = (channel as any).preferredPlayer as PlayerType | undefined;

    if ((window as any).openProPlayer) {
      (window as any).openProPlayer(stream.url, channel.name, drm, headersStr, preferredPlayer);
      setPlayerOpened(true);
    }
  };

  useEffect(() => {
    if (loading || playerOpened) return;

    const channel = findChannelBySlug();
    
    if (channel) {
      openPlayer(channel);
    } else if (!loading) {
      setError('القناة غير موجودة');
    }
  }, [slug, categories, sideMenus, loading, playerOpened]);

  // Handle player close
  useEffect(() => {
    const handlePopState = () => {
      navigate('/');
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [navigate]);

  if (loading) {
    return (
      <div className="fixed inset-0 bg-black flex items-center justify-center">
        <div className="w-16 h-16 border-4 border-white/20 border-t-[hsl(45,100%,50%)] rounded-full animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="fixed inset-0 bg-black flex flex-col items-center justify-center text-white p-4">
        <p className="text-xl mb-4">{error}</p>
        <button
          onClick={() => navigate('/')}
          className="px-6 py-3 bg-white/10 hover:bg-white/20 rounded-full border border-white/20"
        >
          العودة للرئيسية
        </button>
      </div>
    );
  }

  return <PlayerWrapper />;
};

export default ChannelPlayer;
