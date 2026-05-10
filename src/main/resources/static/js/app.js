const API_BASE_URL = 'http://localhost:8081/api';

// --- Utility Functions ---

function showLoader() {
    document.getElementById('globalLoader').classList.remove('hidden');
}

function hideLoader() {
    document.getElementById('globalLoader').classList.add('hidden');
}

function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if(el) {
        el.textContent = message;
        el.classList.remove('hidden');
    } else {
        alert(message);
    }
}

function clearError(elementId) {
    const el = document.getElementById(elementId);
    if(el) {
        el.textContent = '';
        el.classList.add('hidden');
    }
}

function saveUserSession(user) {
    localStorage.setItem('user', JSON.stringify(user));
    if (user) {
        // For citizens, username is null, so use cnic field
        const identifier = user.username || user.cnic;
        if (identifier) {
            sessionStorage.setItem('citizenCnic', identifier);
        }
    }
}

function getUserSession() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
}

function logout() {
    localStorage.removeItem('user');
    sessionStorage.removeItem('citizenCnic');
    window.location.href = 'index.html';
}

// --- Registration Logic ---
async function registerCitizen(event) {
    event.preventDefault();
    clearError('registerError');
    
    const firstName = document.getElementById('firstName').value.trim();
    const lastName = document.getElementById('lastName').value.trim();
    const cnicRaw = document.getElementById('cnic').value.trim();
    const phoneNumber = document.getElementById('phoneNumber').value.trim();
    const dateOfBirth = document.getElementById('dateOfBirth').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        showError('registerError', 'Passwords do not match.');
        return;
    }

    const cnic = cnicRaw.replace(/-/g, '');
    if (cnic.length !== 13) {
        showError('registerError', 'CNIC must be exactly 13 digits.');
        return;
    }

    if (phoneNumber.replace(/\D/g, '').length !== 11) {
        showError('registerError', 'Phone number must be exactly 11 digits.');
        return;
    }

    if (!dateOfBirth) {
        showError('registerError', 'Please select your Date of Birth.');
        return;
    }

    const dobYear = new Date(dateOfBirth).getFullYear();
    if (dobYear > 2015) {
        showError('registerError', 'Date of Birth must be 2015 or earlier.');
        return;
    }
    const email = document.getElementById('email').value.trim();
    const fullName = firstName + ' ' + lastName;

    showLoader();
    try {
        const payload = {
            cnic: cnic,
            password: password,
            fullName: fullName,
            phoneNumber: phoneNumber,
            dateOfBirth: dateOfBirth,
            email: email
        };

        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            // Try to parse the JSON error from Spring Boot
            let errorMsg = 'Registration failed. Please check your details.';
            try {
                const errorData = await response.json();
                if (errorData.message) {
                    errorMsg = errorData.message;
                }
            } catch (e) {
                // If it's not JSON, use the default generic message
            }
            throw new Error(errorMsg);
        }

        const data = await response.json();
        // Automatically log them in after registration
        saveUserSession(data);
        window.location.href = 'citizen-dashboard.html';
    } catch (err) {
        hideLoader();
        showError('registerError', err.message);
    }
}

// --- Login Logic ---
async function loginCitizen(event) {
    event.preventDefault();
    clearError('loginError');

    const cnicRaw = document.getElementById('cnic').value;
    const cnic = cnicRaw.replace(/-/g, '');
    const password = document.getElementById('password').value;

    showLoader();
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                username: cnic,
                password: password
            })
        });

        if (!response.ok) {
            throw new Error('Invalid CNIC or Password.');
        }

        const data = await response.json();
        
        if(data.role !== 'Citizen') {
            throw new Error('This portal is only for Citizens.');
        }

        saveUserSession(data);
        window.location.href = 'citizen-dashboard.html';
    } catch (err) {
        hideLoader();
        showError('loginError', err.message);
    }
}

// --- Dashboard Logic ---

async function loadDashboard() {
    const user = getUserSession();
    if (!user) {
        window.location.href = 'citizen-login.html';
        return;
    }

    document.getElementById('userNameDisplay').textContent = user.fullName;
    await fetchMyTokens(user.userId);
}

async function fetchMyTokens(userId) {
    try {
        const response = await fetch(`${API_BASE_URL}/citizen/my-tokens?citizenId=${userId}`);
        if (!response.ok) throw new Error('Failed to fetch tokens');
        
        const tokens = await response.json();
        renderTokenTable(tokens);
    } catch (err) {
        console.error('Error fetching tokens:', err);
    }
}

function renderTokenTable(tokens) {
    const tbody = document.getElementById('tokenTableBody');
    tbody.innerHTML = '';

    if(tokens.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="color: var(--text-secondary);">No tokens found. Issue one below!</td></tr>';
        return;
    }

    // Sort by issue time descending
    tokens.sort((a, b) => new Date(b.issueTime) - new Date(a.issueTime));

    tokens.forEach(token => {
        const date = new Date(token.issueTime).toLocaleString();
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${token.tokenNumber}</strong></td>
            <td>${token.service ? token.service.serviceName : 'Unknown'}</td>
            <td>${token.priorityType}</td>
            <td>${date}</td>
            <td><span class="badge ${token.status}">${token.status}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function togglePasswordVisibility(inputId, iconElement) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        iconElement.classList.remove('fa-eye');
        iconElement.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        iconElement.classList.remove('fa-eye-slash');
        iconElement.classList.add('fa-eye');
    }
}

function formatCNIC(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length > 13) value = value.substring(0, 13);
    if (value.length > 5) value = value.substring(0, 5) + '-' + value.substring(5);
    if (value.length > 13) value = value.substring(0, 13) + '-' + value.substring(13, 14);
    input.value = value;
}

function formatPhone(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length > 11) value = value.substring(0, 11);
    input.value = value;
}

// Global initialization for inputs
document.addEventListener('DOMContentLoaded', () => {
    const cnicInput = document.getElementById('cnic');
    const phoneInput = document.getElementById('phoneNumber');
    const dobInput = document.getElementById('dateOfBirth');

    if (cnicInput) {
        cnicInput.addEventListener('input', (e) => formatCNIC(e.target));
        cnicInput.addEventListener('keydown', (e) => {
            const digits = e.target.value.replace(/\D/g, '').length;
            // Allow control keys (backspace, delete, arrows, etc.)
            const isControlKey = e.key === 'Backspace' || e.key === 'Delete' || e.key === 'ArrowLeft' || e.key === 'ArrowRight' || e.key === 'Tab';
            if (digits >= 13 && !isControlKey && /^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });
    }

    if (phoneInput) {
        phoneInput.addEventListener('input', (e) => {
            let val = e.target.value.replace(/\D/g, '');
            if (val.length > 11) {
                val = val.substring(0, 11);
            }
            e.target.value = val;
        });

        phoneInput.addEventListener('keydown', (e) => {
            const digits = e.target.value.replace(/\D/g, '').length;
            const isControlKey = e.key === 'Backspace' || e.key === 'Delete' || e.key === 'ArrowLeft' || e.key === 'ArrowRight' || e.key === 'Tab';
            // If already 11 digits and trying to type another digit, block it
            if (digits >= 11 && !isControlKey && /^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });
    }

    if (dobInput) {
        dobInput.setAttribute('max', '2015-12-31');
        
        const validateDOB = (e) => {
            const val = e.target.value;
            if (!val) return;
            
            const date = new Date(val);
            if (date.getFullYear() > 2015) {
                e.target.value = ''; // Reset the input
                showError('registerError', 'Invalid Date: Date of Birth must be 2015 or earlier.');
                // Scroll to error if needed
                document.getElementById('registerError').scrollIntoView({ behavior: 'smooth', block: 'center' });
            } else {
                // Only clear if the current error IS about DOB
                const currentError = document.getElementById('registerError').textContent;
                if (currentError.includes('Date of Birth')) {
                    clearError('registerError');
                }
            }
        };

        dobInput.addEventListener('change', validateDOB);
        dobInput.addEventListener('blur', validateDOB);
    }
});

async function issueToken(event) {
    event.preventDefault();
    clearError('issueError');
    clearError('issueSuccess');

    const serviceId = document.getElementById('serviceSelect').value;
    const priority = document.getElementById('prioritySelect').value;
    const user = getUserSession();

    if (!user) {
        window.location.href = 'citizen-login.html';
        return;
    }

    showLoader();
    try {
        // user.username contains the CNIC because we set it during registration
        const response = await fetch(`${API_BASE_URL}/reception/issue-token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                cnic: user.username,
                serviceId: serviceId,
                priority: priority
            })
        });

        if (!response.ok) {
            throw new Error('Failed to issue token. Make sure your CNIC exists as a Citizen.');
        }

        const data = await response.json();
        hideLoader();
        
        const successEl = document.getElementById('issueSuccess');
        successEl.textContent = `Token ${data.tokenNumber} issued successfully!`;
        successEl.classList.remove('hidden');

        // Refresh table
        await fetchMyTokens(user.userId);
    } catch (err) {
        hideLoader();
        showError('issueError', err.message);
    }
}
