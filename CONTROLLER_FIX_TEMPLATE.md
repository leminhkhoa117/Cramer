# 🎯 REUSABLE CONTROLLER FIX & TEST TEMPLATE
## Spring Boot Controller Repair Pattern - 2026-01-24

---

## 📋 TABLE OF CONTENTS
1. [Quick Fix Checklist](#quick-fix-checklist)
2. [Controller Fix Template](#controller-fix-template)
3. [Test Template](#test-template)
4. [Validation Patterns](#validation-patterns)
5. [Common Issues & Solutions](#common-issues--solutions)
6. [Examples](#examples)

---

## ✅ QUICK FIX CHECKLIST

For each controller, follow this checklist:

- [ ] **Extend BaseController**
- [ ] **Replace all `UUID.fromString(authentication.getName())` with `getCurrentUserId(authentication)`**
- [ ] **Add pagination validation** (`@Min(0)` on page, `@Min(1) @Max(100)` on size)
- [ ] **Add path variable validation** (e.g., `@Min(1)` on IDs)
- [ ] **Add `@Valid` to request body DTOs**
- [ ] **Remove redundant authentication checks**
- [ ] **Fix any potential NPE risks**
- [ ] **Add proper logging**
- [ ] **Update test class**

---

## 🔧 CONTROLLER FIX TEMPLATE

### Step 1: Make Controller Extend BaseController

**BEFORE:**
```java
@RestController
@RequestMapping("/api/your-endpoint")
@Tag(name = "Your API", description = "API description")
public class YourController {

    private static final Logger logger = LoggerFactory.getLogger(YourController.class);

    private final YourService yourService;

    @Autowired
    public YourController(YourService yourService) {
        this.yourService = yourService;
    }

    // ❌ UNSAFE: Custom UUID parsing
    private UUID getCurrentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
```

**AFTER:**
```java
@RestController
@RequestMapping("/api/your-endpoint")
@Tag(name = "Your API", description = "API description")
public class YourController extends BaseController {  // ✅ EXTENDS BaseController

    private static final Logger logger = LoggerFactory.getLogger(YourController.class);

    private final YourService yourService;

    @Autowired
    public YourController(YourService yourService) {
        this.yourService = yourService;
    }

    // ✅ REMOVE custom getCurrentUserId() - inherited from BaseController
}
```

---

### Step 2: Replace UUID Parsing Calls

**FIND & REPLACE:**

Find:
```java
UUID userId = UUID.fromString(authentication.getName());
```

Replace with:
```java
UUID userId = getCurrentUserId(authentication);
```

**TIP:** Use your IDE's "Find & Replace All" in the file for speed!

---

### Step 3: Add Validation to Endpoints

#### **Pattern A: GET with Pagination**

**BEFORE:**
```java
@GetMapping
public ResponseEntity<Page<YourDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication) {

    UUID userId = UUID.fromString(authentication.getName()); // ❌
    Page<YourDTO> results = yourService.getAll(userId, page, size);
    return ResponseEntity.ok(results);
}
```

**AFTER:**
```java
@GetMapping
public ResponseEntity<Page<YourDTO>> getAll(
        @RequestParam(defaultValue = "0") @Min(0) int page,  // ✅ Validation
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,  // ✅ Validation
        Authentication authentication) {

    UUID userId = getCurrentUserId(authentication);  // ✅ Safe
    int cappedSize = Math.min(size, 100);  // ✅ Manual cap
    Page<YourDTO> results = yourService.getAll(userId, page, cappedSize);
    return ResponseEntity.ok(results);
}
```

**Annotations to Add:**
- `@Min(0)` on `page` parameter
- `@Min(1) @Max(100)` on `size` parameter
- Manual cap: `int cappedSize = Math.min(size, 100);`

---

#### **Pattern B: POST with Request Body**

**BEFORE:**
```java
@PostMapping
public ResponseEntity<YourDTO> create(
        @RequestBody YourCreateDTO request,  // ❌ Missing @Valid
        Authentication authentication) {

    UUID userId = UUID.fromString(authentication.getName());  // ❌
    YourDTO created = yourService.create(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

**AFTER:**
```java
@PostMapping
public ResponseEntity<YourDTO> create(
        @Valid @RequestBody YourCreateDTO request,  // ✅ Added @Valid
        Authentication authentication) {

    UUID userId = getCurrentUserId(authentication);  // ✅ Safe
    YourDTO created = yourService.create(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

**Add:** `@Valid` before `@RequestBody`

---

#### **Pattern C: GET by ID with IDOR Protection**

**BEFORE:**
```java
@GetMapping("/{id}")
public ResponseEntity<YourDTO> getById(
        @PathVariable Long id,  // ❌ No validation
        Authentication authentication) {

    UUID userId = UUID.fromString(authentication.getName());  // ❌

    YourDTO item = yourService.getById(id);

    // ❌ POTENTIAL NPE: What if item is null?
    if (!item.getUserId().equals(userId)) {
        return ResponseEntity.status(403).build();
    }

    return ResponseEntity.ok(item);
}
```

**AFTER:**
```java
@GetMapping("/{id}")
public ResponseEntity<YourDTO> getById(
        @PathVariable @Min(1) Long id,  // ✅ Validation
        Authentication authentication) {

    UUID userId = getCurrentUserId(authentication);  // ✅ Safe

    YourDTO item = yourService.getById(id);

    // ✅ Null check BEFORE accessing properties
    if (item == null) {
        logger.warn("Item not found: {}", id);
        return ResponseEntity.notFound().build();
    }

    // ✅ IDOR protection
    if (!item.getUserId().equals(userId)) {
        logger.warn("🚨 IDOR attempt: User {} tried to access item {} owned by {}",
                   userId, id, item.getUserId());
        return ResponseEntity.status(403).build();
    }

    return ResponseEntity.ok(item);
}
```

**Critical:** Always check for `null` BEFORE accessing object properties!

---

#### **Pattern D: DELETE with Ownership Check**

**BEFORE:**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(
        @PathVariable Long id,
        Authentication authentication) {

    UUID userId = UUID.fromString(authentication.getName());  // ❌
    yourService.delete(id, userId);
    return ResponseEntity.noContent().build();
}
```

**AFTER:**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(
        @PathVariable @Min(1) Long id,  // ✅ Validation
        Authentication authentication) {

    UUID userId = getCurrentUserId(authentication);  // ✅ Safe
    yourService.delete(id, userId);  // Service handles ownership check
    return ResponseEntity.noContent().build();
}
```

---

#### **Pattern E: PUT/PATCH with Update**

**BEFORE:**
```java
@PutMapping("/{id}")
public ResponseEntity<YourDTO> update(
        @PathVariable Long id,
        @RequestBody YourUpdateDTO updateDTO,  // ❌ Missing @Valid
        Authentication authentication) {

    UUID userId = UUID.fromString(authentication.getName());  // ❌
    YourDTO updated = yourService.update(id, userId, updateDTO);
    return ResponseEntity.ok(updated);
}
```

**AFTER:**
```java
@PutMapping("/{id}")
public ResponseEntity<YourDTO> update(
        @PathVariable @Min(1) Long id,  // ✅ Validation
        @Valid @RequestBody YourUpdateDTO updateDTO,  // ✅ Added @Valid
        Authentication authentication) {

    UUID userId = getCurrentUserId(authentication);  // ✅ Safe
    YourDTO updated = yourService.update(id, userId, updateDTO);
    return ResponseEntity.ok(updated);
}
```

---

### Step 4: Remove Redundant Authentication Checks

**BEFORE (Redundant):**
```java
@PostMapping("/something")
public ResponseEntity<?> doSomething(Authentication authentication) {

    // ❌ REDUNDANT: Spring Security already handles this
    if (authentication == null || !authentication.isAuthenticated()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    UUID userId = getCurrentUserId(authentication);
    // ... rest of logic
}
```

**AFTER (Clean):**
```java
@PostMapping("/something")
public ResponseEntity<?> doSomething(Authentication authentication) {

    // ✅ Just use it - Spring Security guarantees it's authenticated
    UUID userId = getCurrentUserId(authentication);
    // ... rest of logic
}
```

**Rule:** If the endpoint requires authentication via Spring Security config, you don't need manual checks!

---

### Step 5: Fix Enum/Pattern Validation

**BEFORE:**
```java
@GetMapping("/filter")
public ResponseEntity<?> filterByType(
        @RequestParam String type,  // ❌ No validation
        Authentication authentication) {

    // User could send anything: "all", "active", "HACKED", "'; DROP TABLE--"
    // ...
}
```

**AFTER:**
```java
@GetMapping("/filter")
public ResponseEntity<?> filterByType(
        @RequestParam @Pattern(regexp = "all|active|inactive") String type,  // ✅ Validation
        Authentication authentication) {

    // Only "all", "active", or "inactive" allowed
    // ...
}
```

**Common Patterns:**
- Status filters: `@Pattern(regexp = "all|active|inactive|pending")`
- Type filters: `@Pattern(regexp = "all|earn|spend")`
- Skills: `@Pattern(regexp = "READING|LISTENING|WRITING|SPEAKING")`

---

## 🧪 TEST TEMPLATE

### Full Test Class Template

```java
package com.cramer.controller;

import com.cramer.dto.YourDTO;
import com.cramer.dto.YourCreateDTO;
import com.cramer.service.YourService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for YourController.
 * Uses @WebMvcTest for fast, focused controller testing.
 *
 * @author Your Team
 * @since 2026-01-24
 */
@WebMvcTest(YourController.class)
@DisplayName("YourController Tests")
class YourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private YourService yourService;

    private static final String BASE_URL = "/api/your-endpoint";
    private static final UUID TEST_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    // ========================================
    // GET /api/your-endpoint (List with Pagination)
    // ========================================

    @Nested
    @DisplayName("GET /api/your-endpoint - List Items")
    class GetAllTests {

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void getAll_noAuth_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());

            verify(yourService, never()).getAll(any(), anyInt(), anyInt());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return paginated list with default params")
        void getAll_authenticated_returnsPage() throws Exception {
            // Arrange
            YourDTO item1 = YourDTO.builder().id(1L).name("Item 1").build();
            YourDTO item2 = YourDTO.builder().id(2L).name("Item 2").build();
            Page<YourDTO> page = new PageImpl<>(Arrays.asList(item1, item2));

            when(yourService.getAll(eq(TEST_USER_ID), eq(0), eq(20)))
                    .thenReturn(page);

            // Act & Assert
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("Item 1"))
                    .andExpect(jsonPath("$.content[1].name").value("Item 2"));

            verify(yourService, times(1)).getAll(TEST_USER_ID, 0, 20);
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should validate negative page number")
        void getAll_negativePage_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest());

            verify(yourService, never()).getAll(any(), anyInt(), anyInt());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should cap size at maximum limit")
        void getAll_excessiveSize_capsAt100() throws Exception {
            // Arrange
            when(yourService.getAll(eq(TEST_USER_ID), eq(0), eq(100)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // Act & Assert
            mockMvc.perform(get(BASE_URL)
                            .param("size", "500"))  // Requesting 500
                    .andExpect(status().isOk());

            // Verify service received capped size (100)
            verify(yourService, times(1)).getAll(TEST_USER_ID, 0, 100);
        }

        @Test
        @WithMockUser(username = "invalid-uuid")
        @DisplayName("Should return 400 when user ID is invalid UUID")
        void getAll_invalidUserId_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isBadRequest());

            verify(yourService, never()).getAll(any(), anyInt(), anyInt());
        }
    }

    // ========================================
    // POST /api/your-endpoint (Create)
    // ========================================

    @Nested
    @DisplayName("POST /api/your-endpoint - Create Item")
    class CreateTests {

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void create_noAuth_returns401() throws Exception {
            YourCreateDTO request = YourCreateDTO.builder()
                    .name("Test Item")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(yourService, never()).create(any(), any());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should create item when valid request")
        void create_validRequest_returns201() throws Exception {
            // Arrange
            YourCreateDTO request = YourCreateDTO.builder()
                    .name("New Item")
                    .description("Description")
                    .build();

            YourDTO created = YourDTO.builder()
                    .id(1L)
                    .name("New Item")
                    .description("Description")
                    .build();

            when(yourService.create(eq(TEST_USER_ID), any(YourCreateDTO.class)))
                    .thenReturn(created);

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("New Item"));

            verify(yourService, times(1)).create(eq(TEST_USER_ID), any());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return 400 when name is blank")
        void create_blankName_returns400() throws Exception {
            // Arrange
            YourCreateDTO request = YourCreateDTO.builder()
                    .name("   ")  // Blank name
                    .build();

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(yourService, never()).create(any(), any());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return 400 when request body is null")
        void create_nullBody_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(yourService, never()).create(any(), any());
        }
    }

    // ========================================
    // GET /api/your-endpoint/{id} (Get by ID)
    // ========================================

    @Nested
    @DisplayName("GET /api/your-endpoint/{id} - Get by ID")
    class GetByIdTests {

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void getById_noAuth_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isUnauthorized());

            verify(yourService, never()).getById(anyLong());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return item when found and user owns it")
        void getById_foundAndOwned_returns200() throws Exception {
            // Arrange
            YourDTO item = YourDTO.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .name("My Item")
                    .build();

            when(yourService.getById(1L)).thenReturn(item);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("My Item"));

            verify(yourService, times(1)).getById(1L);
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return 404 when item not found")
        void getById_notFound_returns404() throws Exception {
            // Arrange
            when(yourService.getById(999L)).thenReturn(null);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound());

            verify(yourService, times(1)).getById(999L);
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return 403 when user doesn't own the item (IDOR protection)")
        void getById_notOwned_returns403() throws Exception {
            // Arrange
            UUID otherUserId = UUID.fromString("999e4567-e89b-12d3-a456-426614174999");
            YourDTO item = YourDTO.builder()
                    .id(1L)
                    .userId(otherUserId)  // Different owner
                    .name("Someone else's item")
                    .build();

            when(yourService.getById(1L)).thenReturn(item);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());

            verify(yourService, times(1)).getById(1L);
        }
    }

    // ========================================
    // PUT /api/your-endpoint/{id} (Update)
    // ========================================

    @Nested
    @DisplayName("PUT /api/your-endpoint/{id} - Update Item")
    class UpdateTests {

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should update item when valid request")
        void update_validRequest_returns200() throws Exception {
            // Arrange
            YourUpdateDTO request = YourUpdateDTO.builder()
                    .name("Updated Name")
                    .build();

            YourDTO updated = YourDTO.builder()
                    .id(1L)
                    .name("Updated Name")
                    .build();

            when(yourService.update(eq(1L), eq(TEST_USER_ID), any()))
                    .thenReturn(updated);

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"));

            verify(yourService, times(1)).update(eq(1L), eq(TEST_USER_ID), any());
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should return 400 when validation fails")
        void update_invalidData_returns400() throws Exception {
            // Arrange
            YourUpdateDTO request = YourUpdateDTO.builder()
                    .name("")  // Invalid: empty name
                    .build();

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(yourService, never()).update(anyLong(), any(), any());
        }
    }

    // ========================================
    // DELETE /api/your-endpoint/{id} (Delete)
    // ========================================

    @Nested
    @DisplayName("DELETE /api/your-endpoint/{id} - Delete Item")
    class DeleteTests {

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
        @DisplayName("Should delete item when user owns it")
        void delete_owned_returns204() throws Exception {
            // Arrange
            doNothing().when(yourService).delete(1L, TEST_USER_ID);

            // Act & Assert
            mockMvc.perform(delete(BASE_URL + "/1")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(yourService, times(1)).delete(1L, TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void delete_noAuth_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(yourService, never()).delete(anyLong(), any());
        }
    }
}
```

---

## 📝 VALIDATION PATTERNS REFERENCE

### Common Validation Annotations

```java
// ===== String Validation =====
@NotBlank(message = "Field cannot be blank")
@NotEmpty(message = "Field cannot be empty")
@Size(min = 1, max = 255, message = "Must be between 1 and 255 characters")
@Email(message = "Must be valid email")
@Pattern(regexp = "regex", message = "Must match pattern")

// ===== Number Validation =====
@Min(value = 0, message = "Must be at least 0")
@Max(value = 100, message = "Must be at most 100")
@Positive(message = "Must be positive")
@PositiveOrZero(message = "Must be positive or zero")

// ===== Pagination =====
@RequestParam(defaultValue = "0") @Min(0) int page
@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size

// ===== Path Variables =====
@PathVariable @Min(1) Long id
@PathVariable @Pattern(regexp = "^[a-z0-9-]+$") String code

// ===== Request Body =====
@Valid @RequestBody YourDTO dto

// ===== Enum Validation =====
@Pattern(regexp = "all|active|inactive")
@Pattern(regexp = "READING|LISTENING|WRITING|SPEAKING")
```

---

## 🚨 COMMON ISSUES & SOLUTIONS

### Issue 1: UUID Parsing Without Error Handling
```java
// ❌ BAD
UUID userId = UUID.fromString(authentication.getName());

// ✅ GOOD
UUID userId = getCurrentUserId(authentication);
```

### Issue 2: NPE Risk in IDOR Checks
```java
// ❌ BAD
YourDTO item = service.getById(id);
if (!item.getUserId().equals(userId)) { // NPE if item is null!
    return ResponseEntity.status(403).build();
}

// ✅ GOOD
YourDTO item = service.getById(id);
if (item == null) {
    return ResponseEntity.notFound().build();
}
if (!item.getUserId().equals(userId)) {
    return ResponseEntity.status(403).build();
}
```

### Issue 3: Missing Validation
```java
// ❌ BAD
@PostMapping
public ResponseEntity<?> create(@RequestBody YourDTO dto) { ... }

// ✅ GOOD
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody YourDTO dto) { ... }
```

### Issue 4: Redundant Auth Checks
```java
// ❌ BAD - Spring Security already handles this
if (authentication == null || !authentication.isAuthenticated()) {
    return ResponseEntity.status(401).build();
}

// ✅ GOOD - Trust Spring Security
UUID userId = getCurrentUserId(authentication);
```

### Issue 5: No Pagination Limits
```java
// ❌ BAD - User can request size=999999
public ResponseEntity<?> getAll(@RequestParam int size) {
    return service.getAll(size);
}

// ✅ GOOD - Capped and validated
public ResponseEntity<?> getAll(
    @RequestParam @Min(1) @Max(100) int size) {
    int cappedSize = Math.min(size, 100);
    return service.getAll(cappedSize);
}
```

---

## 💡 EXAMPLES

### Example 1: Full Controller Fix (Before & After)

**BEFORE:**
```java
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemDTO> create(
            @RequestBody ItemCreateDTO request,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        ItemDTO created = itemService.create(userId, request);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<Page<ItemDTO>> getAll(
            @RequestParam int page,
            @RequestParam int size,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(itemService.getAll(userId, page, size));
    }
}
```

**AFTER:**
```java
@RestController
@RequestMapping("/api/items")
public class ItemController extends BaseController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemDTO> create(
            @Valid @RequestBody ItemCreateDTO request,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        ItemDTO created = itemService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<ItemDTO>> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {

        UUID userId = getCurrentUserId(authentication);
        int cappedSize = Math.min(size, 100);
        return ResponseEntity.ok(itemService.getAll(userId, page, cappedSize));
    }
}
```

---

## ✅ VERIFICATION CHECKLIST

After fixing a controller, verify:

- [ ] Controller extends `BaseController`
- [ ] No `UUID.fromString(authentication.getName())` calls remain
- [ ] All pagination params have `@Min(0)` and `@Max(100)`
- [ ] All POST/PUT endpoints have `@Valid` on request body
- [ ] All IDOR checks have null safety
- [ ] No redundant authentication checks
- [ ] Path variables validated where appropriate
- [ ] Test class uses `@WebMvcTest` for fast tests
- [ ] Test coverage includes: auth, validation, success, 404, 403

---

**End of Template** ✨

Use this template for all remaining controllers:
- TestAttemptController
- SubscriptionController
- WritingController
- ProfileController
- DebugController
- VocabularyController
- And all admin controllers
