import React from 'react';
import '../css/CourseCard.css';

const CourseCard = ({ course }) => {
  return (
    <div className="course-card">
      <div className="course-card-image">
        <img src={course.imageUrl} alt={course.title} />
      </div>
      <div className="course-card-content">
        <h3 className="course-card-title">{course.title}</h3>
        <p className="course-card-info">Kỹ năng: {course.skill}</p>
        <p className="course-card-info">Hoàn thành trong: {course.completionTime} phút</p>
        <p className="course-card-info">Lần làm gần nhất: {course.lastAttempt}</p>
        <div className="course-card-footer">
          {course.score !== null && (
            <span className="course-card-score">{course.score}/{course.total}</span>
          )}
          <span className="course-card-band">Band {course.band}</span>
        </div>
      </div>
    </div>
  );
};

export default CourseCard;
