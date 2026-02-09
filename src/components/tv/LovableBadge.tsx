import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';

const STORAGE_KEY = 'lovable_badge_dismissed';

/**
 * Dismissible Lovable badge that shows once per day
 * Appears in bottom right with X button to close
 */
const LovableBadge: React.FC = () => {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    // Check if badge was dismissed today
    const lastDismissed = localStorage.getItem(STORAGE_KEY);
    if (lastDismissed) {
      const dismissedDate = new Date(lastDismissed);
      const today = new Date();
      // If dismissed today, don't show
      if (
        dismissedDate.getDate() === today.getDate() &&
        dismissedDate.getMonth() === today.getMonth() &&
        dismissedDate.getFullYear() === today.getFullYear()
      ) {
        return;
      }
    }
    // Show badge after a short delay
    const timer = setTimeout(() => setVisible(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  const handleDismiss = () => {
    setVisible(false);
    localStorage.setItem(STORAGE_KEY, new Date().toISOString());
  };

  if (!visible) return null;

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '80px',
        right: '16px',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        background: 'rgba(0, 0, 0, 0.85)',
        borderRadius: '24px',
        padding: '8px 12px',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.5)',
      }}
    >
      {/* X Close Button */}
      <button
        onClick={handleDismiss}
        tabIndex={0}
        aria-label="Dismiss"
        style={{
          background: 'rgba(255, 255, 255, 0.1)',
          border: 'none',
          borderRadius: '50%',
          width: '28px',
          height: '28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          color: '#fff',
          padding: 0,
        }}
      >
        <X size={16} />
      </button>

      {/* Lovable Logo & Text */}
      <a
        href="https://lovable.dev"
        target="_blank"
        rel="noopener noreferrer"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          color: '#fff',
          textDecoration: 'none',
          fontSize: '13px',
          fontWeight: 500,
        }}
      >
        <span style={{ fontSize: '18px' }}>🧡</span>
        <span>Lovable</span>
        <span style={{ color: 'rgba(255,255,255,0.5)' }}>Edit with</span>
      </a>
    </div>
  );
};

export default LovableBadge;
