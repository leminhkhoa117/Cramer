import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { courseApi } from '../api/backendApi';
import './../css/CourseDetail.css';
import FullPageLoader from '../components/FullPageLoader';
import { FaBookOpen, FaHeadphones, FaPen, FaMicrophone, FaArrowLeft } from 'react-icons/fa';

const formatCourseName = (source) => {
    if (source.toLowerCase().startsWith('cam')) {
        return `IELTS Cambridge ${source.substring(3)}`;
    }
    return source;
};

const skills = [
    { name: 'Reading', icon: <FaBookOpen />, color: 'text-blue-500', bg: 'bg-blue-50', time: '60 phút', questions: '40 câu' },
    { name: 'Listening', icon: <FaHeadphones />, color: 'text-red-500', bg: 'bg-red-50', time: '30 phút', questions: '40 câu' },
    { name: 'Writing', icon: <FaPen />, color: 'text-yellow-500', bg: 'bg-yellow-50', time: '60 phút', questions: '2 phần' },
    { name: 'Speaking', icon: <FaMicrophone />, color: 'text-green-500', bg: 'bg-green-50', time: '15 phút', questions: '3 phần' }
];

export default function CourseDetailPage() {
    const { courseName } = useParams();
    const [tests, setTests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchTests = async () => {
            try {
                setLoading(true);
                const response = await courseApi.getTestsByCourse(courseName);
                setTests(response.data);
                setError(null);
            } catch (err) {
                setError('Không thể tải danh sách bài test.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchTests();
    }, [courseName]);

    const showLoader = loading && !error && tests.length === 0;

    return (
        <>
            <AnimatePresence>
                {showLoader && (
                    <FullPageLoader
                        key="loader"
                        message={`Đang tải các bài test của ${formatCourseName(courseName)}...`}
                        subMessage="Vui lòng chờ trong giây lát, chúng tôi đang lấy danh sách bài test cho bạn."
                    />
                )}
            </AnimatePresence>

            <div className="course-detail-page">
                <div className="course-detail-banner">
                    <div className="course-detail-banner__overlay" />
                    <div className="course-container">
                        <Link to="/courses" className="back-link">
                            <FaArrowLeft /> Quay lại danh sách
                        </Link>
                        <h1 className="course-detail-title">{formatCourseName(courseName)}</h1>
                        <p className="course-detail-subtitle">
                            Bộ đề thi chính thức với đầy đủ 4 kỹ năng. Hãy chọn một bài test để bắt đầu luyện tập.
                        </p>
                    </div>
                </div>

                <div className="course-container course-content-container">
                    {loading && <p>Đang tải...</p>}
                    {error && <p className="course-detail-error">{error}</p>}

                    {!loading && !error && (
                        <div className="tests-grid">
                            {tests.map(testNumber => (
                                <div key={testNumber} className="test-card">
                                    <div className="test-card-header">
                                        <h2 className="test-card-title">Test {testNumber}</h2>
                                        <span className="test-card-badge">Full Test</span>
                                    </div>
                                    <div className="test-card-skills">
                                        {skills.map(skill => (
                                            <Link
                                                key={skill.name}
                                                to={`/test/${courseName}/${testNumber}/${skill.name.toLowerCase()}`}
                                                className="test-card-skill-link"
                                            >
                                                <span className={`skill-icon ${skill.color}`}>{skill.icon}</span>
                                                <div className="skill-info">
                                                    <span className="skill-name">{skill.name}</span>
                                                    <span className="skill-meta">{skill.time} • {skill.questions}</span>
                                                </div>
                                                <span className="skill-action">Làm bài</span>
                                            </Link>
                                        ))}
                                    </div>
                                </div>
                            ))}
                            {tests.length === 0 && (
                                <div className="no-tests-message">
                                    <p>Không có bài test nào trong bộ đề này.</p>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
