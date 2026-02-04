import React, { useState, useEffect } from 'react';
import ProPlayer from './ProPlayer';

interface StreamData {
  url: string;
  title: string;
  drm?: string;
  headers?: string;
}

// Global function to open player
declare global {
  interface Window {
    openProPlayer: (url: string, title: string, drm?: string, headers?: string) => void;
  }
}

const PlayerWrapper: React.FC = () => {
  const [stream, setStream] = useState<StreamData | null>(null);

  useEffect(() => {
    // Expose global function
    window.openProPlayer = (url: string, title: string, drm?: string, headers?: string) => {
      setStream({ url, title, drm, headers });
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
  
  return <ProPlayer stream={stream} onClose={() => setStream(null)} />;
};

export default PlayerWrapper;
