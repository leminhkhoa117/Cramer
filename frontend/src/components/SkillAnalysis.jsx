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
import { FiPieChart, FiTarget } from 'react-icons/fi';
import '../css/SkillAnalysis.css';

// Color scheme following UI_GUIDELINES.md
const COLORS = {
  current: '#7c3aed',   // Primary purple
  target: '#6366f1',    // Secondary indigo
};

// Skill labels with Vietnamese translations
const SKILL_META = {
  listening: { label: 'Listening', vi: 'Nghe', icon: '🎧' },
  reading: { label: 'Reading', vi: 'Đọc', icon: '📖' },
  writing: { label: 'Writing', vi: 'Viết', icon: '✍️' },
  speaking: { label: 'Speaking', vi: 'Nói', icon: '🗣️' },
};

// Custom tooltip with glassmorphic styling
const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const skillMeta = Object.values(SKILL_META).find(s => s.label === label);
    return (
      <div className="skill-analysis-tooltip">
        <div className="tooltip-header">
          <span className="tooltip-icon">{skillMeta?.icon}</span>
          <span className="tooltip-skill">{label}</span>
          <span className="tooltip-vi">({skillMeta?.vi})</span>
        </div>
        <div className="tooltip-content">
          {payload.map((entry, index) => (
            <div key={index} className="tooltip-row">
              <span 
                className="tooltip-indicator" 
                style={{ backgroundColor: entry.color }}
              />
              <span className="tooltip-label">{entry.name}:</span>
              <span className="tooltip-value">Band {entry.value?.toFixed(1)}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }
  return null;
};

// Custom legend
const CustomLegend = ({ payload }) => {
  return (
    <div className="skill-analysis-legend">
      {payload.map((entry, index) => (
        <div key={index} className="legend-item">
          <span 
            className="legend-indicator" 
            style={{ backgroundColor: entry.color }}
          />
          <span className="legend-label">{entry.value}</span>
        </div>
      ))}
    </div>
  );
};

// Custom axis tick with skill icons
const CustomAngleTick = ({ payload, x, y, cx, cy }) => {
  const skill = payload.value;
  const meta = Object.values(SKILL_META).find(s => s.label === skill);
  
  // Calculate position offset based on angle
  const radius = 20;
  const angle = Math.atan2(y - cy, x - cx);
  const offsetX = Math.cos(angle) * radius;
  const offsetY = Math.sin(angle) * radius;
  
  return (
    <g transform={`translate(${x + offsetX}, ${y + offsetY})`}>
      <text 
        textAnchor="middle" 
        dominantBaseline="middle"
        className="skill-axis-label"
        style={{ 
          fill: '#1f2937', 
          fontSize: '13px', 
          fontWeight: 600 
        }}
      >
        {meta?.icon} {skill}
      </text>
    </g>
  );
};

const SkillAnalysis = ({ courseData, targets }) => {
  // Process course data to get average scores per skill
  const processedData = useMemo(() => {
    const skillScores = {
      listening: [],
      reading: [],
      writing: [],
      speaking: [],
    };

    // Collect all completed scores by skill
    courseData?.forEach(course => {
      if (course.status === 'COMPLETED' && course.bandScore != null) {
        const skill = course.skill?.toLowerCase();
        if (skillScores[skill]) {
          skillScores[skill].push(course.bandScore);
        }
      }
    });

    // Calculate averages and format for radar chart
    return Object.keys(skillScores).map(skill => {
      const scores = skillScores[skill];
      const avg = scores.length > 0 
        ? scores.reduce((a, b) => a + b, 0) / scores.length 
        : 0;
      const target = targets?.find(t => t.id === skill);
      const targetValue = target ? parseFloat(target.value) : 0;

      return {
        skill: SKILL_META[skill].label,
        skillKey: skill,
        current: parseFloat(avg.toFixed(1)),
        target: targetValue,
        fullMark: 9,
      };
    });
  }, [courseData, targets]);

  // Check if we have any actual data
  const hasCurrentData = processedData.some(d => d.current > 0);
  const hasTargetData = processedData.some(d => d.target > 0);

  // Empty state - no data at all
  if (!hasCurrentData && !hasTargetData) {
    return (
      <div className="skill-analysis-empty">
        <div className="empty-icon">
          <FiPieChart />
        </div>
        <h4>Chưa có dữ liệu phân tích</h4>
        <p>
          Hoàn thành các bài test và đặt mục tiêu để xem biểu đồ phân tích 
          kỹ năng của bạn. Radar chart sẽ so sánh điểm hiện tại với mục tiêu.
        </p>
      </div>
    );
  }

  return (
    <div className="skill-analysis-container">
      {/* Stats summary cards */}
      <div className="skill-analysis-stats">
        {processedData.map(item => (
          <div key={item.skillKey} className="stat-card">
            <div className="stat-icon">{SKILL_META[item.skillKey].icon}</div>
            <div className="stat-info">
              <span className="stat-label">{item.skill}</span>
              <div className="stat-values">
                <span className="stat-current">
                  {item.current > 0 ? item.current.toFixed(1) : '--'}
                </span>
                {item.target > 0 && (
                  <span className="stat-target">
                    <FiTarget /> {item.target.toFixed(1)}
                  </span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Radar chart */}
      <div className="skill-analysis-chart">
        <ResponsiveContainer width="100%" height={380}>
          <RadarChart cx="50%" cy="50%" outerRadius="75%" data={processedData}>
            <defs>
              <linearGradient id="currentGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={COLORS.current} stopOpacity={0.8} />
                <stop offset="100%" stopColor={COLORS.current} stopOpacity={0.3} />
              </linearGradient>
              <linearGradient id="targetGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={COLORS.target} stopOpacity={0.6} />
                <stop offset="100%" stopColor={COLORS.target} stopOpacity={0.2} />
              </linearGradient>
            </defs>
            <PolarGrid 
              stroke="rgba(124, 58, 237, 0.15)" 
              gridType="polygon"
            />
            <PolarAngleAxis 
              dataKey="skill" 
              tick={<CustomAngleTick />}
              tickLine={false}
            />
            <PolarRadiusAxis 
              angle={90} 
              domain={[0, 9]} 
              tick={{ fill: '#64748b', fontSize: 11 }}
              tickCount={5}
              axisLine={false}
            />
            <Tooltip content={<CustomTooltip />} />
            <Legend content={<CustomLegend />} />
            
            {/* Target polygon (behind) */}
            {hasTargetData && (
              <Radar
                name="Mục tiêu"
                dataKey="target"
                stroke={COLORS.target}
                strokeWidth={2}
                fill="url(#targetGradient)"
                fillOpacity={0.5}
                dot={{ r: 4, fill: COLORS.target, stroke: '#fff', strokeWidth: 2 }}
              />
            )}
            
            {/* Current scores polygon (front) */}
            {hasCurrentData && (
              <Radar
                name="Điểm hiện tại"
                dataKey="current"
                stroke={COLORS.current}
                strokeWidth={3}
                fill="url(#currentGradient)"
                fillOpacity={0.6}
                dot={{ r: 5, fill: COLORS.current, stroke: '#fff', strokeWidth: 2 }}
              />
            )}
          </RadarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default SkillAnalysis;
