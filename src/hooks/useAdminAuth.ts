import { useState, useEffect } from 'react';
import { 
  onAuthStateChanged, 
  signInWithEmailAndPassword, 
  signOut,
  sendPasswordResetEmail,
  User 
} from 'firebase/auth';
import { auth } from '@/lib/firebase';

const ALLOWED_EMAIL = 'hussinmostafa223@gmail.com';

export interface AuthState {
  user: User | null;
  loading: boolean;
  error: string | null;
  isAuthorized: boolean;
}

export const useAdminAuth = () => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    loading: true,
    error: null,
    isAuthorized: false
  });

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        const isAuthorized = user.email?.toLowerCase() === ALLOWED_EMAIL.toLowerCase();
        setAuthState({
          user,
          loading: false,
          error: isAuthorized ? null : 'تم رفض الوصول: هذا الحساب غير مخوّل.',
          isAuthorized
        });
        
        // Sign out unauthorized users
        if (!isAuthorized) {
          signOut(auth);
        }
      } else {
        setAuthState({
          user: null,
          loading: false,
          error: null,
          isAuthorized: false
        });
      }
    });

    return () => unsubscribe();
  }, []);

  const login = async (email: string, password: string) => {
    setAuthState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      // Check email before attempting login
      if (email.toLowerCase() !== ALLOWED_EMAIL.toLowerCase()) {
        setAuthState(prev => ({
          ...prev,
          loading: false,
          error: 'تم رفض الوصول: هذا الحساب غير مخوّل.'
        }));
        return false;
      }

      await signInWithEmailAndPassword(auth, email, password);
      return true;
    } catch (error: any) {
      let errorMessage = 'فشل تسجيل الدخول. الرجاء المحاولة مرة أخرى.';
      
      if (error.code === 'auth/invalid-credential') {
        errorMessage = 'البريد الإلكتروني أو كلمة المرور غير صحيحة.';
      } else if (error.code === 'auth/user-not-found') {
        errorMessage = 'لا يوجد حساب بهذا البريد الإلكتروني.';
      } else if (error.code === 'auth/wrong-password') {
        errorMessage = 'كلمة المرور غير صحيحة.';
      } else if (error.code === 'auth/too-many-requests') {
        errorMessage = 'محاولات كثيرة. الرجاء المحاولة لاحقاً.';
      }
      
      setAuthState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage
      }));
      return false;
    }
  };

  const logout = async () => {
    try {
      await signOut(auth);
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  const resetPassword = async (email: string) => {
    try {
      await sendPasswordResetEmail(auth, email);
      return { success: true, message: 'تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك.' };
    } catch (error: any) {
      return { 
        success: false, 
        message: error.code === 'auth/user-not-found' 
          ? 'لا يوجد حساب بهذا البريد الإلكتروني.' 
          : 'فشل إرسال رابط إعادة التعيين.'
      };
    }
  };

  return {
    ...authState,
    login,
    logout,
    resetPassword
  };
};
