import React, { useState, useEffect } from 'react';

const SettingsSection: React.FC = () => {
  const [darkMode, setDarkMode] = useState(true);

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'light') {
      setDarkMode(false);
      document.body.classList.add('light-mode');
    } else {
      setDarkMode(true);
      document.body.classList.remove('light-mode');
    }
  }, []);

  const toggleDarkMode = () => {
    const newValue = !darkMode;
    setDarkMode(newValue);
    if (newValue) {
      document.body.classList.remove('light-mode');
      localStorage.setItem('theme', 'dark');
    } else {
      document.body.classList.add('light-mode');
      localStorage.setItem('theme', 'light');
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div className="setting-row" tabIndex={0} onClick={toggleDarkMode}>
        <span style={{ fontWeight: 'bold' }}>Dark Mode</span>
        <label className="ios-switch">
          <input type="checkbox" checked={darkMode} onChange={() => {}} />
          <span className="slider" />
        </label>
      </div>
      <div className="setting-row" tabIndex={0}>
        <span style={{ fontWeight: 'bold' }}>Telegram Channel</span>
        <span style={{ color: 'hsl(var(--gold))' }}>❯</span>
      </div>
      <div className="setting-row" tabIndex={0}>
        <span style={{ fontWeight: 'bold' }}>Contact Us</span>
        <span style={{ color: 'hsl(var(--gold))' }}>❯</span>
      </div>
    </div>
  );
};

export default SettingsSection;
