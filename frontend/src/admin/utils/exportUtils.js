/**
 * Export Utilities - Xuất dữ liệu ra file Excel
 * 
 * Re-export từ exportExcel.js và thêm các hàm mới cho Finance
 */
import * as XLSX from 'xlsx';

// Re-export các hàm đã có
export { exportUsersToExcel, exportTransactionsToExcel } from './exportExcel';

/**
 * Export finance report to Excel file (.xlsx)
 * @param {Array} data - Finance/transaction data from API
 */
export const exportFinanceReport = (data) => {
    if (!data || data.length === 0) {
        alert('Không có dữ liệu để xuất');
        return false;
    }

    // Define columns with Vietnamese headers
    const columns = [
        { key: 'orderCode', header: 'Mã đơn hàng', width: 20 },
        { key: 'username', header: 'Tên người dùng', width: 20 },
        { key: 'fullName', header: 'Họ tên', width: 25 },
        { key: 'product', header: 'Sản phẩm', width: 30 },
        { key: 'type', header: 'Loại', width: 15 },
        { key: 'amount', header: 'Số tiền (VNĐ)', width: 15 },
        { key: 'status', header: 'Trạng thái', width: 15 },
        { key: 'createdAt', header: 'Ngày tạo', width: 22 },
        { key: 'paidAt', header: 'Ngày thanh toán', width: 22 },
    ];

    // Format data
    const formattedData = data.map(row => ({
        orderCode: row['Mã đơn hàng'] || row.order_code || '',
        username: row['Tên người dùng'] || row.username || '',
        fullName: row['Họ tên'] || row.full_name || '',
        product: row['Sản phẩm'] || row.product_name || row.description || '',
        type: formatType(row['Loại'] || row.type),
        amount: row['Số tiền (VNĐ)'] || row.amount_vnd || row.amount || 0,
        status: formatPaymentStatus(row['Trạng thái'] || row.status),
        createdAt: formatDateTime(row['Ngày tạo'] || row.created_at),
        paidAt: formatDateTime(row['Ngày thanh toán'] || row.paid_at),
    }));

    try {
        // Create worksheet
        const ws = XLSX.utils.json_to_sheet(formattedData, {
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
        XLSX.utils.book_append_sheet(wb, ws, 'Báo cáo tài chính');

        // Generate filename with date
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0];
        const filename = `Cramer_Finance_Report_${dateStr}.xlsx`;

        // Download file
        XLSX.writeFile(wb, filename);

        return true;
    } catch (error) {
        console.error('Error exporting to Excel:', error);
        alert('Lỗi khi xuất file Excel: ' + error.message);
        return false;
    }
};

/**
 * Generic export to Excel
 */
export const exportToExcel = (data, filename, sheetName = 'Sheet1') => {
    if (!data || data.length === 0) {
        alert('Không có dữ liệu để xuất');
        return false;
    }

    try {
        const ws = XLSX.utils.json_to_sheet(data);
        
        // Auto-size columns
        const colWidths = Object.keys(data[0]).map(key => {
            let maxLen = key.length;
            data.forEach(row => {
                const value = row[key];
                if (value !== null && value !== undefined) {
                    const len = String(value).length;
                    if (len > maxLen) maxLen = len;
                }
            });
            return { wch: Math.min(maxLen + 2, 50) };
        });
        
        ws['!cols'] = colWidths;

        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, sheetName);

        const dateStr = new Date().toISOString().split('T')[0];
        XLSX.writeFile(wb, `${filename}_${dateStr}.xlsx`);

        return true;
    } catch (error) {
        console.error('Error exporting to Excel:', error);
        alert('Lỗi khi xuất file Excel: ' + error.message);
        return false;
    }
};

// Helper functions
function formatType(type) {
    switch (type?.toUpperCase()) {
        case 'SUBSCRIPTION':
            return 'Đăng ký';
        case 'LUA_PACK':
            return 'Gói Lúa';
        default:
            return type || '';
    }
}

function formatPaymentStatus(status) {
    switch (status?.toUpperCase()) {
        case 'PAID':
            return 'Đã thanh toán';
        case 'PENDING':
            return 'Chờ thanh toán';
        case 'CANCELLED':
            return 'Đã hủy';
        case 'REFUNDED':
            return 'Đã hoàn tiền';
        default:
            return status || '';
    }
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    try {
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return '';
        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return '';
    }
}

/**
 * Export data to CSV file
 */
export const exportToCsv = (data, filename) => {
    if (!data || data.length === 0) {
        alert('Không có dữ liệu để xuất');
        return false;
    }

    try {
        // Get headers from first row
        const headers = Object.keys(data[0]);
        
        // Create CSV content
        const csvContent = [
            // Header row
            headers.join(','),
            // Data rows
            ...data.map(row => 
                headers.map(header => {
                    let value = row[header];
                    // Handle special characters and commas
                    if (value === null || value === undefined) {
                        return '';
                    }
                    value = String(value);
                    // Escape quotes and wrap in quotes if contains comma
                    if (value.includes(',') || value.includes('"') || value.includes('\n')) {
                        value = '"' + value.replace(/"/g, '""') + '"';
                    }
                    return value;
                }).join(',')
            )
        ].join('\n');

        // Add BOM for UTF-8
        const BOM = '\uFEFF';
        const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
        
        // Download
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `${filename}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        return true;
    } catch (error) {
        console.error('Error exporting to CSV:', error);
        alert('Lỗi khi xuất file CSV: ' + error.message);
        return false;
    }
};

/**
 * Export data to PDF file
 * Note: This creates a simple HTML-to-PDF approach
 */
export const exportToPdf = (data, title, filename) => {
    if (!data || data.length === 0) {
        alert('Không có dữ liệu để xuất');
        return false;
    }

    try {
        // Get headers from first row
        const headers = Object.keys(data[0]);
        
        // Create HTML content
        let htmlContent = `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${title}</title>
                <style>
                    body { font-family: Arial, sans-serif; padding: 20px; }
                    h1 { color: #333; margin-bottom: 20px; }
                    table { width: 100%; border-collapse: collapse; font-size: 12px; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #8B5CF6; color: white; }
                    tr:nth-child(even) { background-color: #f9f9f9; }
                    .date { color: #666; font-size: 12px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <h1>${title}</h1>
                <p class="date">Ngày xuất: ${new Date().toLocaleString('vi-VN')}</p>
                <table>
                    <thead>
                        <tr>
                            ${headers.map(h => `<th>${h}</th>`).join('')}
                        </tr>
                    </thead>
                    <tbody>
                        ${data.map(row => `
                            <tr>
                                ${headers.map(h => `<td>${row[h] !== null && row[h] !== undefined ? row[h] : ''}</td>`).join('')}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </body>
            </html>
        `;

        // Open print dialog
        const printWindow = window.open('', '_blank');
        printWindow.document.write(htmlContent);
        printWindow.document.close();
        
        // Wait for content to load then print
        printWindow.onload = () => {
            printWindow.print();
            // Close window after printing
            printWindow.onafterprint = () => {
                printWindow.close();
            };
        };

        return true;
    } catch (error) {
        console.error('Error exporting to PDF:', error);
        alert('Lỗi khi xuất file PDF: ' + error.message);
        return false;
    }
};

export default {
    exportToExcel,
    exportFinanceReport,
    exportToCsv,
    exportToPdf
};
