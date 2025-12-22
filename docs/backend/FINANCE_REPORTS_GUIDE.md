# Hướng dẫn hoàn thiện Finance Reports

## Các thay đổi đã thực hiện

### 1. Frontend
- **ReportsPage.jsx**: Đã viết lại với đầy đủ logic:
  - Date range picker hoạt động
  - Granularity selector (ngày/tuần/tháng)
  - Nút Làm mới
  - Export CSV/Excel/PDF
  - 4 tab báo cáo đầy đủ

- **ReportsPage.css**: Thêm styling cho dropdown select

- **adminApi.js**: Thêm các API mới:
  - `getReports(dateFrom, dateTo, granularity)`
  - `getSubscriptionAnalysis(dateFrom, dateTo)`
  - `getLuaEconomy(dateFrom, dateTo)`
  - `getAcquisition(dateFrom, dateTo)`

- **exportUtils.js**: Thêm hàm `exportToCsv()` và `exportToPdf()`

### 2. Backend
- **AdminFinanceController.java**: Thêm 4 API endpoints mới:
  - `GET /api/admin/finance/reports`
  - `GET /api/admin/finance/reports/subscriptions`
  - `GET /api/admin/finance/reports/lua-economy`
  - `GET /api/admin/finance/reports/acquisition`

### 3. Database Migration
- **015_sample_data_for_reports.sql**: Script để thêm dữ liệu mẫu

---

## Các bước cần thực hiện

### Bước 1: Chạy Migration trong Supabase Dashboard

1. Mở Supabase Dashboard (https://supabase.com/dashboard)
2. Chọn project `jpocdgkrvohmjkejclpl`
3. Vào **SQL Editor**
4. Copy nội dung file `docs/backend/migrations/015_sample_data_for_reports.sql`
5. Paste và nhấn **Run**

### Bước 2: Restart Backend

```powershell
cd d:\Mon hoc Summer 25\Cramer\backend
.\gradlew bootRun
```

### Bước 3: Kiểm tra Frontend

```powershell
cd d:\Mon hoc Summer 25\Cramer\frontend
npm run dev
```

Mở browser và truy cập: `http://localhost:5173/admin/finance/reports`

---

## Kiểm tra dữ liệu trong Supabase

Sau khi chạy migration, bạn có thể kiểm tra bằng các query sau:

### Kiểm tra User Subscriptions
```sql
SELECT 
    us.status, 
    COUNT(*) as count,
    st.code as tier
FROM user_subscriptions us
JOIN subscription_tiers st ON us.tier_id = st.id
GROUP BY us.status, st.code;
```

### Kiểm tra Credit Transactions
```sql
SELECT 
    type, 
    category,
    COUNT(*) as count,
    SUM(ABS(amount)) as total_amount
FROM credit_transactions
GROUP BY type, category
ORDER BY type, category;
```

### Kiểm tra User Credits
```sql
SELECT 
    COUNT(*) as users_with_credits,
    AVG(balance) as avg_balance,
    SUM(balance) as total_balance
FROM user_credits;
```

---

## Lưu ý quan trọng

1. Backend APIs đã được update để xử lý các trường hợp thiếu dữ liệu - sẽ trả về fallback data thay vì lỗi 500

2. Các tab báo cáo sẽ hiển thị dữ liệu thật từ database nếu có, hoặc 0 nếu không có dữ liệu

3. Cohort data trong tab "Phân tích Subscriptions" vẫn là dữ liệu mẫu do cần logic phức tạp hơn để tính toán retention theo cohort
