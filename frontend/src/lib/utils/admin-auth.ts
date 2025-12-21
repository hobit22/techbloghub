// 인증 관련 유틸리티
export const adminAuth = {
  isLoggedIn: (): boolean => {
    if (typeof window === 'undefined') return false;
    return !!localStorage.getItem('admin-auth');
  },

  logout: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('admin-auth');
      window.location.href = '/admin/login';
    }
  },

  getAuth: (): string | null => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('admin-auth');
  },

  setAuth: (auth: string): void => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('admin-auth', auth);
    }
  },

  removeAuth: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('admin-auth');
    }
  },
};
