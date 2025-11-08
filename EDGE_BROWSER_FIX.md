# Fix: Edge Browser Stuck on "Initializing Application..."

## 🔴 Vấn Đề

Khi chạy ứng dụng trên **Microsoft Edge**, bạn có thể gặp lỗi:
- Màn hình bị stuck ở "Initializing Application..."
- Console hiển thị lỗi: `SecurityError: The request was denied`
- Ứng dụng không thể đăng nhập

## 🔍 Nguyên Nhân

Edge (và một số trình duyệt khác) có cài đặt privacy nghiêm ngặt, chặn **third-party cookies** và **localStorage** - điều mà Supabase Authentication cần để hoạt động.

## ✅ Giải Pháp

### **Option 1: Bật Cookies Cho Trang (Khuyến Nghị)**

1. Mở Edge và truy cập `http://localhost:3000`
2. Click vào **biểu tượng khóa (🔒)** bên trái thanh địa chỉ
3. Click **"Cookies and site permissions"**
4. Đảm bảo **"Cookies"** được set thành **"Allowed"**
5. Reload trang (Ctrl + R)

### **Option 2: Tắt "Tracking Prevention" Cho Localhost**

1. Mở Edge Settings: `edge://settings/privacy`
2. Tìm **"Tracking prevention"**
3. Click **"Exceptions"**
4. Thêm `http://localhost:3000` vào danh sách ngoại lệ
5. Reload trang

### **Option 3: Chuyển Sang InPrivate Mode Với Cookies Enabled**

1. Mở Edge InPrivate window (Ctrl + Shift + N)
2. Vào Settings trong InPrivate window
3. Đảm bảo **"Block third-party cookies"** là **OFF**
4. Truy cập `http://localhost:3000`

### **Option 4: Sử Dụng Trình Duyệt Khác (Dễ Nhất)**

Ứng dụng hoạt động tốt trên:
- ✅ **Google Chrome**
- ✅ **Mozilla Firefox**
- ✅ **Zen Browser**
- ✅ **Brave**

---

## 🛠️ Technical Details

Ứng dụng đã được cập nhật với:
1. **Fallback storage**: Nếu localStorage bị chặn, sử dụng in-memory storage
2. **Error handling**: Hiển thị thông báo lỗi rõ ràng thay vì stuck
3. **Browser detection**: Tự động detect và xử lý browser security issues

---

## 📝 Ghi Chú Cho Developer

Nếu bạn đang develop và gặp vấn đề tương tự:

```javascript
// Code đã được update trong supabaseClient.js
const supabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    storage: isLocalStorageAvailable() ? undefined : memoryStorage,
    persistSession: isLocalStorageAvailable(),
  },
});
```

Và trong `AuthContext.jsx`:
```javascript
try {
  // Auth setup
} catch (error) {
  if (error.name === 'SecurityError') {
    setError('Browser blocking authentication. Enable cookies or try different browser.');
  }
}
```

---

## ❓ Vẫn Gặp Vấn Đề?

Nếu sau khi thử tất cả các cách trên mà vẫn không được:

1. **Clear browser cache**: Ctrl + Shift + Delete
2. **Disable all extensions** temporarily
3. **Check console logs**: F12 → Console tab
4. **Report issue** with console error logs

---

**Cập nhật lần cuối:** November 8, 2025
