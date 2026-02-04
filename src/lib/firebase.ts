import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getDatabase } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyBxT35NMrvWYPJRvWek_NKeu8QtNInISC4",
  authDomain: "cinema-plus-d1238.firebaseapp.com",
  projectId: "cinema-plus-d1238",
  storageBucket: "cinema-plus-d1238.firebasestorage.app",
  messagingSenderId: "659730944639",
  appId: "1:659730944639:web:1c00b6f7118bf85bdde54a",
  measurementId: "G-GHC58Z4YY6",
  databaseURL: "https://cinema-plus-d1238-default-rtdb.firebaseio.com"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getDatabase(app);
