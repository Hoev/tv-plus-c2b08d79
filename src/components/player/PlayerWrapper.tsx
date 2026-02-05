import React, { useState, useEffect } from 'react';
import ProPlayer from './ProPlayer';
import JWPlayer from './JWPlayer';
import type { PlayerType } from '@/types/admin';

interface StreamData {
  url: string;
  title: string;
  drm?: string;
  headers?: string;
  preferredPlayer?: PlayerType;
}

const PlayerWrapper: React.FC = () => {
  const [stream, setStream] = useState<StreamData | null>(null);

  useEffect(() => {
    // Expose global function
    (window as any).openProPlayer = (url: string, title: string, drm?: string, headers?: string, preferredPlayer?: PlayerType) => {
      setStream({ url, title, drm, headers, preferredPlayer });
      history.pushState({ player: true }, '');
    };

    // Listen for custom event
    const handleOpen = (e: CustomEvent<StreamData>) => setStream(e.detail);
    window.addEventListener('open-player', handleOpen as EventListener);
    
    return () => {
      window.removeEventListener('open-player', handleOpen as EventListener);
    };
  }, []);

  if (!stream) return null;

  // Render JWPlayer if preferred, otherwise default ProPlayer
  if (stream.preferredPlayer === 'jwplayer') {
    return (
      <JWPlayer
        url={stream.url}
        title={stream.title}
        drm={stream.drm}
        onClose={() => setStream(null)}
      />
    );
  }

  return <ProPlayer stream={stream} onClose={() => setStream(null)} />;
};

export default PlayerWrapper;
