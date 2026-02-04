import { getMessaging, getToken, onMessage } from 'firebase/messaging';
import { initializeApp, getApps } from 'firebase/app';
import { getFirestore, collection, addDoc, serverTimestamp, getDocs } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: "AIzaSyBxT35NMrvWYPJRvWek_NKeu8QtNInISC4",
  authDomain: "cinema-plus-d1238.firebaseapp.com",
  projectId: "cinema-plus-d1238",
  storageBucket: "cinema-plus-d1238.firebasestorage.app",
  messagingSenderId: "659730944639",
  appId: "1:659730944639:web:1c00b6f7118bf85bdde54a",
  measurementId: "G-GHC58Z4YY6"
};

const VAPID_KEY = 'BM7kORUWGDkcUnjshA5UoA8dDIajLk_StvHlLixh6c5S1Gy6GxhQjh0matJlMF-waZ1GeFe075hehx1-JJ1GHxg';

// Initialize Firebase app for Firestore (separate from Realtime Database)
const getFirebaseApp = () => {
  if (getApps().length === 0) {
    return initializeApp(firebaseConfig);
  }
  return getApps()[0];
};

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

    // Get FCM token
    const app = getFirebaseApp();
    const messaging = getMessaging(app);
    
    const token = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: registration
    });

    if (token) {
      console.log('FCM Token:', token);
      await saveTokenToFirestore(token);
      return token;
    }

    return null;
  } catch (error) {
    console.error('Error getting notification permission:', error);
    return null;
  }
};

export const saveTokenToFirestore = async (token: string): Promise<void> => {
  try {
    const app = getFirebaseApp();
    const firestore = getFirestore(app);
    
    // Check if token already exists
    const tokensRef = collection(firestore, 'fcm_tokens');
    const snapshot = await getDocs(tokensRef);
    const existingToken = snapshot.docs.find(doc => doc.data().token === token);
    
    if (existingToken) {
      console.log('Token already exists in Firestore');
      return;
    }

    // Save new token
    await addDoc(tokensRef, {
      token,
      platform: 'web',
      createdAt: serverTimestamp()
    });
    
    console.log('Token saved to Firestore');
  } catch (error) {
    console.error('Error saving token to Firestore:', error);
  }
};

export const getAllTokens = async (): Promise<string[]> => {
  try {
    const app = getFirebaseApp();
    const firestore = getFirestore(app);
    const tokensRef = collection(firestore, 'fcm_tokens');
    const snapshot = await getDocs(tokensRef);
    
    return snapshot.docs.map(doc => doc.data().token as string);
  } catch (error) {
    console.error('Error fetching tokens:', error);
    return [];
  }
};

export const setupForegroundNotifications = (onNotificationReceived?: (payload: any) => void) => {
  try {
    const app = getFirebaseApp();
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
          const route = payload.data?.route;
          if (route) {
            window.location.href = route;
          }
          notification.close();
        };
      }
      
      if (onNotificationReceived) {
        onNotificationReceived(payload);
      }
    });
  } catch (error) {
    console.error('Error setting up foreground notifications:', error);
  }
};

// Listen for notification clicks from service worker
export const setupDeepLinkListener = () => {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('message', (event) => {
      if (event.data?.type === 'NOTIFICATION_CLICK' && event.data?.route) {
        window.location.href = event.data.route;
      }
    });
  }
};
