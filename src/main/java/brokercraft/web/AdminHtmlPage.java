package brokercraft.web;

/**
 * AdminHtmlPage — full HTML for the Admin web dashboard.
 *
 * Changes from v1:
 *  - Added login screen: page starts on login, dashboard hidden until authenticated
 *  - Success messages auto-clear after 3 seconds
 *  - Logout button added to header
 */
public final class AdminHtmlPage {

    private AdminHtmlPage() {}

    public static String build() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>BrokerCraft — Admin</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: 'Segoe UI', system-ui, sans-serif;
    background: #060a13;
    color: #f1f5f9;
    min-height: 100vh;
  }

  /* ════════════════════════════════════════
     LOGIN SCREEN
  ════════════════════════════════════════ */
  #loginScreen {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #060a13 0%, #0c1222 50%, #111827 100%);
  }
  .login-box {
    background: rgba(15,23,42,0.95);
    border: 1px solid rgba(99,102,241,0.35);
    border-radius: 20px;
    padding: 40px 44px;
    width: 100%;
    max-width: 420px;
    box-shadow: 0 10px 40px rgba(37,99,235,0.2);
  }
  .login-logo {
    font-size: 28px; font-weight: 800; margin-bottom: 4px;
    background: linear-gradient(to right, #60a5fa, #a78bfa);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  }
  .login-sub { color: #64748b; font-size: 13px; margin-bottom: 28px; }
  .login-label { font-size: 11px; font-weight: 700; color: #94a3b8;
    text-transform: uppercase; margin-bottom: 6px; display: block; }
  .login-input {
    width: 100%; padding: 12px 14px; background: #0b1220; color: #f1f5f9;
    border: 1.5px solid #334155; border-radius: 12px; font-size: 14px;
    margin-bottom: 16px;
  }
  .login-input:focus { outline: none; border-color: #6366f1; }
  .login-btn {
    width: 100%; padding: 13px; border: none; border-radius: 12px; cursor: pointer;
    font-size: 14px; font-weight: 700; color: white;
    background: linear-gradient(to right, #2563eb, #6366f1);
    margin-top: 4px;
  }
  .login-btn:hover { opacity: 0.88; }
  .login-err { color: #f87171; font-size: 13px; margin-top: 10px; min-height: 20px; }

  /* ════════════════════════════════════════
     DASHBOARD (hidden until logged in)
  ════════════════════════════════════════ */
  #dashboard { display: none; }

  /* ── Header ── */
  .header {
    background: linear-gradient(to right, #060a13, #0c1222);
    border-bottom: 1px solid rgba(99,102,241,0.3);
    padding: 14px 28px;
    display: flex; align-items: center; gap: 16px;
  }
  .logo {
    font-size: 22px; font-weight: 800;
    background: linear-gradient(to right, #60a5fa, #a78bfa);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  }
  .header-sub { color: #94a3b8; font-size: 13px; }
  .header-admin { color: #a78bfa; font-size: 13px; font-weight: 700; }
  .header-spacer { flex: 1; }
  .badge { padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; border: 1px solid; }
  .badge-on  { background: rgba(34,197,94,0.15);  border-color: rgba(74,222,128,0.5);  color: #4ade80; }
  .badge-off { background: rgba(251,191,36,0.12); border-color: rgba(251,191,36,0.4);  color: #fbbf24; }
  .btn { padding: 8px 18px; border-radius: 10px; border: none; cursor: pointer; font-size: 13px; font-weight: 700; }
  .btn-green  { background: linear-gradient(to right,#059669,#10b981); color: white; }
  .btn-yellow { background: linear-gradient(to right,#d97706,#f59e0b); color: white; }
  .btn-red    { background: linear-gradient(to right,#dc2626,#ef4444); color: white; }
  .btn-blue   { background: linear-gradient(to right,#2563eb,#6366f1); color: white; }
  .btn-gray   { background: #1e293b; color: #e2e8f0; border: 1px solid #475569; }
  .btn:hover  { opacity: 0.85; }

  /* ── Stat cards ── */
  .stats-row {
    display: flex; gap: 14px; padding: 16px 28px;
    background: rgba(10,15,28,0.6);
    border-bottom: 1px solid rgba(99,102,241,0.15);
  }
  .stat-card {
    flex: 1;
    background: linear-gradient(145deg,rgba(30,41,59,0.9),rgba(15,23,42,0.95));
    border: 1px solid rgba(148,163,184,0.12); border-radius: 14px; padding: 14px 18px;
  }
  .stat-card.highlight {
    border-color: rgba(96,165,250,0.4);
    background: linear-gradient(145deg,rgba(37,99,235,0.25),rgba(15,23,42,0.95));
  }
  .stat-label { font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; }
  .stat-value { font-size: 26px; font-weight: 800; color: #f8fafc; margin-top: 4px; }

  /* ── Tabs ── */
  .tabs {
    display: flex; padding: 0 28px;
    border-bottom: 1px solid rgba(99,102,241,0.2);
  }
  .tab {
    padding: 12px 24px; cursor: pointer; font-size: 13px; font-weight: 700;
    color: #94a3b8; border-bottom: 2px solid transparent; transition: all 0.2s;
  }
  .tab:hover { color: #e2e8f0; }
  .tab.active { color: #e0e7ff; border-bottom-color: #6366f1; background: rgba(79,70,229,0.15); }

  /* ── Content ── */
  .content { padding: 24px 28px 60px; }
  .tab-panel { display: none; }
  .tab-panel.active { display: block; }

  /* ── Cards ── */
  .card {
    background: rgba(17,24,39,0.88); border: 1px solid rgba(148,163,184,0.15);
    border-radius: 16px; padding: 22px; margin-bottom: 20px;
  }
  .card-title { font-size: 15px; font-weight: 700; color: #e2e8f0; margin-bottom: 16px; }

  /* ── Forms ── */
  .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .form-grid.cols3 { grid-template-columns: 1fr 1fr 1fr; }
  input[type=text], input[type=password], input:not([type]), select {
    width: 100%; padding: 10px 14px; background: #0b1220; color: #f1f5f9;
    border: 1.5px solid #334155; border-radius: 10px; font-size: 13px;
  }
  input:focus, select:focus { outline: none; border-color: #6366f1; }
  input::placeholder { color: #64748b; }
  .form-actions { margin-top: 14px; display: flex; gap: 10px; align-items: center; }

  /* ── Messages — auto-fade via .fade-out class ── */
  .msg {
    font-size: 13px; padding: 8px 14px; border-radius: 8px;
    transition: opacity 0.5s ease;
  }
  .msg-ok  { background: rgba(34,197,94,0.15);  color: #4ade80; }
  .msg-err { background: rgba(239,68,68,0.15);  color: #f87171; }
  .msg.fade-out { opacity: 0; }

  /* ── Tables ── */
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th {
    background: #0f172a; color: #cbd5e1; font-weight: 700; font-size: 12px;
    padding: 12px 14px; text-align: left; border-bottom: 1px solid #334155;
  }
  td { padding: 11px 14px; border-bottom: 1px solid rgba(51,65,85,0.4); color: #f1f5f9; }
  tr:nth-child(odd)  td { background: #0b1220; }
  tr:nth-child(even) td { background: #0f172a; }
  tr:hover td { background: #1e293b; }
  .badge-buy     { color: #4ade80; font-weight: 700; }
  .badge-sell    { color: #f87171; font-weight: 700; }
  .badge-pending { color: #fbbf24; font-weight: 700; }

  /* ── Footer ── */
  .footer {
    position: fixed; bottom: 0; left: 0; right: 0;
    background: rgba(6,10,19,0.98); border-top: 1px solid rgba(99,102,241,0.2);
    padding: 8px 28px; font-size: 12px; color: #64748b;
    display: flex; justify-content: space-between;
  }
</style>
</head>
<body>

<!-- ════════════════════════════════════════
     LOGIN SCREEN
════════════════════════════════════════ -->
<div id="loginScreen">
  <div class="login-box">
    <div class="login-logo">BrokerCraft</div>
    <div class="login-sub">Admin Dashboard — sign in to continue</div>

    <label class="login-label">Username</label>
    <input id="loginUsername" class="login-input" type="text"
           placeholder="admin" onkeydown="if(event.key==='Enter') doLogin()"/>

    <label class="login-label">Password</label>
    <input id="loginPassword" class="login-input" type="password"
           placeholder="••••••••" onkeydown="if(event.key==='Enter') doLogin()"/>

    <button class="login-btn" onclick="doLogin()">Sign In</button>
    <div class="login-err" id="loginErr"></div>
  </div>
</div>

<!-- ════════════════════════════════════════
     DASHBOARD (shown after login)
════════════════════════════════════════ -->
<div id="dashboard">

  <!-- Header -->
  <div class="header">
    <span class="logo">BrokerCraft</span>
    <span class="header-sub">Admin Command Center</span>
    <span class="header-admin" id="headerAdmin"></span>
    <div class="header-spacer"></div>
    <span id="simBadge" class="badge badge-off">● SIM OFF</span>
    <button class="btn btn-green"  onclick="startSim()">Start Market</button>
    <button class="btn btn-yellow" onclick="stopSim()">Stop Market</button>
    <button class="btn btn-gray"   onclick="doLogout()">Logout</button>
  </div>

  <!-- Stat cards -->
  <div class="stats-row">
    <div class="stat-card highlight">
      <div class="stat-label">Pending Clients</div>
      <div class="stat-value" id="statPending">—</div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Active Brokers</div>
      <div class="stat-value" id="statBrokers">—</div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Total Transactions</div>
      <div class="stat-value" id="statTx">—</div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Listed Stocks</div>
      <div class="stat-value" id="statStocks">—</div>
    </div>
  </div>

  <!-- Tabs -->
  <div class="tabs">
    <div class="tab active" onclick="switchTab('overview')">Overview</div>
    <div class="tab" onclick="switchTab('approvals')">Client Approvals</div>
    <div class="tab" onclick="switchTab('companies')">Companies</div>
    <div class="tab" onclick="switchTab('ipos')">IPO Approvals</div>
    <div class="tab" onclick="switchTab('brokers')">Brokers</div>
    <div class="tab" onclick="switchTab('transactions')">Transactions</div>
  </div>

  <!-- Tab content -->
  <div class="content">

    <!-- Overview -->
    <div id="tab-overview" class="tab-panel active">
      <div class="card">
        <div class="card-title">Live Market Prices</div>
        <table>
          <thead><tr><th>Symbol</th><th>Company</th><th>Price (ETB)</th></tr></thead>
          <tbody id="stocksBody"><tr><td colspan="3">Loading...</td></tr></tbody>
        </table>
      </div>
    </div>

    <!-- Approvals -->
    <div id="tab-approvals" class="tab-panel">
      <div class="card">
        <div class="card-title">Pending Client Registrations</div>
        <table>
          <thead><tr><th>Full Name</th><th>Username</th><th>Email</th><th>Status</th><th>Action</th></tr></thead>
          <tbody id="pendingBody"><tr><td colspan="5">Loading...</td></tr></tbody>
        </table>
      </div>
      <div id="approveForm" style="display:none;" class="card">
        <div class="card-title">Approve Client</div>
        <p style="color:#94a3b8;margin-bottom:12px;">
          Approving: <strong id="approveClientName"></strong>
        </p>
        <div class="form-grid">
          <select id="brokerSelect"><option value="">Select broker...</option></select>
        </div>
        <div class="form-actions">
          <button class="btn btn-blue" onclick="confirmApprove()">Approve &amp; Assign</button>
          <button class="btn btn-gray" onclick="cancelApprove()">Cancel</button>
          <span id="approveMsg" class="msg"></span>
        </div>
      </div>
    </div>

    <!-- Brokers -->
    <div id="tab-brokers" class="tab-panel">
      <div class="card">
        <div class="card-title">Create New Broker Account</div>
        <div class="form-grid cols3">
          <input id="bUsername" placeholder="Username"/>
          <input id="bPassword" placeholder="Password" type="password"/>
          <input id="bFullName" placeholder="Full Name"/>
          <input id="bDept"     placeholder="Department (optional)" style="grid-column:span 3"/>
        </div>
        <div class="form-actions">
          <button class="btn btn-blue" onclick="createBroker()">+ Create Broker</button>
          <span id="brokerMsg" class="msg"></span>
        </div>
      </div>

      <div class="card">
        <div class="card-title">Registered Brokers</div>
        <table>
          <thead><tr><th>Full Name</th><th>Username</th><th>Department</th><th>Clients</th><th>Actions</th></tr></thead>
          <tbody id="brokersBody"><tr><td colspan="5">Loading...</td></tr></tbody>
        </table>
      </div>

      <!-- Edit broker form (hidden until edit clicked) -->
      <div id="editBrokerForm" style="display:none;" class="card">
        <div class="card-title">Edit Broker</div>
        <div class="form-grid cols3">
          <input id="editBFullName"   placeholder="Full Name"/>
          <input id="editBDept"       placeholder="Department" style="grid-column:span 2"/>
        </div>
        <div class="form-actions">
          <button class="btn btn-blue"  onclick="confirmEditBroker()">Save Changes</button>
          <button class="btn btn-gray"  onclick="cancelEditBroker()">Cancel</button>
          <span id="editBrokerMsg" class="msg"></span>
        </div>
      </div>

      <!-- Reassign client form -->
      <div class="card">
        <div class="card-title">Reassign Client to Different Broker</div>
        <p style="color:#94a3b8;margin-bottom:12px;font-size:13px;">
          Move an approved client from their current broker to a new one.
        </p>
        <div class="form-grid">
          <select id="reassignClientSelect"><option value="">Select client...</option></select>
          <select id="reassignBrokerSelect"><option value="">Select new broker...</option></select>
        </div>
        <div class="form-actions">
          <button class="btn btn-blue" onclick="doReassign()">Reassign Client</button>
          <span id="reassignMsg" class="msg"></span>
        </div>
      </div>
    </div>

    <!-- Transactions -->
    <div id="tab-transactions" class="tab-panel">
      <div class="card">
        <div class="card-title">All Platform Transactions</div>
        <table>
          <thead><tr><th>Time</th><th>Client</th><th>Side</th><th>Symbol</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
          <tbody id="txBody"><tr><td colspan="7">Loading...</td></tr></tbody>
        </table>
      </div>
    </div>

    <!-- Companies -->
    <div id="tab-companies" class="tab-panel">
      <div class="card">
        <div class="card-title">Pending Company Registrations</div>
        <table>
          <thead><tr><th>Company Name</th><th>Username</th><th>Email</th><th>Industry</th><th>Description</th><th>Action</th></tr></thead>
          <tbody id="companiesBody"><tr><td colspan="6">Loading...</td></tr></tbody>
        </table>
      </div>
    </div>

    <!-- IPO Approvals -->
    <div id="tab-ipos" class="tab-panel">
      <div class="card">
        <div class="card-title">Pending IPO Listings</div>
        <table>
          <thead><tr><th>Company</th><th>Symbol</th><th>Shares</th><th>Price (ETB)</th><th>Deadline</th><th>Description</th><th>Action</th></tr></thead>
          <tbody id="iposBody"><tr><td colspan="7">Loading...</td></tr></tbody>
        </table>
      </div>
      <div class="card" style="margin-top:20px;">
        <div class="card-title">All IPO Listings</div>
        <table>
          <thead><tr><th>Company</th><th>Symbol</th><th>Offered</th><th>Remaining</th><th>Price</th><th>Deadline</th><th>Status</th></tr></thead>
          <tbody id="allIposBody"><tr><td colspan="7">Loading...</td></tr></tbody>
        </table>
      </div>
    </div>

  </div><!-- /content -->

  <!-- Footer -->
  <div class="footer">
    <span>BrokerCraft Admin v1.1</span>
    <span id="lastUpdated">Connecting...</span>
  </div>

</div><!-- /dashboard -->

<script>
// ════════════════════════════════════════
// STATE
// ════════════════════════════════════════
let currentTab = 'overview';
let pendingApproveClientId = null;
let loggedInAdmin = null;
let refreshTimer = null;

// ════════════════════════════════════════
// LOGIN
// ════════════════════════════════════════

/**
 * doLogin — calls /api/admin/login with username+password.
 * On success: hides login screen, shows dashboard, starts auto-refresh.
 */
async function doLogin() {
  const username = document.getElementById('loginUsername').value.trim();
  const password = document.getElementById('loginPassword').value;
  const errEl    = document.getElementById('loginErr');
  errEl.textContent = '';

  if (!username || !password) {
    errEl.textContent = 'Enter username and password.'; return;
  }

  try {
    const res = await post('/api/admin/login', { username, password });
    if (res.success) {
      loggedInAdmin = res.fullName || username;
      document.getElementById('loginScreen').style.display = 'none';
      document.getElementById('dashboard').style.display   = 'block';
      document.getElementById('headerAdmin').textContent   = '● ' + loggedInAdmin;
      startDashboard();
    } else {
      errEl.textContent = res.error || 'Invalid credentials.';
    }
  } catch (e) {
    errEl.textContent = 'Cannot reach server. Is it running?';
  }
}

function doLogout() {
  loggedInAdmin = null;
  if (refreshTimer) clearInterval(refreshTimer);
  document.getElementById('dashboard').style.display    = 'none';
  document.getElementById('loginScreen').style.display  = 'flex';
  document.getElementById('loginUsername').value = '';
  document.getElementById('loginPassword').value = '';
  document.getElementById('loginErr').textContent = '';
}

function startDashboard() {
  refresh();
  refreshTimer = setInterval(refresh, 5000);
}

// ════════════════════════════════════════
// TAB SWITCHING
// ════════════════════════════════════════
function switchTab(name) {
  currentTab = name;
  const names = ['overview','approvals','companies','ipos','brokers','transactions'];
  document.querySelectorAll('.tab').forEach((t, i) =>
    t.classList.toggle('active', names[i] === name));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.getElementById('tab-' + name).classList.add('active');
  loadTab(name);
}

function loadTab(name) {
  if (name === 'overview')     loadStocks();
  if (name === 'approvals')    loadPending();
  if (name === 'companies')    loadCompanies();
  if (name === 'ipos')         loadIpos();
  if (name === 'brokers')      loadBrokers();
  if (name === 'transactions') loadTransactions();
}

// ════════════════════════════════════════
// API HELPERS
// ════════════════════════════════════════
async function get(url) {
  const r = await fetch(url); return r.json();
}
async function post(url, body) {
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return r.json();
}

// ════════════════════════════════════════
// STATS
// ════════════════════════════════════════
async function loadStats() {
  const s = await get('/api/stats');
  document.getElementById('statPending').textContent = s.pendingCount ?? '—';
  document.getElementById('statBrokers').textContent = s.brokerCount  ?? '—';
  document.getElementById('statTx').textContent      = s.txCount      ?? '—';
  document.getElementById('statStocks').textContent  = s.stockCount   ?? '—';
  const badge = document.getElementById('simBadge');
  badge.textContent = s.simRunning ? '● SIM ON' : '● SIM OFF';
  badge.className   = 'badge ' + (s.simRunning ? 'badge-on' : 'badge-off');
  document.getElementById('lastUpdated').textContent =
    'Last updated: ' + new Date().toLocaleTimeString();
}

// ════════════════════════════════════════
// STOCKS
// ════════════════════════════════════════
async function loadStocks() {
  const stocks = await get('/api/stocks');
  const tbody = document.getElementById('stocksBody');
  if (!stocks.length) {
    tbody.innerHTML = '<tr><td colspan="3">No stocks found.</td></tr>'; return;
  }
  tbody.innerHTML = stocks.map(s => `
    <tr>
      <td><strong>${s.symbol}</strong></td>
      <td>${s.companyName}</td>
      <td style="color:#93c5fd;font-weight:700;">${s.price.toFixed(2)} ETB</td>
    </tr>`).join('');
}

// ════════════════════════════════════════
// PENDING CLIENTS
// ════════════════════════════════════════
async function loadPending() {
  const list = await get('/api/pending');
  const tbody = document.getElementById('pendingBody');
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="5" style="color:#64748b;">No pending registrations.</td></tr>';
    return;
  }
  tbody.innerHTML = list.map(c => `
    <tr>
      <td>${c.fullName}</td>
      <td>${c.username}</td>
      <td>${c.email}</td>
      <td><span class="badge-pending">${c.status}</span></td>
      <td>
        <button class="btn btn-blue" style="padding:5px 12px;font-size:12px;"
          onclick="startApprove(${c.userId}, '${c.fullName}')">Approve</button>
        <button class="btn btn-red" style="padding:5px 12px;font-size:12px;margin-left:6px;"
          onclick="rejectClient(${c.userId})">Reject</button>
      </td>
    </tr>`).join('');
}

async function startApprove(clientId, name) {
  pendingApproveClientId = clientId;
  document.getElementById('approveClientName').textContent = name;
  clearMsg('approveMsg');
  const brokers = await get('/api/brokers');
  const sel = document.getElementById('brokerSelect');
  sel.innerHTML = '<option value="">Select broker...</option>' +
    brokers.map(b => `<option value="${b.id}">${b.fullName} (${b.username})</option>`).join('');
  document.getElementById('approveForm').style.display = 'block';
  document.getElementById('approveForm').scrollIntoView({ behavior: 'smooth' });
}

function cancelApprove() {
  pendingApproveClientId = null;
  document.getElementById('approveForm').style.display = 'none';
}

async function confirmApprove() {
  const brokerId = parseInt(document.getElementById('brokerSelect').value);
  if (!brokerId) { showMsg('approveMsg', 'Select a broker first.', false); return; }
  const res = await post('/api/clients/approve', {
    clientId: pendingApproveClientId, brokerId
  });
  showMsg('approveMsg', res.message || res.error, res.success);
  if (res.success) {
    setTimeout(() => { cancelApprove(); loadPending(); loadStats(); }, 1500);
  }
}

async function rejectClient(clientId) {
  if (!confirm('Reject this client registration?')) return;
  const res = await post('/api/clients/reject', { clientId });
  alert(res.message || res.error);
  loadPending();
  loadStats();
}

// ════════════════════════════════════════
// BROKERS
// ════════════════════════════════════════
let editingBrokerId = null;

async function loadBrokers() {
  const brokers = await get('/api/brokers');
  const tbody = document.getElementById('brokersBody');
  if (!brokers.length) {
    tbody.innerHTML = '<tr><td colspan="5">No brokers yet.</td></tr>'; return;
  }
  tbody.innerHTML = brokers.map(b => `
    <tr>
      <td><strong>${b.fullName}</strong></td>
      <td>${b.username}</td>
      <td><span style="color:#a78bfa;">${b.department || 'General'}</span></td>
      <td><span style="color:#93c5fd;font-weight:700;">${b.clientCount}</span> clients</td>
      <td>
        <button class="btn btn-gray" style="padding:5px 12px;font-size:12px;"
          onclick="startEditBroker(${b.id},'${b.fullName}','${b.department||'General'}')">Edit</button>
        <button class="btn btn-red" style="padding:5px 12px;font-size:12px;margin-left:6px;"
          onclick="doDeleteBroker(${b.id},'${b.fullName}')">Delete</button>
      </td>
    </tr>`).join('');

  // Also populate reassign dropdowns
  const clientSel  = document.getElementById('reassignClientSelect');
  const brokerSel  = document.getElementById('reassignBrokerSelect');
  brokerSel.innerHTML = '<option value="">Select new broker...</option>' +
    brokers.map(b => `<option value="${b.id}">${b.fullName} (${b.username})</option>`).join('');

  const clients = await get('/api/clients/approved');
  clientSel.innerHTML = '<option value="">Select client...</option>' +
    clients.map(c => {
      const currentBroker = brokers.find(b => b.id === c.brokerId);
      const brokerName = currentBroker ? ` → currently: ${currentBroker.fullName}` : '';
      return `<option value="${c.userId}">${c.fullName} (${c.username})${brokerName}</option>`;
    }).join('');
}

function startEditBroker(id, fullName, dept) {
  editingBrokerId = id;
  document.getElementById('editBFullName').value = fullName;
  document.getElementById('editBDept').value     = dept;
  clearMsg('editBrokerMsg');
  document.getElementById('editBrokerForm').style.display = 'block';
  document.getElementById('editBrokerForm').scrollIntoView({ behavior: 'smooth' });
}

function cancelEditBroker() {
  editingBrokerId = null;
  document.getElementById('editBrokerForm').style.display = 'none';
}

async function confirmEditBroker() {
  const fullName   = document.getElementById('editBFullName').value.trim();
  const department = document.getElementById('editBDept').value.trim();
  if (!fullName) { showMsg('editBrokerMsg', 'Full name is required.', false); return; }
  const res = await post('/api/brokers/update', {
    brokerId: editingBrokerId, fullName, department
  });
  showMsg('editBrokerMsg', res.message || res.error, res.success);
  if (res.success) {
    setTimeout(() => { cancelEditBroker(); loadBrokers(); loadStats(); }, 1500);
  }
}

async function doDeleteBroker(brokerId, name) {
  if (!confirm(`Delete broker "${name}"?\n\nTheir assigned clients will be unassigned and will need to be reassigned to another broker.`)) return;
  const res = await post('/api/brokers/delete', { brokerId });
  alert(res.message || res.error);
  loadBrokers();
  loadStats();
}

async function doReassign() {
  const clientSel  = document.getElementById('reassignClientSelect');
  const brokerSel  = document.getElementById('reassignBrokerSelect');
  const clientId   = parseInt(clientSel.value);
  const brokerId   = parseInt(brokerSel.value);

  if (!clientId) { showMsg('reassignMsg', 'Select a client.', false); return; }
  if (!brokerId) { showMsg('reassignMsg', 'Select a broker.', false); return; }

  const res = await post('/api/clients/reassign', { clientId, brokerId });
  showMsg('reassignMsg', res.message || res.error, res.success);

  if (res.success) {
    // Update the client option label to show the new broker — do NOT reload dropdowns
    const brokerName = brokerSel.options[brokerSel.selectedIndex].text;
    const opt = clientSel.options[clientSel.selectedIndex];
    // Strip old "→ currently: ..." part and replace with new broker
    const baseName = opt.text.replace(/\s*→.*$/, '');
    opt.text = baseName + ' → currently: ' + brokerName.split(' (')[0];

    // Also refresh the broker client counts in the table after a short delay
    setTimeout(() => {
      // Only reload the broker table rows, not the dropdowns
      reloadBrokerTableOnly();
    }, 1500);
  }
}

async function reloadBrokerTableOnly() {
  const brokers = await get('/api/brokers');
  const tbody = document.getElementById('brokersBody');
  if (!brokers.length) {
    tbody.innerHTML = '<tr><td colspan="5">No brokers yet.</td></tr>'; return;
  }
  tbody.innerHTML = brokers.map(b => `
    <tr>
      <td><strong>${b.fullName}</strong></td>
      <td>${b.username}</td>
      <td><span style="color:#a78bfa;">${b.department || 'General'}</span></td>
      <td><span style="color:#93c5fd;font-weight:700;">${b.clientCount}</span> clients</td>
      <td>
        <button class="btn btn-gray" style="padding:5px 12px;font-size:12px;"
          onclick="startEditBroker(${b.id},'${b.fullName}','${b.department||'General'}')">Edit</button>
        <button class="btn btn-red" style="padding:5px 12px;font-size:12px;margin-left:6px;"
          onclick="doDeleteBroker(${b.id},'${b.fullName}')">Delete</button>
      </td>
    </tr>`).join('');
}

async function createBroker() {
  const username   = document.getElementById('bUsername').value.trim();
  const password   = document.getElementById('bPassword').value;
  const fullName   = document.getElementById('bFullName').value.trim();
  const department = document.getElementById('bDept').value.trim();

  if (!username || !password || !fullName) {
    showMsg('brokerMsg', 'Username, password and full name are required.', false); return;
  }

  const res = await post('/api/brokers/create', { username, password, fullName, department });

  if (res.id) {
    // Clear form immediately
    document.getElementById('bUsername').value = '';
    document.getElementById('bPassword').value = '';
    document.getElementById('bFullName').value = '';
    document.getElementById('bDept').value     = '';
    // Show success — auto-clears after 3 seconds
    showMsg('brokerMsg', 'Broker created successfully.', true);
    loadBrokers();
    loadStats();
  } else {
    showMsg('brokerMsg', res.error || 'Failed to create broker.', false);
  }
}

// ════════════════════════════════════════
// TRANSACTIONS
// ════════════════════════════════════════
async function loadTransactions() {
  const txs = await get('/api/transactions');
  const tbody = document.getElementById('txBody');
  if (!txs.length) {
    tbody.innerHTML = '<tr><td colspan="7" style="color:#64748b;">No transactions yet.</td></tr>'; return;
  }
  tbody.innerHTML = txs.map(t => `
    <tr>
      <td style="color:#94a3b8;font-size:12px;">${t.timestamp || ''}</td>
      <td>${t.clientName || t.clientId}</td>
      <td><span class="${t.type === 'BUY' ? 'badge-buy' : 'badge-sell'}">${t.type}</span></td>
      <td><strong>${t.symbol}</strong></td>
      <td>${t.quantity}</td>
      <td>${t.price.toFixed(2)} ETB</td>
      <td style="color:#93c5fd;">${(t.quantity * t.price).toFixed(2)} ETB</td>
    </tr>`).join('');
}

// ════════════════════════════════════════
// SIMULATION
// ════════════════════════════════════════
async function startSim() { await post('/api/simulation/start', {}); loadStats(); }
async function stopSim()  { await post('/api/simulation/stop',  {}); loadStats(); }

// ════════════════════════════════════════
// COMPANIES
// ════════════════════════════════════════
async function loadCompanies() {
  const list = await get('/api/companies/pending');
  const tbody = document.getElementById('companiesBody');
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="6" style="color:#64748b;">No pending company registrations.</td></tr>';
    return;
  }
  tbody.innerHTML = list.map(c => `
    <tr>
      <td><strong>${c.fullName}</strong></td>
      <td>${c.username}</td>
      <td>${c.email}</td>
      <td><span style="color:#a78bfa;">${c.industry}</span></td>
      <td style="color:#94a3b8;font-size:12px;">${c.description || '—'}</td>
      <td>
        <button class="btn btn-blue" style="padding:5px 12px;font-size:12px;"
          onclick="approveCompany(${c.userId})">Approve</button>
        <button class="btn btn-red" style="padding:5px 12px;font-size:12px;margin-left:6px;"
          onclick="rejectCompany(${c.userId})">Reject</button>
      </td>
    </tr>`).join('');
}

async function approveCompany(companyId) {
  if (!confirm('Approve this company?')) return;
  const res = await post('/api/companies/approve', { companyId });
  alert(res.message || res.error);
  loadCompanies(); loadStats();
}

async function rejectCompany(companyId) {
  if (!confirm('Reject this company registration?')) return;
  const res = await post('/api/companies/reject', { companyId });
  alert(res.message || res.error);
  loadCompanies();
}

// ════════════════════════════════════════
// IPOs
// ════════════════════════════════════════
async function loadIpos() {
  // Pending IPOs (need approval)
  const pending = await get('/api/ipos/pending');
  const pendingTbody = document.getElementById('iposBody');
  if (!pending.length) {
    pendingTbody.innerHTML = '<tr><td colspan="7" style="color:#64748b;">No pending IPOs.</td></tr>';
  } else {
    pendingTbody.innerHTML = pending.map(i => `
      <tr>
        <td><strong>${i.companyName}</strong></td>
        <td style="color:#93c5fd;font-weight:700;">${i.symbol}</td>
        <td>${i.sharesOffered.toLocaleString()}</td>
        <td>${i.pricePerShare.toFixed(2)} ETB</td>
        <td>${i.deadline}</td>
        <td style="color:#94a3b8;font-size:12px;">${i.description || '—'}</td>
        <td>
          <button class="btn btn-green" style="padding:5px 12px;font-size:12px;"
            onclick="approveIpo(${i.id})">Approve</button>
          <button class="btn btn-red" style="padding:5px 12px;font-size:12px;margin-left:6px;"
            onclick="rejectIpo(${i.id})">Reject</button>
        </td>
      </tr>`).join('');
  }

  // All IPOs overview
  const all = await get('/api/ipos/all');
  const allTbody = document.getElementById('allIposBody');
  if (!all.length) {
    allTbody.innerHTML = '<tr><td colspan="7" style="color:#64748b;">No IPOs yet.</td></tr>';
  } else {
    allTbody.innerHTML = all.map(i => {
      const statusColor = {OPEN:'#4ade80',PENDING:'#fbbf24',CLOSED:'#94a3b8',REJECTED:'#f87171'}[i.status] || '#f1f5f9';
      return `<tr>
        <td>${i.companyName}</td>
        <td><strong>${i.symbol}</strong></td>
        <td>${i.sharesOffered.toLocaleString()}</td>
        <td>${i.sharesRemaining.toLocaleString()}</td>
        <td>${i.pricePerShare.toFixed(2)} ETB</td>
        <td>${i.deadline}</td>
        <td><span style="color:${statusColor};font-weight:700;">${i.status}</span></td>
      </tr>`;
    }).join('');
  }
}

async function approveIpo(ipoId) {
  if (!confirm('Approve this IPO? The stock will go live on the market immediately.')) return;
  const res = await post('/api/ipos/approve', { ipoId });
  alert(res.message || res.error);
  loadIpos(); loadStats();
}

async function rejectIpo(ipoId) {
  if (!confirm('Reject this IPO?')) return;
  const res = await post('/api/ipos/reject', { ipoId });
  alert(res.message || res.error);
  loadIpos();
}
// ════════════════════════════════════════
// MESSAGE HELPERS
// auto-clears success messages after 3s
// ════════════════════════════════════════
function showMsg(id, text, ok) {
  const el = document.getElementById(id);
  el.textContent = text;
  el.className = 'msg ' + (ok ? 'msg-ok' : 'msg-err');
  el.style.opacity = '1';
  if (ok) {
    // auto-fade after 3 seconds
    setTimeout(() => {
      el.classList.add('fade-out');
      setTimeout(() => clearMsg(id), 500);
    }, 3000);
  }
}

function clearMsg(id) {
  const el = document.getElementById(id);
  el.textContent = '';
  el.className = 'msg';
  el.style.opacity = '1';
}

// ════════════════════════════════════════
// AUTO-REFRESH
// ════════════════════════════════════════
function refresh() {
  loadStats();
  loadTab(currentTab);
}
</script>
</body>
</html>
""";
    }
}
