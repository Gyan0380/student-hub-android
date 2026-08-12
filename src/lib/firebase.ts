import { initializeApp, getApps, getApp, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";
import { getFirestore, type Firestore } from "firebase/firestore";
import { getStorage, type FirebaseStorage } from "firebase/storage";
import { getFunctions, type Functions } from "firebase/functions";

/**
 * Firebase client configuration for the StudentHub project (student-a866d).
 * These are public client config values — safe to ship in the browser bundle.
 * Never put Admin SDK / service-account credentials here.
 */
export const firebaseConfig = {
  apiKey: "AIzaSyCDLlqMtCGKfbchKblBNNLec9Y4AkRXL0",
  authDomain: "student-a866d.firebaseapp.com",
  projectId: "student-a866d",
  storageBucket: "student-a866d.firebasestorage.app",
  messagingSenderId: "742359477068",
  appId: "1:742359477068:web:0d8481cbd5032428a9fde9",
  measurementId: "G-LYQXVZ4VFB",
};

/** Lazy init so nothing touches the Firebase SDK during SSR. */
export function firebaseApp(): FirebaseApp {
  return getApps().length ? getApp() : initializeApp(firebaseConfig);
}

export const firebaseAuth = (): Auth => getAuth(firebaseApp());
export const firebaseDb = (): Firestore => getFirestore(firebaseApp());
export const firebaseStorage = (): FirebaseStorage => getStorage(firebaseApp());
export const firebaseFunctions = (): Functions => getFunctions(firebaseApp());

/** Username → auth email mapping used by the existing StudentHub accounts. */
export const emailForUsername = (username: string) =>
  `${username.trim().toLowerCase()}@studentchat.com`;
