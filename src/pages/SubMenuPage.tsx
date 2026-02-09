import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useIptvData } from '@/hooks/useIptvData';
import { isAndroidApp, sendToAndroid, buildAndroidStreamConfig } from '@/lib/androidBridge';
import PlayerWrapper from '@/components/player/PlayerWrapper';
import type { SubChannel, StreamConfig, AndroidStreamConfig } from '@/types/admin';
import type { PlayerType } from '@/types/admin';

// Generate slug from name
const generateSlug = (name: string): string => {
  return name
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\-]/g, '')
    .replace(/--+/g, '-')
    .trim();
};

const SubMenuPage: React.FC = () => {
  const { menuSlug } = useParams<{ menuSlug: string }>();
  const navigate = useNavigate();
  const { sideMenus, loading: dataLoading, error } = useIptvData();
  const [showLoader, setShowLoader] = useState(true);
  const [contentVisible, setContentVisible] = useState(false);

  // Find the menu by slug
  const menu = useMemo(() => {
    if (!menuSlug) return null;
    return Object.values(sideMenus).find(
      (m) => generateSlug(m.name) === menuSlug
    );
  }, [sideMenus, menuSlug]);

  // Get sorted channels
  const channels = useMemo(() => {
    if (!menu?.channels) return [];
    return Object.values(menu.channels).sort((a, b) => a.sortOrder - b.sortOrder);
  }, [menu]);

  // Handle loading transition
  useEffect(() => {
    if (!dataLoading) {
      const timer = setTimeout(() => {
        setShowLoader(false);
        setTimeout(() => {
          setContentVisible(true);
          // Focus first channel
          const firstChannel = document.getElementById('first-submenu-channel');
          if (firstChannel) firstChannel.focus();
        }, 100);
      }, 800);
      return () => clearTimeout(timer);
    }
  }, [dataLoading]);

  // Build DRM string from config
  const buildDrmString = useCallback((drm: StreamConfig['drm']): string | undefined => {
    if (!drm) return undefined;
    const mode = drm.clearKeyMode || 'separate';
    if (mode === 'combined' && drm.clearKeyCombined) return drm.clearKeyCombined;
    if (mode === 'url' && drm.clearKeyUrl) return drm.clearKeyUrl;
    if (drm.clearKeyId && drm.clearKeyKey) return `${drm.clearKeyId}:${drm.clearKeyKey}`;
    return undefined;
  }, []);

  // Handle channel click
  const handleChannelClick = useCallback((channel: SubChannel) => {
    const androidStream = channel.androidStream;
    
    // Build DRM config for Android
    const buildAndroidDrmConfig = (stream?: AndroidStreamConfig) => {
      if (!stream) return {};
      let drmKeyId = stream.drmKeyId;
      let drmKey = stream.drmKey;
      if (stream.drmClearKeyMode === 'combined' && stream.drmClearKeyCombined) {
        const parts = stream.drmClearKeyCombined.split(':');
        if (parts.length === 2) {
          drmKeyId = parts[0];
          drmKey = parts[1];
        }
      }
      return {
        drmLicenseUrl: stream.drmLicenseUrl,
        drmScheme: stream.drmScheme,
        drmKeyId,
        drmKey,
      };
    };

    // Check if running in Android app
    if (isAndroidApp()) {
      const streamUrl = androidStream?.url || channel.stream?.url;
      if (!streamUrl) {
        alert('لا يوجد رابط بث لهذه القناة.');
        return;
      }
      const drmConfig = buildAndroidDrmConfig(androidStream);
      const streamData = buildAndroidStreamConfig(channel.name, {
        url: streamUrl,
        actionType: channel.androidActionType || 'native',
        headers: androidStream?.headers || {
          userAgent: channel.stream?.userAgent,
          referrer: channel.stream?.referrer,
          cookie: channel.stream?.cookies,
        },
        intentUri: androidStream?.intentUri,
        servers: androidStream?.servers,
        ...drmConfig,
      });
      if (sendToAndroid(streamData)) return;
    }

    // Web player fallback
    if (!channel.stream?.url) {
      alert('لا يوجد رابط بث لهذه القناة.');
      return;
    }
    const drm = buildDrmString(channel.stream.drm);
    const headers: Record<string, string> = {};
    if (channel.stream.userAgent) headers['User-Agent'] = channel.stream.userAgent;
    if (channel.stream.referrer) headers['Referer'] = channel.stream.referrer;
    if (channel.stream.cookies) headers['Cookie'] = channel.stream.cookies;
    const headersStr = Object.keys(headers).length ? JSON.stringify(headers) : undefined;
    if (window.openProPlayer) {
      window.openProPlayer(channel.stream.url, channel.name, drm, headersStr, channel.preferredPlayer);
    }
  }, [buildDrmString]);

  // TV remote navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!contentVisible) return;

      const focusable = Array.from(document.querySelectorAll('.submenu-channel-card[tabindex="0"]')) as HTMLElement[];
      const index = focusable.indexOf(document.activeElement as HTMLElement);
      
      // Get grid columns dynamically
      const grid = document.getElementById('submenu-grid');
      if (!grid) return;
      const styles = window.getComputedStyle(grid);
      const template = styles.getPropertyValue('grid-template-columns');
      const columns = template.split(' ').length;

      if (index === -1 && focusable.length > 0) {
        focusable[0].focus();
        return;
      }

      switch (e.key) {
        case 'ArrowRight':
          if (index > 0) focusable[index - 1].focus();
          break;
        case 'ArrowLeft':
          if (index < focusable.length - 1) focusable[index + 1].focus();
          break;
        case 'ArrowUp':
          if (index >= columns) focusable[index - columns].focus();
          break;
        case 'ArrowDown':
          if (index + columns < focusable.length) focusable[index + columns].focus();
          break;
        case 'Backspace':
        case 'Escape':
          navigate('/');
          break;
        case 'Enter':
          (document.activeElement as HTMLElement)?.click();
          break;
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [contentVisible, navigate]);

  // Not found state
  if (!dataLoading && !menu) {
    return (
      <div className="submenu-page">
        <div className="submenu-header">
          <h1>TV <span>PLUS</span></h1>
        </div>
        <div style={{ textAlign: 'center', padding: '60px 20px', color: 'hsl(var(--text-gray))' }}>
          <p>القائمة غير موجودة</p>
          <button
            onClick={() => navigate('/')}
            style={{
              marginTop: '20px',
              padding: '12px 24px',
              background: 'hsl(var(--gold))',
              color: '#000',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            العودة للرئيسية
          </button>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="submenu-page">
        {/* Loader */}
        <div 
          className="submenu-loader"
          style={{
            opacity: showLoader ? 1 : 0,
            pointerEvents: showLoader ? 'auto' : 'none',
          }}
        >
          <div className="spinner" />
        </div>

        {/* Header */}
        <header className="submenu-header">
          <button 
            className="submenu-back-btn"
            onClick={() => navigate('/')}
            tabIndex={0}
          >
            <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
              <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
            </svg>
          </button>
          <h1>TV <span>PLUS</span></h1>
        </header>

        {/* Menu Title */}
        <h2 className="submenu-title">{menu?.name?.toUpperCase() || 'CHANNELS'}</h2>

        {/* Channels Grid */}
        <div 
          className="submenu-grid" 
          id="submenu-grid"
          style={{
            display: contentVisible ? 'grid' : 'none',
            opacity: contentVisible ? 1 : 0,
          }}
        >
          {channels.map((channel, idx) => (
            <div
              key={channel.id}
              id={idx === 0 ? 'first-submenu-channel' : undefined}
              className="submenu-channel-card"
              tabIndex={0}
              onClick={() => handleChannelClick(channel)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleChannelClick(channel); }}
            >
              <img
                src={channel.imageUrl || 'https://via.placeholder.com/300x170?text=TV'}
                alt={channel.name}
                onError={(e) => {
                  (e.target as HTMLImageElement).src = 'https://via.placeholder.com/300x170?text=TV';
                }}
              />
              <div className="submenu-card-overlay">
                <span className="submenu-card-title">{channel.name}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {contentVisible && channels.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px', color: 'hsl(var(--text-gray))' }}>
            <p>لا توجد قنوات في هذه القائمة</p>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div style={{ textAlign: 'center', padding: '40px', color: 'hsl(var(--text-gray))' }}>
            <p>خطأ في تحميل البيانات</p>
            <p style={{ fontSize: '14px', marginTop: '10px' }}>{error}</p>
          </div>
        )}
      </div>

      {/* Player Container */}
      <PlayerWrapper />
    </>
  );
};

export default SubMenuPage;
