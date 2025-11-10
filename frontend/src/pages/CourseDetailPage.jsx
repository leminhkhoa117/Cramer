import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { courseApi } from '../api/backendApi';
import './../css/CourseDetail.css';

const formatCourseName = (source) => {
    if (source.toLowerCase().startsWith('cam')) {
        return `IELTS Cambridge ${source.substring(3)}`;
    }
    return source;
};

const skills = ['Reading', 'Listening', 'Writing', 'Speaking'];

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

    return (
        <div className="course-detail-page">
            <div className="container">
                <h1 className="course-detail-title">Các bài test trong {formatCourseName(courseName)}</h1>

                {loading && <p>Đang tải...</p>}
                {error && <p className="course-detail-error">{error}</p>}

                {!loading && !error && (
                    <div className="tests-grid">
                        {tests.map(testNumber => (
                            <div key={testNumber} className="test-card">
                                <h2 className="test-card-title">Test {testNumber}</h2>
                                <div className="test-card-skills">
                                    {skills.map(skill => (
                                        <Link
                                            key={skill}
                                            to={`/test/${courseName}/${testNumber}/${skill.toLowerCase()}`}
                                            className="test-card-skill-link"
                                        >
                                            Bắt đầu làm bài {skill}
                                        </Link>
                                    ))}
                                </div>
                            </div>
                        ))}
                         {tests.length === 0 && (
                            <p>Không có bài test nào trong bộ đề này.</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
