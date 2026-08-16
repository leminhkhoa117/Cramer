
/**
 * InButtonSpinner — Hiển thị spinner trong nút khi đang tải.
 *
 * Dùng chung cho mọi nút cần loading state: Resume, Save, Submit, Pay, v.v.
 *
 * @param {string} label - Text hiển thị kế bên spinner (mặc định: "Đang tải...")
 * @param {'light'|'dark'} variant - Màu spinner: 'light' cho nút primary, 'dark' cho nút secondary (mặc định: 'light')
 */
const InButtonSpinner = ({ label = 'Đang tải...', variant = 'light' }) => {
  const spinnerClass = variant === 'dark' ? 'cm-loading cm-loading--dark' : 'cm-loading';
  return <span className={spinnerClass}>{label}</span>;
};

export default InButtonSpinner;
