/**
 * Cramer homepage — shared content constants.
 * Centralises copy/data so sections stay declarative.
 */

export const HERO_BADGES = [
    'Tập luyện miễn phí 100%',
    'Môi trường thi thử giống thật nhất',
    'Nhiều tuỳ chọn học tập',
];

export const HERO_STATS = [
    { target: 10000, suffix: '+', label: 'Học viên' },
    { target: 500, suffix: '+', label: 'Đề thi' },
    { target: 95, suffix: '%', label: 'Hài lòng' },
];

// Floating glyphs in the hero 3D scene.
// Mix of IELTS-themed letters/symbols. Position is in [x, y, z] world space.
export const HERO_GLYPHS = [
    { char: 'A', position: [-2.6, 1.2, -1], color: '#a78bfa', scale: 0.55 },
    { char: '✎', position: [2.8, 0.8, -0.4], color: '#fb923c', scale: 0.6 },
    { char: '♪', position: [-2.2, -1.1, 0.5], color: '#22d3ee', scale: 0.5 },
    { char: '★', position: [2.2, -1.4, -0.6], color: '#f472b6', scale: 0.5 },
    { char: 'B', position: [0, 1.9, -1.4], color: '#818cf8', scale: 0.5 },
    { char: '?', position: [0, -2.0, -0.2], color: '#34d399', scale: 0.45 },
];

export const TESTIMONIALS = [
    {
        quote:
            'Đây là một trong những web vippro nhất mà mình từng được thử qua với nhiều dạng đề, thật sự là rất yêu founder của trang này!',
        author: 'Chí Phong',
        role: 'Học viên IELTS',
        band: '7.5',
        accent: '#a78bfa',
    },
    {
        quote:
            'Mình thích nhất là tính năng AI writing, nó vô cùng hữu ích vì chỉ ra được điểm mạnh/yếu rõ ràng, mà điểm còn chính xác.',
        author: 'Song Vũ',
        role: 'Học viên IELTS',
        band: '7.0',
        accent: '#22d3ee',
    },
    {
        quote:
            'Thật sự ấn tượng với tính năng AI speaking. Bình thường thi thử rất tốn kém, nay có Cramer chi phí phải chăng hơn rất nhiều mà còn giống thi thật.',
        author: 'Hồng Em',
        role: 'Học viên IELTS',
        band: '6.5',
        accent: '#fb923c',
    },
    {
        quote:
            'Có rất nhiều tính năng miễn phí, nhưng vì những tính năng trả phí hay ho quá nên mình đã quyết định xuống tiền và thật sự là không hề hối hận.',
        author: 'Minh Anh',
        role: 'Học viên IELTS',
        band: '8.0',
        accent: '#f472b6',
    },
    {
        quote:
            'Lộ trình cá nhân hóa của Cramer giúp mình biết chính xác phải luyện gì mỗi ngày. Sau 3 tháng, điểm Reading tăng từ 6.0 lên 7.5 một cách rõ rệt.',
        author: 'Thu Hà',
        role: 'Sinh viên Ngoại Thương',
        band: '7.5',
        accent: '#60a5fa',
    },
    {
        quote:
            'Phần feedback Writing chi tiết từng câu, chỉ ra lỗi grammar, collocation lẫn tone. Giáo viên thật cũng không thể bình luận tỉ mỉ như vậy trong thời gian ngắn.',
        author: 'Đức Trung',
        role: 'Kỹ sư phần mềm',
        band: '8.5',
        accent: '#34d399',
    },
    {
        quote:
            'Mình đi làm full-time nên chỉ luyện được buổi tối. Cramer có mobile UX mượt, đề ngắn-gọn để tranh thủ 20 phút nghỉ trưa cũng hiệu quả.',
        author: 'Phương Linh',
        role: 'Marketing Lead',
        band: '7.0',
        accent: '#f59e0b',
    },
    {
        quote:
            'Mình rớt IELTS hai lần trước khi biết đến Cramer. Thi lần này được 7.0 overall, riêng Speaking nhảy từ 5.5 lên 7.0 nhờ phần AI examiner luyện mỗi ngày.',
        author: 'Tuấn Kiệt',
        role: 'Du học sinh Úc',
        band: '7.0',
        accent: '#ef4444',
    },
    {
        quote:
            'Con mình ôn thi lớp 10 chuyên Anh. Ngân hàng đề Reading và Listening của Cramer đủ phong phú để thay thế hẳn việc mua sách ôn luyện đắt đỏ.',
        author: 'Chị Ngọc Mai',
        role: 'Phụ huynh',
        band: '6.5',
        accent: '#c084fc',
    },
    {
        quote:
            'Giao diện đẹp, không rối, không quảng cáo. Làm bài test trên Cramer cảm giác chuyên nghiệp và tập trung hơn nhiều so với các web miễn phí khác.',
        author: 'Hải Đăng',
        role: 'Học sinh THPT',
        band: '6.0',
        accent: '#14b8a6',
    },
    {
        quote:
            'Phần tracking tiến độ theo skill rất trực quan. Mình thấy rõ Listening là điểm yếu và biết nên dồn thời gian vào đâu thay vì học dàn trải.',
        author: 'Khánh Vy',
        role: 'Student ambassador',
        band: '7.5',
        accent: '#ec4899',
    },
    {
        quote:
            'Là giáo viên IELTS, mình giao bài tập qua Cramer cho học viên rồi review lại điểm AI chấm. Tiết kiệm cho mình ít nhất 5 giờ mỗi tuần.',
        author: 'Thầy Hoàng',
        role: 'Giáo viên IELTS',
        band: '8.5',
        accent: '#6366f1',
    },
    {
        quote:
            'Mình đã thử nhiều nền tảng quốc tế, nhưng Cramer là cái duy nhất hiểu cách người Việt học tiếng Anh — từ cách giải thích đến ví dụ.',
        author: 'Bảo Trâm',
        role: 'Content Creator',
        band: '8.0',
        accent: '#0ea5e9',
    },
    {
        quote:
            'Tính năng speaking pronunciation check thật sự xịn — mình phát hiện ra cả đống lỗi intonation mà giáo viên Việt thường không bắt được.',
        author: 'Gia Huy',
        role: 'Developer',
        band: '7.5',
        accent: '#84cc16',
    },
    {
        quote:
            'Sinh con xong mình chỉ có 30 phút mỗi ngày để ôn. Cramer chia nhỏ bài học theo block nên mình vẫn duy trì được thói quen và đạt band mục tiêu.',
        author: 'Quỳnh Như',
        role: 'Working mom',
        band: '7.0',
        accent: '#d946ef',
    },
];

export const FAQS = [
    {
        question: 'Cramer có miễn phí không?',
        answer:
            'Cramer cung cấp nhiều tính năng miễn phí bao gồm luyện thi Reading, Listening với hàng trăm đề. Các tính năng cao cấp như AI Writing & Speaking, lộ trình cá nhân hóa có phí hợp lý để duy trì chất lượng dịch vụ.',
    },
    {
        question: 'Làm sao để bắt đầu luyện tập?',
        answer:
            'Rất đơn giản! Bạn chỉ cần đăng ký tài khoản, làm bài test đầu vào để đánh giá trình độ, sau đó hệ thống sẽ tự động đề xuất lộ trình học tập phù hợp với mục tiêu của bạn.',
    },
    {
        question: 'AI chấm điểm Writing và Speaking có chính xác không?',
        answer:
            'AI của Cramer được huấn luyện dựa trên tiêu chí chấm điểm chính thức của IELTS và liên tục được cải tiến. Độ chính xác đạt trung bình 0.5 band so với giám khảo thật, giúp bạn tự tin hơn khi thi thật.',
    },
    {
        question: 'Tôi có thể hủy đăng ký bất cứ lúc nào không?',
        answer:
            'Hoàn toàn có thể! Bạn có thể hủy đăng ký gói premium bất cứ lúc nào. Tài khoản sẽ tự động chuyển về gói miễn phí khi hết thời hạn đã thanh toán, không mất phí ẩn.',
    },
    {
        question: 'Cramer có phù hợp với người mới bắt đầu không?',
        answer:
            'Có! Cramer phù hợp với mọi trình độ từ beginner đến advanced. Hệ thống sẽ tự động điều chỉnh độ khó dựa trên kết quả bài test đầu vào và tiến trình học tập của bạn.',
    },
    {
        question: 'Đề thi trên Cramer có giống đề thi thật không?',
        answer:
            'Các đề thi trên Cramer được biên soạn theo format chuẩn của Cambridge IELTS, với thời gian và cấu trúc giống hệt phòng thi thật, giúp bạn làm quen và tự tin hơn khi bước vào kỳ thi chính thức.',
    },
];
