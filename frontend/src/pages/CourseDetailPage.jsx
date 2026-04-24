import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { useCourseStore } from '../stores';
import './../css/course-detail.css';
import FullPageLoader from '../components/FullPageLoader';
import { FaBookOpen, FaHeadphones, FaPen, FaArrowLeft } from 'react-icons/fa';

const formatCourseName = (source) => {
    if (source.toLowerCase().startsWith('cam')) {
        return `IELTS Cambridge ${source.substring(3)}`;
    }
    return source;
};

const skills = [
    { name: 'Reading', icon: <FaBookOpen />, color: 'skill-icon-reading', time: '60 phút', questions: '40 câu' },
    { name: 'Listening', icon: <FaHeadphones />, color: 'skill-icon-listening', time: '30 phút', questions: '40 câu' },
    { name: 'Writing', icon: <FaPen />, color: 'skill-icon-writing', time: '60 phút', questions: '2 phần' }
];

export default function CourseDetailPage() {
    const { courseName } = useParams();
    const [displayName, setDisplayName] = useState(null);
    const [hasFetched, setHasFetched] = useState(false);

    // Zustand store for course tests caching
    const {
        courseTests,
        fetchCourseTests,
        getCachedTests,
        fetchCourseDetails,
        getCachedDetails,
        loading,
        error
    } = useCourseStore();

    // Get tests from cache or fetch
    const tests = courseTests[courseName] || [];

    // If we already have cached tests, mark as fetched immediately
    const hasCachedTests = !!courseTests[courseName];

    useEffect(() => {
        if (hasCachedTests) setHasFetched(true);
    }, [hasCachedTests]);

    useEffect(() => {
        const loadData = async () => {
            // Fetch course details (name)
            const cachedDetails = getCachedDetails(courseName);
            if (cachedDetails) {
                setDisplayName(cachedDetails.name);
            } else {
                const details = await fetchCourseDetails(courseName);
                if (details?.name) {
                    setDisplayName(details.name);
                }
            }

            // Fetch tests
            const cachedTests = getCachedTests(courseName);
            if (!cachedTests) {
                try {
                    await fetchCourseTests(courseName);
                } catch (err) {
                    console.error('Failed to fetch course tests:', err);
                } finally {
                    setHasFetched(true);
                }
            }
        };
        loadData();
    }, [courseName, getCachedTests, fetchCourseTests, getCachedDetails, fetchCourseDetails]);

    const showLoader = (!hasFetched || loading) && !error && tests.length === 0;

    // Use displayName if available, otherwise fallback to formatted courseName
    const title = displayName || formatCourseName(courseName);

    return (
        <>
            <AnimatePresence>
                {showLoader && (
                    <FullPageLoader
                        key="loader"
                        message={`Đang tải các bài test của ${title}...`}
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
                        <h1 className="course-detail-title">{title}</h1>
                        <p className="course-detail-subtitle">
                            Bộ đề thi chính thức với 3 kỹ năng hiện có: Reading, Listening và Writing.
                        </p>
                    </div>
                </div>

                <div className="course-container course-content-container">
                    {loading && <p>Đang tải...</p>}
                    {error && <p className="course-detail-error">{error}</p>}

                    {!loading && !error && hasFetched && (
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
