import axios from 'axios';

const API = axios.create({
  baseURL: process.env.REACT_APP_API_URL || '/api',
  timeout: 30000,
});

// ── Token storage helpers ─────────────────────────────────────────────────────
const getAccessToken  = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');
const setTokens = (access, refresh) => {
  localStorage.setItem('accessToken',  access);
  localStorage.setItem('refreshToken', refresh);
};
const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('playerId');
};

// ── Request interceptor: attach Bearer token ──────────────────────────────────
API.interceptors.request.use(config => {
  const token = getAccessToken();
  if (token) config.headers['Authorization'] = `Bearer ${token}`;
  return config;
});

// ── Response interceptor: silent token refresh on 401 ────────────────────────
let isRefreshing = false;
let failedQueue  = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(p => error ? p.reject(error) : p.resolve(token));
  failedQueue = [];
};

API.interceptors.response.use(
  res => res,
  async err => {
    const original = err.config;
    if (err.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          original.headers['Authorization'] = `Bearer ${token}`;
          return API(original);
        });
      }
      original._retry = true;
      isRefreshing = true;
      try {
        const refresh = getRefreshToken();
        if (!refresh) throw new Error('No refresh token');
        const { data } = await axios.post('/api/auth/refresh', { refreshToken: refresh });
        setTokens(data.accessToken, data.refreshToken);
        processQueue(null, data.accessToken);
        original.headers['Authorization'] = `Bearer ${data.accessToken}`;
        return API(original);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        clearTokens();
        window.location.href = '/';
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(err);
  }
);

// ── Auth endpoints ────────────────────────────────────────────────────────────
export const register = async (username, password) => {
  const { data } = await API.post('/auth/register', { username, password });
  setTokens(data.accessToken, data.refreshToken);
  return data;
};

export const login = async (username, password) => {
  const { data } = await API.post('/auth/login', { username, password });
  setTokens(data.accessToken, data.refreshToken);
  return data;
};

export const logout = async () => {
  try {
    await API.post('/auth/logout', { refreshToken: getRefreshToken() });
  } finally {
    clearTokens();
  }
};

export const getMe = () =>
  API.get('/auth/me').then(r => r.data);

// ── Game endpoints ────────────────────────────────────────────────────────────
export const getMyPlayer   = ()             => API.get('/players/me').then(r => r.data);
export const getLeaderboard = ()            => API.get('/leaderboard').then(r => r.data);
export const generateQuest  = (skill)       => API.get('/quest', { params: { skill } }).then(r => r.data);
export const submitAnswer   = (questId, selectedIndex) =>
  API.post('/answer', { questId, selectedIndex }).then(r => r.data);

export const isLoggedIn = () => !!getAccessToken();
