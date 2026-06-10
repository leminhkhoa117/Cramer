const EXCEL_MIME_TYPE = 'application/vnd.ms-excel;charset=utf-8;';

const escapeHtml = (value) => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const normalizeFilename = (filename) => {
    const baseName = String(filename || 'cramer-export')
        .trim()
        .replace(/[\\/:*?"<>|]+/g, '-')
        .replace(/\s+/g, '_');
    return baseName || 'cramer-export';
};

export const downloadExcelTable = ({ rows, columns, filename, sheetName = 'Sheet1' }) => {
    const normalizedRows = Array.isArray(rows) ? rows : [];
    const normalizedColumns = Array.isArray(columns) && columns.length > 0
        ? columns
        : Object.keys(normalizedRows[0] || {}).map(key => ({ key, header: key }));

    const tableHead = normalizedColumns
        .map(column => `<th>${escapeHtml(column.header || column.key)}</th>`)
        .join('');
    const tableBody = normalizedRows
        .map(row => `<tr>${normalizedColumns
            .map(column => `<td>${escapeHtml(row[column.key])}</td>`)
            .join('')}</tr>`)
        .join('');

    const workbook = `<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<head>
  <meta charset="UTF-8">
  <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>${escapeHtml(sheetName)}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
  <style>
    table { border-collapse: collapse; }
    th { background: #8B5CF6; color: #fff; font-weight: 700; }
    th, td { border: 1px solid #d9d9d9; padding: 6px 8px; mso-number-format: "\\@"; }
  </style>
</head>
<body><table><thead><tr>${tableHead}</tr></thead><tbody>${tableBody}</tbody></table></body>
</html>`;

    const blob = new Blob(['\uFEFF' + workbook], { type: EXCEL_MIME_TYPE });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${normalizeFilename(filename)}.xls`;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
};