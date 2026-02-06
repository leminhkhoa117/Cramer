import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSpeakingStore } from '../../stores';
import GradingLoader from '../common/GradingLoader';

/**
 * ProcessingScreen - Shows while processing speaking transcripts and evaluation
 *
 * Uses the shared GradingLoader component with Speaking-specific content
 */
export default function ProcessingScreen() {
  const navigate = useNavigate();
  const { completeSession, sourceContext } = useSpeakingStore();
  const [gradingStatus, setGradingStatus] = useState('PENDING');
  const [taskStatuses, setTaskStatuses] = useState({ 1: 'PENDING', 2: 'PENDING', 3: 'PENDING', 4: 'PENDING' });

  /**
   * Simulate grading process progression
   * In production, this would track actual API response status
   */
  useEffect(() => {
    // Start grading after brief delay
    const startTimer = setTimeout(() => {
      setGradingStatus('GRADING');
      setTaskStatuses(prev => ({ ...prev, 1: 'GRADING' }));
    }, 500);

    // Simulate Fluency analysis (Part 1)
    const fluencyTimer = setTimeout(() => {
      setTaskStatuses(prev => ({ ...prev, 1: 'COMPLETED', 2: 'GRADING' }));
    }, 2500);

    // Simulate Lexical analysis (Part 2)
    const lexicalTimer = setTimeout(() => {
      setTaskStatuses(prev => ({ ...prev, 2: 'COMPLETED', 3: 'GRADING' }));
    }, 4500);

    // Simulate Grammar analysis (Part 3)
    const grammarTimer = setTimeout(() => {
      setTaskStatuses(prev => ({ ...prev, 3: 'COMPLETED', 4: 'GRADING' }));
    }, 6500);

    // Complete grading
    const completeTimer = setTimeout(() => {
      setTaskStatuses({ 1: 'COMPLETED', 2: 'COMPLETED', 3: 'COMPLETED', 4: 'COMPLETED' });
      setGradingStatus('COMPLETED');
    }, 8000);

    // Navigate to results after completion
    const resultTimer = setTimeout(() => {
      completeSession();
    }, 9000);

    return () => {
      clearTimeout(startTimer);
      clearTimeout(fluencyTimer);
      clearTimeout(lexicalTimer);
      clearTimeout(grammarTimer);
      clearTimeout(completeTimer);
      clearTimeout(resultTimer);
    };
  }, [completeSession]);

  /**
   * Handle back button click - navigate to courses
   */
  const handleBackClick = () => {
    if (sourceContext?.courseName) {
      navigate(`/courses/${sourceContext.courseName}`);
    } else {
      navigate('/courses');
    }
  };

  // Speaking-specific stages
  const speakingStages = [
    { key: 'receive', label: 'Nhận audio' },
    { key: 'fluency', label: 'Fluency' },
    { key: 'lexical', label: 'Lexical' },
    { key: 'grammar', label: 'Grammar' },
    { key: 'pronun', label: 'Phát âm' },
  ];

  // Speaking-specific segments for progress bar (4 IELTS Speaking criteria)
  const speakingSegments = [
    { abbr: 'FC', label: 'Fluency' },
    { abbr: 'LR', label: 'Lexical' },
    { abbr: 'GR', label: 'Grammar' },
    { abbr: 'PR', label: 'Pronun' },
  ];

  // Speaking-specific carousel content
  const speakingCarousels = {
    action: [
      "Đang phân tích bản ghi âm của bạn...",
      "Đang đánh giá độ lưu loát và mạch lạc...",
      "Đang phân tích vốn từ vựng sử dụng...",
      "Đang kiểm tra độ chính xác ngữ pháp...",
      "Đang đánh giá phát âm và ngữ điệu...",
      "Đang tổng hợp nhận xét chi tiết...",
    ],
    tips: [
      { icon: '🎤', text: 'Nói với tốc độ tự nhiên, không quá nhanh cũng không quá chậm.' },
      { icon: '💬', text: 'Sử dụng các từ nối như "however", "moreover" để tăng điểm Coherence.' },
      { icon: '📚', text: 'Paraphrase câu hỏi thay vì lặp lại nguyên văn.' },
      { icon: '🎯', text: 'Trả lời đầy đủ nhưng không lan man ra ngoài chủ đề.' },
      { icon: '🗣️', text: 'Thể hiện cảm xúc qua ngữ điệu để bài nói sinh động hơn.' },
      { icon: '⏱️', text: 'Part 2 cần nói ít nhất 1-2 phút, tối đa 2 phút.' },
    ],
    stats: [
      { icon: '📈', text: 'Band 7+ yêu cầu nói trôi chảy với ít do dự.' },
      { icon: '🌍', text: 'IELTS Speaking được chấm bởi examiner được đào tạo quốc tế.' },
      { icon: '⏱️', text: 'Bài thi Speaking kéo dài 11-14 phút với 3 phần.' },
      { icon: '🎤', text: 'AI Cramer phân tích hơn 30 yếu tố phát âm và ngữ điệu.' },
      { icon: '🏆', text: 'Chỉ 3% thí sinh đạt Band 8+ trong Speaking.' },
      { icon: '💪', text: 'Luyện tập đều đặn có thể cải thiện 0.5-1.0 band trong 2 tháng.' },
    ],
  };

  return (
    <GradingLoader
      status={gradingStatus}
      taskStatuses={taskStatuses}
      stages={speakingStages}
      segments={speakingSegments}
      carousels={speakingCarousels}
      onBackClick={handleBackClick}
      backButtonText="Quay về Dashboard"
      title="Cramer đang chấm điểm Speaking"
      subtitle="AI đang phân tích bài nói của bạn theo tiêu chuẩn IELTS Speaking"
    />
  );
}
