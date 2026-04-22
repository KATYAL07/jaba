/**
 * Unity Eats - app.js
 * Vanilla ES6+ SPA logic: auth, API calls, role dashboards
 */

// ============================================================
// CONFIG
// ============================================================
const API_BASE = 'http://localhost:8080/api';
const TOKEN_KEY = 'ue_token';
const USER_KEY  = 'ue_user';

// ============================================================
// STATE
// ============================================================
let currentUser = null;
let currentToken = null;

// ============================================================
// UTILITIES
// ============================================================
const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

function getToken()  { return localStorage.getItem(TOKEN_KEY); }
function getUser()   { const u = localStorage.getItem(USER_KEY); return u ? JSON.parse(u) : null; }
function saveAuth(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  currentToken = token;
  currentUser  = user;
}
function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  currentToken = null;
  currentUser  = null;
}

/** Authenticated fetch wrapper */
async function api(path, method = 'GET', body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && getToken()) headers['Authorization'] = `Bearer ${getToken()}`;
  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(API_BASE + path, opts);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw { status: res.status, data };
  return data;
}

/** Show a toast notification */
function toast(msg, type = 'success', duration = 4000) {
  const icons = { success: '✅', error: '❌', info: 'ℹ️' };
  const tc = $('#toast-container');
  if (!tc) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<span>${icons[type]}</span><span>${msg}</span>`;
  tc.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateX(20px)'; el.style.transition = '.3s'; setTimeout(() => el.remove(), 300); }, duration);
}

/** Clear all field errors on a form */
function clearFormErrors(formId) {
  $$(`#${formId} .field-error`).forEach(e => e.textContent = '');
  $$(`#${formId} input, #${formId} select, #${formId} textarea`).forEach(e => e.classList.remove('error'));
  const banner = $(`#${formId} .form-error-banner`) || document.getElementById(formId.replace('form','error').replace('-','_'));
  if (banner) banner.classList.add('hidden');
}

/** Show server-returned field errors */
function showFieldErrors(fieldErrors, prefix = '') {
  if (!fieldErrors) return;
  Object.entries(fieldErrors).forEach(([field, msg]) => {
    const el = document.getElementById(`${prefix}${field}-error`) || document.getElementById(`${prefix}${field.replace(/([A-Z])/g,'-$1').toLowerCase()}-error`);
    const inp = document.getElementById(`${prefix}${field}`) || document.getElementById(`${prefix}${field.replace(/([A-Z])/g,'-$1').toLowerCase()}`);
    if (el) el.textContent = msg;
    if (inp) inp.classList.add('error');
  });
}

function showBanner(bannerId, msg) {
  const el = document.getElementById(bannerId);
  if (!el) return;
  el.textContent = msg;
  el.classList.remove('hidden');
}

function setLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  const txt = btn.querySelector('.btn-text');
  const spin = btn.querySelector('.btn-spinner');
  btn.disabled = loading;
  if (txt)  txt.style.opacity  = loading ? '0' : '1';
  if (spin) spin.classList.toggle('hidden', !loading);
}

function getStatusBadge(status) {
  return `<span class="status-badge status-${status}">${status.replace('_',' ')}</span>`;
}

function timeAgo(dateStr) {
  const d = new Date(dateStr), now = new Date();
  const diff = Math.floor((now - d) / 60000);
  if (diff < 1) return 'Just now';
  if (diff < 60) return `${diff}m ago`;
  if (diff < 1440) return `${Math.floor(diff/60)}h ago`;
  return `${Math.floor(diff/1440)}d ago`;
}

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  currentToken = getToken();
  currentUser  = getUser();
  setTimeout(() => {
    $('#loading-overlay').style.opacity = '0';
    setTimeout(() => $('#loading-overlay').classList.add('hidden'), 400);
    if (currentToken && currentUser) showApp();
    else showAuthModal();
  }, 800);
  bindAuthEvents();
  bindNavEvents();
});

// ============================================================
// AUTH MODAL
// ============================================================
function showAuthModal() {
  $('#auth-modal').classList.remove('hidden');
  $('#app').classList.add('hidden');
}

function hideAuthModal() {
  $('#auth-modal').classList.add('hidden');
  $('#app').classList.remove('hidden');
}

function bindAuthEvents() {
  // Tab switching
  $$('.auth-tab').forEach(tab => {
    tab.addEventListener('click', () => switchAuthTab(tab.dataset.tab));
  });
  $$('[data-switch-to]').forEach(btn => {
    btn.addEventListener('click', () => switchAuthTab(btn.dataset.switchTo));
  });

  // Password toggles
  $('#toggle-login-pw').addEventListener('click', () => togglePw('login-password','toggle-login-pw'));
  $('#toggle-reg-pw').addEventListener('click',   () => togglePw('reg-password','toggle-reg-pw'));

  // Password strength
  $('#reg-password').addEventListener('input', e => checkPwStrength(e.target.value));

  // Role change -> show/hide org field
  $('#reg-role').addEventListener('change', e => {
    const needs = ['RESTAURANT','NGO'].includes(e.target.value);
    $('#org-name-group').style.display = needs ? 'flex' : 'none';
    $('#reg-org').required = needs;
  });

  // Quick fill
  $$('.quick-fill-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      $('#login-email').value    = btn.dataset.email;
      $('#login-password').value = btn.dataset.pw;
    });
  });

  // Form submits
  $('#login-form').addEventListener('submit', handleLogin);
  $('#register-form').addEventListener('submit', handleRegister);
}

function switchAuthTab(tab) {
  $$('.auth-tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
  $('#login-form').classList.toggle('hidden', tab !== 'login');
  $('#register-form').classList.toggle('hidden', tab !== 'register');
}

function togglePw(inputId, btnId) {
  const inp = document.getElementById(inputId);
  inp.type = inp.type === 'password' ? 'text' : 'password';
}

function checkPwStrength(pw) {
  const bar   = $('#pw-strength-bar');
  const label = $('#pw-strength-label');
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[@$!%*?&]/.test(pw)) score++;
  const colors = ['','#ff5252','#ffd740','#69f0ae','#00e676'];
  const labels = ['','Weak','Fair','Good','Strong'];
  bar.style.width   = `${score * 25}%`;
  bar.style.background = colors[score] || '';
  label.textContent = pw.length ? labels[score] : '';
  label.style.color = colors[score] || '';
}

async function handleLogin(e) {
  e.preventDefault();
  clearFormErrors('login-form');
  setLoading('login-submit-btn', true);
  try {
    const data = await api('/auth/login', 'POST', {
      email:    $('#login-email').value.trim(),
      password: $('#login-password').value
    }, false);
    saveAuth(data.token, data);
    hideAuthModal();
    showApp();
    toast(`Welcome back, ${data.fullName}! 👋`);
  } catch(err) {
    const msg = err.data?.message || 'Login failed. Check your credentials.';
    if (err.data?.fieldErrors) showFieldErrors(err.data.fieldErrors, 'login-');
    showBanner('login-error-banner', msg);
    toast(msg, 'error');
  } finally {
    setLoading('login-submit-btn', false);
  }
}

async function handleRegister(e) {
  e.preventDefault();
  clearFormErrors('register-form');
  setLoading('register-submit-btn', true);
  const payload = {
    fullName:         $('#reg-fullname').value.trim(),
    email:            $('#reg-email').value.trim(),
    phone:            $('#reg-phone').value.trim(),
    password:         $('#reg-password').value,
    role:             $('#reg-role').value,
    organizationName: $('#reg-org').value.trim() || null
  };
  try {
    const data = await api('/auth/register', 'POST', payload, false);
    saveAuth(data.token, data);
    hideAuthModal();
    showApp();
    toast(`Welcome to Unity Eats, ${data.fullName}! 🍃`);
  } catch(err) {
    const msg = err.data?.message || 'Registration failed. Please try again.';
    if (err.data?.fieldErrors) showFieldErrors(err.data.fieldErrors, 'reg-');
    showBanner('register-error-banner', msg);
    toast(msg, 'error');
  } finally {
    setLoading('register-submit-btn', false);
  }
}

// ============================================================
// NAV & APP SHELL
// ============================================================
function bindNavEvents() {
  $('#logout-btn').addEventListener('click', () => {
    clearAuth();
    $('#app').classList.add('hidden');
    showAuthModal();
    toast('Logged out successfully.', 'info');
  });
}

function showApp() {
  const u = currentUser;
  if (!u) return;
  $('#app').classList.remove('hidden');
  // Populate nav
  $('#nav-user-name').textContent  = u.fullName;
  $('#nav-user-role').textContent  = u.role;
  $('#nav-avatar').textContent     = u.fullName.charAt(0).toUpperCase();
  loadStats();
  loadRoleDashboard(u.role);
}

// ============================================================
// STATS
// ============================================================
async function loadStats() {
  try {
    const s = await api('/public/stats', 'GET', null, false);
    animateCount('stat-available', s.available || 0);
    animateCount('stat-accepted',  s.accepted  || 0);
    animateCount('stat-delivered', s.delivered || 0);
    animateCount('stat-total',     s.total     || 0);
  } catch {}
}

function animateCount(id, target) {
  const el = document.getElementById(id);
  if (!el) return;
  let curr = 0;
  const step = Math.ceil(target / 30);
  const iv = setInterval(() => {
    curr = Math.min(curr + step, target);
    el.textContent = curr;
    if (curr >= target) clearInterval(iv);
  }, 40);
}

// ============================================================
// ROLE DASHBOARDS
// ============================================================
function loadRoleDashboard(role) {
  const titles = { RESTAURANT:'Restaurant Dashboard', NGO:'NGO Dashboard', VOLUNTEER:'Volunteer Dashboard', BENEFICIARY:'Community Dashboard' };
  const subs   = { RESTAURANT:'Post surplus food & track your listings', NGO:'Browse & accept available food donations', VOLUNTEER:'Pick up & deliver accepted donations', BENEFICIARY:'View available food distributions near you' };
  $('#dashboard-title').textContent    = titles[role] || 'Dashboard';
  $('#dashboard-subtitle').textContent = subs[role]   || '';
  // Show correct panel
  $$('.role-panel').forEach(p => p.classList.add('hidden'));
  const panel = document.getElementById(`panel-${role.toLowerCase()}`);
  if (panel) panel.classList.remove('hidden');
  // Role-specific actions & data
  const actions = $('#dashboard-actions');
  actions.innerHTML = '';
  if (role === 'RESTAURANT') {
    const btn = document.createElement('button');
    btn.className = 'btn-primary';
    btn.id = 'open-post-form-btn';
    btn.innerHTML = '+ Post Food';
    btn.addEventListener('click', () => { $('#post-food-card').style.display = 'block'; btn.style.display = 'none'; });
    actions.appendChild(btn);
    bindPostFoodForm();
    loadRestaurantListings();
  } else if (role === 'NGO') {
    const btn = document.createElement('button');
    btn.className = 'btn-secondary';
    btn.innerHTML = '🔄 Refresh';
    btn.addEventListener('click', () => { loadAvailableListings(); loadNgoListings(); });
    actions.appendChild(btn);
    loadAvailableListings();
    loadNgoListings();
  } else if (role === 'VOLUNTEER') {
    const btn = document.createElement('button');
    btn.className = 'btn-secondary';
    btn.innerHTML = '🔄 Refresh';
    btn.addEventListener('click', () => { loadActiveDeliveries(); loadVolunteerListings(); });
    actions.appendChild(btn);
    loadActiveDeliveries();
    loadVolunteerListings();
  } else if (role === 'BENEFICIARY') {
    loadPublicListings();
  }
}

// ============================================================
// RESTAURANT
// ============================================================
function bindPostFoodForm() {
  const form = $('#post-food-form');
  const closeBtn = $('#close-post-form');
  const cancelBtn = $('#cancel-post');
  const close = () => { $('#post-food-card').style.display = 'none'; $('#open-post-form-btn').style.display = ''; form.reset(); clearFormErrors('post-food-form'); };
  if (closeBtn) closeBtn.addEventListener('click', close);
  if (cancelBtn) cancelBtn.addEventListener('click', close);
  form.addEventListener('submit', async e => {
    e.preventDefault();
    clearFormErrors('post-food-form');
    setLoading('post-food-submit', true);
    const payload = {
      foodName:      $('#food-name').value.trim(),
      category:      $('#food-category').value.trim(),
      quantity:      parseInt($('#food-quantity').value),
      unit:          $('#food-unit').value.trim(),
      description:   $('#food-description').value.trim() || null,
      pickupAddress: $('#food-address').value.trim(),
      expiryHours:   parseInt($('#food-expiry').value)
    };
    try {
      await api('/food', 'POST', payload);
      toast('Food listing posted successfully! 📦', 'success');
      form.reset();
      $('#post-food-card').style.display = 'none';
      $('#open-post-form-btn').style.display = '';
      loadRestaurantListings();
      loadStats();
    } catch(err) {
      if (err.data?.fieldErrors) {
        const map = { foodName:'food-name', category:'food-category', quantity:'food-quantity', unit:'food-unit', pickupAddress:'food-address', expiryHours:'food-expiry', description:'food-description' };
        Object.entries(err.data.fieldErrors).forEach(([f,m]) => {
          const id = map[f]; if (!id) return;
          const el = document.getElementById(`${id}-error`); if (el) el.textContent = m;
          const inp = document.getElementById(id); if (inp) inp.classList.add('error');
        });
      }
      const msg = err.data?.message || 'Failed to post listing.';
      showBanner('post-food-error', msg);
      toast(msg, 'error');
    } finally {
      setLoading('post-food-submit', false);
    }
  });
}

async function loadRestaurantListings() {
  const grid = $('#restaurant-listings');
  grid.innerHTML = skeletons(3);
  try {
    const listings = await api('/food/my-listings');
    renderListings(grid, listings, 'restaurant');
  } catch { grid.innerHTML = emptyState('📋','No listings yet','Post your first surplus food listing above.'); }
}

// ============================================================
// NGO
// ============================================================
async function loadAvailableListings() {
  const grid = $('#available-listings-ngo');
  grid.innerHTML = skeletons(3);
  try {
    const listings = await api('/food/available');
    renderListings(grid, listings, 'ngo');
  } catch { grid.innerHTML = emptyState('🌐','No available listings','Check back later for new food donations.'); }
}

async function loadNgoListings() {
  const grid = $('#ngo-accepted-listings');
  grid.innerHTML = skeletons(2);
  try {
    const listings = await api('/food/ngo-listings');
    renderListings(grid, listings, 'ngo-accepted');
  } catch { grid.innerHTML = emptyState('✅','Nothing accepted yet','Browse available listings above to accept food donations.'); }
}

// ============================================================
// VOLUNTEER
// ============================================================
async function loadActiveDeliveries() {
  const grid = $('#active-deliveries');
  grid.innerHTML = skeletons(2);
  try {
    const listings = await api('/food/active-deliveries');
    renderListings(grid, listings, 'volunteer-browse');
  } catch { grid.innerHTML = emptyState('📦','No deliveries available','NGOs are accepting listings. Check back soon.'); }
}

async function loadVolunteerListings() {
  const grid = $('#volunteer-deliveries');
  grid.innerHTML = skeletons(2);
  try {
    const listings = await api('/food/volunteer-listings');
    renderListings(grid, listings, 'volunteer-mine');
  } catch { grid.innerHTML = emptyState('🚴','No deliveries assigned','Browse above and assign yourself to a delivery.'); }
}

// ============================================================
// BENEFICIARY / PUBLIC
// ============================================================
async function loadPublicListings() {
  const grid = $('#public-listings');
  grid.innerHTML = skeletons(3);
  try {
    const listings = await api('/public/listings', 'GET', null, false);
    renderListings(grid, listings, 'public');
  } catch { grid.innerHTML = emptyState('🌿','No listings yet','Food donations will appear here once restaurants post them.'); }
}

// ============================================================
// RENDER LISTINGS
// ============================================================
function renderListings(grid, listings, context) {
  if (!listings.length) {
    const msgs = {
      'restaurant':'No listings yet. Post your first surplus food!',
      'ngo':'No available listings right now.',
      'ngo-accepted':'You haven\'t accepted any listings yet.',
      'volunteer-browse':'No active deliveries available.',
      'volunteer-mine':'No deliveries assigned to you.',
      'public':'No food available right now.'
    };
    grid.innerHTML = emptyState('📭','Nothing here',msgs[context]||'');
    return;
  }
  grid.innerHTML = '';
  listings.forEach(l => {
    const card = document.createElement('div');
    card.className = 'food-card';
    card.innerHTML = `
      <div class="card-top">
        <div>
          <div class="food-title">${escHtml(l.foodName)}</div>
          <div class="org-badge">${escHtml(l.restaurantOrg || l.restaurantName || '')}</div>
        </div>
        ${getStatusBadge(l.status)}
      </div>
      <span class="food-category">${escHtml(l.category)}</span>
      <div class="food-meta">
        <div class="meta-row"><span>📦</span><span><strong>${l.quantity} ${escHtml(l.unit)}</strong></span></div>
        <div class="meta-row"><span>📍</span><span>${escHtml(l.pickupAddress)}</span></div>
        <div class="meta-row"><span>⏰</span><span>Expires in ${l.expiryHours}h · Posted ${timeAgo(l.createdAt)}</span></div>
        ${l.description ? `<div class="meta-row"><span>📝</span><span>${escHtml(l.description)}</span></div>` : ''}
        ${l.ngoName ? `<div class="meta-row"><span>🤝</span><span>Accepted by ${escHtml(l.ngoName)}</span></div>` : ''}
        ${l.volunteerName ? `<div class="meta-row"><span>🚴</span><span>Delivery: ${escHtml(l.volunteerName)}</span></div>` : ''}
      </div>
      <div class="food-card-actions" id="actions-${l.id}"></div>
    `;
    grid.appendChild(card);
    addCardActions(card, l, context);
  });
}

function addCardActions(card, l, context) {
  const actDiv = card.querySelector(`#actions-${l.id}`);
  if (context === 'ngo' && l.status === 'AVAILABLE') {
    const btn = makeBtn('🤝 Accept This Food', 'btn-primary btn-sm', async () => {
      btn.disabled = true; btn.textContent = 'Accepting...';
      try {
        await api(`/food/${l.id}/accept`, 'PATCH');
        toast(`You've accepted "${l.foodName}"! 🎉`);
        loadAvailableListings(); loadNgoListings(); loadStats();
      } catch(e) { toast(e.data?.message || 'Could not accept.', 'error'); btn.disabled = false; btn.textContent = '🤝 Accept This Food'; }
    });
    actDiv.appendChild(btn);
  } else if (context === 'volunteer-browse' && l.status === 'ACCEPTED') {
    const btn = makeBtn('🚴 Take This Delivery', 'btn-primary btn-sm', async () => {
      btn.disabled = true; btn.textContent = 'Assigning...';
      try {
        await api(`/food/${l.id}/assign`, 'PATCH');
        toast(`Delivery assigned! Time to pick up "${l.foodName}". 🚀`);
        loadActiveDeliveries(); loadVolunteerListings();
      } catch(e) { toast(e.data?.message || 'Could not assign.', 'error'); btn.disabled = false; btn.textContent = '🚴 Take This Delivery'; }
    });
    actDiv.appendChild(btn);
  } else if (context === 'volunteer-mine') {
    if (l.status === 'ASSIGNED') {
      const btn = makeBtn('📦 Mark Picked Up', 'btn-primary btn-sm', async () => {
        btn.disabled = true;
        try {
          await api(`/food/${l.id}/status`, 'PATCH', { status: 'PICKED_UP' });
          toast(`Marked as picked up! Deliver "${l.foodName}" to the community.`);
          loadVolunteerListings(); loadStats();
        } catch(e) { toast(e.data?.message || 'Could not update.', 'error'); btn.disabled = false; }
      });
      actDiv.appendChild(btn);
    } else if (l.status === 'PICKED_UP') {
      const btn = makeBtn('✅ Mark Delivered', 'btn-primary btn-sm', async () => {
        btn.disabled = true;
        try {
          await api(`/food/${l.id}/status`, 'PATCH', { status: 'DELIVERED' });
          toast(`"${l.foodName}" delivered! You just made someone's day. 🌟`, 'success', 6000);
          loadVolunteerListings(); loadStats();
        } catch(e) { toast(e.data?.message || 'Could not update.', 'error'); btn.disabled = false; }
      });
      actDiv.appendChild(btn);
    }
  }
}

// ============================================================
// HELPERS
// ============================================================
function makeBtn(text, className, onClick) {
  const b = document.createElement('button');
  b.className = className;
  b.innerHTML = text;
  b.addEventListener('click', onClick);
  return b;
}

function escHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function skeletons(n) {
  return `<div class="loading-cards">${'<div class="skeleton-card"></div>'.repeat(n)}</div>`;
}

function emptyState(icon, title, msg) {
  return `<div class="empty-state"><div class="empty-icon">${icon}</div><h4>${title}</h4><p>${msg}</p></div>`;
}

// Navbar scroll effect
window.addEventListener('scroll', () => {
  const nav = document.getElementById('navbar');
  if (nav) nav.style.boxShadow = window.scrollY > 20 ? '0 4px 30px rgba(0,0,0,.5)' : 'none';
});
