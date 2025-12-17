# Tech Specs: Hệ thống Quản trị Cramer CMS

> **Phiên bản:** 1.0.0  
> **Tác giả:** Quốc Hữu - Project Manager :) 

---

## Mục lục

1. [Tổng quan Dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc Hệ thống](#2-kiến-trúc-hệ-thống)
3. [Xác thực và Phân quyền](#3-xác-thực-và-phân-quyền)
4. [Module 1: Quản lý Người dùng](#4-module-1-quản-lý-người-dùng)
5. [Module 2: Quản lý Tài chính](#5-module-2-quản-lý-tài-chính)
6. [Module 3: Quản lý Nội dung Đề thi](#6-module-3-quản-lý-nội-dung-đề-thi)
7. [Các Thành phần Dùng chung](#7-các-thành-phần-dùng-chung)
8. [Tham chiếu Database Schema](#8-tham-chiếu-database-schema)
9. [Danh sách API Endpoints](#9-danh-sách-api-endpoints)
10. [Hướng dẫn UI/UX](#10-hướng-dẫn-uiux)
11. [Các Lưu ý Bảo mật](#11-các-lưu-ý-bảo-mật)
12. [Lộ trình Triển khai](#12-lộ-trình-triển-khai)

---

## 1. Tổng quan Dự án

### 1.1 Bối cảnh

Hiện tại, việc quản trị hệ thống Cramer đang gặp một số hạn chế:

**Vấn đề 1: Phụ thuộc vào Supabase Dashboard**
- Các thao tác quản trị (xem danh sách user, chỉnh sửa subscription, thêm đề thi) đều phải thực hiện trực tiếp trên Supabase Table Editor và yêu cầu kiến thức SQL để thực hiện các truy vấn phức tạp, và không có giao diện thân thiện cho người quản trị không chuyên kỹ thuật (yeah, it's me).

**Vấn đề 2: Thiếu Audit Trail (Nhật ký Kiểm toán)**
- Không theo dõi được ai đã thay đổi gì, khi nào, gây khó khăn trong việc rollback khi có lỗi xảy ra, không đáp ứng yêu cầu compliance (tuân thủ) nếu mở rộng quy mô (yeah, lại là luật).

**Vấn đề 3: Quy trình Nhập liệu Thủ công**
- Nhập đề thi mới phải viết SQL script khiến dễ xảy ra lỗi cú pháp JSON trong `question_content`, và quan trọng nhất là không có preview để kiểm tra trước khi lưu.

### 1.2 Giải pháp: Cramer CMS Admin

Cramer CMS Admin là một **hệ thống quản trị web-based** được tích hợp trực tiếp vào ứng dụng Cramer hiện tại. Thay vì tạo một ứng dụng riêng biệt, hệ thống này:

- **Chạy trên cùng domain** với ứng dụng chính (ví dụ: `cramer.edu.vn/admin`, test local thì là `localhost:3000/admin`).
- **Sử dụng chung cơ sở hạ tầng** với frontend và backend hiện tại.
- **Tự động chuyển đổi giao diện** khi phát hiện admin đăng nhập (chỉ có 2 tài khoản được cho phép đăng nhập duy nhất).

### 1.3 Đối tượng Sử dụng

Hệ thống được thiết kế cho **2 tài khoản admin cố định**.

### 1.4 Ba Module Chính

Hệ thống được chia thành 3 module với các vai trò rõ ràng:

#### Module 1: Quản lý Người dùng
**Mục đích:** Quản lý toàn bộ thông tin và trạng thái của người dùng Cramer

**Các chức năng chính:**
- Xem danh sách tất cả người dùng với bộ lọc linh hoạt
- Chỉnh sửa thông tin cá nhân (username, họ tên, số điện thoại...)
- Thay đổi gói đăng ký (Cramerie ↔ Cramerich)
- Quản lý số dư Lúa (thêm/trừ credits)
- Xử lý trạng thái tài khoản (ban, deactivate, delete)
- Thao tác hàng loạt cho nhiều users cùng lúc

#### Module 2: Quản lý Tài chính
**Mục đích:** Theo dõi doanh thu và các giao dịch tài chính của hệ thống

**Các chức năng chính:**
- Dashboard tổng quan doanh thu theo thời gian thực.
- Lịch sử giao dịch chi tiết đến từng transaction.
- Báo cáo tài chính theo ngày/tuần/tháng/năm.
- Phân tích doanh thu theo gói đăng ký và Lúa.
- Export báo cáo ở nhiều định dạng (CSV, Excel, PDF).

#### Module 3: Quản lý Nội dung Đề thi
**Mục đích:** Thêm, sửa, xóa nội dung đề thi IELTS cho 4 kỹ năng

**Các chức năng chính:**
- Quản lý phân cấp: Topic → Test → Skill → Part → Question.
- Editor trực quan cho từng loại câu hỏi.
- Import câu hỏi từ file CSV hoặc JSON.
- Preview đề thi trước khi publish.
- Quản lý trạng thái: Draft → Published → Archived.

---

## 2. Kiến trúc Hệ thống

### 2.1 Tổng quan Kiến trúc

Cramer CMS không phải là một ứng dụng riêng biệt mà là một **phần mở rộng** của ứng dụng Cramer hiện tại. Điều này có nghĩa là:

**Về mặt Frontend:**
- Các trang admin nằm trong folder riêng (`/src/admin/`).
- Sử dụng chung React Router với route prefix `/admin/*`.
- Chia sẻ các component cơ bản (Button, Modal, Input) với app chính.
- Có stylesheet riêng để phân biệt với giao diện user.

**Về mặt Backend:**
- Các endpoint admin nằm trong package riêng (`/controller/admin/`).
- Có filter riêng để kiểm tra quyền admin trước khi xử lý request.
- Tái sử dụng các service và repository hiện có.
- Thêm service mới cho các tác vụ quản trị đặc thù.

**Về mặt Database:**
- Sử dụng chung database PostgreSQL trên Supabase.
- Thêm một số bảng mới cho audit logging và content status.
- Thêm một số cột vào bảng `profiles` để quản lý trạng thái account.

### 2.2 Luồng Xử lý Request

Khi admin truy cập hệ thống:

```
1. User đăng nhập bình thường qua Supabase Auth
2. Frontend kiểm tra user ID có nằm trong danh sách admin không
3. Nếu CÓ → Redirect đến /admin/dashboard
4. Nếu KHÔNG → Hiển thị giao diện user bình thường
5. Mỗi API call đến /api/admin/* sẽ qua AdminAuthFilter
6. Filter kiểm tra JWT token → lấy user ID → check admin list
7. Nếu PASS → Tiếp tục xử lý request
8. Nếu FAIL → Trả về 403 Forbidden
```
> Này tui không rõ về kĩ thuật mà nhờ AI, nên có gì ông có thể thay đổi tùy ý nha.

### 2.3 Cấu trúc Thư mục

Dưới đây là cấu trúc thư mục được đề xuất cho phần admin:

**Frontend (`frontend/src/admin/`):**
```
admin/
├── components/          # Các component chỉ dùng cho admin
│   ├── AdminLayout.jsx      # Layout chung (sidebar + header + content)
│   ├── AdminSidebar.jsx     # Menu điều hướng bên trái
│   ├── DataTable/           # Component bảng dữ liệu tái sử dụng
│   ├── Charts/              # Các biểu đồ (doanh thu, user growth...)
│   └── Modals/              # Các modal (import, batch action...)
│
├── pages/               # Các trang admin
│   ├── DashboardPage.jsx    # Trang tổng quan
│   ├── users/               # Module quản lý user
│   ├── finance/             # Module quản lý tài chính
│   └── content/             # Module quản lý đề thi
│
├── stores/              # Zustand stores riêng cho admin
│   └── useAdminStore.js
│
├── api/                 # API client riêng cho admin
│   └── adminApi.js
│
└── css/                 # Stylesheet riêng cho admin
    └── admin.css
```

**Backend (`backend/src/main/java/com/cramer/controller/admin/`):**
```
admin/
├── AdminUserController.java       # API quản lý user
├── AdminFinanceController.java    # API quản lý tài chính
└── AdminContentController.java    # API quản lý đề thi
```

---

## 3. Xác thực và Phân quyền

### 3.1 Cơ chế Xác định Admin

Thay vì sử dụng hệ thống roles phức tạp với database, chúng ta áp dụng phương pháp **hardcoded admin IDs** vì:

**Lý do chọn cách tiếp cận này:**
1. **Đơn giản:** Chỉ có 2 admin, không cần hệ thống phân quyền phức tạp.
2. **Bảo mật:** Admin ID không thể bị thay đổi qua UI, phải sửa code.
3. **Hiệu suất:** Không cần query database để kiểm tra quyền.

**Cách hoạt động:**
- Danh sách admin user ID được lưu trong biến môi trường `ADMIN_USER_IDS`.
- Khi khởi động, server đọc biến này và lưu vào memory.
- Mỗi request đến `/api/admin/*` sẽ check user ID trong memory.

### 3.2 Tự động Chuyển hướng đến Admin Dashboard

Khi một trong hai admin đăng nhập, hệ thống sẽ tự động nhận diện và chuyển hướng:

**Luồng xử lý:**
1. User nhấn nút đăng nhập.
2. Supabase Auth xác thực và trả về JWT token.
3. Frontend lấy user ID từ token.
4. Kiểm tra: `ADMIN_USER_IDS.includes(userId)` → true/false.
5. Nếu true: Navigate đến `/admin` thay vì `/dashboard`.
6. Nếu false: Navigate đến `/dashboard` như bình thường.

**Lưu ý quan trọng:**
- Admin vẫn có thể truy cập giao diện user nếu cần test (không cần quá ưu tiên).
- Có nút "Về trang User" trên admin dashboard.
- Tất cả session vẫn chia sẻ cùng một JWT token.

### 3.3 Bảo vệ Routes ở Frontend

Để ngăn user thường truy cập vào các trang admin:

**AdminRouteGuard component:**
- Wrap tất cả các routes trong `/admin/*`.
- Kiểm tra quyền admin trước khi render children.
- Nếu không phải admin → redirect về trang chủ.
- Không hiển thị flash content (nội dung nhấp nháy) trước khi redirect.

### 3.4 Bảo vệ API Endpoints ở Backend

Để ngăn gọi trực tiếp API admin mà không có quyền:

**AdminAuthFilter:**
- Intercept tất cả requests đến `/api/admin/**`.
- Lấy JWT token từ header `Authorization`.
- Validate token và extract user ID.
- Check user ID trong admin list.
- Nếu không phải admin → trả về `403 Forbidden`.
- Nếu hợp lệ → cho phép request tiếp tục đến controller.

---

## 4. Module 1: Quản lý Người dùng

### 4.1 Mục đích và Phạm vi

Module Quản lý Người dùng cho phép admin kiểm soát toàn bộ thông tin liên quan đến user, bao gồm:

- **Thông tin cá nhân:** Username, họ tên, email, số điện thoại, địa chỉ
- **Gói đăng ký:** Cramerie (miễn phí) hoặc Cramerich (trả phí)
- **Số dư Lúa:** Credits dùng để mua các tính năng bổ sung
- **Trạng thái tài khoản:** Active, Banned, Deactivated, Deleted
- **Hoạt động:** Lịch sử đăng nhập, số bài thi đã làm

### 4.2 Màn hình Danh sách Người dùng

Đây là màn hình chính của module, hiển thị tất cả user dưới dạng bảng với đầy đủ tính năng tìm kiếm và lọc.

#### 4.2.1 Chức năng Tìm kiếm

**Tìm kiếm nhanh:**
- Thanh tìm kiếm ở đầu trang
- Tìm theo: username, email, họ tên
- Kết quả hiển thị real-time khi gõ (debounce 300ms)
- Highlight từ khóa trong kết quả

**Ví dụ sử dụng:**
> Admin muốn tìm user có email "nguyenvana@gmail.com"
> → Gõ "nguyenvana" vào thanh tìm kiếm
> → Bảng tự động filter hiển thị chỉ những user khớp

#### 4.2.2 Chức năng Lọc (Filter)

**Các bộ lọc có sẵn:**

| Bộ lọc | Kiểu | Mô tả |
|--------|------|-------|
| Trạng thái | Dropdown | Active, Banned, Deactivated, All |
| Gói đăng ký | Dropdown | Cramerie, Cramerich, All |
| Ngày đăng ký | Date range | Từ ngày - Đến ngày |
| Số dư Lúa | Range slider | Từ X - Đến Y |

**Cách bộ lọc hoạt động:**
- Các bộ lọc được kết hợp với nhau bằng AND.
- Ví dụ: "Cramerich" + "Active" → Chỉ hiển thị user Cramerich đang active.
- Có nút "Xóa bộ lọc" để reset về mặc định.
- Bộ lọc được lưu vào URL để có thể bookmark/share.

#### 4.2.3 Chức năng Sắp xếp

**Các cột có thể sắp xếp:**
- Username (A-Z, Z-A).
- Ngày đăng ký (Mới nhất, Cũ nhất).
- Số dư Lúa (Cao → Thấp, Thấp → Cao).
- Lần đăng nhập cuối (Gần đây nhất, Lâu nhất).

**Cách sử dụng:**
- Click vào header cột để sắp xếp.
- Click lần 2 để đảo chiều (asc ↔ desc).
- Mũi tên hiển thị chiều sắp xếp hiện tại.

#### 4.2.4 Chức năng Phân trang

**Cấu hình phân trang:**
- Số dòng mỗi trang: 25 / 50 / 100 (dropdown chọn)
- Hiển thị: "Đang xem 1-25 trong tổng số 1,234 người dùng"
- Nút điều hướng: ◀ Trang trước | 1 2 3 ... 50 | Trang sau ▶

#### 4.2.5 Các Thao tác Nhanh trên Bảng

Mỗi dòng (row) có menu thao tác nhanh (3-dot menu hoặc các nút):

| Thao tác | Mô tả | Yêu cầu xác nhận? |
|----------|-------|-------------------|
| Xem chi tiết | Mở trang detail của user | Không |
| Đổi gói | Chuyển Cramerie ↔ Cramerich | Có |
| Thêm Lúa | Cộng credits vào tài khoản | Có (nhập số lượng) |
| Ban | Khóa truy cập tính năng | Có (nhập lý do) |
| Xóa | Đánh dấu xóa sau 30 ngày | Có (nhập lý do) |

### 4.3 Màn hình Chi tiết Người dùng

Khi click vào một user trong danh sách, admin sẽ thấy trang chi tiết với đầy đủ thông tin.

#### 4.3.1 Bố cục Màn hình

Màn hình được chia thành **các tab** để tổ chức thông tin:

```
┌─────────────────────────────────────────────────────────────┐
│  👤 quochuu54                                   [Ban] [Xóa] │
│  Cramerich • Active • Đăng ký: 01/12/2025                   │
├─────────────────────────────────────────────────────────────┤
│ [Hồ sơ] [Đăng ký] [Credits] [Hoạt động] [Nhật ký Admin]     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Nội dung của tab được chọn hiển thị ở đây                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 4.3.2 Tab "Hồ sơ" (Profile)

**Mục đích:** Xem và chỉnh sửa thông tin cá nhân của user

**Các trường thông tin:**

| Trường | Có thể sửa? | Ghi chú |
|--------|-------------|---------|
| Avatar | ✅ Có | Nhập URL ảnh, có preview |
| Username | ✅ Có | Kiểm tra unique khi lưu |
| Họ tên | ✅ Có | - |
| Email | ❌ Không | Được quản lý bởi Supabase Auth |
| Số điện thoại | ✅ Có | Validate format VN |
| Địa chỉ | ✅ Có | - |
| Trạng thái | ✅ Có | Dropdown: Active/Banned/Deactivated |
| Ngày đăng ký | ❌ Không | Chỉ xem |

**Lưu thay đổi:**
- Nút "Lưu" ở cuối form
- Validate trước khi lưu
- Hiển thị toast notification khi thành công/thất bại
- Tự động ghi vào audit log

#### 4.3.3 Tab "Đăng ký" (Subscription)

**Mục đích:** Quản lý gói đăng ký và các quota của user

**Thông tin hiển thị:**

```
Gói hiện tại: CRAMERICH
Trạng thái: Active
Bắt đầu: 01/12/2025
Hết hạn: 01/01/2026 (còn 18 ngày)
Tự động gia hạn: ✅ Bật

─── Quota sử dụng tháng này ───
Lượt thi (ATTEMPT):     12 / 50 đã dùng
Lượt chấm AI (ATTEMPT_AI): 3 / 30 đã dùng
Tin nhắn Chatbot:       45 / 100 đã dùng
```

**Các thao tác có thể thực hiện:**

| Thao tác | Mô tả |
|----------|-------|
| **Đổi gói** | Nâng/hạ gói đăng ký. Nếu hạ gói → có hiệu lực cuối kỳ |
| **Gia hạn** | Thêm 30/60/90 ngày vào thời hạn hiện tại |
| **Tặng miễn phí** | Cho 1 tháng Cramerich miễn phí (promotion) |
| **Reset quota** | Đặt lại các counter về 0 (dùng khi user report lỗi) |
| **Bật/tắt AI Grading** | Toggle chức năng chấm AI bằng R1 |

#### 4.3.4 Tab "Credits" (Số dư Lúa)

**Mục đích:** Xem và điều chỉnh số dư Lúa của user

**Dashboard số dư:**
```
Số dư hiện tại: 🌾 350 Lúa

Tổng đã nhận:   1,200 Lúa
Tổng đã tiêu:     850 Lúa

[+ Thêm Lúa]    [- Trừ Lúa]
```

**Lịch sử giao dịch Lúa:**
- Bảng liệt kê tất cả transactions
- Cột: Ngày | Loại | Số lượng | Số dư sau | Mô tả
- Filter theo loại: Earn / Spend / Bonus / Purchase
- Link đến chi tiết giao dịch thanh toán (nếu có)

**Thêm/Trừ Lúa:**
- Modal popup khi click nút
- Nhập số lượng (1-10000)
- Chọn lý do: Promotion / Hoàn tiền / Bù lỗi / Khác
- Nhập ghi chú chi tiết
- Xác nhận trước khi thực hiện

#### 4.3.5 Tab "Hoạt động" (Activity)

**Mục đích:** Xem lịch sử hoạt động của user trên hệ thống

**Các metric tổng quan:**
- Số bài thi đã làm: 45 bài
- Điểm trung bình: 6.5
- Streak cao nhất: 12 ngày
- Từ vựng đã lưu: 234 từ

**Timeline hoạt động gần đây:**
```
Hôm nay
  14:30 - Hoàn thành bài Reading Cambridge 17 Test 1 (Score: 32/40)
  10:15 - Lưu 3 từ vựng mới

Hôm qua
  20:00 - Nâng cấp lên Cramerich
  18:45 - Hoàn thành bài Listening Cambridge 17 Test 1

3 ngày trước
  ...
```

#### 4.3.6 Tab "Nhật ký Admin" (Admin Log)

**Mục đích:** Xem tất cả các thao tác admin đã thực hiện trên user này

**Bảng nhật ký:**

| Thời gian | Admin | Thao tác | Chi tiết |
|-----------|-------|----------|----------|
| 14/12 15:00 | admin1@... | Đổi gói | Cramerie → Cramerich |
| 13/12 10:30 | admin2@... | Thêm Lúa | +100 Lúa (Promotion) |
| 10/12 09:00 | admin1@... | Sửa profile | Đổi username: user123 → quochuu54 |

**Mục đích của tab này:**
- Audit trail: Ai đã làm gì, khi nào
- Debug: Truy vết khi có khiếu nại
- Compliance: Đáp ứng yêu cầu bảo mật dữ liệu

### 4.4 Thao tác Hàng loạt (Batch Operations)

**Mục đích:** Thực hiện cùng một thao tác cho nhiều user cùng lúc, tiết kiệm thời gian khi cần xử lý số lượng lớn.

#### 4.4.1 Cách Chọn Nhiều User

**Cách 1: Chọn thủ công**
- Checkbox ở đầu mỗi dòng trong bảng
- Checkbox "Chọn tất cả" ở header để chọn tất cả trên trang hiện tại
- Counter hiển thị: "Đã chọn 15 người dùng"

**Cách 2: Chọn theo bộ lọc**
- Sau khi filter, có nút "Chọn tất cả N kết quả"
- Áp dụng cho TẤT CẢ user khớp filter, không chỉ trang hiện tại
- Ví dụ: Filter "Cramerie + Active" → 500 users → "Chọn tất cả 500 người dùng"

#### 4.4.2 Các Thao tác Hàng loạt Hỗ trợ

| Thao tác | Mô tả | Ví dụ sử dụng |
|----------|-------|---------------|
| **Đổi trạng thái** | Chuyển nhiều user sang trạng thái mới | Ban tất cả spam accounts |
| **Đổi gói** | Nâng/hạ gói cho nhiều user | Tặng Cramerich cho nhóm beta testers |
| **Thêm Lúa** | Cộng credits cho nhiều user | Promotion dịp Tết |
| **Reset quota** | Reset usage counters | Fix bug sau khi patch |
| **Export** | Xuất danh sách ra file | Báo cáo marketing |

#### 4.4.3 Quy trình Thực hiện Batch Operation

```
Bước 1: Chọn users (checkbox hoặc filter-based)
         ↓
Bước 2: Click "Thao tác hàng loạt" → Chọn loại thao tác
         ↓
Bước 3: Nhập tham số (nếu cần)
        Ví dụ: Thêm Lúa → Nhập số lượng + lý do
         ↓
Bước 4: Preview kết quả
        "Sẽ thêm 100 Lúa cho 50 người dùng đã chọn"
        Danh sách 5 user đầu tiên để kiểm tra
         ↓
Bước 5: Xác nhận
        Nhập mật khẩu admin hoặc mã xác nhận
         ↓
Bước 6: Thực thi
        Progress bar hiển thị tiến trình
        "Đã xử lý 30/50..."
         ↓
Bước 7: Kết quả
        "Thành công: 48 | Thất bại: 2"
        Link để xem chi tiết lỗi
```

### 4.5 Quản lý Trạng thái Tài khoản

#### 4.5.1 Các Trạng thái và Ý nghĩa

Mỗi tài khoản user có thể ở một trong 4 trạng thái:

**1. ACTIVE (Hoạt động)**
- Trạng thái bình thường, mặc định khi đăng ký
- User có quyền truy cập đầy đủ theo gói đăng ký
- Có thể chuyển sang: Banned, Deactivated

**2. BANNED (Bị khóa)**
- User vẫn có thể đăng nhập nhưng không thể:
  - Làm bài thi
  - Sử dụng chatbot
  - Mua gói/Lúa
- Hiển thị banner "Tài khoản của bạn đã bị khóa" + lý do
- Có thể chuyển về: Active (Unban)

**3. DEACTIVATED (Ngừng hoạt động)**
- User tự yêu cầu xóa tài khoản hoặc admin đánh dấu
- Không thể đăng nhập
- Sau 30 ngày → Tự động chuyển sang DELETED
- Có thể hủy trong 30 ngày → Về lại Active

**4. DELETED (Đã xóa)**
- Dữ liệu được ẩn danh hóa (anonymize)
- Email đổi thành hash, username đổi thành "deleted_xxxxx"
- Giữ lại record để audit (không xóa hoàn toàn database row)
- Không thể phục hồi

#### 4.5.2 Sơ đồ Chuyển đổi Trạng thái

```
               ┌─────────────────────────────────────┐
               │                                     │
               ▼                                     │
         ┌──────────┐    Ban      ┌──────────┐      │
    ─────│  ACTIVE  │────────────▶│  BANNED  │      │
         └────┬─────┘◀────────────└──────────┘      │
              │        Unban                         │
              │                                      │
              │ Deactivate                           │
              ▼                                      │
         ┌──────────────┐    Hủy trong 30 ngày       │
         │ DEACTIVATED  │────────────────────────────┘
         └───────┬──────┘
                 │
                 │ Sau 30 ngày (tự động)
                 ▼
         ┌──────────┐
         │ DELETED  │ ← Không thể phục hồi
         └──────────┘
```

#### 4.5.3 Quy trình Ban User

Khi admin quyết định ban một user:

1. **Chọn user** và click "Ban" hoặc qua context menu.
2. **Modal hiện lên** yêu cầu:
   - Lý do ban (dropdown): Vi phạm ToS / Spam / Fraud / Khác.
   - Chi tiết (textarea): Mô tả cụ thể vi phạm.
   - Thời hạn (optional): Vĩnh viễn / 7 ngày / 30 ngày / 90 ngày.
3. **Xác nhận** bằng cách nhấn "Ban người dùng này".
4. **Hệ thống thực hiện:**
   - Cập nhật `profiles.account_status = 'BANNED'`.
   - Lưu lý do vào `profiles.status_reason`.
   - Ghi nhật ký admin.
   - Gửi email thông báo cho user (nếu được config).

---

## 5. Module 2: Quản lý Tài chính

### 5.1 Mục đích và Phạm vi

Module Quản lý Tài chính cung cấp cái nhìn toàn diện về tình hình tài chính của Cramer, bao gồm:

- **Doanh thu:** Tiền thực tế nhận được từ subscriptions và Lúa packs.
- **Giao dịch:** Chi tiết từng transaction từ PayOS.
- **Báo cáo:** Phân tích xu hướng, so sánh, dự báo.

**Tại sao cần module này?**

Trước đây, để biết doanh thu tháng, admin phải:
1. Đăng nhập PayOS dashboard xem tổng thu.  
2. Query database để đếm subscriptions mới.
3. Tự tính toán và đối chiếu.

Với module này, tất cả thông tin được tập trung tại một nơi, tự động cập nhật, và có sẵn biểu đồ trực quan.

### 5.2 Trang Tổng quan Doanh thu (Dashboard)

#### 5.2.1 Các Thẻ Metric Chính

Phần đầu trang hiển thị 4 thẻ metric quan trọng nhất:

**Thẻ 1: Tổng Doanh thu**
- Hiển thị tổng số tiền đã nhận được trong kỳ được chọn.
- So sánh với kỳ trước (% tăng/giảm).
- Breakdown theo nguồn: Subscriptions vs Lúa Packs.

**Thẻ 2: Subscriptions Mới**
- Số lượng đăng ký mới trong kỳ.
- Phân loại theo tier (Cramerich).
- Churn rate: Số user hủy đăng ký.
- Net change: Mới - Hủy = Tăng trưởng thực.

**Thẻ 3: Doanh số Lúa**
- Doanh thu từ việc bán Lúa packs.
- Số gói đã bán.
- Gói phổ biến nhất (% doanh số).
- Giá trị trung bình mỗi giao dịch.

**Thẻ 4: Tăng trưởng**
- Tỷ lệ tăng trưởng so với kỳ trước.
- Month-over-Month (MoM).
- Year-over-Year (YoY) nếu có đủ data.

#### 5.2.2 Bộ Lọc Thời gian

Admin có thể chọn khoảng thời gian để xem dữ liệu:

| Tùy chọn | Mô tả | Ví dụ |
|----------|-------|-------|
| Hôm nay | Từ 00:00 đến hiện tại | 14/12/2025 |
| Tuần này | Từ thứ Hai đến nay | 09/12 - 14/12 |
| Tháng này | Từ ngày 1 đến nay | 01/12 - 14/12 |
| Tháng trước | Tháng calendar trước | 01/11 - 30/11 |
| Quý này | Q1/Q2/Q3/Q4 | Q4: 01/10 - 31/12 |
| Năm nay | Từ 1/1 đến nay | 01/01 - 14/12/2025 |
| Tùy chỉnh | Date picker chọn from-to | Bất kỳ khoảng nào |

Khi thay đổi bộ lọc, tất cả metric và biểu đồ tự động update mà không cần tải lại trang.

#### 5.2.3 Biểu đồ Xu hướng Doanh thu

Phần giữa trang hiển thị biểu đồ đường (line chart) thể hiện xu hướng doanh thu theo thời gian.

**Các thành phần của biểu đồ:**
- **Trục X:** Thời gian (ngày/tuần/tháng tùy khoảng chọn)
- **Trục Y:** Số tiền (VND, format có dấu phân cách)
- **Đường tổng:** Tổng doanh thu (màu xanh dương đậm)
- **Đường subscriptions:** Doanh thu từ gói đăng ký (màu xanh lá)
- **Đường Lúa:** Doanh thu từ Lúa packs (màu vàng cam)
- **Đường so sánh:** Kỳ trước (nét đứt, màu xám)

**Tương tác với biểu đồ:**
- Hover vào điểm: Hiển thị tooltip với số liệu cụ thể của ngày đó
- Click vào điểm: Chuyển đến danh sách giao dịch của ngày đó
- Zoom: Kéo để phóng to một khoảng thời gian cụ thể

#### 5.2.4 Biểu đồ Phân bố

Bên dưới biểu đồ xu hướng có 2 biểu đồ tròn (donut chart):

**Biểu đồ trái: Phân bổ theo Sản phẩm**
- Cramerich Monthly: X%
- Lúa - Xe Lúa (1000): Y%
- Lúa - Gói Lúa (500): Z%
- Lúa - Túi Lúa (100): W%

**Biểu đồ phải: Phân bổ theo Phương thức**
- Chuyển khoản ngân hàng: X%
- Ví MoMo: Y%
- Ví ZaloPay: Z%

#### 5.2.5 Bảng Top Chi tiêu

Góc dưới bên phải hiển thị bảng xếp hạng user chi tiêu nhiều nhất trong kỳ:

**Cột thông tin:**
- Hạng: 1, 2, 3... (có icon huy chương cho top 3)
- Username: Tên người dùng (link đến trang chi tiết)
- Tổng chi: Số tiền đã chi (VND)
- Nút "Xem": Mở trang chi tiết user

**Ý nghĩa của tính năng này:**
- Nhận diện VIP customers để chăm sóc đặc biệt
- Phân tích hành vi người dùng trả nhiều tiền
- Cơ hội upsell cho top spenders

### 5.3 Trang Lịch sử Giao dịch

**Mục đích:** Hiển thị chi tiết từng giao dịch tài chính xảy ra trên hệ thống.

#### 5.3.1 Các Loại Giao dịch

Hệ thống theo dõi nhiều loại giao dịch khác nhau:

**Giao dịch Thu (Incoming):**
- `SUBSCRIPTION_NEW`: User mới mua gói Cramerich
- `SUBSCRIPTION_RENEWAL`: Tự động gia hạn
- `SUBSCRIPTION_UPGRADE`: Nâng cấp từ Cramerie lên Cramerich
- `LUA_PACK_PURCHASE`: Mua gói Lúa

**Giao dịch Chi (Outgoing):**
- `REFUND_FULL`: Hoàn tiền 100%
- `REFUND_PARTIAL`: Hoàn tiền một phần
- `CHARGEBACK`: Tranh chấp từ ngân hàng

**Giao dịch Nội bộ (Internal):**
- `LUA_EARNED`: Bonus Lúa (không có tiền thực)
- `LUA_SPENT`: User tiêu Lúa vào features
- `LUA_EXPIRED`: Lúa hết hạn (nếu có policy)

#### 5.3.2 Cấu trúc Bảng Giao dịch

| Cột | Mô tả | Lọc được? |
|-----|-------|-----------|
| ID | Mã giao dịch nội bộ | Tìm kiếm |
| Order Code | Mã đơn PayOS (7 số) | Tìm kiếm |
| Người dùng | Avatar + Username | Tìm kiếm |
| Loại | Badge màu theo loại | Dropdown |
| Chi tiết | Tên gói / sản phẩm | - |
| Số tiền | Format VND với màu +/- | Range |
| Trạng thái | Pending/Paid/Failed | Dropdown |
| Thời gian | DD/MM/YYYY HH:MM | Date range |
| Thao tác | Xem chi tiết, Hoàn tiền | - |

#### 5.3.3 Các Trạng thái Giao dịch

**PENDING (Đang chờ) - Badge màu vàng:**
- Đơn hàng đã được tạo trên PayOS
- User đã nhận được link thanh toán
- Đang chờ user hoàn tất thanh toán
- Link thanh toán còn hiệu lực 24 giờ

**PAID (Thành công) - Badge màu xanh:**
- Thanh toán đã hoàn tất qua PayOS
- Tiền đã vào tài khoản Cramer
- Credits/Subscription đã được cấp cho user
- Không thể hủy giao dịch

**CANCELLED (Đã hủy) - Badge màu xám:**
- User chủ động hủy thanh toán
- Có thể hủy trước khi hoàn tất
- Không có tiền được chuyển
- User có thể tạo đơn mới

**EXPIRED (Hết hạn) - Badge màu xám đậm:**
- Link thanh toán đã quá 24 giờ
- User không thực hiện thanh toán
- Đơn hàng tự động đóng
- User cần tạo đơn mới nếu muốn mua

**FAILED (Thất bại) - Badge màu đỏ:**
- Có lỗi kỹ thuật khi xử lý
- Có thể do PayOS hoặc hệ thống Cramer
- Cần admin investigate
- Có thể cần hoàn tiền manual

#### 5.3.4 Xem Chi tiết Giao dịch

Khi click vào một giao dịch, một drawer/modal hiện ra với đầy đủ thông tin:

**Phần 1: Thông tin Đơn hàng**
- Order Code: Mã đơn PayOS
- Payment Link ID: ID của link thanh toán
- Checkout URL: Link thanh toán (có thể copy)
- Trạng thái: Badge + text mô tả
- Thời gian tạo: DD/MM/YYYY HH:MM:SS
- Thời gian thanh toán: (nếu đã paid)

**Phần 2: Thông tin Người mua**
- User ID: UUID (click to copy)
- Username: Tên đăng nhập
- Email: Email đăng ký
- Link: Nút "Xem hồ sơ" để đến trang chi tiết user

**Phần 3: Thông tin Sản phẩm**
- Loại: SUBSCRIPTION hoặc LUA_PACK
- Tên sản phẩm: Cramerich / Xe Lúa / Gói Lúa...
- Giá: Số tiền VND
- Mô tả: Description gửi đến PayOS

**Phần 4: Timeline**
- Danh sách các sự kiện theo thứ tự thời gian
- Ví dụ: Tạo đơn → Thanh toán → Kích hoạt subscription → Cộng bonus

**Các nút thao tác:**
- [Hoàn tiền]: Mở form hoàn tiền
- [Đóng]: Đóng modal

#### 5.3.5 Chức năng Hoàn tiền

Khi cần hoàn tiền cho user (ví dụ: user khiếu nại, lỗi hệ thống, hoặc yêu cầu hợp lệ):

**Quy trình hoàn tiền:**

1. **Mở chi tiết giao dịch** đã PAID
2. **Click "Hoàn tiền"** để mở form
3. **Chọn loại hoàn tiền:**
   - Toàn bộ: Hoàn 100% số tiền
   - Một phần: Nhập số tiền cụ thể muốn hoàn
4. **Nhập lý do hoàn tiền:**
   - Dropdown: Yêu cầu của user / Lỗi hệ thống / Chính sách / Khác
   - Textarea: Chi tiết cụ thể
5. **Chọn xử lý Benefits:**
   - Thu hồi: Trừ lại số Lúa đã cấp, hủy subscription
   - Giữ nguyên: User vẫn giữ (ưu đãi đặc biệt)
6. **Xác nhận và thực hiện**

**Lưu ý quan trọng:**
- Hệ thống Cramer chỉ đánh dấu và ghi nhận
- Tiền hoàn thực tế cần xử lý qua PayOS dashboard
- Hoặc chuyển khoản manual nếu PayOS không hỗ trợ

### 5.4 Trang Báo cáo Tài chính

#### 5.4.1 Các Loại Báo cáo Có sẵn

**Báo cáo 1: Tổng hợp Doanh thu**
- **Mục đích:** Cái nhìn tổng quan về doanh thu trong kỳ
- **Nội dung bao gồm:**
  - Tổng doanh thu theo ngày/tuần/tháng
  - Phân theo nguồn: Subscriptions vs Lúa
  - So sánh với kỳ trước
  - Xu hướng tăng/giảm
- **Phù hợp cho:** Báo cáo hàng tháng, theo dõi KPIs

**Báo cáo 2: Phân tích Subscriptions**
- **Mục đích:** Đánh giá sức khỏe của subscription business
- **Nội dung bao gồm:**
  - MRR (Monthly Recurring Revenue): Doanh thu định kỳ hàng tháng
  - Churn rate: Tỷ lệ % user hủy đăng ký
  - LTV (Lifetime Value): Giá trị vòng đời dự kiến của user
  - Cohort retention: Tỷ lệ giữ chân theo nhóm đăng ký
- **Phù hợp cho:** Đánh giá chiến lược giá, planning

**Báo cáo 3: Kinh tế Lúa**
- **Mục đích:** Theo dõi "tiền ảo" trong hệ thống
- **Nội dung bao gồm:**
  - Tổng Lúa đã phát hành (purchased + bonus)
  - Tổng Lúa đã tiêu (spent on features)
  - Lúa đang lưu hành (tổng balance của users)
  - Top features tiêu Lúa nhiều nhất
- **Phù hợp cho:** Cân bằng economy, điều chỉnh pricing

**Báo cáo 4: User Acquisition Cost**
- **Mục đích:** Đánh giá chi phí thu hút user mới
- **Nội dung bao gồm:**
  - Chi phí Lúa bonus cho user mới
  - Tỷ lệ conversion: Free → Paid
  - Thời gian trung bình để convert
  - ROI của promotions
- **Phù hợp cho:** Marketing, budget allocation

#### 5.4.2 Tạo và Xuất Báo cáo

**Bước 1: Chọn loại báo cáo**
- Dropdown hoặc tabs để chọn
- Mô tả ngắn về mỗi loại báo cáo

**Bước 2: Cấu hình tham số**
- Khoảng thời gian: From date - To date
- Granularity: Theo ngày / Tuần / Tháng
- Filters (nếu có): Theo gói, theo segment

**Bước 3: Xem trước trên màn hình**
- Báo cáo render realtime
- Có thể điều chỉnh tham số và xem lại ngay

**Bước 4: Xuất file**
- Formats hỗ trợ:
  - CSV: Dữ liệu thô, dễ import vào Excel/Google Sheets
  - Excel (.xlsx): Đã format sẵn, có styles
  - PDF: Để in hoặc gửi email cho stakeholders

---

## 6. Module 3: Quản lý Nội dung Đề thi

### 6.1 Mục đích và Phạm vi

Module này cho phép admin thêm, sửa, xóa nội dung đề thi IELTS mà không cần viết SQL queries. Đây là module phức tạp nhất vì phải xử lý nhiều loại dữ liệu khác nhau.

**Các vấn đề hiện tại khi nhập đề thi:**
- Phải viết SQL script dài hàng trăm dòng
- JSON format phức tạp (`question_content`, `section_layout`)
- Dễ sai cú pháp, khó debug
- Không có preview trước khi đưa lên production
- Khó track đề nào đang draft, đề nào đã publish

**Giải pháp của module này:**
- Giao diện visual để thêm/sửa từng câu hỏi
- Form input thay vì viết JSON thủ công
- Import từ CSV/JSON với validation tự động
- Preview đề thi như user sẽ thấy
- Workflow: Draft → Published → Archived

### 6.2 Cấu trúc Phân cấp Nội dung

Nội dung đề thi được tổ chức theo cấu trúc phân cấp như sau:

**Level 1: Topic (exam_source)**
- Đây là nguồn đề thi: Cambridge 17, Cambridge 18, Real Test...
- Mỗi topic có thể có nhiều tests

**Level 2: Test (test_number)**
- Một bộ đề hoàn chỉnh: Test 1, Test 2, Test 3, Test 4
- Mỗi test có 4 skills

**Level 3: Skill**
- Reading: 3 passages, 40 câu hỏi
- Listening: 4 parts, 40 câu hỏi
- Writing: 2 tasks
- Speaking: 3 parts (future)

**Level 4: Section/Part**
- Reading: Part 1 (Q1-13), Part 2 (Q14-26), Part 3 (Q27-40)
- Listening: Part 1-4 (Q1-10, Q11-20, Q21-30, Q31-40)

**Level 5: Question**
- Câu hỏi đơn lẻ với type, content, answer

### 6.3 Trang Danh sách Đề thi

#### 6.3.1 Chế độ Xem Cây (Tree View)

Đây là giao diện mặc định, hiển thị dạng cây có thể expand/collapse:

**Cách hoạt động:**
- Click vào icon ▶ để mở rộng node
- Click vào icon ▼ để thu gọn node
- Double click vào tên để mở editor

**Thông tin hiển thị mỗi node:**
- Topic: Tên + số tests
- Test: Tên + trạng thái (Draft/Published)
- Skill: Icon + progress (X/40 câu)

**Màu sắc trạng thái:**
- ✅ Xanh lá: Đủ câu hỏi, đã publish
- ⚠️ Vàng: Thiếu câu hỏi hoặc có lỗi
- ⏳ Xám: Chưa có nội dung

#### 6.3.2 Chế độ Xem Lưới (Grid View)

Tùy chọn xem dạng card để có cái nhìn tổng quan nhanh:

**Mỗi card hiển thị:**
- Topic + Test name
- 4 icons cho 4 skills với trạng thái
- Badge trạng thái: Draft/Published/Archived
- Nút Edit và Menu (dropdown)

**Ưu điểm của Grid View:**
- Thấy được nhiều tests cùng lúc
- Dễ so sánh tiến độ các tests
- Phù hợp khi đang nhập nhiều đề

#### 6.3.3 Trạng thái Nội dung

Mỗi test/skill có thể ở một trong các trạng thái:

| Trạng thái | Ý nghĩa | User thấy? |
|------------|---------|------------|
| **DRAFT** | Đang soạn, chưa hoàn chỉnh | ❌ Không |
| **PUBLISHED** | Hoàn chỉnh, sẵn sàng làm bài | ✅ Có |
| **ARCHIVED** | Ngừng sử dụng, lưu trữ | ❌ Không |

**Tại sao cần ARCHIVED thay vì DELETE?**
- Giữ lại data để reference
- Có thể restore nếu cần
- User đã làm bài vẫn xem được lịch sử

### 6.4 Trang Editor Đề thi

#### 6.4.1 Bố cục Màn hình

Màn hình editor chia làm các phần:

**Header:**
- Breadcrumb: Home > Cambridge 17 > Test 1
- Tên test đang edit
- Nút Lưu và Publish

**Tab Bar:**
- 4 tabs cho 4 skills: Reading, Listening, Writing, Speaking
- Badge số lượng câu hỏi trên mỗi tab

**Content Area:**
- Chia 2 panel: Navigation (40%) + Editor (60%)
- Panel trái: Danh sách parts và questions
- Panel phải: Form edit câu hỏi được chọn

#### 6.4.2 Tab Reading

**Panel trái - Part Navigator:**
- Danh sách 3 parts (Passage 1, 2, 3)
- Expand để thấy danh sách câu hỏi
- Icon ✅/⚠️ cho biết trạng thái mỗi câu
- Click để chọn câu hỏi cần edit

**Panel phải - Section Editor (khi chọn Part):**
- Passage Text: Textarea lớn để nhập đoạn văn
- Support HTML: `<strong>` cho title, paragraph markers
- Preview button để xem rendered

**Panel phải - Question Editor (khi chọn Question):**
- Form động theo question type
- Validate realtime
- Preview button

#### 6.4.3 Tab Listening

**Panel trái - Part Navigator:**
- 4 parts với danh sách câu hỏi
- Tương tự Reading

**Panel phải - Section Config (khi chọn Part):**
- Audio URL: Input nhập URL file mp3
- Transcript: Textarea nhập transcript
- Section Layout: Visual builder cho block structure

**Panel phải - Question Editor (khi chọn Question):**
- Tương tự Reading
- Có thêm block_type cho Listening

#### 6.4.4 Tab Writing

**Task 1 Section:**
- Image Upload: Drag-drop hoặc nhập URL
- Image Preview: Xem trước chart/map
- Image Description: Mô tả chi tiết cho AI grading
- Task Description: Đề bài Writing Task 1

**Task 2 Section:**
- Essay Topic: Đề bài Writing Task 2
- Sample Essays: Thêm nhiều sample với band scores
- Marking Criteria: Notes cho từng band

### 6.5 Question Editor Chi tiết

#### 6.5.1 Các Loại Câu hỏi Hỗ trợ

Mỗi loại câu hỏi có form editor phù hợp:

**1. FILL_IN_BLANK (Điền vào chỗ trống)**
- Mô tả: User gõ từ/cụm từ vào ô trống
- Form fields:
  - Question text (có chứa `____`)
  - Correct answers (có thể nhiều đáp án chấp nhận)
  - Word limit dropdown

**2. TRUE_FALSE_NOT_GIVEN**
- Mô tả: User chọn T/F/NG cho statement
- Form fields:
  - Statement text
  - Correct answer (radio buttons)

**3. YES_NO_NOT_GIVEN**
- Tương tự như TRUE_FALSE_NOT_GIVEN
- Dùng cho opinion questions

**4. MULTIPLE_CHOICE**
- Mô tả: User chọn 1 trong nhiều options
- Form fields:
  - Question text
  - Options (A, B, C, D...) với nút thêm/xóa
  - Correct answer (click để chọn)

**5. MULTIPLE_CHOICE_MULTIPLE_ANSWERS**
- Mô tả: User chọn nhiều options đúng
- Form fields:
  - Tương tự MULTIPLE_CHOICE
  - Cho phép chọn nhiều correct answers

**6. MATCHING_HEADINGS**
- Mô tả: Match paragraphs với headings
- Form fields:
  - Paragraph identifier (A, B, C...)
  - List of headings (i, ii, iii...)
  - Correct heading cho paragraph

**7. MATCHING_INFORMATION**
- Mô tả: Match statements với paragraphs
- Form fields:
  - Statement text
  - Paragraph options
  - Correct paragraph

**8. MATCHING_FEATURES**
- Mô tả: Match items với people/features
- Form fields:
  - Item text
  - Features list
  - Correct feature

**9. MATCHING_SENTENCE_ENDINGS**
- Mô tả: Match sentence beginnings với endings
- Form fields:
  - Sentence beginning
  - Possible endings
  - Correct ending

**10. SUMMARY_COMPLETION**
- Mô tả: Điền từ vào summary
- Form fields:
  - Summary text template (cho Q đầu tiên)
  - Answer cho mỗi blank

**11. SUMMARY_COMPLETION_OPTIONS**
- Mô tả: Chọn từ từ word bank điền vào summary
- Form fields:
  - Summary text
  - Word bank options
  - Correct option cho mỗi blank

**12. TABLE_COMPLETION**
- Mô tả: Điền vào ô trong bảng
- Form fields:
  - Table HTML (cho Q đầu tiên)
  - Answer cho mỗi blank

**13. DIAGRAM_LABEL_COMPLETION**
- Mô tả: Label các phần của diagram
- Form fields:
  - Diagram image URL
  - Labels và correct answers

#### 6.5.2 Validation Tự động

Khi admin nhập liệu, hệ thống kiểm tra ngay lập tức:

**Error (Phải sửa - Đỏ):**
- Đáp án đúng không được để trống
- JSON structure phải valid
- FILL_IN_BLANK phải có đúng 1 `____` placeholder
- Multiple choice phải có ít nhất 2 options

**Warning (Nên xem lại - Vàng):**
- Word limit chưa được set cho completion questions
- Image URL trả về 404
- Explanation chưa được thêm

**Info (Gợi ý - Xanh):**
- Đề nghị thêm explanation cho câu hỏi
- Đề nghị review lại formatting

#### 6.5.3 Preview Câu hỏi

Nút "Preview" hiển thị câu hỏi như user sẽ thấy trên app:

**Preview bao gồm:**
- Instructions box (dựa trên question type)
- Question text với formatting
- Input area (text box, radio buttons, dropdown...)
- Styling giống app thật

**Mục đích của Preview:**
- Verify formatting chính xác
- Catch lỗi trước khi publish
- Demo cho team xem flow

### 6.6 Import Wizard

#### 6.6.1 Tổng quan

Import Wizard cho phép nhập nhiều câu hỏi cùng lúc từ file, rất hữu ích khi:
- Nhập đề thi mới hoàn toàn
- Migrate data từ nguồn khác
- Batch update nhiều câu hỏi

**Các phương thức import:**

| Phương thức | Mô tả | Khi nào dùng |
|-------------|-------|--------------|
| **Manual** | Nhập từng câu qua form | Sửa vài câu |
| **CSV** | Upload file .csv | Data dạng bảng |
| **JSON** | Upload/paste JSON | Data đúng format |
| **SQL** | Paste SQL script | Migrate từ scripts cũ |

#### 6.6.2 Quy trình Import CSV

**Bước 1: Upload File**
- Drag-drop zone để kéo thả file
- Hoặc click để browse
- Support: .csv, max 10MB
- Download template mẫu

**Bước 2: Preview Data**
- Hiển thị 5 dòng đầu tiên
- Kiểm tra encoding (UTF-8)
- Xác nhận có header row

**Bước 3: Map Columns**
- Dropdown để map cột CSV với database fields
- Auto-detect nếu tên cột khớp
- Bỏ qua cột không cần

**Bước 4: Validate**
- Chạy validation cho tất cả rows
- Hiển thị kết quả: X thành công, Y lỗi, Z warning
- Chi tiết lỗi với số dòng và field

**Bước 5: Confirm Import**
- Summary những gì sẽ xảy ra
- Cảnh báo nếu overwrite data
- Nhập "CONFIRM" để xác nhận

**Bước 6: Execute**
- Progress bar hiển thị tiến độ
- Có thể cancel giữa chừng
- Kết quả cuối cùng

#### 6.6.3 Template CSV

Format CSV được recommend:

```
question_number,question_type,text,options,correct_answer,word_limit
1,FILL_IN_BLANK,"The ____ of London...",,"[""population""]",ONE WORD ONLY
2,TRUE_FALSE_NOT_GIVEN,"The railway was expensive",,"[""TRUE""]",
```

**Lưu ý quan trọng:**
- File phải UTF-8 encoding
- JSON columns phải escape đúng cách
- Dùng double quotes cho text có dấu phẩy

### 6.7 Publish Workflow

#### 6.7.1 Pre-publish Checklist

Trước khi publish một test, hệ thống chạy checklist tự động:

**Kiểm tra Reading:**
- [ ] Passage 1, 2, 3 có nội dung
- [ ] Đủ 40 câu hỏi
- [ ] Tất cả câu có đáp án
- [ ] Không có lỗi validation

**Kiểm tra Listening:**
- [ ] 4 parts có audio URL
- [ ] 4 parts có transcript
- [ ] Đủ 40 câu hỏi
- [ ] Section layout hợp lệ

**Kiểm tra Writing:**
- [ ] Task 1 có image + description
- [ ] Task 2 có topic

**Kết quả checklist:**
- ✅ Pass: Đủ điều kiện publish
- ⚠️ Warning: Có cảnh báo nhưng vẫn publish được
- ❌ Fail: Phải sửa trước khi publish

#### 6.7.2 Các Trạng thái và Chuyển đổi

**DRAFT → PUBLISHED:**
- Requirement: Pass pre-publish checklist
- Effect: Test hiển thị cho users
- Ghi nhật ký: Ai publish, khi nào

**PUBLISHED → DRAFT (Unpublish):**
- Requirement: Admin confirm
- Effect: Test ẩn khỏi users
- Note: Users đang làm dở sẽ bị interrupted

**PUBLISHED → ARCHIVED:**
- Requirement: Admin confirm
- Effect: Test ẩn, đánh dấu archived
- Note: Giữ lại data, có thể restore

**ARCHIVED → PUBLISHED (Restore):**
- Requirement: Pass pre-publish checklist
- Effect: Test hiển thị trở lại

---

## 7. Các Thành phần Dùng chung

### 7.1 DataTable Component

Đây là component bảng dữ liệu được dùng ở nhiều nơi trong admin.

**Tính năng chính:**
- **Sorting:** Click header để sắp xếp, click lại để đảo chiều
- **Filtering:** Dropdown/input filters ở mỗi cột
- **Pagination:** Phân trang với page size options
- **Selection:** Checkbox để chọn rows cho batch actions
- **Actions:** Row-level buttons hoặc dropdown menu
- **Export:** Xuất data đã filter ra file

**Sử dụng lại:**
- User list page
- Transaction history page
- Question list in test editor
- Admin audit log

### 7.2 Import Modal Component

Modal dùng cho việc import data từ file.

**Tính năng chính:**
- **Multi-format:** Support CSV, JSON, manual entry
- **Drag-drop:** Kéo thả file vào zone
- **Column mapping:** Map columns với fields
- **Validation:** Kiểm tra lỗi trước khi import
- **Progress:** Hiển thị tiến độ import

**Sử dụng lại:**
- Import questions
- Import users (future)
- Import transactions (future)

### 7.3 Chart Components

Các biểu đồ được dùng trong module tài chính.

**Các loại charts:**
- **Line Chart:** Xu hướng theo thời gian
- **Bar Chart:** So sánh giữa các categories
- **Pie/Donut Chart:** Phân bổ tỷ lệ
- **Area Chart:** Stacked trends

**Library recommend:** Recharts (React-based, responsive)

---

## 8. Tham chiếu Database Schema

### 8.1 Các Bảng Mới Cần Tạo

**Bảng 1: admin_audit_log**
- Mục đích: Ghi nhật ký mọi thao tác admin
- Columns: id, admin_user_id, action, target_type, target_id, old_value, new_value, ip_address, created_at

**Bảng 2: content_publish_status**
- Mục đích: Track trạng thái publish của content
- Columns: id, exam_source, test_number, skill, status, published_at, published_by, notes

### 8.2 Các Cột Mới Cần Thêm vào Profiles

- `account_status`: VARCHAR(20), default 'ACTIVE'
- `status_reason`: TEXT, nullable
- `status_updated_at`: TIMESTAMPTZ, nullable
- `status_updated_by`: UUID, nullable
- `deactivation_scheduled_at`: TIMESTAMPTZ, nullable
- `last_login_at`: TIMESTAMPTZ, nullable
- `login_count`: INTEGER, default 0

---

## 9. Danh sách API Endpoints

### 9.1 Admin User Management APIs

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/users` | Danh sách users với filters |
| GET | `/api/admin/users/{id}` | Chi tiết user |
| PUT | `/api/admin/users/{id}` | Cập nhật profile |
| PUT | `/api/admin/users/{id}/status` | Đổi trạng thái |
| PUT | `/api/admin/users/{id}/subscription` | Đổi subscription |
| POST | `/api/admin/users/{id}/credits` | Thêm/trừ credits |
| POST | `/api/admin/users/batch` | Thao tác hàng loạt |
| GET | `/api/admin/users/export` | Export danh sách |

### 9.2 Admin Financial APIs

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/finance/overview` | Dashboard metrics |
| GET | `/api/admin/finance/transactions` | Lịch sử giao dịch |
| GET | `/api/admin/finance/transactions/{id}` | Chi tiết giao dịch |
| POST | `/api/admin/finance/transactions/{id}/refund` | Hoàn tiền |
| GET | `/api/admin/finance/reports/{type}` | Tạo báo cáo |
| GET | `/api/admin/finance/export` | Export dữ liệu |

### 9.3 Admin Content Management APIs

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/content/topics` | Danh sách topics |
| GET | `/api/admin/content/tests` | Danh sách tests |
| GET | `/api/admin/content/tests/{source}/{number}` | Chi tiết test |
| PUT | `/api/admin/content/tests/{source}/{number}` | Cập nhật test |
| DELETE | `/api/admin/content/tests/{source}/{number}` | Xóa test |
| GET | `/api/admin/content/sections/{id}` | Chi tiết section |
| PUT | `/api/admin/content/sections/{id}` | Cập nhật section |
| POST | `/api/admin/content/sections` | Tạo section mới |
| PUT | `/api/admin/content/questions/{id}` | Cập nhật question |
| POST | `/api/admin/content/questions` | Tạo question mới |
| POST | `/api/admin/content/questions/batch` | Batch import |
| POST | `/api/admin/content/import/validate` | Validate import data |
| PUT | `/api/admin/content/tests/{source}/{number}/publish` | Publish test |

---

## 10. Hướng dẫn UI/UX

### 10.1 Nguyên tắc Thiết kế

1. **Nhất quán:** Giống với style của app chính (glassmorphism, dark theme)
2. **Hiệu quả:** Ít clicks cho các thao tác thường xuyên
3. **An toàn:** Xác nhận trước khi thực hiện thay đổi lớn
4. **Phản hồi:** Loading states, success/error messages rõ ràng
5. **Trợ giúp:** Tooltips, hints cho các tính năng phức tạp

### 10.2 Bảng Màu Admin

- **Primary (Admin accent):** Tím (#8B5CF6) - Phân biệt với user UI
- **Success:** Xanh lá (#10B981)
- **Warning:** Vàng cam (#F59E0B)
- **Danger:** Đỏ (#EF4444)
- **Info:** Xanh dương (#3B82F6)

### 10.3 Layout Chuẩn

- **Sidebar:** Cố định bên trái, có thể collapse
- **Header:** Cố định trên cùng, chứa breadcrumb và notifications
- **Content:** Scrollable, full width còn lại

---

## 11. Các Lưu ý Bảo mật

### 11.1 Authentication

- ✅ Xác thực JWT trên mọi admin endpoint
- ✅ Whitelist admin user IDs
- ✅ Session timeout sau 30 phút không hoạt động
- ⏳ Xem xét 2FA cho admin accounts

### 11.2 Authorization

- ✅ Routes `/api/admin/**` riêng biệt
- ✅ Filter kiểm tra quyền admin
- ✅ Frontend route guards
- ⏳ Phân quyền chi tiết theo action (future)

### 11.3 Audit Trail

Ghi nhật ký tất cả thao tác admin với thông tin:
- ID admin thực hiện
- Loại thao tác
- Entity bị tác động
- Giá trị trước/sau thay đổi
- Thời gian
- IP address

### 11.4 Rate Limiting (Khuyến nghị)

- General endpoints: 100 requests/phút
- Batch operations: 10 requests/phút
- Destructive actions: 5 requests/phút

---

## 12. Lộ trình Triển khai

### Phase 1: Foundation (Tuần 1-2)
- [ ] Admin authentication system
- [ ] Admin layout và navigation
- [ ] DataTable component cơ bản
- [ ] Audit logging setup

### Phase 2: User Management (Tuần 3-4)
- [ ] User list với search/filter
- [ ] User detail view (tất cả tabs)
- [ ] Status management
- [ ] Subscription editing
- [ ] Credit management
- [ ] Batch operations

### Phase 3: Financial Management (Tuần 5-6)
- [ ] Revenue dashboard
- [ ] Transaction history
- [ ] Financial reports
- [ ] Export functionality

### Phase 4: Content Management (Tuần 7-9)
- [ ] Test listing và navigation
- [ ] Section editor
- [ ] Question editor (tất cả types)
- [ ] Import wizard (CSV/JSON)
- [ ] Content validation
- [ ] Publish workflow

### Phase 5: Polish & Testing (Tuần 10)
- [ ] Performance optimization
- [ ] Security audit
- [ ] User acceptance testing
- [ ] Documentation hoàn thiện

---

## 13. Implementation Plan (Frontend-First Approach)

> **Cập nhật:** 16/12/2025  
> **Phương pháp:** Xây dựng giao diện trước với mock data, sau đó tích hợp backend

### 13.1 Tổng quan Phương pháp

Thay vì xây dựng đồng thời frontend và backend, chúng ta sẽ:
1. **Xây dựng toàn bộ UI** với mock data trước
2. **Demo và nhận feedback** sớm về UX
3. **Sau đó mới xây dựng backend** và thay thế mock data bằng API calls

**Ưu điểm:**
- Demo được sớm cho stakeholders
- Validate UX trước khi đầu tư vào backend
- Frontend dev và Backend dev có thể work parallel sau Phase 1
- Dễ iterate trên UI mà không ảnh hưởng backend

### 13.2 Cấu trúc Thư mục Admin

```
frontend/src/admin/
├── components/
│   ├── AdminLayout.jsx       # Layout chung (sidebar + header + content)
│   ├── AdminSidebar.jsx      # Sidebar navigation
│   ├── AdminHeader.jsx       # Header với breadcrumb
│   ├── DataTable/            # Reusable table component
│   │   ├── DataTable.jsx
│   │   ├── DataTablePagination.jsx
│   │   └── DataTable.css
│   ├── Charts/               # Chart components (Recharts wrapper)
│   │   ├── LineChart.jsx
│   │   ├── DonutChart.jsx
│   │   └── MetricCard.jsx
│   └── Modals/               # Shared modal components
│       ├── ConfirmModal.jsx
│       └── ImportWizard.jsx
│
├── pages/
│   ├── AdminDashboard.jsx    # Trang tổng quan admin
│   ├── users/                # Module quản lý users
│   │   ├── UserListPage.jsx
│   │   ├── UserDetailPage.jsx
│   │   └── components/
│   │       ├── UserProfileTab.jsx
│   │       ├── UserSubscriptionTab.jsx
│   │       ├── UserCreditsTab.jsx
│   │       ├── UserActivityTab.jsx
│   │       └── UserAuditLogTab.jsx
│   ├── finance/              # Module tài chính
│   │   ├── FinanceDashboard.jsx
│   │   ├── TransactionHistoryPage.jsx
│   │   └── FinanceReportsPage.jsx
│   └── content/              # Module nội dung đề thi
│       ├── ContentListPage.jsx
│       ├── TestEditorPage.jsx
│       └── components/
│           ├── QuestionNavigator.jsx
│           ├── QuestionEditor.jsx
│           └── PublishChecklist.jsx
│
├── mock/
│   ├── mockUsers.js          # Mock data cho users
│   ├── mockFinance.js        # Mock data cho finance
│   ├── mockContent.js        # Mock data cho content
│   └── index.js              # Export tất cả mock data
│
├── hooks/
│   └── useAdminAuth.js       # Hook kiểm tra quyền admin
│
└── css/
    ├── admin.css             # Admin base styles
    ├── admin-variables.css   # CSS variables cho admin theme
    └── components/           # Component-specific styles
```

---

### 13.3 Phase 1: Setup & Admin Shell (1-2 ngày)

#### Mục tiêu
Tạo khung cơ bản cho admin panel, có thể navigate giữa các trang.

#### Tasks

| # | Task | File(s) | Độ ưu tiên |
|---|------|---------|------------|
| 1.1 | Tạo cấu trúc thư mục admin | `frontend/src/admin/` | 🔴 Cao |
| 1.2 | Tạo AdminLayout component | `AdminLayout.jsx` | 🔴 Cao |
| 1.3 | Tạo AdminSidebar với menu items | `AdminSidebar.jsx` | 🔴 Cao |
| 1.4 | Tạo AdminHeader với breadcrumb | `AdminHeader.jsx` | 🔴 Cao |
| 1.5 | Setup admin CSS với theme tím | `admin.css`, `admin-variables.css` | 🔴 Cao |
| 1.6 | Thêm routes `/admin/*` vào App.jsx | `App.jsx` | 🔴 Cao |
| 1.7 | Tạo AdminRouteGuard component | `AdminRouteGuard.jsx` | 🟡 Trung bình |
| 1.8 | Tạo AdminDashboard placeholder | `AdminDashboard.jsx` | 🟡 Trung bình |

#### CSS Variables cho Admin Theme

```css
/* admin-variables.css */
:root {
  /* Admin Primary - Tím để phân biệt với user UI */
  --admin-primary: #8B5CF6;
  --admin-primary-hover: #7C3AED;
  --admin-primary-light: rgba(139, 92, 246, 0.1);
  
  /* Status Colors */
  --admin-success: #10B981;
  --admin-warning: #F59E0B;
  --admin-danger: #EF4444;
  --admin-info: #3B82F6;
  
  /* Background */
  --admin-bg-primary: #0F0F23;
  --admin-bg-secondary: #1A1A2E;
  --admin-bg-card: rgba(255, 255, 255, 0.05);
  
  /* Sidebar */
  --admin-sidebar-width: 260px;
  --admin-sidebar-collapsed-width: 80px;
}
```

#### Deliverables Phase 1
- [ ] Admin shell hoạt động với sidebar navigation
- [ ] Có thể click menu items để navigate (pages rỗng)
- [ ] Theme tím đã được apply
- [ ] Route guard chặn non-admin users (tạm thời hardcode check)

---

### 13.4 Phase 2: Module Quản lý Người dùng - UI (3-4 ngày)

#### Mock Data Structure

```javascript
// mock/mockUsers.js
export const mockUsers = [
  {
    id: "550e8400-e29b-41d4-a716-446655440001",
    username: "quochuu54",
    fullName: "Nguyễn Quốc Hữu",
    email: "quochuu@gmail.com",
    phone: "0901234567",
    address: "Quận 1, TP.HCM",
    avatarUrl: null,
    subscription: "CRAMERICH",
    subscriptionStatus: "ACTIVE",
    subscriptionStartDate: "2025-12-01",
    subscriptionEndDate: "2026-01-01",
    autoRenew: true,
    accountStatus: "ACTIVE",
    statusReason: null,
    credits: 350,
    totalCreditsEarned: 1200,
    totalCreditsSpent: 850,
    createdAt: "2025-01-15T10:30:00Z",
    lastLoginAt: "2025-12-15T14:30:00Z",
    loginCount: 145,
    testsCompleted: 45,
    averageScore: 6.5,
    highestStreak: 12,
    vocabularySaved: 234,
  },
  // ... thêm 20-30 mock users với dữ liệu đa dạng
];

export const mockUserActivities = [
  {
    userId: "550e8400-e29b-41d4-a716-446655440001",
    activities: [
      {
        id: 1,
        type: "TEST_COMPLETED",
        description: "Hoàn thành bài Reading Cambridge 17 Test 1",
        metadata: { score: "32/40", testId: 123 },
        createdAt: "2025-12-15T14:30:00Z",
      },
      // ... more activities
    ],
  },
];

export const mockUserAuditLogs = [
  {
    id: 1,
    adminEmail: "admin1@cramer.edu.vn",
    action: "UPDATE_SUBSCRIPTION",
    targetUserId: "550e8400-e29b-41d4-a716-446655440001",
    oldValue: { subscription: "CRAMERIE" },
    newValue: { subscription: "CRAMERICH" },
    createdAt: "2025-12-14T15:00:00Z",
  },
  // ... more logs
];

// =========================================================================
// QUOTA SYSTEM - Hạn mức sử dụng theo gói đăng ký
// =========================================================================

// Định nghĩa 4 loại hạn mức chính
export const quotaTypes = [
  { key: "basicGrading", label: "Lượt chấm thường", icon: "📝" },
  { key: "advancedGrading", label: "Lượt chấm nâng cao", icon: "🎯" },
  { key: "aiChat", label: "Lượt trò chuyện AI", icon: "💬" },
  { key: "vocabTranslation", label: "Lượt dịch từ vựng", icon: "📖" },
];

// Hạn mức mặc định theo tier (per month)
export const defaultQuotaLimits = {
  FREE: {
    basicGrading: 5,
    advancedGrading: 0,      // Không có quyền truy cập
    aiChat: 10,
    vocabTranslation: 20,
  },
  CRAMERIE: {
    basicGrading: 30,
    advancedGrading: 10,
    aiChat: 100,
    vocabTranslation: 200,
  },
  CRAMERICH: {
    basicGrading: -1,        // -1 = unlimited
    advancedGrading: 50,
    aiChat: -1,              // unlimited
    vocabTranslation: -1,    // unlimited
  },
};

// Mock user quotas với usage và custom limits
export const mockUserQuotas = {
  "user-id-here": {
    basicGrading: { used: 45, limit: -1, customLimit: null },
    advancedGrading: { used: 28, limit: 50, customLimit: 100 }, // Custom tăng limit
    aiChat: { used: 156, limit: -1, customLimit: null },
    vocabTranslation: { used: 89, limit: -1, customLimit: null },
    resetDate: "2026-01-01", // Ngày reset hạn mức
  },
};
```

#### Tasks

| # | Task | File(s) | Độ ưu tiên |
|---|------|---------|------------|
| 2.1 | Tạo DataTable component cơ bản | `DataTable/` | 🔴 Cao |
| 2.2 | Tạo UserListPage với table | `UserListPage.jsx` | 🔴 Cao |
| 2.3 | Implement search bar (filter local) | `UserListPage.jsx` | 🔴 Cao |
| 2.4 | Implement filters (status, subscription) | `UserListPage.jsx` | 🔴 Cao |
| 2.5 | Implement pagination (client-side) | `DataTablePagination.jsx` | 🔴 Cao |
| 2.6 | Tạo UserDetailPage layout với tabs | `UserDetailPage.jsx` | 🔴 Cao |
| 2.7 | Tạo UserProfileTab | `UserProfileTab.jsx` | 🔴 Cao |
| 2.8 | Tạo UserSubscriptionTab | `UserSubscriptionTab.jsx` | 🟡 Trung bình |
| 2.9 | Tạo UserCreditsTab với transaction history | `UserCreditsTab.jsx` | 🟡 Trung bình |
| 2.10 | Tạo UserActivityTab với timeline | `UserActivityTab.jsx` | 🟡 Trung bình |
| 2.11 | Tạo UserAuditLogTab | `UserAuditLogTab.jsx` | 🟡 Trung bình |
| 2.12 | Tạo BanUserModal | `BanUserModal.jsx` | 🟢 Thấp |
| 2.13 | Tạo AddCreditsModal | `AddCreditsModal.jsx` | 🟢 Thấp |
| 2.14 | Tạo BatchActionModal | `BatchActionModal.jsx` | 🟢 Thấp |

#### Deliverables Phase 2
- [ ] Danh sách users hiển thị đầy đủ với mock data
- [ ] Search và filter hoạt động (client-side)
- [ ] Pagination hoạt động
- [ ] User detail page với 5 tabs đầy đủ
- [ ] Các modal để thực hiện actions (UI only, chưa có logic)

---

### 13.5 Phase 3: Module Quản lý Tài chính - UI (2-3 ngày)

#### Mock Data Structure

```javascript
// mock/mockFinance.js
export const mockFinanceOverview = {
  totalRevenue: 125000000,
  totalRevenueChange: 12.5, // % so với kỳ trước
  subscriptionRevenue: 89000000,
  luaRevenue: 36000000,
  
  newSubscriptions: 45,
  subscriptionChange: 8.3,
  churnedSubscriptions: 5,
  
  luaPacksSold: 234,
  luaPacksChange: -2.1,
  averageOrderValue: 153846,
  
  growthRate: 15.2,
};

export const mockRevenueChart = [
  { date: "2025-12-01", total: 4200000, subscriptions: 3000000, lua: 1200000 },
  { date: "2025-12-02", total: 3800000, subscriptions: 2500000, lua: 1300000 },
  // ... 30 ngày data
];

export const mockTransactions = [
  {
    id: "txn-001",
    orderCode: "1234567",
    paymentLinkId: "pl_abc123",
    userId: "550e8400-e29b-41d4-a716-446655440001",
    username: "quochuu54",
    userEmail: "quochuu@gmail.com",
    type: "SUBSCRIPTION_NEW",
    productName: "Cramerich Monthly",
    amount: 199000,
    status: "PAID",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-12-15T14:30:00Z",
    paidAt: "2025-12-15T14:32:15Z",
  },
  // ... more transactions
];

export const mockTopSpenders = [
  { rank: 1, userId: "uuid-1", username: "vip_user1", totalSpent: 2500000 },
  { rank: 2, userId: "uuid-2", username: "vip_user2", totalSpent: 1890000 },
  // ... top 10
];
```

#### Tasks

| # | Task | File(s) | Độ ưu tiên |
|---|------|---------|------------|
| 3.1 | Tạo MetricCard component | `MetricCard.jsx` | 🔴 Cao |
| 3.2 | Tạo FinanceDashboard với 4 metric cards | `FinanceDashboard.jsx` | 🔴 Cao |
| 3.3 | Tạo time filter component | `TimeFilter.jsx` | 🔴 Cao |
| 3.4 | Tạo LineChart component (Recharts) | `LineChart.jsx` | 🔴 Cao |
| 3.5 | Tạo DonutChart component (Recharts) | `DonutChart.jsx` | 🟡 Trung bình |
| 3.6 | Tạo TopSpendersTable | `TopSpendersTable.jsx` | 🟡 Trung bình |
| 3.7 | Tạo TransactionHistoryPage | `TransactionHistoryPage.jsx` | 🔴 Cao |
| 3.8 | Tạo StatusBadge component | `StatusBadge.jsx` | 🔴 Cao |
| 3.9 | Tạo TransactionDetailDrawer | `TransactionDetailDrawer.jsx` | 🟡 Trung bình |
| 3.10 | Tạo FinanceReportsPage | `FinanceReportsPage.jsx` | 🟢 Thấp |
| 3.11 | Tạo ExportButton (placeholder) | `ExportButton.jsx` | 🟢 Thấp |

#### Deliverables Phase 3
- [ ] Finance dashboard với charts và metrics
- [ ] Transaction history với filters và pagination
- [ ] Transaction detail drawer
- [ ] Reports page (UI skeleton)

---

### 13.6 Phase 4: Module Quản lý Nội dung - UI (4-5 ngày)

#### Mock Data Structure

```javascript
// mock/mockContent.js
export const mockTopics = [
  {
    id: 1,
    source: "Cambridge 17",
    displayName: "Cambridge IELTS 17",
    testsCount: 4,
    tests: [
      {
        number: 1,
        status: "PUBLISHED",
        publishedAt: "2025-11-01T00:00:00Z",
        skills: {
          reading: { 
            questionCount: 40, 
            status: "complete",
            sections: [
              { id: 1, name: "Passage 1", questionRange: "1-13" },
              { id: 2, name: "Passage 2", questionRange: "14-26" },
              { id: 3, name: "Passage 3", questionRange: "27-40" },
            ]
          },
          listening: { 
            questionCount: 40, 
            status: "complete",
            sections: [
              { id: 4, name: "Part 1", questionRange: "1-10", hasAudio: true },
              { id: 5, name: "Part 2", questionRange: "11-20", hasAudio: true },
              { id: 6, name: "Part 3", questionRange: "21-30", hasAudio: true },
              { id: 7, name: "Part 4", questionRange: "31-40", hasAudio: true },
            ]
          },
          writing: { status: "complete", hasTask1: true, hasTask2: true },
          speaking: { status: "draft" },
        },
      },
      // ... Test 2, 3, 4
    ],
  },
  // ... more topics (Cambridge 18, Real Tests, etc.)
];

export const mockQuestions = [
  {
    id: 1,
    sectionId: 1,
    questionNumber: 1,
    questionType: "FILL_IN_BLANK",
    text: "The ____ of London has grown significantly over the past century.",
    correctAnswers: ["population"],
    wordLimit: "ONE WORD ONLY",
    explanation: "Paragraph 1 mentions 'the population of London'...",
    hasValidation: true,
    validationErrors: [],
  },
  {
    id: 2,
    sectionId: 1,
    questionNumber: 2,
    questionType: "TRUE_FALSE_NOT_GIVEN",
    text: "The railway system was expensive to build.",
    correctAnswers: ["TRUE"],
    explanation: "Paragraph 2 states that 'the construction costs were enormous'...",
    hasValidation: true,
    validationErrors: [],
  },
  // ... more questions với đủ 13 types
];
```

#### Tasks

| # | Task | File(s) | Độ ưu tiên |
|---|------|---------|------------|
| 4.1 | Tạo ContentListPage với Tree View | `ContentListPage.jsx` | 🔴 Cao |
| 4.2 | Implement expand/collapse tree nodes | `TreeNode.jsx` | 🔴 Cao |
| 4.3 | Tạo Grid View alternative | `ContentGridView.jsx` | 🟡 Trung bình |
| 4.4 | Tạo TestEditorPage layout | `TestEditorPage.jsx` | 🔴 Cao |
| 4.5 | Tạo skill tabs (Reading/Listening/Writing/Speaking) | `TestEditorPage.jsx` | 🔴 Cao |
| 4.6 | Tạo QuestionNavigator (left panel) | `QuestionNavigator.jsx` | 🔴 Cao |
| 4.7 | Tạo QuestionEditor base component | `QuestionEditor.jsx` | 🔴 Cao |
| 4.8 | Implement FILL_IN_BLANK editor | `editors/FillInBlankEditor.jsx` | 🔴 Cao |
| 4.9 | Implement TRUE_FALSE_NOT_GIVEN editor | `editors/TrueFalseEditor.jsx` | 🔴 Cao |
| 4.10 | Implement MULTIPLE_CHOICE editor | `editors/MultipleChoiceEditor.jsx` | 🔴 Cao |
| 4.11 | Implement các editor còn lại (10 types) | `editors/` | 🟡 Trung bình |
| 4.12 | Tạo QuestionPreview modal | `QuestionPreview.jsx` | 🟡 Trung bình |
| 4.13 | Tạo SectionEditor (cho passage/audio) | `SectionEditor.jsx` | 🟡 Trung bình |
| 4.14 | Tạo ImportWizard multi-step | `ImportWizard.jsx` | 🟡 Trung bình |
| 4.15 | Tạo PublishChecklist modal | `PublishChecklist.jsx` | 🟢 Thấp |

#### Deliverables Phase 4
- [ ] Content list với Tree/Grid view
- [ ] Test editor với navigation và form editors
- [ ] Support tất cả 13 question types
- [ ] Question preview
- [ ] Import wizard UI (không có actual import logic)
- [ ] Publish checklist UI

---

### 13.7 Phase 5: Shared Components (Song song với các phases khác)

Các components được xây dựng dần trong quá trình làm các phases:

| Component | Mô tả | Sử dụng ở |
|-----------|-------|-----------|
| `DataTable` | Bảng với sort, filter, pagination, selection | Users, Transactions, Audit logs |
| `MetricCard` | Card hiển thị số liệu với trend indicator | Finance Dashboard |
| `StatusBadge` | Badge với màu theo status | Transactions, Users, Content |
| `LineChart` | Wrapper Recharts line chart | Finance Dashboard |
| `DonutChart` | Wrapper Recharts donut chart | Finance Dashboard |
| `ConfirmModal` | Modal xác nhận action | Ban user, Delete, Publish |
| `DateRangePicker` | Chọn khoảng ngày | Filters, Reports |
| `SearchInput` | Input tìm kiếm với debounce | User list, Transaction list |
| `Toast` | Thông báo success/error | Sau mọi action |

---

### 13.8 Phase 6: Backend Integration (SAU KHI UI HOÀN THÀNH)

Sau khi tất cả UI đã sẵn sàng với mock data:

#### 6.1 Database Changes
- [ ] Tạo bảng `admin_audit_log`
- [ ] Tạo bảng `content_publish_status`
- [ ] Thêm columns vào bảng `profiles` (account_status, status_reason, etc.)
- [ ] Tạo indexes cho các columns thường query

#### 6.2 Backend APIs
- [ ] Tạo `AdminAuthFilter` kiểm tra admin quyền
- [ ] Implement tất cả endpoints trong Section 9 (Admin APIs)
- [ ] Thêm audit logging cho mọi admin action

#### 6.3 Frontend Integration
- [ ] Tạo `adminApi.js` với các API functions
- [ ] Tạo `useAdminStore.js` Zustand store
- [ ] Thay thế mock data bằng API calls
- [ ] Implement loading states và error handling
- [ ] Kết nối actual authentication

---

### 13.9 Timeline Tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  FRONTEND-FIRST IMPLEMENTATION TIMELINE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Phase 1: Setup & Shell          ████░░░░░░░░░░░░░░░░░░░░░░  (1-2 ngày)    │
│  Phase 2: User Management UI     ░░░░████████░░░░░░░░░░░░░░  (3-4 ngày)    │
│  Phase 3: Finance UI             ░░░░░░░░░░░░█████░░░░░░░░░  (2-3 ngày)    │
│  Phase 4: Content UI             ░░░░░░░░░░░░░░░░░████████░  (4-5 ngày)    │
│  Phase 5: Shared Components      ████████████████████████░░  (song song)   │
│                                                                             │
│  ────────────────────────────────────────────────────────────────────────  │
│  TỔNG THỜI GIAN UI: ~10-14 ngày làm việc                                   │
│  ────────────────────────────────────────────────────────────────────────  │
│                                                                             │
│  Phase 6: Backend Integration    ░░░░░░░░░░░░░░░░░░░░░░░░██████  (TBD)     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 13.10 Checklist Theo Dõi Tiến Độ

#### Phase 1: Setup & Admin Shell
- [ ] Tạo cấu trúc thư mục `frontend/src/admin/`
- [ ] `AdminLayout.jsx` hoàn thành
- [ ] `AdminSidebar.jsx` hoàn thành
- [ ] `AdminHeader.jsx` hoàn thành
- [ ] `admin.css` và `admin-variables.css` hoàn thành
- [ ] Routes `/admin/*` đã thêm vào `App.jsx`
- [ ] `AdminRouteGuard.jsx` hoàn thành
- [ ] `AdminDashboard.jsx` placeholder hoàn thành
- **Trạng thái:** ⏳ Chưa bắt đầu

#### Phase 2: Module Quản lý Người dùng
- [ ] Mock data users đã tạo
- [ ] `DataTable` component hoàn thành
- [ ] `UserListPage.jsx` hoàn thành
- [ ] `UserDetailPage.jsx` với 5 tabs hoàn thành
- [ ] Các modals (Ban, Credits, Batch) hoàn thành
- **Trạng thái:** ⏳ Chưa bắt đầu

#### Phase 3: Module Quản lý Tài chính
- [ ] Mock data finance đã tạo
- [ ] `FinanceDashboard.jsx` hoàn thành
- [ ] Charts (Line, Donut) hoàn thành
- [ ] `TransactionHistoryPage.jsx` hoàn thành
- [ ] `TransactionDetailDrawer.jsx` hoàn thành
- [ ] `FinanceReportsPage.jsx` hoàn thành
- **Trạng thái:** ⏳ Chưa bắt đầu

#### Phase 4: Module Quản lý Nội dung
- [ ] Mock data content đã tạo
- [ ] `ContentListPage.jsx` (Tree + Grid view) hoàn thành
- [ ] `TestEditorPage.jsx` hoàn thành
- [ ] `QuestionNavigator.jsx` hoàn thành
- [ ] `QuestionEditor.jsx` với 13 types hoàn thành
- [ ] `ImportWizard.jsx` hoàn thành
- [ ] `PublishChecklist.jsx` hoàn thành
- **Trạng thái:** ⏳ Chưa bắt đầu

#### Phase 6: Backend Integration
- [ ] Database schema updated
- [ ] Backend APIs implemented
- [ ] Frontend connected to APIs
- [ ] Authentication integrated
- **Trạng thái:** ⏳ Chưa bắt đầu

---

**KẾT THÚC TÀI LIỆU**

