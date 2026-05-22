package brokercraft.web;

/**
 * AdminHtmlPage — returns the full HTML string for the Admin web dashboard.
 *
 * This is a single-page app written in plain HTML + CSS + JavaScript.
 * No external frameworks. Everything is inline so the server serves one file.
 *
 * How it works:
 *   1. Page loads → calls /api/stats to fill the header cards
 *   2. Each tab calls its own API when clicked
 *   3. A timer refreshes stats + current tab every 5 seconds automatically
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
<title>BrokerCraft — Admin Dashboard</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: 'Segoe UI', system-ui, sans-serif;
    background: #060a13;
    color: #f1f5f9;
    min-height: 100vh;
  }

  /* ── Header ── */
  .header {
    background: linear-gradient(to right, #060a13, #0c1222);
    border-bottom: 1px solid rgba(99,102,241,0.3);
    padding: 14px 28px;
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .logo { font-size: 22px; font-weight: 800;
    background: linear-gradient(to right, #60a5fa, #a78bfa);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
  .header-sub { color: #94a3b8; font-size: 13px; }
  .header-spacer { flex: 1; }
  .badge {
    padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 700;
    border: 1px solid;
  }
  .badge-on  { background: rgba(34,197,94,0.15);  border-color: rgba(74,222,128,0.5);  color: #4ade80; }
  .badge-off { background: rgba(251,191,36,0.12); border-color: rgba(251,191,36,0.4);  color: #fbbf24; }
  .btn {
    padding: 8px 18px; border-radius: 10px; border: none; cursor: pointer;
    font-size: 13px; font-weight: 700;
  }
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
    flex: 1; background: linear-gradient(145deg,rgba(30,41,59,0.9),rgba(15,23,42,0.95));
    border: 1px solid rgba(148,163,184,0.12); border-radius: 14px; padding: 14px 18px;
  }
  .stat-card.highlight { border-color: rgba(96,165,250,0.4);
    background: linear-gradient(145deg,rgba(37,99,235,0.25),rgba(15,23,42,0.95)); }
  .stat-label { font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; }
  .stat-value { font-size: 26px; font-weight: 800; color: #f8fafc; margin-top: 4px; }

  /* ── Tabs ── */
  .tabs { display: flex; gap: 0; padding: 0 28px;
    border-bottom: 1px solid rgba(99,102,241,0.2); background: transparent; }
  .tab {
    padding: 12px 24px; cursor: pointer; font-size: 13px; font-weight: 700;
    color: #94a3b8; border-bottom: 2px solid transparent; transition: all 0.2s;
  }
  .tab:hover { color: #e2e8f0; }
  .tab.active { color: #e0e7ff; border-bottom-color: #6366f1;
    background: rgba(79,70,229,0.15); }

  /* ── Content area ── */
  .content { padding: 24px 28px; }
  .tab-panel { display: none; }
  .tab-panel.active { display: block; }

  /* ── Cards ── */
  .card {
    background: rgba(17,24,39,0.88); border: 1px solid rgba(148,163,184,0.15);
    border-radius: 16px; padding: 22px; margin-bottom: 20px;
  }
  .card-title { font-size: 15px; font-weight: 700; color: #e2e8f0; margin-bottom: 16px; }

  /* ── Form ── */
  .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .form-grid.cols3 { grid-template-columns: 1fr 1fr 1fr; }
  input, select {
    width: 100%; padding: 10px 14px; background: #0b1220; color: #f1f5f9;
    border: 1.5px solid #334155; border-radius: 10px; font-size: 13px;
  }
  input:focus, select:focus { outline: none; border-color: #6366f1; }
  input::placeholder { color: #64748b; }
  .form-actions { margin-top: 14px; display: flex; gap: 10px; align-items: center; }
  .msg { font-size: 13px; padding: 8px 14px; border-radius: 8px; }
  .msg-ok  { background: rgba(34,197,94,0.15);  color: #4ade80; }
  .msg-err { background: rgba(239,68,68,0.15);  color: #f87171; }

  /* ── Tables ── */
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th {
    background: #0f172a; color: #cbd5e1; font-weight: 700; font-size: 12px;
    padding: 12px 14px; text-align: left; border-bottom: 1px solid #334155;
  }
  td { padding: 11px 14px; border-bottom: 1px solid rgba(51,65,85,0.4); color: #f1f5f9; }
  tr:nth-child(odd) td { background: #0b1220; }
  tr:nth-child(even) td { background: #0f172a; }
  tr:hover td { background: #1e293b; }
  .badge-buy  { color: #4ade80; font-weight: 700; }
  .badge-sell { color: #f87171; font-weight: 700; }
  .badge-pending  { color: #fbbf24; font-weight: 700; }
  .badge-approved { color: #4ade80; font-weight: 700; }

  /* ── Footer ── */
  .footer {
    position: fixed; bottom: 0; left: 0; right: 0;
    background: rgba(6,10,19,0.98); border-top: 1px solid rgba(99,102,241,0.2);
    padding: 8px 28px; font-size: 12px; color: #64748b;
    display: flex; justify-content: space-between;
  }
  #lastUpdated { color: #475569; }
</style>
</head>
<body>

<!-- ── Header ── -->
<div class="header">
  <span class="logo">BrokerCraft</span>
  <span class="header-sub">Admin Command Center</span>
  <div class="header-spacer"></div>
  <span id="simBadge" class="badge badge-off">● SIM OFF</span>
  <button class="btn btn-green"  onclick="startSim()">Start Market</button>
  <button class="btn btn-yellow" onclick="stopSim()">Stop Market</button>
</div>

<!-- ── Stat cards ── -->
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

<!-- ── Tabs ── -->
<div class="tabs">
  <div class="tab active" onclick="switchTab('overview')">Overview</div>
  <div class="tab" onclick="switchTab('approvals')">Approvals</div>
  <div class="tab" onclick="switchTab('brokers')">Brokers</div>
  <div class="tab" onclick="switchTab('transactions')">Transactions</div>
</div>

<!-- ── Tab content ── -->
<div class="content">

  <!-- Overview tab -->
  <div id="tab-overview" class="tab-panel active">
    <div class="card">
      <div class="card-title">Live Market Prices</div>
      <table>
        <thead><tr><th>Symbol</th><th>Company</th><th>Price (ETB)</th></tr></thead>
        <tbody id="stocksBody"><tr><td colspan="3">Loading...</td></tr></tbody>
      </table>
    </div>
  </div>

  <!-- Approvals tab -->
  <div id="tab-approvals" class="tab-panel">
    <div class="card">
      <div class="card-title">Pending Client Registrations</div>
      <table>
        <thead><tr><th>Full Name</th><th>Username</th><th>Email</th><th>Status</th><th>Action</th></tr></thead>
        <tbody id="pendingBody"><tr><td colspan="5">Loading...</td></tr></tbody>
      </table>
    </div>
    <!-- Broker selector used by approve buttons -->
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
        <span id="approveMsg"></span>
      </div>
    </div>
  </div>

  <!-- Brokers tab -->
  <div id="tab-brokers" class="tab-panel">
    <div class="card">
      <div class="card-title">Create New Broker Account</div>
      <div class="form-grid cols3">
        <input id="bUsername"   placeholder="Username"/>
        <input id="bPassword"   placeholder="Password" type="password"/>
        <input id="bFullName"   placeholder="Full Name"/>
        <input id="bDept"       placeholder="Department" style="grid-column:span 3"/>
      </div>
      <div class="form-actions">
        <button class="btn btn-blue" onclick="createBroker()">+ Create Broker</button>
        <span id="brokerMsg"></span>
      </div>
    </div>
    <div class="card">
      <div class="card-title">Registered Brokers</div>
      <table>
        <thead><tr><th>Full Name</th><th>Username</th><th>Role</th></tr></thead>
        <tbody id="brokersBody"><tr><td colspan="3">Loading...</td></tr></tbody>
      </table>
    </div>
  </div>

  <!-- Transactions tab -->
  <div id="tab-transactions" class="tab-panel">
    <div class="card">
      <div class="card-title">All Platform Transactions</div>
      <table>
        <thead><tr><th>Time</th><th>Client</th><th>Side</th><th>Symbol</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
        <tbody id="txBody"><tr><td colspan="7">Loading...</td></tr></tbody>
      </table>
    </div>
  </div>

</div><!-- /content -->

<!-- ── Footer ── -->
<div class="footer">
  <span>BrokerCraft Admin v1.0</span>
  <span id="lastUpdated">Connecting...</span>
</div>

<script>
// ── State ────────────────────────────────────────────────────────────────────
let currentTab = 'overview';
let pendingApproveClientId = null;

// ── Tab switching ─────────────────────────────────────────────────────────────
function switchTab(name) {
  currentTab = name;
  document.querySelectorAll('.tab').forEach((t, i) => {
    const names = ['overview','approvals','brokers','transactions'];
    t.classList.toggle('active', names[i] === name);
  });
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.getElementById('tab-' + name).classList.add('active');
  loadTab(name);
}

function loadTab(name) {
  if (name === 'overview')      loadStocks();
  if (name === 'approvals')     loadPending();
  if (name === 'brokers')       loadBrokers();
  if (name === 'transactions')  loadTransactions();
}

// ── API helpers ───────────────────────────────────────────────────────────────
async function get(url) {
  const r = await fetch(url); return r.json();
}
async function post(url, body) {
  const r = await fetch(url, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(body)
  });
  return r.json();
}

// ── Stats (header cards) ──────────────────────────────────────────────────────
async function loadStats() {
  const s = await get('/api/stats');
  document.getElementById('statPending').textContent = s.pendingCount ?? '—';
  document.getElementById('statBrokers').textContent = s.brokerCount  ?? '—';
  document.getElementById('statTx').textContent      = s.txCount      ?? '—';
  document.getElementById('statStocks').textContent  = s.stockCount   ?? '—';
  const badge = document.getElementById('simBadge');
  if (s.simRunning) {
    badge.textContent = '● SIM ON';
    badge.className = 'badge badge-on';
  } else {
    badge.textContent = '● SIM OFF';
    badge.className = 'badge badge-off';
  }
  document.getElementById('lastUpdated').textContent =
    'Last updated: ' + new Date().toLocaleTimeString();
}

// ── Stocks ────────────────────────────────────────────────────────────────────
async function loadStocks() {
  const stocks = await get('/api/stocks');
  const tbody = document.getElementById('stocksBody');
  if (!stocks.length) { tbody.innerHTML = '<tr><td colspan="3">No stocks found.</td></tr>'; return; }
  tbody.innerHTML = stocks.map(s => `
    <tr>
      <td><strong>${s.symbol}</strong></td>
      <td>${s.companyName}</td>
      <td style="color:#93c5fd;font-weight:700;">${s.price.toFixed(2)} ETB</td>
    </tr>`).join('');
}

// ── Pending clients ───────────────────────────────────────────────────────────
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

// Show the approve form for a specific client
async function startApprove(clientId, name) {
  pendingApproveClientId = clientId;
  document.getElementById('approveClientName').textContent = name;
  document.getElementById('approveMsg').textContent = '';

  // Load brokers into the select dropdown
  const brokers = await get('/api/brokers');
  const sel = document.getElementById('brokerSelect');
  sel.innerHTML = '<option value="">Select broker...</option>' +
    brokers.map(b => `<option value="${b.id}">${b.fullName} (${b.username})</option>`).join('');

  document.getElementById('approveForm').style.display = 'block';
  document.getElementById('approveForm').scrollIntoView({behavior:'smooth'});
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
    cancelApprove();
    loadPending();
    loadStats();
  }
}

async function rejectClient(clientId) {
  if (!confirm('Reject this client registration?')) return;
  const res = await post('/api/clients/reject', { clientId });
  alert(res.message || res.error);
  loadPending();
  loadStats();
}

// ── Brokers ───────────────────────────────────────────────────────────────────
async function loadBrokers() {
  const brokers = await get('/api/brokers');
  const tbody = document.getElementById('brokersBody');
  if (!brokers.length) { tbody.innerHTML = '<tr><td colspan="3">No brokers yet.</td></tr>'; return; }
  tbody.innerHTML = brokers.map(b => `
    <tr>
      <td>${b.fullName}</td>
      <td>${b.username}</td>
      <td><span style="color:#a78bfa;font-weight:700;">${b.role}</span></td>
    </tr>`).join('');
}

async function createBroker() {
  const username   = document.getElementById('bUsername').value.trim();
  const password   = document.getElementById('bPassword').value;
  const fullName   = document.getElementById('bFullName').value.trim();
  const department = document.getElementById('bDept').value.trim();
  if (!username || !password || !fullName) {
    showMsg('brokerMsg', 'Fill in all fields.', false); return;
  }
  const res = await post('/api/brokers/create', {username, password, fullName, department});
  if (res.id) {
    showMsg('brokerMsg', 'Broker created successfully.', true);
    document.getElementById('bUsername').value = '';
    document.getElementById('bPassword').value = '';
    document.getElementById('bFullName').value = '';
    document.getElementById('bDept').value = '';
    loadBrokers();
    loadStats();
  } else {
    showMsg('brokerMsg', res.error || 'Failed.', false);
  }
}

// ── Transactions ──────────────────────────────────────────────────────────────
async function loadTransactions() {
  const txs = await get('/api/transactions');
  const tbody = document.getElementById('txBody');
  if (!txs.length) { tbody.innerHTML = '<tr><td colspan="7" style="color:#64748b;">No transactions yet.</td></tr>'; return; }
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

// ── Simulation ────────────────────────────────────────────────────────────────
async function startSim() {
  await post('/api/simulation/start', {});
  loadStats();
}
async function stopSim() {
  await post('/api/simulation/stop', {});
  loadStats();
}

// ── Utility ───────────────────────────────────────────────────────────────────
function showMsg(id, text, ok) {
  const el = document.getElementById(id);
  el.textContent = text;
  el.className = 'msg ' + (ok ? 'msg-ok' : 'msg-err');
}

// ── Auto-refresh every 5 seconds ──────────────────────────────────────────────
function refresh() {
  loadStats();
  loadTab(currentTab);
}

refresh(); // initial load
setInterval(refresh, 5000); // auto-refresh
</script>
</body>
</html>
""";
    }
}
