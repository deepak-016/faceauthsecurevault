/* ═══════════════════════════════════════
   FaceAuth – shared utilities (script.js)
═══════════════════════════════════════ */

const API = 'http://localhost:8086';

/* ─── Session helpers ─── */
function saveSession(token, user) {
  localStorage.setItem('fa_token', token);
  localStorage.setItem('fa_user', JSON.stringify(user));
}
function getSession() {
  const token = localStorage.getItem('fa_token');
  const user  = JSON.parse(localStorage.getItem('fa_user') || 'null');
  return { token, user };
}
function getToken() {
  return localStorage.getItem('fa_token');
}
function getUser() {
  return JSON.parse(localStorage.getItem('fa_user') || 'null');
}
function clearSession() {
  localStorage.removeItem('fa_token');
  localStorage.removeItem('fa_user');
}
function isLoggedIn() {
  return !!localStorage.getItem('fa_token');
}
function requireAuth() {
  if (!isLoggedIn()) window.location.href = 'index.html';
}
function requireGuest() {
  if (isLoggedIn()) window.location.href = 'dashboard.html';
}
function logoutUser() {
  clearSession();
  window.location.href = 'index.html';
}

/* ─── Populate UI with user info ─── */
function populateUserUI() {
  const user = getUser();
  if (!user) return;
  const name     = user.fullName || user.name || user.username || 'User';
  const email    = user.email || '';
  const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0,2);

  document.querySelectorAll('[data-user-name]').forEach(el => el.textContent = name);
  document.querySelectorAll('[data-user-email]').forEach(el => el.textContent = email);
  document.querySelectorAll('[data-user-initials]').forEach(el => el.textContent = initials);
  document.querySelectorAll('[data-user-avatar]').forEach(el => el.textContent = initials);

  const greetEl = document.getElementById('greeting-name');
  if (greetEl) greetEl.textContent = name.split(' ')[0];
}

/* ─── Toast ─── */
function showToast(message, type = 'info', duration = 3000) {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.classList.add('show'), 10);
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

/* ─── Field error helpers ─── */
function setFieldError(el, msg) {
  if (!el) return;
  el.classList.add('error');
  const err = document.getElementById(el.id + '-error');
  if (err) { err.textContent = msg; err.classList.add('show'); }
}
function clearFieldError(el) {
  if (!el) return;
  el.classList.remove('error');
  const err = document.getElementById(el.id + '-error');
  if (err) { err.textContent = ''; err.classList.remove('show'); }
}
function clearFormErrors(form) {
  form.querySelectorAll('.form-control').forEach(clearFieldError);
  form.querySelectorAll('.form-error').forEach(e => {
    e.textContent = ''; e.classList.remove('show');
  });
}

/* ─── Button loading ─── */
function setButtonLoading(btn, loading) {
  if (!btn) return;
  btn.disabled = loading;
  btn.dataset.originalText = btn.dataset.originalText || btn.textContent;
  btn.textContent = loading ? 'Please wait...' : btn.dataset.originalText;
}

/* ─── Validators ─── */
function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
function passwordStrength(pwd) {
  let score = 0;
  if (pwd.length >= 8) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  return score;
}

/* ─── Core API fetch ─── */
async function apiCall(endpoint, method = 'GET', body = null) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  try {
    const res = await fetch(API + endpoint, {
      method,
      headers,
      body: body ? JSON.stringify(body) : null
    });
    let data = {};
    try { data = await res.json(); } catch(e) {}
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    return { ok: false, status: 0,
      data: { message: 'Cannot connect to server. Is Spring Boot running?' }};
  }
}

/* alias used by dashboard.html */
async function apiFetch(endpoint, method = 'GET', body = null) {
  return apiCall(endpoint, method, body);
}

/* ─── Auth API calls ─── */
async function registerUser({ fullName, username, email, password }) {
  return apiCall('/api/auth/register', 'POST', { fullName, username, email, password });
}

async function loginUser({ username, password }) {
  return apiCall('/api/auth/login', 'POST', { username, password });
}

async function verifyFace(username, imageBase64) {
  return apiCall('/api/auth/face/verify', 'POST', { username, imageBase64 });
}

async function enrollFace(imageBase64) {
  const user = getUser();
  const name = user ? (user.username || user.fullName || user.name) : '';
  return apiCall('/api/auth/face/enroll', 'POST', { username: name, imageBase64 });
}

/* ─── Dashboard API calls ─── */
async function getDashboardStats() {
  return apiCall('/api/dashboard/stats');
}

async function getUserActivity() {
  return apiCall('/api/dashboard/activity');
}

async function getFaceStatus() {
  return apiCall('/api/face/status');
}

/* ─── File API calls ─── */
async function uploadFile(file, userId) {
  const token = getToken();
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const formData = new FormData();
  formData.append('file', file);
  formData.append('userId', userId);
  try {
    const res = await fetch(API + '/api/files/upload', {
      method: 'POST',
      headers,
      body: formData
    });
    let data = {};
    try { data = await res.json(); } catch(e) { data = { message: await res.text() }; }
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    return { ok: false, status: 0, data: { message: 'Cannot connect to server.' } };
  }
}

async function getUserFiles(userId) {
  return apiCall(`/api/files/user/${userId}`);
}

async function deleteUserFile(fileId) {
  return apiCall(`/api/files/${fileId}`, 'DELETE');
}

function getFileDownloadUrl(storedFileName, userId) {
  const token = getToken();
  return `${API}/api/files/download/${storedFileName}?token=${encodeURIComponent(token || '')}`;
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function getFileIcon(fileName) {
  const ext = (fileName || '').split('.').pop().toLowerCase();
  const icons = {
    pdf: '📄', doc: '📝', docx: '📝', xls: '📊', xlsx: '📊',
    ppt: '📑', pptx: '📑', png: '🖼️', jpg: '🖼️', jpeg: '🖼️',
    gif: '🖼️', mp4: '🎬', mp3: '🎵', zip: '🗜️', rar: '🗜️',
    txt: '📃', csv: '📊', json: '🔧', js: '🔧', html: '🌐'
  };
  return icons[ext] || '📁';
}
