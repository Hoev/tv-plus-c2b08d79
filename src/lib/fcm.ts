import { getMessaging, getToken, onMessage } from 'firebase/messaging';
import { db } from './firebase';
import { ref, push, get, set, query, orderByChild, equalTo } from 'firebase/database';

const VAPID_KEY = 'BM7kORUWGDkcUnjshA5UoA8dDIajLk_StvHlLixh6c5S1Gy6GxhQjh0matJlMF-waZ1GeFe075hehx1-JJ1GHxg';

export type NotificationActionType = 'main_channel' | 'side_menu' | 'sub_channel' | 'external_link';

export interface NotificationPayload {
  title: string;
  body: string;
  actionType: NotificationActionType;
  targetId?: string; // channel id, menu id, or sub-channel id
  parentMenuId?: string; // for sub-channel, the parent menu id
  externalUrl?: string;
}

export const requestNotificationPermission = async (): Promise<string | null> => {
  try {
    // Check if browser supports notifications
    if (!('Notification' in window)) {
      console.error('This browser does not support notifications');
      return null;
    }

    // Request permission
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      console.log('Notification permission denied');
      return null;
    }

    // Register service worker
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
    console.log('Service worker registered:', registration);

    // Get FCM token using the messaging instance from firebase.ts
    const { getApp } = await import('firebase/app');
    const app = getApp();
    const messaging = getMessaging(app);
    
    const token = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: registration
    });

    if (token) {
      console.log('FCM Token:', token);
      await saveTokenToDatabase(token);
      return token;
    }

    return null;
  } catch (error) {
    console.error('Error getting notification permission:', error);
    return null;
  }
};

// Check if already prompted for notifications
export const hasPromptedForNotifications = (): boolean => {
  return localStorage.getItem('notification_prompted') === 'true';
};

export const setNotificationPrompted = (): void => {
  localStorage.setItem('notification_prompted', 'true');
};

// Auto-prompt for notifications on first visit
export const autoPromptNotifications = async (): Promise<void> => {
  if (hasPromptedForNotifications()) {
    return;
  }
  
  setNotificationPrompted();
  
  // Small delay to let the app load first
  setTimeout(async () => {
    await requestNotificationPermission();
  }, 2000);
};

export const saveTokenToDatabase = async (token: string): Promise<void> => {
  try {
    const tokensRef = ref(db, 'fcm_tokens');
    
    // Check if token already exists
    const snapshot = await get(tokensRef);
    if (snapshot.exists()) {
      const tokens = snapshot.val();
      const existingKey = Object.keys(tokens).find(key => tokens[key].token === token);
      if (existingKey) {
        console.log('Token already exists in database');
        return;
      }
    }

    // Save new token
    await push(tokensRef, {
      token,
      platform: 'web',
      createdAt: Date.now()
    });
    
    console.log('Token saved to Realtime Database');
  } catch (error) {
    console.error('Error saving token to database:', error);
  }
};

export const getAllTokens = async (): Promise<string[]> => {
  try {
    const tokensRef = ref(db, 'fcm_tokens');
    const snapshot = await get(tokensRef);
    
    if (!snapshot.exists()) {
      return [];
    }
    
    const tokens = snapshot.val();
    return Object.values(tokens).map((item: any) => item.token as string);
  } catch (error) {
    console.error('Error fetching tokens:', error);
    return [];
  }
};

export const getTokenCount = async (): Promise<number> => {
  const tokens = await getAllTokens();
  return tokens.length;
};

export const setupForegroundNotifications = (onNotificationReceived?: (payload: any) => void) => {
  try {
    import('firebase/app').then(async ({ getApp }) => {
      const app = getApp();
      const messaging = getMessaging(app);
      
      onMessage(messaging, (payload) => {
        console.log('Foreground notification received:', payload);
        
        // Show notification manually in foreground
        if (Notification.permission === 'granted') {
          const notification = new Notification(payload.notification?.title || 'TV PLUS', {
            body: payload.notification?.body,
            icon: '/favicon.ico',
            data: payload.data
          });

          notification.onclick = () => {
            handleNotificationClick(payload.data);
            notification.close();
          };
        }
        
        if (onNotificationReceived) {
          onNotificationReceived(payload);
        }
      });
    });
  } catch (error) {
    console.error('Error setting up foreground notifications:', error);
  }
};

// Handle notification click based on action type
export const handleNotificationClick = (data: any) => {
  if (!data) return;
  
  const actionType = data.actionType as NotificationActionType;
  
  switch (actionType) {
    case 'external_link':
      if (data.externalUrl) {
        window.open(data.externalUrl, '_blank');
      }
      break;
    case 'main_channel':
      if (data.targetId) {
        // Navigate to main page with channel selected
        window.location.href = `/?channel=${data.targetId}`;
      }
      break;
    case 'side_menu':
      if (data.targetId) {
        // Navigate to main page with side menu open
        window.location.href = `/?menu=${data.targetId}`;
      }
      break;
    case 'sub_channel':
      if (data.targetId && data.parentMenuId) {
        // Navigate to sub-channel
        window.location.href = `/?menu=${data.parentMenuId}&subchannel=${data.targetId}`;
      }
      break;
    default:
      window.location.href = '/';
  }
};

// Listen for notification clicks from service worker
export const setupDeepLinkListener = () => {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('message', (event) => {
      if (event.data?.type === 'NOTIFICATION_CLICK') {
        handleNotificationClick(event.data);
      }
    });
  }
};
