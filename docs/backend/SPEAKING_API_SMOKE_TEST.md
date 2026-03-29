# Speaking API Smoke Test

## Mục tiêu

Tài liệu này dùng để smoke test luồng Speaking runtime sau khi official Speaking data đã được backfill vào shared hierarchy.

Official target hiện tại:

- `cam17`
- `tests.id = 1`
- bank contract: `30 / 1 / 15`

## Chuẩn bị

1. Backend chạy tại `http://localhost:8080`
2. Có JWT Supabase user hợp lệ
3. User có đủ Lúa để tạo session
4. Import file Postman collection:
   - `docs/backend/postman/Speaking_API.postman_collection.json`

## Collection variables cần set

- `baseUrl` = `http://localhost:8080`
- `jwt` = Supabase access token của user test
- `officialSpeakingTestId` = `1`

## Luồng smoke test khuyến nghị

### Bước 1 — Create session

Request:

- `1. Create session (FULL)`

Kỳ vọng:

- HTTP `201`
- response có `sessionId`
- response có `sessionBlueprint`
- response có `turns`
- collection tự set:
  - `sessionId`
  - `part1TurnIndex`
  - `part1SourceQuestionId`
  - `part1QuestionSnapshot`
  - `part2TurnIndex`
  - `part2SourceQuestionId`
  - `part2QuestionSnapshot`

### Bước 2 — Get session

Request:

- `2. Get session`

Kỳ vọng:

- HTTP `200`
- session thuộc đúng user
- `testId = 1`

### Bước 3 — Save transcript cho Part 1

Request:

- `3. Save transcript (Part 1 sample)`

Kỳ vọng:

- HTTP `200`
- trả về `transcriptId`
- `status = saved`

### Bước 4 — Save transcript cho Part 2

Request:

- `4. Save transcript (Part 2 sample for FULL flow)`

Kỳ vọng:

- HTTP `200`
- nếu FULL flow đang defer Part 3, backend có thể materialize Part 3 sau bước này

### Bước 5 — Poll lại session

Request:

- `2. Get session`

Kỳ vọng:

- nếu backend materialize deferred Part 3, `sessionBlueprint` sẽ chứa turns Part 3 đã được chọn

### Bước 6 — Get history

Request:

- `9. Get history`

Kỳ vọng:

- HTTP `200`
- session mới tạo xuất hiện trong lịch sử user

## Lưu ý về complete

`Complete session` chỉ thành công khi toàn bộ selected turns trong `sessionBlueprint` đã có transcript.

Vì session FULL có nhiều turns Part 1 và có thể có thêm Part 3 sau khi lưu transcript Part 2, request `5. Complete session` thường sẽ trả lỗi nếu bạn mới chỉ save 1-2 transcript mẫu.

Điều này là đúng theo contract hiện tại.

## Khi nào coi là smoke test data pass

Data backfill được coi là pass khi:

1. `Create session` thành công trên `testId = 1`
2. `Get session` trả về blueprint hợp lệ
3. Có thể save transcript cho ít nhất 1 turn hợp lệ
4. `Get history` trả về session của user

## Các lỗi thường gặp

### 1. `Published Speaking test not found`

Nguyên nhân thường là backend đang trỏ sai DB hoặc official speaking data chưa có ở DB hiện tại.

### 2. `requires at least ... published PART_x prompts`

Nguyên nhân là bank dữ liệu ở DB đang thiếu hoặc chưa đúng môi trường vừa backfill.

### 3. `Insufficient Lúa balance`

Đây là lỗi quota/credit, không phải lỗi dữ liệu Speaking.

### 4. `turnIndex` / `questionSnapshot` mismatch

Do request transcript không dùng đúng turn đã được backend chốt trong `sessionBlueprint`.
