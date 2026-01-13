import axios, { AxiosError, AxiosInstance } from 'axios';
import { API_URL, logger } from '@/lib/config';

logger.log('API_BASE_URL:', API_URL);

// Public API client (no authentication)
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Admin API client (with authentication)
export const adminClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Admin auth interceptor
adminClient.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const auth = localStorage.getItem('admin-auth');
      if (auth) {
        config.headers.Authorization = `Basic ${auth}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Admin response interceptor (401 redirect)
adminClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('admin-auth');
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);

// API Error type
export type ApiError = {
  message: string;
  status: number;
  details?: unknown;
};

// Error handler
export const handleApiError = (error: unknown): ApiError => {
  if (axios.isAxiosError(error)) {
    return {
      message: error.response?.data?.message || error.message,
      status: error.response?.status || 500,
      details: error.response?.data,
    };
  }
  return {
    message: 'An unexpected error occurred',
    status: 500,
  };
};

