/**
 * Mock Content Data
 * Dữ liệu giả để phát triển UI Content Management
 */

// Test status definitions
export const testStatuses = [
  { value: 'DRAFT', label: 'Nháp', color: 'neutral' },
  { value: 'REVIEW', label: 'Đang duyệt', color: 'warning' },
  { value: 'PUBLISHED', label: 'Đã xuất bản', color: 'success' },
  { value: 'ARCHIVED', label: 'Lưu trữ', color: 'info' },
];

// Skill status definitions
export const skillStatuses = [
  { value: 'empty', label: 'Chưa có', color: 'neutral' },
  { value: 'draft', label: 'Đang soạn', color: 'warning' },
  { value: 'complete', label: 'Hoàn thành', color: 'success' },
];

// Question types
export const questionTypes = [
  { value: 'FILL_IN_BLANK', label: 'Điền từ', category: 'reading' },
  { value: 'TRUE_FALSE_NOT_GIVEN', label: 'True/False/Not Given', category: 'reading' },
  { value: 'YES_NO_NOT_GIVEN', label: 'Yes/No/Not Given', category: 'reading' },
  { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm', category: 'all' },
  { value: 'MATCHING_HEADINGS', label: 'Nối heading', category: 'reading' },
  { value: 'MATCHING_INFORMATION', label: 'Nối thông tin', category: 'reading' },
  { value: 'MATCHING_FEATURES', label: 'Nối đặc điểm', category: 'reading' },
  { value: 'MATCHING_SENTENCE_ENDINGS', label: 'Nối câu', category: 'reading' },
  { value: 'SENTENCE_COMPLETION', label: 'Hoàn thành câu', category: 'all' },
  { value: 'SUMMARY_COMPLETION', label: 'Hoàn thành tóm tắt', category: 'reading' },
  { value: 'NOTE_COMPLETION', label: 'Hoàn thành ghi chú', category: 'listening' },
  { value: 'TABLE_COMPLETION', label: 'Hoàn thành bảng', category: 'listening' },
  { value: 'DIAGRAM_LABELLING', label: 'Ghi nhãn sơ đồ', category: 'listening' },
];

// Mock topics with tests
export const mockTopics = [
  {
    id: 1,
    source: 'Cambridge 17',
    displayName: 'Cambridge IELTS 17',
    description: 'Bộ đề thi Cambridge IELTS 17 chính thức',
    testsCount: 4,
    publishedTests: 4,
    createdAt: '2025-01-15T00:00:00Z',
    updatedAt: '2025-12-01T00:00:00Z',
    tests: [
      {
        id: 101,
        topicId: 1,
        number: 1,
        name: 'Test 1',
        status: 'PUBLISHED',
        publishedAt: '2025-11-01T00:00:00Z',
        totalAttempts: 1250,
        skills: {
          reading: {
            questionCount: 40,
            status: 'complete',
            sectionsCount: 3,
          },
          listening: {
            questionCount: 40,
            status: 'complete',
            sectionsCount: 4,
            hasAudio: true,
          },
          writing: {
            status: 'complete',
            hasTask1: true,
            hasTask2: true,
          },
          speaking: {
            status: 'complete',
            hasPart1: true,
            hasPart2: true,
            hasPart3: true,
          },
        },
      },
      {
        id: 102,
        topicId: 1,
        number: 2,
        name: 'Test 2',
        status: 'PUBLISHED',
        publishedAt: '2025-11-05T00:00:00Z',
        totalAttempts: 980,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'draft', hasPart1: true, hasPart2: false, hasPart3: false },
        },
      },
      {
        id: 103,
        topicId: 1,
        number: 3,
        name: 'Test 3',
        status: 'PUBLISHED',
        publishedAt: '2025-11-10T00:00:00Z',
        totalAttempts: 756,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
      {
        id: 104,
        topicId: 1,
        number: 4,
        name: 'Test 4',
        status: 'PUBLISHED',
        publishedAt: '2025-11-15T00:00:00Z',
        totalAttempts: 612,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
    ],
  },
  {
    id: 2,
    source: 'Cambridge 18',
    displayName: 'Cambridge IELTS 18',
    description: 'Bộ đề thi Cambridge IELTS 18 chính thức',
    testsCount: 4,
    publishedTests: 3,
    createdAt: '2025-03-01T00:00:00Z',
    updatedAt: '2025-12-10T00:00:00Z',
    tests: [
      {
        id: 201,
        topicId: 2,
        number: 1,
        name: 'Test 1',
        status: 'PUBLISHED',
        publishedAt: '2025-12-01T00:00:00Z',
        totalAttempts: 423,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
      {
        id: 202,
        topicId: 2,
        number: 2,
        name: 'Test 2',
        status: 'PUBLISHED',
        publishedAt: '2025-12-05T00:00:00Z',
        totalAttempts: 287,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'draft', hasTask1: true, hasTask2: false },
          speaking: { status: 'empty', hasPart1: false, hasPart2: false, hasPart3: false },
        },
      },
      {
        id: 203,
        topicId: 2,
        number: 3,
        name: 'Test 3',
        status: 'PUBLISHED',
        publishedAt: '2025-12-10T00:00:00Z',
        totalAttempts: 156,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'draft', hasPart1: true, hasPart2: true, hasPart3: false },
        },
      },
      {
        id: 204,
        topicId: 2,
        number: 4,
        name: 'Test 4',
        status: 'DRAFT',
        publishedAt: null,
        totalAttempts: 0,
        skills: {
          reading: { questionCount: 25, status: 'draft', sectionsCount: 2 },
          listening: { questionCount: 0, status: 'empty', sectionsCount: 0, hasAudio: false },
          writing: { status: 'empty', hasTask1: false, hasTask2: false },
          speaking: { status: 'empty', hasPart1: false, hasPart2: false, hasPart3: false },
        },
      },
    ],
  },
  {
    id: 3,
    source: 'Real Tests',
    displayName: 'Đề thi thực tế',
    description: 'Bộ sưu tập đề thi IELTS thực tế',
    testsCount: 6,
    publishedTests: 4,
    createdAt: '2025-02-01T00:00:00Z',
    updatedAt: '2025-12-12T00:00:00Z',
    tests: [
      {
        id: 301,
        topicId: 3,
        number: 1,
        name: 'Real Test 2024-01',
        status: 'PUBLISHED',
        publishedAt: '2025-06-01T00:00:00Z',
        totalAttempts: 890,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
      {
        id: 302,
        topicId: 3,
        number: 2,
        name: 'Real Test 2024-02',
        status: 'PUBLISHED',
        publishedAt: '2025-07-01T00:00:00Z',
        totalAttempts: 756,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
      {
        id: 303,
        topicId: 3,
        number: 3,
        name: 'Real Test 2024-03',
        status: 'PUBLISHED',
        publishedAt: '2025-08-01T00:00:00Z',
        totalAttempts: 623,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'draft', hasPart1: true, hasPart2: true, hasPart3: false },
        },
      },
      {
        id: 304,
        topicId: 3,
        number: 4,
        name: 'Real Test 2024-04',
        status: 'PUBLISHED',
        publishedAt: '2025-09-01T00:00:00Z',
        totalAttempts: 512,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
      {
        id: 305,
        topicId: 3,
        number: 5,
        name: 'Real Test 2024-05',
        status: 'REVIEW',
        publishedAt: null,
        totalAttempts: 0,
        skills: {
          reading: { questionCount: 40, status: 'complete', sectionsCount: 3 },
          listening: { questionCount: 40, status: 'complete', sectionsCount: 4, hasAudio: true },
          writing: { status: 'complete', hasTask1: true, hasTask2: true },
          speaking: { status: 'complete', hasPart1: true, hasPart2: true, hasPart3: true },
        },
      },
      {
        id: 306,
        topicId: 3,
        number: 6,
        name: 'Real Test 2024-06',
        status: 'DRAFT',
        publishedAt: null,
        totalAttempts: 0,
        skills: {
          reading: { questionCount: 30, status: 'draft', sectionsCount: 2 },
          listening: { questionCount: 20, status: 'draft', sectionsCount: 2, hasAudio: true },
          writing: { status: 'draft', hasTask1: true, hasTask2: false },
          speaking: { status: 'empty', hasPart1: false, hasPart2: false, hasPart3: false },
        },
      },
    ],
  },
];

// Content overview stats
export const mockContentOverview = {
  totalTopics: 3,
  totalTests: 14,
  publishedTests: 11,
  draftTests: 2,
  reviewTests: 1,
  totalQuestions: 1120,
  totalAttempts: 7245,
};

// Recent content activities
export const mockContentActivities = [
  { id: 1, type: 'PUBLISHED', description: 'Cambridge 18 Test 3 đã được xuất bản', createdAt: '2025-12-10T14:00:00Z', adminEmail: 'admin@cramer.edu.vn' },
  { id: 2, type: 'UPDATED', description: 'Cập nhật 5 câu hỏi trong Cambridge 17 Test 2', createdAt: '2025-12-08T10:30:00Z', adminEmail: 'content@cramer.edu.vn' },
  { id: 3, type: 'CREATED', description: 'Tạo mới Real Test 2024-06', createdAt: '2025-12-05T09:00:00Z', adminEmail: 'content@cramer.edu.vn' },
  { id: 4, type: 'REVIEW', description: 'Real Test 2024-05 đã được gửi duyệt', createdAt: '2025-12-01T16:45:00Z', adminEmail: 'content@cramer.edu.vn' },
];

// Helper: Get status color
export const getStatusColor = (status) => {
  const statusObj = testStatuses.find(s => s.value === status);
  return statusObj ? statusObj.color : 'neutral';
};

// Helper: Get skill status color
export const getSkillStatusColor = (status) => {
  const statusObj = skillStatuses.find(s => s.value === status);
  return statusObj ? statusObj.color : 'neutral';
};

// Helper: Get topic by ID
export const getTopicById = (id) => {
  return mockTopics.find(t => t.id === id);
};

// Helper: Get test by ID
export const getTestById = (testId) => {
  for (const topic of mockTopics) {
    const test = topic.tests.find(t => t.id === testId);
    if (test) return { ...test, topicName: topic.displayName };
  }
  return null;
};

// Helper: Flatten all tests for search/filter
export const getAllTests = () => {
  const tests = [];
  mockTopics.forEach(topic => {
    topic.tests.forEach(test => {
      tests.push({
        ...test,
        topicId: topic.id,
        topicName: topic.displayName,
        topicSource: topic.source,
      });
    });
  });
  return tests;
};
