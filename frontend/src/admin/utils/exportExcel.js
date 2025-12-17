/**
 * Export Utilities - Xuất file Excel với định dạng đẹp
 * 
 * Sử dụng SheetJS (xlsx) để tạo file Excel.
 */
import * as XLSX from 'xlsx';

/**
 * Export danh sách users ra file Excel
 * @param {Array} users - Mảng các user objects
 */
export function exportUsersToExcel(users) {
    if (!users || users.length === 0) {
        alert('Không có dữ liệu để xuất');
        return;
    }

    // Define columns with Vietnamese headers
    const columns = [
        { key: 'username', header: 'Username', width: 20 },
        { key: 'fullName', header: 'Họ và tên', width: 25 },
        { key: 'email', header: 'Email', width: 30 },
        { key: 'phoneNumber', header: 'Số điện thoại', width: 15 },
        { key: 'subscription', header: 'Gói đăng ký', width: 15 },
        { key: 'accountStatus', header: 'Trạng thái', width: 15 },
        { key: 'credits', header: 'Số Lúa', width: 12 },
        { key: 'lastLoginAt', header: 'Đăng nhập gần nhất', width: 20 },
        { key: 'createdAt', header: 'Ngày tạo', width: 20 },
    ];

    // Format data
    const data = users.map(user => ({
        username: user.username || '',
        fullName: user.fullName || '',
        email: user.email || '',
        phoneNumber: user.phoneNumber || '',
        subscription: formatSubscription(user.subscription),
        accountStatus: formatStatus(user.accountStatus),
        credits: user.credits || 0,
        lastLoginAt: formatDateTime(user.lastLoginAt),
        createdAt: formatDateTime(user.createdAt),
    }));

    // Create worksheet
    const ws = XLSX.utils.json_to_sheet(data, {
        header: columns.map(c => c.key)
    });

    // Style header - replace default headers with Vietnamese
    columns.forEach((col, index) => {
        const cellRef = XLSX.utils.encode_cell({ r: 0, c: index });
        if (ws[cellRef]) {
            ws[cellRef].v = col.header;
        }
    });

    // Set column widths
    ws['!cols'] = columns.map(col => ({ wch: col.width }));

    // Create workbook
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Danh sách người dùng');

    // Generate filename with date
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0];
    const filename = `Cramer_Users_${dateStr}.xlsx`;

    // Download file
    XLSX.writeFile(wb, filename);
}

/**
 * Format subscription for display
 */
function formatSubscription(subscription) {
    switch (subscription?.toUpperCase()) {
        case 'CRAMERICH':
            return 'Cramerich';
        case 'CRAMERIE':
        case 'FREE':
        default:
            return 'Cramerie (Free)';
    }
}

/**
 * Format status for display
 */
function formatStatus(status) {
    switch (status?.toUpperCase()) {
        case 'ACTIVE':
            return 'Hoạt động';
        case 'BANNED':
            return 'Bị cấm';
        case 'DEACTIVATED':
            return 'Ngừng hoạt động';
        case 'DELETED':
            return 'Đã xóa';
        default:
            return status || 'Hoạt động';
    }
}

/**
 * Format datetime for display
 */
function formatDateTime(dateString) {
    if (!dateString) return '-';
    try {
        const date = new Date(dateString);
        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return dateString;
    }
}

/**
 * Export danh sách transactions ra file Excel
 * @param {Array} transactions - Mảng các transaction objects
 */
export function exportTransactionsToExcel(transactions) {
    if (!transactions || transactions.length === 0) {
        alert('Không có dữ liệu để xuất');
        return;
    }

    const columns = [
        { key: 'id', header: 'Mã giao dịch', width: 15 },
        { key: 'username', header: 'Người dùng', width: 20 },
        { key: 'type', header: 'Loại', width: 15 },
        { key: 'amount', header: 'Số tiền', width: 15 },
        { key: 'status', header: 'Trạng thái', width: 12 },
        { key: 'createdAt', header: 'Ngày tạo', width: 20 },
    ];

    const data = transactions.map(tx => ({
        id: tx.id,
        username: tx.username || tx.user?.username || '',
        type: tx.type,
        amount: tx.amount,
        status: tx.status,
        createdAt: formatDateTime(tx.createdAt),
    }));

    const ws = XLSX.utils.json_to_sheet(data, {
        header: columns.map(c => c.key)
    });

    columns.forEach((col, index) => {
        const cellRef = XLSX.utils.encode_cell({ r: 0, c: index });
        if (ws[cellRef]) {
            ws[cellRef].v = col.header;
        }
    });

    ws['!cols'] = columns.map(col => ({ wch: col.width }));

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Giao dịch');

    const now = new Date();
    const dateStr = now.toISOString().split('T')[0];
    const filename = `Cramer_Transactions_${dateStr}.xlsx`;

    XLSX.writeFile(wb, filename);
}
