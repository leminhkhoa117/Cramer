import React, { useMemo } from 'react';
import {
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  ResponsiveContainer,
  Tooltip,
  Legend,
} from 'recharts';
import '../css/SkillAnalysis.css';

const SkillAnalysis = ({ courseData, targets }) => {
  const processData = useMemo(() => {
    const skillScores = {
      listening: [],
      reading: [],
      writing: [],
      speaking: [],
    };

    courseData.forEach(course => {
      if (course.status === 'COMPLETED' && course.bandScore != null) {
        if (skillScores[course.skill]) {
          skillScores[course.skill].push(course.bandScore);
        }
      }
    });

    const avgScores = Object.keys(skillScores).map(skill => {
      const scores = skillScores[skill];
      const avg = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0;
      const target = targets.find(t => t.id === skill);

      return {
        skill: skill.charAt(0).toUpperCase() + skill.slice(1),
        'Điểm TB': parseFloat(avg.toFixed(2)),
        'Mục tiêu': target ? parseFloat(target.value) : 0,
      };
    });

    return avgScores;
  }, [courseData, targets]);

  const hasData = processData.some(d => d['Điểm TB'] > 0);

  if (!hasData) {
    return (
      <div className="chart-placeholder">
        <h4>Phân tích Kỹ năng</h4>
        <p>Chưa có đủ dữ liệu để phân tích. Hãy hoàn thành các bài test để xem phân tích chi tiết!</p>
      </div>
    );
  }

  return (
    <div className="skill-analysis-container">
      <h4>So sánh Kỹ năng và Mục tiêu</h4>
      <ResponsiveContainer width="100%" height={350}>
        <RadarChart cx="50%" cy="50%" outerRadius="80%" data={processData}>
          <PolarGrid stroke="#e0e0e0" />
          <PolarAngleAxis dataKey="skill" tick={{ fill: '#333', fontSize: 14 }} />
          <PolarRadiusAxis angle={30} domain={[0, 9]} tick={{ fill: '#666' }} />
          <Tooltip
            contentStyle={{
              backgroundColor: 'rgba(255, 255, 255, 0.9)',
              borderRadius: '8px',
              borderColor: '#ccc',
            }}
          />
          <Legend wrapperStyle={{ color: '#333' }} />
          <Radar
            name="Điểm TB"
            dataKey="Điểm TB"
            stroke="#8c52ff"
            fill="#8c52ff"
            fillOpacity={0.6}
          />
          <Radar
            name="Mục tiêu"
            dataKey="Mục tiêu"
            stroke="#27afdb"
            fill="#27afdb"
            fillOpacity={0.4}
          />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default SkillAnalysis;
