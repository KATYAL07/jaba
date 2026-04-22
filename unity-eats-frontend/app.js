/**
 * Unity Eats - app.js
 * Multi-Page Application logic.
 */

const API_BASE = 'http://localhost:8080/api';
const TOKEN_KEY = 'ue_token';
const USER_KEY  = 'ue_user';

const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

function getToken()  { return localStorage.getItem(TOKEN_KEY); }
function getUser()   { const u = localStorage.getItem(USER_KEY); return u ? JSON.parse(u) : null; }
function saveAuth(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}
function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

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

function toast(msg, type = 'success', duration = 4000) {
  const tc = $('#toast-container');
  if (!tc) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  const icons = { success: '✅', error: '❌', info: 'ℹ️' };
  el.innerHTML = `<span>${icons[type]}</span><span>${msg}</span>`;
  tc.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateX(20px)'; setTimeout(() => el.remove(), 300); }, duration);
}

function escHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
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

function emptyState(icon, title, msg) {
  return `<div class="empty-state"><div class="empty-icon">${icon}</div><h4>${title}</h4><p>${msg}</p></div>`;
}

function getDashboardUrl(role) {
  const urls = {
    'RESTAURANT': 'restaurant-dashboard.html',
    'NGO': 'ngo-dashboard.html',
    'VOLUNTEER': 'volunteer-dashboard.html',
    'BENEFICIARY': 'beneficiary-dashboard.html'
  };
  return urls[role] || 'login.html';
}

function populateNav() {
  const user = getUser();
  if (!user) return;
  const nameEl = $('#nav-user-name');
  const roleEl = $('#nav-user-role');
  const avEl = $('#nav-avatar');
  if (nameEl) nameEl.textContent = user.fullName;
  if (roleEl) roleEl.textContent = user.role;
  if (avEl) avEl.textContent = user.fullName.charAt(0).toUpperCase();

  const logoutBtn = $('#logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      clearAuth();
      window.location.href = 'login.html';
    });
  }
}

// ============================================================
// PAGE INITIALIZATION
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  const page = document.body.dataset.page;
  
  // Auth Check
  const token = getToken();
  const user = getUser();
  
  if (page === 'login') {
    if (token && user) {
      window.location.href = getDashboardUrl(user.role);
      return;
    }
    initLoginPage();
  } else {
    if (!token || !user) {
      window.location.href = 'login.html';
      return;
    }
    populateNav();
    if (page === 'restaurant') initRestaurantDashboard();
    else if (page === 'ngo') initNgoDashboard();
    else if (page === 'volunteer') initVolunteerDashboard();
    else if (page === 'beneficiary') initBeneficiaryDashboard();
  }

  // Remove loader
  setTimeout(() => {
    const loader = $('#loading-overlay');
    if (loader) {
      loader.style.opacity = '0';
      setTimeout(() => loader.classList.add('hidden'), 400);
    }
  }, 400);
});

// ============================================================
// LOGIN PAGE LOGIC
// ============================================================
function initLoginPage() {
  $$('.auth-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      $$('.auth-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      $('#login-form').classList.toggle('hidden', tab.dataset.tab !== 'login');
      $('#register-form').classList.toggle('hidden', tab.dataset.tab !== 'register');
    });
  });

  $$('.quick-fill-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      $('#login-email').value = btn.dataset.email;
      $('#login-password').value = btn.dataset.pw;
    });
  });

  $('#reg-role').addEventListener('change', e => {
    const needs = ['RESTAURANT','NGO'].includes(e.target.value);
    $('#org-name-group').style.display = needs ? 'flex' : 'none';
    $('#reg-org').required = needs;
  });

  $('#login-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = $('#login-submit-btn');
    btn.disabled = true;
    try {
      const data = await api('/auth/login', 'POST', {
        email: $('#login-email').value.trim(),
        password: $('#login-password').value
      }, false);
      saveAuth(data.token, data);
      window.location.href = getDashboardUrl(data.role);
    } catch(err) {
      alert(err.data?.message || 'Login failed.');
      btn.disabled = false;
    }
  });

  $('#register-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = $('#register-submit-btn');
    btn.disabled = true;
    try {
      const data = await api('/auth/register', 'POST', {
        fullName: $('#reg-fullname').value.trim(),
        email: $('#reg-email').value.trim(),
        phone: $('#reg-phone').value.trim(),
        password: $('#reg-password').value,
        role: $('#reg-role').value,
        organizationName: $('#reg-org').value.trim() || null
      }, false);
      saveAuth(data.token, data);
      window.location.href = getDashboardUrl(data.role);
    } catch(err) {
      alert(err.data?.message || 'Registration failed.');
      btn.disabled = false;
    }
  });
}

// ============================================================
// SHARED RENDERING LOGIC
// ============================================================
// Entities have nested fields now: l.restaurant.organizationName, etc.
function createCardHtml(l, context) {
  let orgName = '';
  if (l.restaurant && l.restaurant.organizationName) {
    orgName = l.restaurant.organizationName;
  } else if (l.restaurant && l.restaurant.fullName) {
    orgName = l.restaurant.fullName;
  }

  let ngoName = l.acceptedByNgo ? l.acceptedByNgo.fullName : '';
  let volName = l.assignedVolunteer ? l.assignedVolunteer.fullName : '';

  return `
    <div class="card-top">
      <div>
        <div class="food-title">${escHtml(l.foodName)}</div>
        <div class="org-badge">${escHtml(orgName)}</div>
      </div>
      ${getStatusBadge(l.status)}
    </div>
    <span class="food-category">${escHtml(l.category)}</span>
    <div class="food-meta">
      <div class="meta-row"><span>📦</span><span><strong>${l.quantity} ${escHtml(l.unit)}</strong></span></div>
      <div class="meta-row"><span>📍</span><span>${escHtml(l.pickupAddress)}</span></div>
      <div class="meta-row"><span>⏰</span><span>Expires in ${l.expiryHours}h · Posted ${timeAgo(l.createdAt)}</span></div>
      ${l.description ? `<div class="meta-row"><span>📝</span><span>${escHtml(l.description)}</span></div>` : ''}
      ${ngoName ? `<div class="meta-row"><span>🤝</span><span>Accepted by ${escHtml(ngoName)}</span></div>` : ''}
      ${volName ? `<div class="meta-row"><span>🚴</span><span>Delivery: ${escHtml(volName)}</span></div>` : ''}
    </div>
    <div class="food-card-actions" id="actions-${l.id}"></div>
  `;
}

// ============================================================
// RESTAURANT
// ============================================================
function initRestaurantDashboard() {
  $('#open-post-form-btn').addEventListener('click', () => {
    $('#post-food-card').style.display = 'block';
  });
  $('#close-post-form').addEventListener('click', () => {
    $('#post-food-card').style.display = 'none';
  });

  $('#post-food-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = $('#post-food-submit');
    btn.disabled = true;
    try {
      await api('/food', 'POST', {
        foodName: $('#food-name').value.trim(),
        category: $('#food-category').value.trim(),
        quantity: parseInt($('#food-quantity').value),
        unit: $('#food-unit').value.trim(),
        description: $('#food-description').value.trim() || null,
        pickupAddress: $('#food-address').value.trim(),
        expiryHours: parseInt($('#food-expiry').value)
      });
      toast('Listing posted!');
      $('#post-food-form').reset();
      $('#post-food-card').style.display = 'none';
      loadRestaurantListings();
    } catch(err) {
      toast(err.data?.message || 'Failed to post', 'error');
    } finally {
      btn.disabled = false;
    }
  });

  loadRestaurantListings();
}

async function loadRestaurantListings() {
  const grid = $('#restaurant-listings');
  try {
    const list = await api('/food/my-listings');
    if (!list.length) {
      grid.innerHTML = emptyState('📋', 'No listings yet', 'Post your first surplus food listing above.');
      return;
    }
    grid.innerHTML = '';
    list.forEach(l => {
      const div = document.createElement('div');
      div.className = 'food-card';
      div.innerHTML = createCardHtml(l, 'restaurant');
      grid.appendChild(div);
    });
  } catch {
    grid.innerHTML = emptyState('📋', 'Error', 'Failed to load listings.');
  }
}

// ============================================================
// NGO
// ============================================================
function initNgoDashboard() {
  $('#refresh-btn').addEventListener('click', () => {
    loadNgoAvailable();
    loadNgoAccepted();
  });
  loadNgoAvailable();
  loadNgoAccepted();
}

async function loadNgoAvailable() {
  const grid = $('#available-listings-ngo');
  try {
    const list = await api('/food/available');
    if (!list.length) { grid.innerHTML = emptyState('🌐','No available listings','Check back later.'); return; }
    grid.innerHTML = '';
    list.forEach(l => {
      const div = document.createElement('div');
      div.className = 'food-card';
      div.innerHTML = createCardHtml(l, 'ngo');
      const btn = document.createElement('button');
      btn.className = 'btn-primary btn-sm';
      btn.textContent = '🤝 Accept';
      btn.onclick = async () => {
        btn.disabled = true;
        try { await api(`/food/${l.id}/accept`, 'PATCH'); toast('Accepted!'); loadNgoAvailable(); loadNgoAccepted(); }
        catch(e) { toast('Error accepting', 'error'); btn.disabled = false; }
      };
      div.querySelector(`#actions-${l.id}`).appendChild(btn);
      grid.appendChild(div);
    });
  } catch {}
}

async function loadNgoAccepted() {
  const grid = $('#ngo-accepted-listings');
  try {
    const list = await api('/food/ngo-listings');
    if (!list.length) { grid.innerHTML = emptyState('✅','Nothing accepted yet',''); return; }
    grid.innerHTML = '';
    list.forEach(l => {
      const div = document.createElement('div');
      div.className = 'food-card';
      div.innerHTML = createCardHtml(l, 'ngo');
      grid.appendChild(div);
    });
  } catch {}
}

// ============================================================
// VOLUNTEER
// ============================================================
function initVolunteerDashboard() {
  $('#refresh-btn').addEventListener('click', () => {
    loadVolAvailable();
    loadVolAssigned();
  });
  loadVolAvailable();
  loadVolAssigned();
}

async function loadVolAvailable() {
  const grid = $('#active-deliveries');
  try {
    const list = await api('/food/active-deliveries');
    const av = list.filter(l => l.status === 'ACCEPTED');
    if (!av.length) { grid.innerHTML = emptyState('📦','No deliveries available',''); return; }
    grid.innerHTML = '';
    av.forEach(l => {
      const div = document.createElement('div');
      div.className = 'food-card';
      div.innerHTML = createCardHtml(l, 'vol');
      const btn = document.createElement('button');
      btn.className = 'btn-primary btn-sm';
      btn.textContent = '🚴 Assign to me';
      btn.onclick = async () => {
        btn.disabled = true;
        try { await api(`/food/${l.id}/assign`, 'PATCH'); toast('Assigned!'); loadVolAvailable(); loadVolAssigned(); }
        catch(e) { toast('Error assigning', 'error'); btn.disabled = false; }
      };
      div.querySelector(`#actions-${l.id}`).appendChild(btn);
      grid.appendChild(div);
    });
  } catch {}
}

async function loadVolAssigned() {
  const grid = $('#volunteer-deliveries');
  try {
    const list = await api('/food/volunteer-listings');
    if (!list.length) { grid.innerHTML = emptyState('🚴','No deliveries assigned',''); return; }
    grid.innerHTML = '';
    list.forEach(l => {
      const div = document.createElement('div');
      div.className = 'food-card';
      div.innerHTML = createCardHtml(l, 'vol');
      const acts = div.querySelector(`#actions-${l.id}`);
      
      if (l.status === 'ASSIGNED') {
        const btn = document.createElement('button');
        btn.className = 'btn-primary btn-sm';
        btn.textContent = '📦 Mark Picked Up';
        btn.onclick = async () => {
          btn.disabled = true;
          try { await api(`/food/${l.id}/status`, 'PATCH', { status:'PICKED_UP' }); toast('Picked up!'); loadVolAssigned(); }
          catch(e) { toast('Error', 'error'); btn.disabled = false; }
        };
        acts.appendChild(btn);
      } else if (l.status === 'PICKED_UP') {
        const btn = document.createElement('button');
        btn.className = 'btn-primary btn-sm';
        btn.textContent = '✅ Mark Delivered';
        btn.onclick = async () => {
          btn.disabled = true;
          try { await api(`/food/${l.id}/status`, 'PATCH', { status:'DELIVERED' }); toast('Delivered!'); loadVolAssigned(); }
          catch(e) { toast('Error', 'error'); btn.disabled = false; }
        };
        acts.appendChild(btn);
      }
      grid.appendChild(div);
    });
  } catch {}
}

// ============================================================
// BENEFICIARY
// ============================================================
function initBeneficiaryDashboard() {
  loadPublicListings();
  loadStats();
}

async function loadPublicListings() {
  const grid = $('#public-listings');
  try {
    const list = await api('/public/listings', 'GET', null, false);
    if (!list.length) { grid.innerHTML = emptyState('🌿','No listings yet',''); return; }
    grid.innerHTML = '';
    list.forEach(l => {
      const div = document.createElement('div');
      div.className = 'food-card';
      div.innerHTML = createCardHtml(l, 'public');
      grid.appendChild(div);
    });
  } catch {}
}

async function loadStats() {
  try {
    const s = await api('/public/stats', 'GET', null, false);
    const animate = (id, target) => {
      const el = document.getElementById(id);
      if (!el) return;
      let curr = 0; const step = Math.ceil(target / 30);
      const iv = setInterval(() => { curr = Math.min(curr + step, target); el.textContent = curr; if (curr >= target) clearInterval(iv); }, 40);
    };
    animate('stat-available', s.available || 0);
    animate('stat-accepted', s.accepted || 0);
    animate('stat-delivered', s.delivered || 0);
    animate('stat-total', s.total || 0);
  } catch {}
}
