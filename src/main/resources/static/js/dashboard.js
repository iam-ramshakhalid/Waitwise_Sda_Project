let currentServiceId = null;
let currentServiceName = null;
let currentPriority = null;
let activeServiceIds = new Set(); // To track services already in use

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
    if (activeServiceIds.has(id)) {
        const limitMsg = document.getElementById('limitModalMessage');
        const limitModal = document.getElementById('limitModal');
        limitMsg.textContent = `You already have an active token for ${name}. You can only have one active token per service at a time.`;
        limitModal.classList.remove('hidden');
        return;
    }
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
    const errorEl = document.getElementById('emergencyError');
    
    if (!text) {
        errorEl.textContent = "Please describe your emergency.";
        errorEl.classList.remove('hidden');
        return;
    }
    
    errorEl.classList.add('hidden');
    // Wait for verification
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
            if (currentPriority === 'Emergency') {
                const errorEl = document.getElementById('emergencyError');
                errorEl.textContent = data.message || "Verification failed.";
                errorEl.classList.remove('hidden');
            } else {
                const limitMsg = document.getElementById('limitModalMessage');
                const limitModal = document.getElementById('limitModal');
                limitMsg.textContent = data.message || "Failed to generate token.";
                limitModal.classList.remove('hidden');
            }
            return;
        }

        if (currentPriority === 'Emergency') {
            closeModal('emergencyModal');
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
        
        activeServiceIds.clear(); // Clear before repopulating

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
                if (t.service && t.service.serviceId) {
                    activeServiceIds.add(t.service.serviceId);
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

// Token Cancellation

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
    // Update active spans
    
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

// Payment logic
let selectedPaymentMethod = 'card';
let selectedWallet = 'easypaisa';

function switchPaymentTab(method) {
    selectedPaymentMethod = method;
    const cardTab = document.getElementById('pay-tab-card');
    const walletTab = document.getElementById('pay-tab-wallet');
    const cardForm = document.getElementById('method-card');
    const walletForm = document.getElementById('method-wallet');
    const verifyingText = document.getElementById('verifying-text');

    if (method === 'card') {
        cardTab.style.background = '#3B82F6';
        cardTab.style.color = 'white';
        walletTab.style.background = 'transparent';
        walletTab.style.color = '#94A3B8';
        cardForm.classList.remove('hidden');
        walletForm.classList.add('hidden');
        verifyingText.textContent = 'Bank';
    } else {
        walletTab.style.background = '#10B981';
        walletTab.style.color = 'white';
        cardTab.style.background = 'transparent';
        cardTab.style.color = '#94A3B8';
        walletForm.classList.remove('hidden');
        cardForm.classList.add('hidden');
        verifyingText.textContent = selectedWallet.charAt(0).toUpperCase() + selectedWallet.slice(1);
    }
}

function selectPayWallet(wallet, el) {
    selectedWallet = wallet;
    document.querySelectorAll('.wallet-sel-opt').forEach(opt => {
        opt.classList.remove('active');
        opt.style.background = 'rgba(255,255,255,0.05)';
        opt.style.border = '2px solid transparent';
    });
    
    el.classList.add('active');
    const colors = { 
        easypaisa: '#10B981', 
        jazzcash: '#EF4444', 
        nayapay: '#3B82F6',
        sadapay: '#FF8066' 
    };
    const bgColors = {
        easypaisa: '16, 185, 129',
        jazzcash: '239, 68, 68',
        nayapay: '59, 130, 246',
        sadapay: '255, 128, 102'
    };
    
    el.style.background = `rgba(${bgColors[wallet]}, 0.1)`;
    el.style.border = `2px solid ${colors[wallet]}`;
    
    document.getElementById('verifying-text').textContent = wallet.charAt(0).toUpperCase() + wallet.slice(1);
}

async function executePayment() {
    const btn = document.getElementById('btnFinalPay');
    const processing = document.getElementById('pay-processing');
    const errorEl = document.getElementById('pay-error');
    
    // Clear previous errors
    errorEl.classList.add('hidden');
    errorEl.textContent = '';

    // Validation
    if (selectedPaymentMethod === 'card') {
        const cardNumber = document.getElementById('pay-card-number').value.replace(/\D/g, '');
        const expiry = document.getElementById('pay-expiry').value.trim();
        const cvv = document.getElementById('pay-cvv').value.replace(/\D/g, '');

        if (cardNumber.length !== 16) {
            return showError('Card number must be exactly 16 digits.');
        }

        // Expiry Validation (MM/YY)
        if (!/^\d{2}\/\d{2}$/.test(expiry)) {
            return showError('Expiry must be in MM/YY format.');
        }
        
        const [month, year] = expiry.split('/').map(n => parseInt(n));
        const now = new Date();
        const currentYear = parseInt(now.getFullYear().toString().slice(-2));
        const currentMonth = now.getMonth() + 1;

        if (month < 1 || month > 12) {
            return showError('Invalid month in expiry date.');
        }
        if (year < currentYear || (year === currentYear && month < currentMonth)) {
            return showError('Invalid Expiry: Date cannot be in the past.');
        }

        if (cvv.length !== 3) {
            return showError('CVV must be 3 digits.');
        }
    } else {
        const walletNumber = document.getElementById('pay-wallet-number').value.replace(/\D/g, '');
        if (walletNumber.length !== 10) {
            return showError('Mobile number must be 10 digits.');
        }
    }

    function showError(msg) {
        errorEl.textContent = msg;
        errorEl.classList.remove('hidden');
        return;
    }

    // Simulation
    btn.disabled = true;
    btn.style.opacity = '0.5';
    processing.classList.remove('hidden');
    
    setTimeout(() => {
        processing.innerHTML = '<i class="fas fa-check-circle"></i> Payment Successful!';
        processing.style.background = 'rgba(16, 185, 129, 0.1)';
        processing.style.color = '#10B981';
        processing.style.borderColor = 'rgba(16, 185, 129, 0.2)';
        
        setTimeout(() => {
            closeModal('paymentModal');
            // Restore button for next time
            btn.disabled = false;
            btn.style.opacity = '1';
            processing.classList.add('hidden');
            processing.innerHTML = '<i class="fas fa-spinner fa-spin" style="margin-right: 0.5rem;"></i> Verifying with <span id="verifying-text">Bank</span>...';
            
            generateToken();
        }, 1500);
    }, 2500);
}
