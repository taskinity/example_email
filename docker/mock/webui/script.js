// Taskinity Email Processing Dashboard
// This script provides the interactive functionality for the dashboard

document.addEventListener('DOMContentLoaded', function() {
    // Initialize the dashboard
    initializeDashboard();
    
    // Set up event listeners
    document.getElementById('refresh-btn').addEventListener('click', refreshData);
    document.getElementById('process-btn').addEventListener('click', processEmails);
    document.getElementById('test-btn').addEventListener('click', sendTestEmail);
    
    // Refresh data every 30 seconds
    setInterval(refreshData, 30000);
});

// Initialize the dashboard with mock data
function initializeDashboard() {
    // Update stats
    updateStats();
    
    // Initialize charts
    initializeCharts();
    
    // Populate recent emails table
    populateRecentEmails();
    
    // Update last update time
    updateLastUpdateTime();
}

// Update the statistics counters
function updateStats() {
    // In a real application, these would be fetched from an API
    const totalEmails = Math.floor(Math.random() * 20) + 10;
    const processedEmails = Math.floor(totalEmails * 0.7);
    const pendingEmails = Math.floor(totalEmails * 0.2);
    const errorEmails = totalEmails - processedEmails - pendingEmails;
    
    // Update the DOM
    document.getElementById('total-emails').textContent = totalEmails;
    document.getElementById('processed-emails').textContent = processedEmails;
    document.getElementById('pending-emails').textContent = pendingEmails;
    document.getElementById('error-emails').textContent = errorEmails;
}

// Initialize the charts
function initializeCharts() {
    // Categories chart
    const categoriesCtx = document.getElementById('categories-chart').getContext('2d');
    const categoriesChart = new Chart(categoriesCtx, {
        type: 'pie',
        data: {
            labels: ['Urgent', 'With Attachments', 'Support', 'Orders', 'Regular'],
            datasets: [{
                data: [15, 25, 20, 10, 30],
                backgroundColor: [
                    '#dc3545', // Urgent - red
                    '#fd7e14', // Attachments - orange
                    '#0dcaf0', // Support - cyan
                    '#6f42c1', // Orders - purple
                    '#20c997'  // Regular - teal
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'right',
                }
            }
        }
    });
    
    // Processing time chart
    const timeCtx = document.getElementById('time-chart').getContext('2d');
    const timeChart = new Chart(timeCtx, {
        type: 'bar',
        data: {
            labels: ['Urgent', 'With Attachments', 'Support', 'Orders', 'Regular'],
            datasets: [{
                label: 'Avg. Processing Time (ms)',
                data: [120, 350, 180, 150, 100],
                backgroundColor: [
                    '#dc3545', // Urgent - red
                    '#fd7e14', // Attachments - orange
                    '#0dcaf0', // Support - cyan
                    '#6f42c1', // Orders - purple
                    '#20c997'  // Regular - teal
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

// Populate the recent emails table
function populateRecentEmails() {
    const emailsTableBody = document.getElementById('recent-emails');
    emailsTableBody.innerHTML = ''; // Clear existing rows
    
    // Sample email data - in a real app, this would come from an API
    const sampleEmails = [
        {
            time: '12:45 PM',
            from: 'customer@example.com',
            subject: 'Urgent: Support Request #12345',
            category: 'Urgent',
            status: 'Processed'
        },
        {
            time: '12:30 PM',
            from: 'reports@example.com',
            subject: 'Monthly Report - May 2025',
            category: 'With Attachments',
            status: 'Processed'
        },
        {
            time: '12:15 PM',
            from: 'customer@example.com',
            subject: 'Question about my order #54321',
            category: 'Orders',
            status: 'Processed'
        },
        {
            time: '12:00 PM',
            from: 'newsletter@example.com',
            subject: 'Weekly Newsletter',
            category: 'Regular',
            status: 'Pending'
        },
        {
            time: '11:45 AM',
            from: 'support@example.com',
            subject: 'Re: Technical Issue',
            category: 'Support',
            status: 'Error'
        }
    ];
    
    // Create table rows
    sampleEmails.forEach(email => {
        const row = document.createElement('tr');
        
        // Determine status badge class
        let statusBadgeClass = 'bg-secondary';
        if (email.status === 'Processed') {
            statusBadgeClass = 'bg-success';
        } else if (email.status === 'Pending') {
            statusBadgeClass = 'bg-warning text-dark';
        } else if (email.status === 'Error') {
            statusBadgeClass = 'bg-danger';
        }
        
        // Determine category badge class
        let categoryBadgeClass = 'bg-secondary';
        if (email.category === 'Urgent') {
            categoryBadgeClass = 'bg-danger';
        } else if (email.category === 'With Attachments') {
            categoryBadgeClass = 'bg-warning text-dark';
        } else if (email.category === 'Support') {
            categoryBadgeClass = 'bg-info text-dark';
        } else if (email.category === 'Orders') {
            categoryBadgeClass = 'bg-primary';
        } else if (email.category === 'Regular') {
            categoryBadgeClass = 'bg-success';
        }
        
        row.innerHTML = `
            <td>${email.time}</td>
            <td>${email.from}</td>
            <td>${email.subject}</td>
            <td><span class="badge ${categoryBadgeClass}">${email.category}</span></td>
            <td><span class="badge ${statusBadgeClass}">${email.status}</span></td>
            <td>
                <button class="btn btn-sm btn-outline-primary">View</button>
                <button class="btn btn-sm btn-outline-secondary">Reprocess</button>
            </td>
        `;
        
        emailsTableBody.appendChild(row);
    });
}

// Update the last update time
function updateLastUpdateTime() {
    const now = new Date();
    const timeString = now.toLocaleTimeString();
    document.getElementById('last-update').textContent = timeString;
}

// Refresh all data
function refreshData() {
    updateStats();
    populateRecentEmails();
    updateLastUpdateTime();
    
    // Show a toast notification
    showToast('Data refreshed successfully');
}

// Process emails
function processEmails() {
    // In a real app, this would trigger the email processing flow
    showToast('Email processing started');
    
    // Simulate processing delay
    setTimeout(() => {
        updateStats();
        populateRecentEmails();
        updateLastUpdateTime();
        showToast('Email processing completed');
    }, 2000);
}

// Send a test email
function sendTestEmail() {
    // In a real app, this would send a test email via API
    showToast('Sending test email...');
    
    // Simulate sending delay
    setTimeout(() => {
        showToast('Test email sent successfully');
        
        // Open MailHog UI in a new tab
        window.open('http://localhost:8025', '_blank');
    }, 1500);
}

// Show a toast notification
function showToast(message) {
    // Create toast container if it doesn't exist
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }
    
    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header">
                <strong class="me-auto">Taskinity</strong>
                <small>Just now</small>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;
    
    // Add toast to container
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    
    // Initialize and show the toast
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement);
    toast.show();
    
    // Remove toast after it's hidden
    toastElement.addEventListener('hidden.bs.toast', function() {
        toastElement.remove();
    });
}
