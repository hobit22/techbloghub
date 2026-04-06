// 인증 관련 유틸리티
export const ADMIN_AUTH_STORAGE_KEY = 'admin-auth';

export const adminAuth = {
  isLoggedIn: (): boolean => {
    if (typeof window === 'undefined') return false;
    return !!localStorage.getItem(ADMIN_AUTH_STORAGE_KEY);
  },

  logout: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
      window.location.href = '/admin/login';
    }
  },

  getAuth: (): string | null => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(ADMIN_AUTH_STORAGE_KEY);
  },

  setAuth: (auth: string): void => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(ADMIN_AUTH_STORAGE_KEY, auth);
    }
  },

  removeAuth: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
    }
  },
};
