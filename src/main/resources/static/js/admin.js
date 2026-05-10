let serviceChartInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardStats();
    loadServiceUsage();
});

function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    event.currentTarget.classList.add('active');
    document.querySelectorAll('.admin-section').forEach(el => el.classList.remove('active'));
    document.getElementById(tabId).classList.add('active');

    const titles = {
        'dashboard': 'Dashboard Overview',
        'reports': 'Daily Service Reports',
        'monitoring': 'Service Usage Monitoring',
        'performance': 'Staff Performance',
        'staff': 'Staff Management',
        'financial': 'Financial Audit',
        'blacklist': 'Blacklisted Citizens'
    };
    document.getElementById('page-title').textContent = titles[tabId];

    if (tabId === 'dashboard') { loadDashboardStats(); loadServiceUsage(); }
    else if (tabId === 'reports') { /* user picks date manually */ }
    else if (tabId === 'monitoring') { loadServiceMonitoring(); }
    else if (tabId === 'performance') { loadStaffDropdown(); }
    else if (tabId === 'staff') { loadStaffStats(); loadStaff(); }
    else if (tabId === 'financial') { /* user picks date range manually */ }
    else if (tabId === 'blacklist') { loadBlacklist(); }
}

function logout() { window.location.href = '/index.html'; }

// ─── Dashboard ─────────────────────────────────────────
async function loadDashboardStats() {
    try {
        const response = await fetch('http://localhost:8081/api/admin/dashboard-stats');
        const data = await response.json();
        document.getElementById('val-generated').textContent = data.generatedToday;
        document.getElementById('val-served').textContent = data.servedToday;
        document.getElementById('val-expired').textContent = data.expiredToday;
        document.getElementById('val-cancelled').textContent = data.cancelledToday || 0;
        document.getElementById('val-revenue').textContent = data.totalRevenue.toLocaleString();
    } catch (err) { console.error('Failed to load dashboard stats', err); }
}

async function loadServiceUsage() {
    try {
        const response = await fetch('http://localhost:8081/api/admin/service-usage');
        const data = await response.json();
        renderChart(data.map(d => d.serviceName), data.map(d => d.tokensGenerated), data.map(d => d.totalServed));
    } catch (err) { console.error('Failed to load service usage', err); }
}

function renderChart(labels, generatedData, servedData) {
    const ctx = document.getElementById('serviceChart').getContext('2d');
    if (serviceChartInstance) serviceChartInstance.destroy();
    serviceChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [
                { label: 'Tokens Generated', data: generatedData, backgroundColor: 'rgba(56, 189, 248, 0.8)', borderColor: '#38bdf8', borderWidth: 1, borderRadius: 4 },
                { label: 'Tokens Served', data: servedData, backgroundColor: 'rgba(52, 211, 153, 0.8)', borderColor: '#34d399', borderWidth: 1, borderRadius: 4 }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { color: '#cbd5e1' } }, title: { display: true, text: 'Service Usage (All Time)', color: '#f8fafc', font: { size: 16, weight: '500' } } },
            scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } }, x: { grid: { display: false }, ticks: { color: '#94a3b8' } } }
        }
    });
}

// ─── Staff Management ──────────────────────────────────

function parseRole(role) {
    if (role === 'ReceptionStaff') return { label: 'Reception', assignment: 'Reception Desk', type: 'reception' };
    if (role && role.startsWith('CounterStaff')) {
        const id = role.replace('CounterStaff', '');
        return { label: 'Counter Staff', assignment: `Counter ${id}`, type: 'counter', counterId: id };
    }
    return { label: role, assignment: '-', type: 'unknown' };
}

async function loadStaffStats() {
    try {
        const response = await fetch('http://localhost:8081/api/admin/staff/stats');
        const s = await response.json();
        document.getElementById('stat-total').textContent = s.totalStaff;
        document.getElementById('stat-counter').textContent = s.counterStaff;
        document.getElementById('stat-reception').textContent = s.receptionStaff;
        document.getElementById('stat-active').textContent = s.activeStaff;
    } catch (err) { console.error('Failed to load staff stats', err); }
}

async function loadStaff() {
    try {
        const response = await fetch('http://localhost:8081/api/admin/staff');
        const staffList = await response.json();
        const tbody = document.querySelector('#staff-table tbody');
        tbody.innerHTML = '';

        if (staffList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color: #94a3b8; padding: 2rem;">No staff accounts yet. Create one above.</td></tr>';
            return;
        }

        staffList.forEach(staff => {
            const tr = document.createElement('tr');
            const info = parseRole(staff.role);
            const statusColor = staff.active ? '#34d399' : '#ef4444';
            const statusLabel = staff.active ? 'Active' : 'Inactive';
            const roleBadge = info.type === 'reception'
                ? `<span class="badge" style="background:rgba(251,191,36,0.2);color:#fcd34d;border:1px solid rgba(251,191,36,0.3);">${info.label}</span>`
                : `<span class="badge badge-info">${info.label}</span>`;

            tr.innerHTML = `
                <td>${staff.userId}</td>
                <td style="font-weight:600;">${staff.fullName || '-'}</td>
                <td style="font-family: monospace; color: #60a5fa;">${staff.username}</td>
                <td>${roleBadge}</td>
                <td style="font-size: 0.85rem;">${info.assignment}</td>
                <td><span style="color:${statusColor};font-weight:600;font-size:0.85rem;">● ${statusLabel}</span></td>
                <td>
                    <button class="btn-action btn-view" onclick='viewStaffDetail(${JSON.stringify(staff).replace(/'/g, "\\'")})'><i class="fas fa-eye"></i></button>
                    <button class="btn-action btn-edit" onclick='openEditModal(${JSON.stringify(staff).replace(/'/g, "\\'")})'><i class="fas fa-edit"></i></button>
                    <button class="btn-action btn-del" onclick="deleteStaff(${staff.userId}, '${(staff.fullName || staff.username).replace(/'/g, "\\'")}')""><i class="fas fa-trash"></i></button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) { console.error('Failed to load staff', err); }
}

function toggleCounterSelect() {
    const role = document.getElementById('staff-role').value;
    const cg = document.getElementById('counter-select-group');
    const cs = document.getElementById('staff-counter');
    if (role === 'CounterStaff') { cg.style.display = ''; cs.required = true; }
    else { cg.style.display = 'none'; cs.required = false; }
}

function generatePassword() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
    let pwd = '';
    for (let i = 0; i < 8; i++) pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    document.getElementById('staff-password').value = pwd;
    document.getElementById('staff-password').type = 'text';
    setTimeout(() => { document.getElementById('staff-password').type = 'password'; }, 3000);
}

async function createStaff(event) {
    event.preventDefault();
    const errorDiv = document.getElementById('staff-error');
    const successDiv = document.getElementById('staff-success');
    errorDiv.style.display = 'none';
    successDiv.style.display = 'none';

    const fullName = document.getElementById('staff-name').value.trim();
    const username = document.getElementById('staff-username').value.trim();
    const password = document.getElementById('staff-password').value;
    const phoneNumber = document.getElementById('staff-phone').value.trim();
    const email = document.getElementById('staff-email').value.trim();
    const roleBase = document.getElementById('staff-role').value;

    let role = roleBase;
    if (roleBase === 'CounterStaff') role = 'CounterStaff' + document.getElementById('staff-counter').value;

    if (!/^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(email.toLowerCase())) {
        errorDiv.textContent = 'Only @gmail.com email addresses are allowed.';
        errorDiv.style.display = 'block';
        return;
    }

    if (phoneNumber.replace(/\D/g, '').length !== 11) {
        errorDiv.textContent = 'Phone number must be exactly 11 digits.';
        errorDiv.style.display = 'block';
        return;
    }

    if (password.length < 4) {
        errorDiv.textContent = 'Password must be at least 4 characters.';
        errorDiv.style.display = 'block';
        return;
    }

    try {
        const response = await fetch('http://localhost:8081/api/admin/staff', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, username, password, role, phoneNumber, email })
        });
        const data = await response.json();

        if (response.ok) {
            document.getElementById('staff-form').reset();
            toggleCounterSelect();
            showCreatedCredentials(data);
            loadStaff();
            loadStaffStats();
        } else {
            errorDiv.textContent = data.message || 'Failed to create staff account.';
            errorDiv.style.display = 'block';
        }
    } catch (err) {
        console.error(err);
        errorDiv.textContent = 'Network error. Please try again.';
        errorDiv.style.display = 'block';
    }
}

function showCreatedCredentials(data) {
    const info = parseRole(data.role);
    const modal = document.getElementById('credentialsModal');
    document.getElementById('cred-name').textContent = data.fullName;
    document.getElementById('cred-username').textContent = data.username;
    document.getElementById('cred-password').textContent = data.password;
    document.getElementById('cred-role').textContent = info.label;
    document.getElementById('cred-assignment').textContent = info.assignment;
    modal.classList.remove('hidden');
}

function closeCredentialsModal() {
    document.getElementById('credentialsModal').classList.add('hidden');
}

function copyCredentials() {
    const u = document.getElementById('cred-username').textContent;
    const p = document.getElementById('cred-password').textContent;
    const r = document.getElementById('cred-assignment').textContent;
    const text = `WaitWise Staff Credentials\nName: ${document.getElementById('cred-name').textContent}\nUsername: ${u}\nPassword: ${p}\nAssignment: ${r}`;
    navigator.clipboard.writeText(text).then(() => {
        const btn = document.getElementById('btn-copy-cred');
        btn.textContent = '✓ Copied!';
        setTimeout(() => { btn.innerHTML = '<i class="fas fa-copy"></i> Copy Credentials'; }, 2000);
    });
}

// ─── View Detail Modal ─────────────────────────────────

function viewStaffDetail(staff) {
    const info = parseRole(staff.role);
    const m = document.getElementById('detailModal');
    document.getElementById('detail-name').textContent = staff.fullName || '-';
    document.getElementById('detail-username').textContent = staff.username;
    document.getElementById('detail-password').textContent = staff.password;
    document.getElementById('detail-role').textContent = info.label;
    document.getElementById('detail-assignment').textContent = info.assignment;
    document.getElementById('detail-phone').textContent = staff.phoneNumber || 'Not provided';
    document.getElementById('detail-email').textContent = staff.email || 'Not provided';
    document.getElementById('detail-status').textContent = staff.active ? 'Active' : 'Inactive';
    document.getElementById('detail-status').style.color = staff.active ? '#34d399' : '#ef4444';
    document.getElementById('detail-created').textContent = staff.createdAt ? new Date(staff.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'N/A';
    m.classList.remove('hidden');
}

function closeDetailModal() { document.getElementById('detailModal').classList.add('hidden'); }

function togglePasswordVisibility(spanId, btn) {
    const span = document.getElementById(spanId);
    if (span.style.filter === 'blur(5px)') {
        span.style.filter = 'none';
        btn.innerHTML = '<i class="fas fa-eye-slash"></i>';
    } else {
        span.style.filter = 'blur(5px)';
        btn.innerHTML = '<i class="fas fa-eye"></i>';
    }
}

// ─── Edit Modal ────────────────────────────────────────

function openEditModal(staff) {
    const info = parseRole(staff.role);
    document.getElementById('edit-id').value = staff.userId;
    document.getElementById('edit-name').value = staff.fullName || '';
    document.getElementById('edit-phone').value = staff.phoneNumber || '';
    document.getElementById('edit-email').value = staff.email || '';
    document.getElementById('edit-password').value = '';
    document.getElementById('edit-active').value = staff.active ? 'true' : 'false';

    const roleSelect = document.getElementById('edit-role');
    const counterGroup = document.getElementById('edit-counter-group');
    const counterSelect = document.getElementById('edit-counter');

    if (info.type === 'reception') {
        roleSelect.value = 'ReceptionStaff';
        counterGroup.style.display = 'none';
    } else {
        roleSelect.value = 'CounterStaff';
        counterGroup.style.display = '';
        counterSelect.value = info.counterId || '1';
    }

    document.getElementById('editModal').classList.remove('hidden');
}

function closeEditModal() { document.getElementById('editModal').classList.add('hidden'); }

function toggleEditCounter() {
    const r = document.getElementById('edit-role').value;
    document.getElementById('edit-counter-group').style.display = r === 'CounterStaff' ? '' : 'none';
}

function generateEditPassword() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
    let pwd = '';
    for (let i = 0; i < 8; i++) pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    document.getElementById('edit-password').value = pwd;
    document.getElementById('edit-password').type = 'text';
    setTimeout(() => { document.getElementById('edit-password').type = 'password'; }, 3000);
}

async function saveStaffEdit(event) {
    event.preventDefault();
    const id = document.getElementById('edit-id').value;
    const fullName = document.getElementById('edit-name').value.trim();
    const phoneNumber = document.getElementById('edit-phone').value.trim();
    const email = document.getElementById('edit-email').value.trim();
    const password = document.getElementById('edit-password').value;
    const active = document.getElementById('edit-active').value;
    const roleBase = document.getElementById('edit-role').value;
    let role = roleBase;
    if (roleBase === 'CounterStaff') role = 'CounterStaff' + document.getElementById('edit-counter').value;

    if (email && !/^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(email.toLowerCase())) {
        alert('Only @gmail.com email addresses are allowed.');
        return;
    }

    if (phoneNumber && phoneNumber.replace(/\D/g, '').length !== 11) {
        alert('Phone number must be exactly 11 digits.');
        return;
    }

    const payload = { fullName, role, phoneNumber, email, active };
    if (password) payload.password = password;

    try {
        const response = await fetch(`http://localhost:8081/api/admin/staff/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (response.ok) {
            closeEditModal();
            loadStaff();
            loadStaffStats();
        } else {
            const data = await response.json();
            alert(data.message || 'Failed to update');
        }
    } catch (err) { console.error(err); alert('Network error'); }
}

async function deleteStaff(id, name) {
    if (!confirm(`Are you sure you want to delete staff account "${name}"? They will no longer be able to login.`)) return;
    try {
        const response = await fetch(`http://localhost:8081/api/admin/staff/${id}`, { method: 'DELETE' });
        if (response.ok) { loadStaff(); loadStaffStats(); }
    } catch (err) { console.error(err); }
}

// ─── Blacklist ─────────────────────────────────────────

async function loadBlacklist() {
    try {
        const response = await fetch('http://localhost:8081/api/admin/blacklisted');
        const list = await response.json();
        const tbody = document.querySelector('#blacklist-table tbody');
        tbody.innerHTML = '';
        list.forEach(citizen => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${citizen.cnic}</td>
                <td>${citizen.fullName}</td>
                <td>${citizen.phoneNumber}</td>
                <td>${citizen.noShowCount}</td>
                <td><span class="badge badge-danger">Blacklisted</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) { console.error('Failed to load blacklist', err); }
}

// ─── Reports ───────────────────────────────────────────

async function openReport(type) {
    const modal = document.getElementById('reportModal');
    const title = document.getElementById('modalTitle');
    const thead = document.getElementById('report-table-head');
    const tbody = document.getElementById('report-table-body');
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Loading...</td></tr>';
    modal.classList.remove('hidden');

    try {
        if (type === 'revenue') {
            title.textContent = "Total Revenue Report";
            thead.innerHTML = `<th>Service Name</th><th>Citizen Name</th><th>Amount (PKR)</th><th>Transaction Time</th>`;
            const response = await fetch('http://localhost:8081/api/admin/reports/revenue');
            const data = await response.json();
            tbody.innerHTML = '';
            if (data.length === 0) { tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No revenue data found.</td></tr>'; return; }
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `<td>${item.serviceName}</td><td>${item.citizenName}</td><td style="color: #34d399; font-weight: 600;">${item.amount}</td><td>${new Date(item.time).toLocaleString()}</td>`;
                tbody.appendChild(tr);
            });
        } else {
            const typesMap = { 'generated': 'Tokens Generated Today', 'served': 'Tokens Served Today', 'expired': 'Tokens Expired Today', 'cancelled': 'Tokens Cancelled Today' };
            title.textContent = typesMap[type];
            thead.innerHTML = `<th>Token #</th><th>Service</th><th>Priority</th><th>Citizen</th><th>Time</th>`;
            const response = await fetch(`http://localhost:8081/api/admin/reports/tokens?type=${type}`);
            const data = await response.json();
            tbody.innerHTML = '';
            if (data.length === 0) { tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No data found.</td></tr>'; return; }
            data.forEach(item => {
                const tr = document.createElement('tr');
                let badgeClass = 'badge-info';
                if (item.priority === 'Golden') badgeClass = 'badge-gold';
                else if (item.priority === 'Emergency') badgeClass = 'badge-danger';
                tr.innerHTML = `
                    <td style="font-weight:600;">${item.tokenNumber}</td><td>${item.serviceName}</td>
                    <td><span class="badge ${badgeClass}" style="${item.priority === 'Golden' ? 'background:rgba(251,191,36,0.2);color:#fcd34d;border:1px solid rgba(251,191,36,0.3);' : ''}">${item.priority}</span></td>
                    <td>${item.citizenName}</td><td>${new Date(item.time).toLocaleTimeString()}</td>`;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error('Failed to load report', err);
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color: #fca5a5;">Error loading report data.</td></tr>';
    }
}

function closeReport() { document.getElementById('reportModal').classList.add('hidden'); }

// ═══════════════════════════════════════════════════════════════
// MODULE 2 — Generate Daily Service Reports (GUI)
// ═══════════════════════════════════════════════════════════════

let dailyChartInstance = null;

async function generateDailyReport() {
    const date = document.getElementById('report-date').value;
    if (!date) { alert('Please select a date'); return; }

    document.getElementById('report-no-data').style.display = 'none';
    document.getElementById('report-results').style.display = 'none';

    try {
        const response = await fetch(`http://localhost:8081/api/admin/reports/daily?date=${date}`);
        const data = await response.json();

        if (!data.hasData) {
            document.getElementById('report-no-data').style.display = 'block';
            return;
        }

        document.getElementById('rpt-total').textContent = data.totalTokens;
        document.getElementById('rpt-served').textContent = data.servedTokens;
        document.getElementById('rpt-expired').textContent = data.expiredTokens;
        document.getElementById('rpt-cancelled').textContent = data.cancelledTokens;
        document.getElementById('report-results').style.display = 'block';

        // Render doughnut chart
        const ctx = document.getElementById('dailyReportChart').getContext('2d');
        if (dailyChartInstance) dailyChartInstance.destroy();
        dailyChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Served', 'Expired', 'Cancelled', 'Waiting/Other'],
                datasets: [{
                    data: [data.servedTokens, data.expiredTokens, data.cancelledTokens,
                           Math.max(0, data.totalTokens - data.servedTokens - data.expiredTokens - data.cancelledTokens)],
                    backgroundColor: ['rgba(52,211,153,0.8)', 'rgba(248,113,113,0.8)', 'rgba(249,115,22,0.8)', 'rgba(56,189,248,0.8)'],
                    borderColor: ['#34d399', '#f87171', '#f97316', '#38bdf8'], borderWidth: 2
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { labels: { color: '#cbd5e1' } }, title: { display: true, text: `Daily Report — ${data.date}`, color: '#f8fafc', font: { size: 16, weight: '500' } } }
            }
        });
    } catch (err) {
        console.error('Failed to generate daily report', err);
        document.getElementById('report-no-data').textContent = 'Error loading report data.';
        document.getElementById('report-no-data').style.display = 'block';
    }
}

// ═══════════════════════════════════════════════════════════════
// MODULE 3 — Monitor Service Usage
// ═══════════════════════════════════════════════════════════════

let monitorChartInstance = null;

async function loadServiceMonitoring() {
    const period = document.getElementById('monitor-period').value;
    document.getElementById('monitor-error').style.display = 'none';
    document.getElementById('busiest-card').style.display = 'none';

    try {
        const response = await fetch(`http://localhost:8081/api/admin/service-usage/period?period=${period}`);
        const data = await response.json();

        const services = data.services || [];
        const labels = services.map(s => s.serviceName);
        const generated = services.map(s => s.tokensGenerated);
        const served = services.map(s => s.tokensServed);

        // Show busiest service
        if (data.busiestService) {
            document.getElementById('busiest-name').textContent = data.busiestService;
            document.getElementById('busiest-count').textContent = `${data.busiestServiceTokens} tokens generated`;
            document.getElementById('busiest-card').style.display = 'block';
        }

        // Render chart
        const ctx = document.getElementById('monitorChart').getContext('2d');
        if (monitorChartInstance) monitorChartInstance.destroy();

        const periodLabels = { week: 'This Week', month: 'This Month', '3months': 'Last 3 Months', '6months': 'Last 6 Months', year: 'This Year' };
        monitorChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    { label: 'Generated', data: generated, backgroundColor: 'rgba(56,189,248,0.8)', borderColor: '#38bdf8', borderWidth: 1, borderRadius: 6 },
                    { label: 'Served', data: served, backgroundColor: 'rgba(52,211,153,0.8)', borderColor: '#34d399', borderWidth: 1, borderRadius: 6 }
                ]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: {
                    legend: { labels: { color: '#cbd5e1' } },
                    title: { display: true, text: `Service Usage — ${periodLabels[period] || period}`, color: '#f8fafc', font: { size: 16, weight: '500' } }
                },
                scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } }, x: { grid: { display: false }, ticks: { color: '#94a3b8' } } }
            }
        });
    } catch (err) {
        console.error('Failed to load service monitoring', err);
        document.getElementById('monitor-error').style.display = 'block';
    }
}

// ═══════════════════════════════════════════════════════════════
// MODULE 4 — Evaluate Staff Performance
// ═══════════════════════════════════════════════════════════════

async function loadStaffDropdown() {
    try {
        const response = await fetch('http://localhost:8081/api/admin/staff');
        const staffList = await response.json();
        const select = document.getElementById('perf-staff');
        select.innerHTML = '<option value="">-- Select Staff --</option>';
        staffList.forEach(s => {
            const info = parseRole(s.role);
            select.innerHTML += `<option value="${s.userId}">${s.fullName || s.username} (${info.label} — ${info.assignment})</option>`;
        });
    } catch (err) { console.error('Failed to load staff dropdown', err); }
}

async function loadStaffPerformance() {
    const staffId = document.getElementById('perf-staff').value;
    if (!staffId) { alert('Please select a staff member'); return; }

    document.getElementById('perf-no-data').style.display = 'none';
    document.getElementById('perf-results').style.display = 'none';

    try {
        const response = await fetch(`http://localhost:8081/api/admin/staff/${staffId}/performance`);
        const data = await response.json();

        if (data.message && (data.message.includes('Not enough data') || data.message.includes('not applicable'))) {
            document.getElementById('perf-no-data').textContent = data.message;
            document.getElementById('perf-no-data').style.display = 'block';
            return;
        }

        document.getElementById('perf-name').textContent = data.staffName + (data.serviceName ? ` — ${data.serviceName}` : '');
        document.getElementById('perf-served').textContent = data.tokensServed || 0;
        document.getElementById('perf-avg').textContent = data.avgServiceTime || 0;
        document.getElementById('perf-noshow').textContent = data.noShowCount || 0;
        document.getElementById('perf-results').style.display = 'block';
    } catch (err) {
        console.error('Failed to load staff performance', err);
        document.getElementById('perf-no-data').textContent = 'Error loading performance data.';
        document.getElementById('perf-no-data').style.display = 'block';
    }
}

// ═══════════════════════════════════════════════════════════════
// MODULE 6 — Financial Audit
// ═══════════════════════════════════════════════════════════════

async function generateFinancialReport() {
    const startDate = document.getElementById('fin-start').value;
    const endDate = document.getElementById('fin-end').value;

    document.getElementById('fin-no-data').style.display = 'none';
    document.getElementById('fin-results').style.display = 'none';
    document.getElementById('btn-export-pdf').style.display = 'none';

    let url = 'http://localhost:8081/api/admin/reports/financial?';
    if (startDate) url += `startDate=${startDate}&`;
    if (endDate) url += `endDate=${endDate}`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (!data.hasData) {
            document.getElementById('fin-no-data').style.display = 'block';
            return;
        }

        document.getElementById('fin-sold').textContent = data.totalSold;
        document.getElementById('fin-revenue').textContent = Number(data.totalRevenue).toLocaleString();

        const tbody = document.querySelector('#fin-table tbody');
        tbody.innerHTML = '';
        (data.transactions || []).forEach(t => {
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${t.serviceName}</td><td>${t.citizenName}</td><td style="color:#34d399;font-weight:600;">${t.amount}</td><td>${new Date(t.transactionTime).toLocaleString()}</td>`;
            tbody.appendChild(tr);
        });

        document.getElementById('fin-results').style.display = 'block';
        document.getElementById('btn-export-pdf').style.display = 'inline-flex';
    } catch (err) {
        console.error('Failed to generate financial report', err);
        document.getElementById('fin-no-data').textContent = 'Error loading financial data.';
        document.getElementById('fin-no-data').style.display = 'block';
    }
}

function exportFinancialPDF() {
    const content = document.getElementById('fin-results');
    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
        <html><head><title>WaitWise Financial Report</title>
        <style>
            body { font-family: 'Inter', Arial, sans-serif; padding: 2rem; color: #1e293b; }
            h1 { color: #0f172a; margin-bottom: 0.5rem; }
            .summary { display: flex; gap: 2rem; margin: 1.5rem 0; }
            .summary-card { background: #f1f5f9; border-radius: 12px; padding: 1.5rem; flex: 1; }
            .summary-card .label { font-size: 0.8rem; color: #64748b; text-transform: uppercase; }
            .summary-card .value { font-size: 2rem; font-weight: 700; color: #0f172a; }
            table { width: 100%; border-collapse: collapse; margin-top: 1.5rem; }
            th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; }
            th { background: #f8fafc; font-weight: 600; color: #475569; font-size: 0.8rem; text-transform: uppercase; }
            .footer { margin-top: 2rem; color: #94a3b8; font-size: 0.8rem; text-align: center; }
        </style></head><body>
        <h1>WaitWise Financial Report</h1>
        <p style="color:#64748b;">Generated: ${new Date().toLocaleString()}</p>
        <div class="summary">
            <div class="summary-card"><div class="label">Total Tickets Sold</div><div class="value">${document.getElementById('fin-sold').textContent}</div></div>
            <div class="summary-card"><div class="label">Total Revenue (PKR)</div><div class="value">${document.getElementById('fin-revenue').textContent}</div></div>
        </div>
        ${document.getElementById('fin-table-wrap').innerHTML}
        <div class="footer">WaitWise Admin Portal — Confidential Financial Report</div>
        </body></html>
    `);
    printWindow.document.close();
    setTimeout(() => { printWindow.print(); }, 500);
}
