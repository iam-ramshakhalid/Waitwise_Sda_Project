let currentServiceId = null;
let currentServiceName = null;
let currentPriority = null;

document.addEventListener('DOMContentLoaded', () => {
    let cnic = sessionStorage.getItem('citizenCnic');
    
    if (!cnic) {
        const userStr = localStorage.getItem('user');
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                const identifier = user.username || user.cnic;
                if (user && identifier) {
                    cnic = identifier;
                    sessionStorage.setItem('citizenCnic', cnic);
                }
            } catch (e) {}
        }
    }

    if (!cnic) {
        window.location.href = 'citizen-login.html';
        return;
    }
    loadDashboardMetrics();
    loadHistory();
    setInterval(pollQueueStatus, 15000); // Poll every 15s
});

function switchTab(tabId) {
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    
    document.getElementById(`tab-${tabId}`).classList.add('active');
    event.currentTarget.classList.add('active');
    
    if (tabId === 'dashboard') loadDashboardMetrics();
    if (tabId === 'history') loadHistory();
    if (tabId === 'getToken') resetGetTokenFlow();
}

function resetGetTokenFlow() {
    document.getElementById('step-1-service').classList.remove('hidden');
    document.getElementById('step-2-priority').classList.add('hidden');
    document.getElementById('step-3-result').classList.add('hidden');
    currentServiceId = null;
    currentPriority = null;
}

function selectService(id, name) {
    currentServiceId = id;
    currentServiceName = name;
    document.getElementById('selectedServiceText').textContent = name;
    document.getElementById('step-1-service').classList.add('hidden');
    document.getElementById('step-2-priority').classList.remove('hidden');
}

function goBackToServices() {
    document.getElementById('step-1-service').classList.remove('hidden');
    document.getElementById('step-2-priority').classList.add('hidden');
}

function selectPriority(type) {
    currentPriority = type;
    if (type === 'Emergency') {
        document.getElementById('emergencyText').value = '';
        document.getElementById('emergencyError').classList.add('hidden');
        document.getElementById('emergencyModal').classList.remove('hidden');
    } else if (type === 'Golden') {
        document.getElementById('paymentModal').classList.remove('hidden');
    } else {
        generateToken();
    }
}

function closeModal(id) {
    document.getElementById(id).classList.add('hidden');
}

async function submitEmergency() {
    const text = document.getElementById('emergencyText').value.trim();
    if (!text) {
        document.getElementById('emergencyError').textContent = "Please describe your emergency.";
        document.getElementById('emergencyError').classList.remove('hidden');
        return;
    }
    closeModal('emergencyModal');
    generateToken(text);
}

function submitPayment() {
    closeModal('paymentModal');
    generateToken();
}

async function generateToken(emergencyDescription = '') {
    const cnic = sessionStorage.getItem('citizenCnic');
    try {
        const response = await fetch('http://localhost:8081/api/citizen/issue-token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                cnic: cnic,
                serviceId: currentServiceId,
                serviceName: currentServiceName,
                priorityRequest: currentPriority,
                emergencyDescription: emergencyDescription
            })
        });

        const data = await response.json();
        if (!response.ok) {
            alert(data.message || "Failed to generate token.");
            return;
        }

        renderDigitalToken(data);
    } catch (e) {
        alert("Error connecting to server.");
    }
}

function renderDigitalToken(data) {
    document.getElementById('step-2-priority').classList.add('hidden');
    document.getElementById('step-3-result').classList.remove('hidden');

    const token = data.token;
    document.getElementById('resTokenNumber').textContent = token.tokenNumber;
    document.getElementById('resService').textContent = token.service.serviceName;
    document.getElementById('resPosition').textContent = data.queuePosition;
    document.getElementById('resWaitTime').textContent = data.estimatedWaitTime + ' min';
    document.getElementById('resCitizenId').textContent = token.citizenCnic + ' | ' + token.citizenName;
    
    const badge = document.getElementById('resPriority');
    badge.textContent = token.priorityType.toUpperCase();
    badge.className = `badge ${token.priorityType.toLowerCase()}`;
    
    if (currentPriority === 'Emergency' && token.priorityType !== 'Emergency') {
        alert("Notice: Your emergency claim was not verified by the system. You have been assigned normal/senior priority based on your age.");
    }

    sessionStorage.setItem('activeTokenId', token.tokenId);
}

async function loadDashboardMetrics() {
    const cnic = sessionStorage.getItem('citizenCnic');
    try {
        const res = await fetch(`http://localhost:8081/api/citizen/dashboard-metrics?cnic=${cnic}`);
        const data = await res.json();
        
        document.getElementById('dashUserName').textContent = data.citizenName || 'Citizen';
        document.getElementById('metricActive').textContent = data.active;
        document.getElementById('metricServed').textContent = data.served;
        document.getElementById('metricCancelled').textContent = data.cancelled;

        const tbody = document.getElementById('recentTokenTableBody');
        tbody.innerHTML = '';
        
        let activeTokensHtml = '';
        let activeTokensCount = 0;
        let firstActiveTokenId = null;

        data.recentTokens.forEach(t => {
            const dateStr = new Date(t.issueTime).toLocaleString();
            let waitStr = t.status === 'Waiting' ? (t.estimatedWaitTime + ' min') : '-';
            
            tbody.innerHTML += `
                <tr>
                    <td style="font-weight: bold;">${t.tokenNumber}</td>
                    <td>${t.service.serviceName}</td>
                    <td><span class="badge ${t.priorityType.toLowerCase()}">${t.priorityType}</span></td>
                    <td style="color: #94A3B8;">${dateStr}</td>
                    <td>${waitStr}</td>
                    <td>${t.status}</td>
                </tr>
            `;

            if (t.status === 'Waiting' || t.status === 'Called') {
                activeTokensCount++;
                if (!firstActiveTokenId) {
                    firstActiveTokenId = t.tokenId;
                    sessionStorage.setItem('activeTokenId', t.tokenId); // Ensure background polling uses the first one
                }
                activeTokensHtml += `
                    <div class="digital-token" style="min-width: 300px; max-width: 320px; flex-shrink: 0; margin-right: 1.5rem; margin-bottom: 1rem;">
                        <div class="token-header">
                            <h2 style="font-size: 1.25rem;">Active Token</h2>
                            <span class="badge ${t.priorityType.toLowerCase()}">${t.priorityType.toUpperCase()}</span>
                        </div>
                        <div class="token-body" style="margin-bottom: 0.5rem;">
                            <h1 style="font-size: 3rem;">${t.tokenNumber}</h1>
                            <p style="font-size: 0.8rem;">${t.service.serviceName}</p>
                            <p style="font-size: 0.7rem; color: #60A5FA; margin-top: 0.25rem;">
                                <i class="fas fa-user" style="margin-right: 0.25rem;"></i>${t.citizenCnic || ''} | ${t.citizenName || ''}
                            </p>
                        </div>
                        <div class="token-footer">
                            <div class="stat">
                                <span class="label">STATUS</span>
                                <span class="value" style="font-size: 1rem;">${t.status}</span>
                            </div>
                            <div class="stat">
                                <span class="label">EST. WAIT</span>
                                <span class="value" style="font-size: 1rem;" id="resEstWaitSpan_${t.tokenId}">${waitStr}</span>
                            </div>
                            <div class="stat">
                                <span class="label">NOW SERVING</span>
                                <span class="value" style="color: #10B981; font-weight: bold; font-size: 1rem;" id="nowServingSpan_${t.tokenId}">Load...</span>
                            </div>
                        </div>
                        ${t.status === 'Waiting' ? `
                        <div style="margin-top: 1rem; text-align: center;">
                            <button onclick="confirmCancelToken(${t.tokenId}, '${t.tokenNumber}')" class="btn-cancel-token" style="
                                background: rgba(239, 68, 68, 0.15);
                                color: #fca5a5;
                                border: 1px solid rgba(239, 68, 68, 0.3);
                                padding: 0.5rem 1.25rem;
                                border-radius: 8px;
                                cursor: pointer;
                                font-size: 0.85rem;
                                font-weight: 600;
                                transition: all 0.2s ease;
                            " onmouseover="this.style.background='#ef4444';this.style.color='white'" onmouseout="this.style.background='rgba(239,68,68,0.15)';this.style.color='#fca5a5'">
                                <i class="fas fa-times-circle" style="margin-right: 0.3rem;"></i>Cancel Token
                            </button>
                        </div>` : ''}
                    </div>
                `;
            }
        });

        const activeContainer = document.getElementById('dashboardActiveTokenContainer');
        if (activeTokensCount > 0) {
            activeContainer.innerHTML = activeTokensHtml;
            activeContainer.classList.remove('hidden');
            activeContainer.style.display = 'flex';
            activeContainer.style.flexDirection = 'row';
            activeContainer.style.alignItems = 'stretch';
            activeContainer.style.justifyContent = 'flex-start';
            activeContainer.style.overflowX = 'auto';
            activeContainer.style.paddingBottom = '1rem';
            activeContainer.style.gap = '1rem';
            
            // Trigger poll immediately for all to populate "now serving"
            setTimeout(pollQueueStatus, 500);
        } else {
            activeContainer.innerHTML = '';
            activeContainer.classList.add('hidden');
        }
        
    } catch (e) {}
}

async function loadHistory() {
    const cnic = sessionStorage.getItem('citizenCnic');
    try {
        const res = await fetch(`http://localhost:8081/api/citizen/token-history?cnic=${cnic}`);
        window.historyData = await res.json();
        filterHistory();
    } catch (e) {}
}

function filterHistory() {
    const filter = document.getElementById('statusFilter').value;
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '';
    
    if (!window.historyData) return;
    
    window.historyData.forEach(t => {
        if (filter !== 'ALL' && t.status !== filter) return;
        
        const dateStr = new Date(t.issueTime).toLocaleString();
        let waitStr = t.status === 'Waiting' ? (t.estimatedWaitTime + ' min') : '-';

        // Show cancel button for Waiting tokens
        let actionCol = '';
        if (t.status === 'Waiting') {
            actionCol = `<button onclick="confirmCancelToken(${t.tokenId}, '${t.tokenNumber}')" style="
                background: rgba(239, 68, 68, 0.15);
                color: #fca5a5;
                border: 1px solid rgba(239, 68, 68, 0.3);
                padding: 0.35rem 0.75rem;
                border-radius: 6px;
                cursor: pointer;
                font-size: 0.75rem;
                font-weight: 600;
                transition: all 0.2s ease;
            " onmouseover="this.style.background='#ef4444';this.style.color='white'" onmouseout="this.style.background='rgba(239,68,68,0.15)';this.style.color='#fca5a5'">
                Cancel
            </button>`;
        }

        tbody.innerHTML += `
            <tr>
                <td style="font-weight: bold; color: #C8A85C;">${t.tokenNumber}</td>
                <td>${t.service.serviceName}</td>
                <td><span class="badge ${t.priorityType.toLowerCase()}">${t.priorityType}</span></td>
                <td style="color: #94A3B8;">${dateStr}</td>
                <td>${waitStr}</td>
                <td>${t.status}</td>
                <td style="font-size: 0.7rem; color: #60A5FA;">${t.citizenCnic || ''}</td>
                <td>${actionCol}</td>
            </tr>
        `;
    });
}

// --- Token Cancellation ---

function confirmCancelToken(tokenId, tokenNumber) {
    document.getElementById('cancelTokenNumber').textContent = tokenNumber;
    document.getElementById('cancelTokenId').value = tokenId;
    document.getElementById('cancelModal').classList.remove('hidden');
}

function closeCancelModal() {
    document.getElementById('cancelModal').classList.add('hidden');
}

async function executeCancelToken() {
    const tokenId = document.getElementById('cancelTokenId').value;
    const cnic = sessionStorage.getItem('citizenCnic');
    closeCancelModal();

    try {
        const response = await fetch('http://localhost:8081/api/citizen/cancel-token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                tokenId: tokenId,
                cnic: cnic
            })
        });

        const data = await response.json();
        if (!response.ok) {
            alert(data.message || "Failed to cancel token.");
            return;
        }

        // Show success toast
        showToast(data.message || "Token cancelled successfully!");
        
        // Refresh dashboard and history
        loadDashboardMetrics();
        loadHistory();
    } catch (e) {
        alert("Error connecting to server.");
    }
}

let toastShown = false;
let turnUpModalShown = new Set();

async function pollQueueStatus() {
    // If we have historyData, we can poll for ALL active tokens in the dashboard.
    // We fetch status using the primary active token ID, or fetch individually.
    // For simplicity, we just use the first active token ID for toast, but let's update spans by ID if they exist.
    
    // Iterate over tokens visible in dashboard
    const spans = document.querySelectorAll('[id^="resEstWaitSpan_"]');
    
    for (let span of spans) {
        let tid = span.id.split('_')[1];
        if (tid) {
            try {
                const res = await fetch(`http://localhost:8081/api/citizen/queue-status?tokenId=${tid}`);
                if (!res.ok) continue;
                const data = await res.json();
                
                const waitEl = document.getElementById(`resEstWaitSpan_${tid}`);
                if (waitEl) waitEl.textContent = data.estimatedWaitTime + ' min';
                
                const servingEl = document.getElementById(`nowServingSpan_${tid}`);
                if (servingEl) servingEl.textContent = data.currentlyServing;

                // Toast logic for the first token or any token <= 3 position
                if (data.position <= 3 && data.position > 0 && !toastShown) {
                    showToast(`Only ${data.position} people remain ahead of you. Estimated wait: ${data.estimatedWaitTime} min.`);
                    toastShown = true;
                }
                
                // Turn up logic
                if (data.status === 'Called' && !turnUpModalShown.has(tid)) {
                    document.getElementById('turnUpCounterId').textContent = data.serviceId || 'your service';
                    document.getElementById('turnUpModal').classList.remove('hidden');
                    turnUpModalShown.add(tid);
                }
            } catch(e) {}
        }
    }
    
    // Also update the single result screen if user just generated a token
    const primaryId = sessionStorage.getItem('activeTokenId');
    if (primaryId && document.getElementById('resPosition')) {
        try {
            const res = await fetch(`http://localhost:8081/api/citizen/queue-status?tokenId=${primaryId}`);
            if (res.ok) {
                const data = await res.json();
                document.getElementById('resPosition').textContent = data.position;
                document.getElementById('resWaitTime').textContent = data.estimatedWaitTime + ' min';
            }
        } catch(e) {}
    }
}

function showToast(message) {
    const toast = document.getElementById('toast');
    if (message) {
        document.getElementById('toastMessage').textContent = message;
    }
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 5000);
}

function closeTurnUpModal() {
    document.getElementById('turnUpModal').classList.add('hidden');
}

function logout() {
    sessionStorage.clear();
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}
