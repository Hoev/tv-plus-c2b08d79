// Firebase Messaging Service Worker for Push Notifications
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyBxT35NMrvWYPJRvWek_NKeu8QtNInISC4",
  authDomain: "cinema-plus-d1238.firebaseapp.com",
  projectId: "cinema-plus-d1238",
  storageBucket: "cinema-plus-d1238.firebasestorage.app",
  messagingSenderId: "659730944639",
  appId: "1:659730944639:web:1c00b6f7118bf85bdde54a",
  measurementId: "G-GHC58Z4YY6"
});

const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Received background message:', payload);

  const notificationTitle = payload.notification?.title || 'TV PLUS';
  const notificationOptions = {
    body: payload.notification?.body || '',
    icon: '/favicon.ico',
    badge: '/favicon.ico',
    data: payload.data || {},
    vibrate: [100, 50, 100],
    actions: [
      { action: 'open', title: 'فتح' }
    ]
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click - Deep linking support
self.addEventListener('notificationclick', (event) => {
  console.log('[firebase-messaging-sw.js] Notification clicked:', event);
  event.notification.close();

  const data = event.notification.data || {};
  const actionType = data.actionType;
  
  let targetUrl = '/';
  
  // Build URL based on action type
  switch (actionType) {
    case 'external_link':
      if (data.externalUrl) {
        targetUrl = data.externalUrl;
      }
      break;
    case 'main_channel':
      if (data.targetId) {
        targetUrl = `/?channel=${data.targetId}`;
      }
      break;
    case 'side_menu':
      if (data.targetId) {
        targetUrl = `/?menu=${data.targetId}`;
      }
      break;
    case 'sub_channel':
      if (data.targetId && data.parentMenuId) {
        targetUrl = `/?menu=${data.parentMenuId}&subchannel=${data.targetId}`;
      }
      break;
  }
  
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // For external links, open in new window
      if (actionType === 'external_link' && data.externalUrl) {
        return clients.openWindow(data.externalUrl);
      }
      
      // Try to focus existing window
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.focus();
          client.postMessage({
            type: 'NOTIFICATION_CLICK',
            actionType: actionType,
            targetId: data.targetId,
            parentMenuId: data.parentMenuId,
            externalUrl: data.externalUrl
          });
          return;
        }
      }
      // Open new window if none exists
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});
