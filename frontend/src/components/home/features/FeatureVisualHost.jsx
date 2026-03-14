import React from 'react';

const librarySets = [
  {
    title: 'IELTS Cambridge 18',
    meta: '4 bài test · Academic',
    source: 'Cambridge',
    status: 'Mới',
    cta: 'Xem bài test',
  },
  {
    title: 'IELTS Cambridge 17',
    meta: '4 bài test · Academic',
    source: 'Cambridge',
    status: 'Phổ biến',
    cta: 'Xem bài test',
  },
  {
    title: 'IELTS Mock Practice',
    meta: '2 bài test · Full skills',
    source: 'Practice',
    status: 'Gợi ý',
    cta: 'Làm thử nhanh',
  },
];

const writingCriteria = [
  { label: 'Task Response', score: 0.72, band: '6.5' },
  { label: 'Coherence', score: 0.78, band: '7.0' },
  { label: 'Lexical Resource', score: 0.83, band: '7.5' },
  { label: 'Grammar', score: 0.76, band: '7.0' },
];

const testQuestionRows = [
  { text: '14 _________', state: 'done' },
  { text: '15 _________', state: 'done' },
  { text: '16 _________', state: 'active' },
  { text: '17 _________', state: 'pending' },
  { text: '18 _________', state: 'pending' },
];

const reviewInsights = [
  'Mở bài rõ luận điểm',
  'Liên kết đoạn còn thiếu',
  'Tăng độ đa dạng cấu trúc câu',
];

const speakingWave = [0.32, 0.58, 0.44, 0.7, 0.52, 0.65, 0.38, 0.6, 0.48, 0.74, 0.42, 0.56];

const speakingConversation = [
  'Examiner: Why do people enjoy traveling?',
  'You: It helps us relax and learn new ideas.',
  'Examiner: Can you give an example?',
  'You: Visiting new cities helps broaden perspective.',
];

const dashboardCards = [
  { label: 'Bài đã hoàn thành', value: '18' },
  { label: 'Đang làm', value: '3' },
  { label: 'Lần gần nhất', value: '6.5' },
  { label: 'Mục tiêu band', value: '7.0' },
];

const dashboardHistory = [
  { title: 'CAM18 Test 2 · Reading', primary: 'Tiếp tục', secondary: 'Xem lại' },
  { title: 'CAM17 Test 1 · Writing', primary: 'Làm lại', secondary: 'Review' },
];

const vocabularyRows = [
  { word: 'mitigate', source: 'Reading', status: 'Đã thuộc' },
  { word: 'coherence', source: 'Writing', status: 'Đang học' },
  { word: 'substantial', source: 'Listening', status: 'Đang học' },
  { word: 'articulate', source: 'Speaking', status: 'Đang học' },
];

const assistantMessages = [
  { role: 'assistant', text: 'Xin chào! Mình có thể giúp bạn luyện IELTS.' },
  { role: 'user', text: 'Gợi ý outline cho Task 2 về education?' },
  { role: 'assistant', text: 'Mở bài + 2 thân bài + kết luận, mình đã chuẩn bị sẵn.' },
];

const LibraryVisual = () => (
  <div className="feature-visual-app feature-visual-app--real feature-visual-app--library">
    <div className="feature-real-shell feature-complex-appear" style={{ '--complex-delay': '80ms' }}>
      <div className="feature-real-head">
        <span className="feature-real-title feature-type-line" style={{ '--type-delay': '190ms', '--type-duration': '1800ms' }}>
          Khám phá các bộ đề IELTS
        </span>
        <span className="feature-real-chip">Courses</span>
      </div>

      <div className="feature-library-toolbar">
        <span className="feature-library-search">Tìm kiếm theo tên bộ đề...</span>
        <button type="button">Lọc</button>
      </div>

      <div className="feature-library-filters">
        <span className="is-active">Academic</span>
        <span>Mới nhất</span>
        <span>Đề phổ biến</span>
      </div>
    </div>

    <div className="feature-library-grid feature-complex-appear" style={{ '--complex-delay': '300ms' }}>
      {librarySets.map((item) => (
        <article key={item.title} className="feature-library-card">
          <div className="feature-library-card-head">
            <h4>{item.title}</h4>
            <span>{item.status}</span>
          </div>
          <p>{item.meta}</p>
          <div className="feature-library-card-meta">
            <span>{item.source}</span>
            <span>4 kỹ năng</span>
          </div>
          <div className="feature-library-card-actions">
            <span>{item.cta}</span>
            <span>Mở nhanh</span>
          </div>
        </article>
      ))}
    </div>

    <div className="feature-library-footer feature-complex-appear" style={{ '--complex-delay': '500ms' }}>
      <div className="feature-library-skills">
        <span>Reading</span>
        <span>Listening</span>
        <span>Writing</span>
        <span>Speaking</span>
      </div>
      <div className="feature-library-path">
        <span>Courses / Cambridge 18 / Test 2</span>
        <span className="feature-library-link">Đi tới bài làm</span>
      </div>
    </div>
  </div>
);

const TestSimulatorVisual = () => (
  <div className="feature-visual-app feature-visual-app--real feature-visual-app--test-simulator">
    <header className="feature-test-head feature-complex-appear" style={{ '--complex-delay': '80ms' }}>
      <span className="feature-test-name feature-type-line" style={{ '--type-delay': '200ms', '--type-duration': '1900ms' }}>
        IELTS Reading Test - CAM18 Test 2
      </span>
      <span className="feature-test-timer">43:12</span>
      <div className="feature-test-actions">
        <button type="button">Thoát</button>
        <button type="button">Lưu</button>
        <button type="button">Nộp bài</button>
      </div>
    </header>

    <div className="feature-test-status feature-complex-appear" style={{ '--complex-delay': '180ms' }}>
      <span className="is-active">Autosave bật</span>
      <span>Toàn màn hình</span>
      <span>Tự cuộn passage</span>
    </div>

    <div className="feature-test-layout feature-complex-appear" style={{ '--complex-delay': '290ms' }}>
      <section className="feature-test-panel feature-test-panel--passage">
        <h4>Reading Passage 2</h4>
        <span />
        <span />
        <span />
        <span />
        <span />
        <div className="feature-test-passage-meta">
          <span>Questions 14-26</span>
          <span>Part 2</span>
        </div>
      </section>

      <section className="feature-test-panel feature-test-panel--questions">
        <h4>Questions 14-26</h4>
        <ul>
          {testQuestionRows.map((item) => (
            <li key={item.text}>
              <span>{item.text}</span>
              <em className={`is-${item.state}`}>{item.state}</em>
            </li>
          ))}
        </ul>
      </section>
    </div>

    <footer className="feature-test-foot feature-complex-appear" style={{ '--complex-delay': '510ms' }}>
      <div className="feature-test-foot-left">
        <div className="feature-test-parts">
          <span>Part 1</span>
          <span className="is-active">Part 2</span>
          <span>Part 3</span>
        </div>
        <div className="feature-test-question-nav">
          <span>14</span>
          <span>15</span>
          <span className="is-active">16</span>
          <span>17</span>
          <span>18</span>
          <span>19</span>
        </div>
      </div>
      <div className="feature-test-quick">
        <span>Đã lưu trạng thái</span>
        <span>Nhảy câu nhanh</span>
      </div>
    </footer>
  </div>
);

const AIEvaluationVisual = () => (
  <div className="feature-visual-app feature-visual-app--real feature-visual-app--ai-review">
    <header className="feature-review-head feature-complex-appear" style={{ '--complex-delay': '80ms' }}>
      <span className="feature-type-line" style={{ '--type-delay': '190ms', '--type-duration': '1800ms' }}>
        Writing Review · Task 2
      </span>
      <span className="feature-review-band">Overall 7.0</span>
    </header>

    <div className="feature-review-tabs feature-complex-appear" style={{ '--complex-delay': '180ms' }}>
      <span className="is-active">Tổng quan</span>
      <span>Criteria</span>
      <span>Gợi ý sửa</span>
      <span>Bài mẫu</span>
    </div>

    <div className="feature-review-grid feature-complex-appear" style={{ '--complex-delay': '290ms' }}>
      <ul className="feature-review-criteria">
        {writingCriteria.map((item) => (
          <li key={item.label}>
            <span>{item.label}</span>
            <strong>{item.band}</strong>
            <div className="feature-review-bar">
              <span style={{ '--review-progress': item.score }} />
            </div>
          </li>
        ))}
      </ul>

      <div className="feature-review-panels">
        <span>Viết lại câu (12)</span>
        <span>Viết lại đoạn (4)</span>
        <span>Bài mẫu Band 9.0</span>
        <span>Phân tích từ vựng</span>
        <span>Chấm lại bài viết</span>
      </div>
    </div>

    <div className="feature-review-bottom feature-complex-appear" style={{ '--complex-delay': '500ms' }}>
      <div className="feature-review-note">
        Feedback chi tiết theo từng tiêu chí để bạn sửa đúng trọng tâm.
      </div>
      <div className="feature-review-insights">
        {reviewInsights.map((item) => (
          <span key={item}>{item}</span>
        ))}
      </div>
    </div>
  </div>
);

const SpeakingVisual = () => (
  <div className="feature-visual-app feature-visual-app--real feature-visual-app--speaking">
    <header className="feature-speaking-head feature-complex-appear" style={{ '--complex-delay': '80ms' }}>
      <span className="feature-type-line" style={{ '--type-delay': '200ms', '--type-duration': '1900ms' }}>
        Speaking Session · Part 2
      </span>
      <span className="feature-dev-badge">In development</span>
    </header>

    <div className="feature-speaking-mode feature-complex-appear" style={{ '--complex-delay': '240ms' }}>
      <span className="is-active">FULL</span>
      <span>PART_2_3</span>
      <span className="feature-beta-badge">Beta</span>
      <span>Random topic</span>
    </div>

    <div className="feature-speaking-layout feature-complex-appear" style={{ '--complex-delay': '360ms' }}>
      <section className="feature-speaking-card">
        <h4>Waveform & Recording</h4>
        <div className="feature-speaking-controls">
          <span className="is-recording">REC</span>
          <span>01:52</span>
          <span>Transcript live</span>
        </div>
        <div className="feature-speaking-wave" aria-hidden="true">
          {speakingWave.map((height, index) => (
            <span key={`spk-wave-${index + 1}`} style={{ '--spk-wave': height }} />
          ))}
        </div>
        <p>Live transcript: I think this topic is interesting...</p>
        <p>Detected pause points and pronunciation markers.</p>
      </section>

      <section className="feature-speaking-card">
        <h4>Conversation Panel</h4>
        <ul>
          {speakingConversation.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      </section>
    </div>

    <div className="feature-speaking-footer feature-complex-appear" style={{ '--complex-delay': '520ms' }}>
      <span>Fluency 6.5</span>
      <span>Pronunciation 6.0</span>
      <span>Grammar 6.5</span>
      <span>Transcript</span>
      <span>Part 1/2/3</span>
    </div>
  </div>
);

const DashboardVisual = () => (
  <div className="feature-visual-app feature-visual-app--real feature-visual-app--dashboard-real">
    <header className="feature-dashboard-head feature-complex-appear" style={{ '--complex-delay': '80ms' }}>
      <span className="feature-type-line" style={{ '--type-delay': '190ms', '--type-duration': '1800ms' }}>
        Dashboard học tập
      </span>
      <span className="feature-dev-badge feature-dev-badge--soft">In development</span>
    </header>

    <div className="feature-dashboard-goal feature-complex-appear" style={{ '--complex-delay': '170ms' }}>
      <span>Mục tiêu IELTS: Band 7.0</span>
      <span>Ngày thi: 12/06/2026</span>
    </div>

    <div className="feature-dashboard-tabs feature-complex-appear" style={{ '--complex-delay': '250ms' }}>
      <span className="is-active">Khoá học gần đây</span>
      <span>Biểu đồ tiến độ</span>
      <span>Phân tích kỹ năng</span>
    </div>

    <div className="feature-dashboard-cards feature-complex-appear" style={{ '--complex-delay': '360ms' }}>
      {dashboardCards.map((item) => (
        <div key={item.label} className="feature-dashboard-card">
          <strong>{item.value}</strong>
          <span>{item.label}</span>
        </div>
      ))}
    </div>

    <div className="feature-dashboard-layout feature-complex-appear" style={{ '--complex-delay': '500ms' }}>
      <div className="feature-dashboard-line" aria-hidden="true">
        <svg viewBox="0 0 260 84" preserveAspectRatio="none">
          <polyline points="0,66 44,60 88,48 132,52 176,34 220,30 260,20" />
        </svg>
        <div className="feature-dashboard-legend">
          <span>Reading</span>
          <span>Listening</span>
          <span>Writing</span>
          <span>Speaking</span>
        </div>
      </div>
      <div className="feature-dashboard-history">
        {dashboardHistory.map((item) => (
          <div key={item.title} className="feature-dashboard-history-row">
            <span className="feature-dashboard-history-title">{item.title}</span>
            <div className="feature-dashboard-history-actions">
              <span>{item.primary}</span>
              <span>{item.secondary}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

const SupportVisual = () => (
  <div className="feature-visual-app feature-visual-app--real feature-visual-app--support">
    <header className="feature-support-head feature-complex-appear" style={{ '--complex-delay': '80ms' }}>
      <span className="feature-type-line" style={{ '--type-delay': '190ms', '--type-duration': '1800ms' }}>
        Sổ tay Từ vựng + Trợ lý Cramer
      </span>
      <span className="feature-real-chip">Support</span>
    </header>

    <div className="feature-support-layout feature-complex-appear" style={{ '--complex-delay': '290ms' }}>
      <section className="feature-support-vocab">
        <div className="feature-support-vocab-top">
          <span className="feature-support-search">Tìm kiếm từ vựng...</span>
          <span className="feature-dev-badge feature-dev-badge--soft">In development</span>
        </div>
        <div className="feature-support-vocab-stats">
          <span>Tổng từ: 128</span>
          <span>Đã thuộc: 46</span>
        </div>
        <div className="feature-support-filters">
          <span className="is-active">Tất cả</span>
          <span>Đã thuộc</span>
          <span>Chưa thuộc</span>
        </div>
        <ul>
          {vocabularyRows.map((item) => (
            <li key={item.word}>
              <strong>{item.word}</strong>
              <em>{item.source}</em>
              <span>{item.status}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="feature-support-assistant">
        <div className="feature-support-assistant-head">
          <span>Trợ lý Cramer</span>
          <span>Pro · 120 Lua</span>
        </div>
        <div className="feature-support-chat">
          {assistantMessages.map((message) => (
            <p key={message.text} data-role={message.role}>{message.text}</p>
          ))}
        </div>
        <div className="feature-support-usage">
          <span>Còn 40/50 câu hỏi tháng này</span>
          <div className="feature-support-usage-bar">
            <span style={{ '--usage-width': 0.8 }} />
          </div>
        </div>
        <div className="feature-support-input">
          <span>Nhập câu hỏi...</span>
          <button type="button">Gửi</button>
        </div>
      </section>
    </div>
  </div>
);

const visualRegistry = {
  library: LibraryVisual,
  'test-simulator': TestSimulatorVisual,
  'ai-evaluation': AIEvaluationVisual,
  speaking: SpeakingVisual,
  dashboard: DashboardVisual,
  support: SupportVisual,
};

const FeatureVisualHost = ({ visual }) => {
  const VisualComponent = visualRegistry[visual] || LibraryVisual;

  return (
    <div className={`stacked-feature-visual stacked-feature-visual--${visual}`} aria-hidden="true">
      <div className="stacked-feature-visual-frame">
        <VisualComponent />
      </div>
    </div>
  );
};

export default FeatureVisualHost;
