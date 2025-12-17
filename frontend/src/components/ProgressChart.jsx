import React, { useMemo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { FiTrendingUp, FiCalendar } from 'react-icons/fi';
import '../css/ProgressChart.css';

// Skill color palette following UI_GUIDELINES.md
const SKILL_COLORS = {
  reading: '#22c55e',   // green
  listening: '#3b82f6', // blue
  writing: '#f59e0b',   // amber
  speaking: '#ef4444',  // red
};

const SKILL_LABELS = {
  reading: 'Reading',
  listening: 'Listening',
  writing: 'Writing',
  speaking: 'Speaking',
};

// Custom tooltip with glassmorphic styling
const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div className="progress-chart-tooltip">
        <div className="tooltip-header">
          <FiCalendar className="tooltip-icon" />
          <span className="tooltip-date">{label}</span>
        </div>
        <div className="tooltip-content">
          {payload.map((entry, index) => (
            <div key={index} className="tooltip-row">
              <span 
                className="tooltip-dot" 
                style={{ backgroundColor: entry.color }}
              />
              <span className="tooltip-skill">{entry.name}</span>
              <span className="tooltip-value">Band {entry.value?.toFixed(1)}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }
  return null;
};

// Custom legend with better styling
const CustomLegend = ({ payload }) => {
  return (
    <div className="progress-chart-legend">
      {payload.map((entry, index) => (
        <div key={index} className="legend-item">
          <span 
            className="legend-dot" 
            style={{ backgroundColor: entry.color }}
          />
          <span className="legend-label">{entry.value}</span>
        </div>
      ))}
    </div>
  );
};

const ProgressChart = ({ data }) => {
  // Process data to create multi-line chart format
  // Group by date and show all skills on same date
  const chartData = useMemo(() => {
    if (!data || data.length === 0) return [];

    // Filter completed attempts with band scores
    const completedAttempts = data.filter(
      item => item.status === 'COMPLETED' && item.bandScore != null
    );

    if (completedAttempts.length === 0) return [];

    // Group by date
    const dateMap = new Map();

    completedAttempts.forEach(item => {
      const date = new Date(item.lastAttempt).toLocaleDateString('vi-VN');
      const skill = item.skill?.toLowerCase();
      
      if (!dateMap.has(date)) {
        dateMap.set(date, { date });
      }
      
      const dateEntry = dateMap.get(date);
      // If multiple attempts on same date for same skill, use the latest/highest
      if (!dateEntry[skill] || item.bandScore > dateEntry[skill]) {
        dateEntry[skill] = item.bandScore;
      }
    });

    // Convert to array and sort by date
    return Array.from(dateMap.values()).sort((a, b) => {
      const [dayA, monthA, yearA] = a.date.split('/').map(Number);
      const [dayB, monthB, yearB] = b.date.split('/').map(Number);
      const dateA = new Date(yearA, monthA - 1, dayA);
      const dateB = new Date(yearB, monthB - 1, dayB);
      return dateA - dateB;
    });
  }, [data]);

  // Determine which skills have data
  const activeSkills = useMemo(() => {
    const skills = new Set();
    chartData.forEach(entry => {
      Object.keys(entry).forEach(key => {
        if (key !== 'date' && SKILL_COLORS[key]) {
          skills.add(key);
        }
      });
    });
    return Array.from(skills);
  }, [chartData]);

  // Empty state
  if (chartData.length === 0) {
    return (
      <div className="progress-chart-empty">
        <div className="empty-icon">
          <FiTrendingUp />
        </div>
        <h4>Chưa có dữ liệu tiến độ</h4>
        <p>
          Hoàn thành một bài test để bắt đầu theo dõi tiến độ của bạn. 
          Biểu đồ sẽ hiển thị điểm số theo thời gian cho từng kỹ năng.
        </p>
      </div>
    );
  }

  return (
    <div className="progress-chart-container">
      <ResponsiveContainer width="100%" height={350}>
        <LineChart
          data={chartData}
          margin={{
            top: 20,
            right: 30,
            left: 10,
            bottom: 20,
          }}
        >
          <defs>
            {/* Gradient definitions for lines */}
            {activeSkills.map(skill => (
              <linearGradient key={skill} id={`gradient-${skill}`} x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stopColor={SKILL_COLORS[skill]} stopOpacity={0.8} />
                <stop offset="100%" stopColor={SKILL_COLORS[skill]} stopOpacity={1} />
              </linearGradient>
            ))}
          </defs>
          <CartesianGrid 
            strokeDasharray="3 3" 
            stroke="rgba(124, 58, 237, 0.1)" 
            vertical={false}
          />
          <XAxis 
            dataKey="date" 
            tick={{ fill: '#64748b', fontSize: 12 }}
            tickLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
          />
          <YAxis 
            domain={[0, 9]} 
            tick={{ fill: '#64748b', fontSize: 12 }}
            tickLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            label={{ 
              value: 'Band Score', 
              angle: -90, 
              position: 'insideLeft',
              style: { fill: '#64748b', fontSize: 12 }
            }}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend content={<CustomLegend />} />
          
          {/* Render a line for each active skill */}
          {activeSkills.map(skill => (
            <Line
              key={skill}
              type="monotone"
              dataKey={skill}
              name={SKILL_LABELS[skill]}
              stroke={SKILL_COLORS[skill]}
              strokeWidth={3}
              dot={{ 
                r: 5, 
                fill: SKILL_COLORS[skill],
                strokeWidth: 2,
                stroke: '#fff'
              }}
              activeDot={{ 
                r: 8, 
                fill: SKILL_COLORS[skill],
                stroke: '#fff',
                strokeWidth: 3
              }}
              connectNulls={false}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default ProgressChart;
