import React, { useState } from 'react';
import './../css/Courses.css';
import heroFallback from '../pictures/cambridge-ielts-17.avif';
import { FaSearch } from 'react-icons/fa';

// Updated mock data for a general course list
const mockCourses = [
  {
    id: 1,
    title: 'IELTS Cambridge 17',
    skill: 'Reading',
    duration: '60 phút',
    questions: 40,
    image: heroFallback,
  },
  {
    id: 2,
    title: 'IELTS Cambridge 18',
    skill: 'Listening',
    duration: '40 phút',
    questions: 40,
    image: heroFallback,
  },
  {
    id: 3,
    title: 'IELTS Cambridge 16',
    skill: 'Writing',
    duration: '60 phút',
    questions: 2, // Task 1 & 2
    image: heroFallback,
  },
  {
    id: 4,
    title: 'Official Practice Materials Vol. 2',
    skill: 'Speaking',
    duration: '11-14 phút',
    questions: 3, // Part 1, 2, 3
    image: heroFallback,
  },
  {
    id: 5,
    title: 'IELTS Cambridge 15',
    skill: 'Reading',
    duration: '60 phút',
    questions: 40,
    image: heroFallback,
  },
  {
    id: 6,
    title: 'IELTS Cambridge 18 Full Test',
    skill: 'Full Test',
    duration: '2 giờ 45 phút',
    questions: 85, // Approx.
    image: heroFallback,
  },
];

const skills = ['All', 'Reading', 'Listening', 'Writing', 'Speaking'];

export default function Courses() {
  const [showAll, setShowAll] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSkill, setSelectedSkill] = useState('All');

  const filteredCourses = mockCourses.filter(course => {
    const matchesSearchTerm = course.title.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesSkill = selectedSkill === 'All' || course.skill.toLowerCase().startsWith(selectedSkill.toLowerCase());
    return matchesSearchTerm && matchesSkill;
  });

  const coursesToShow = showAll ? filteredCourses : filteredCourses.slice(0, 4);

  return (
    <div className="new-courses-page">
      <div 
        className="courses-main-banner"
        style={{
          backgroundImage:
            'url("https://thumbs.dreamstime.com/b/diverse-group-adult-students-having-conversations-english-speaking-club-diverse-group-people-talking-to-each-other-251584879.jpg")',
        }}
      >
        <div className="courses-main-banner__overlay" />
        <div className="container">
          <h1 className="courses-main-banner__title">Khám phá các khóa học IELTS của chúng tôi</h1>
          <p className="courses-main-banner__description">Nâng cao kỹ năng của bạn với các bài học chuyên sâu và tài liệu luyện thi chất lượng cao.</p>
        </div>
      </div>
      <div className="new-courses-container">
        {/* Redesigned Filter Section */}
        <div className="courses-search-container">
          <FaSearch className="courses-search-icon" />
          <input
            type="text"
            placeholder="Tìm kiếm theo tên khóa học..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="courses-search-input"
          />
        </div>
        <div className="skill-filters">
          {skills.map(skill => (
            <button
              key={skill}
              className={`skill-filter-btn ${selectedSkill === skill ? 'active' : ''}`}
              onClick={() => setSelectedSkill(skill)}
            >
              {skill}
            </button>
          ))}
        </div>

        <h1 className="new-courses-title">Available Courses ({filteredCourses.length})</h1>

        <div className="new-courses-grid">
          {coursesToShow.map(course => (
            <div className="new-course-card" key={course.id}>
              <div className="new-course-card__image-container">
                <img src={course.image} alt={course.title} className="new-course-card__image" />
              </div>
              <div className="new-course-card__content">
                <div className="new-course-card__header">
                  <span className="new-course-card__title">{course.title}</span>
                </div>
                <div className="new-course-card__meta">
                  <p><strong>Kỹ năng:</strong> {course.skill}</p>
                  <p><strong>Thời gian:</strong> {course.duration}</p>
                  <p><strong>Số câu hỏi:</strong> {course.questions}</p>
                </div>
                <div className="new-course-card__footer">
                  <button className="new-course-card__details-button">
                    Xem chi tiết
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {filteredCourses.length === 0 && (
          <p className="courses-no-results">Không tìm thấy khóa học nào phù hợp.</p>
        )}

        {!showAll && filteredCourses.length > 4 && (
          <div className="new-courses-view-all-container">
            <button onClick={() => setShowAll(true)} className="new-courses-view-all-button">
              Xem tất cả
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
