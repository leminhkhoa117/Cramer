import React from 'react';
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
import '../css/ProgressChart.css';

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div className="progress-chart-tooltip">
        <p className="tooltip-label">{`Ngày: ${label}`}</p>
        <p className="tooltip-intro">{`Band: ${payload[0].value}`}</p>
        <p className="tooltip-skill">{`Kỹ năng: ${payload[0].payload.skill}`}</p>
      </div>
    );
  }
  return null;
};

const ProgressChart = ({ data }) => {
  const chartData = data
    .filter(item => item.status === 'COMPLETED' && item.bandScore != null)
    .map(item => ({
      date: new Date(item.lastAttempt).toLocaleDateString('vi-VN'),
      bandScore: item.bandScore,
      skill: item.skill.charAt(0).toUpperCase() + item.skill.slice(1),
    }))
    .sort((a, b) => new Date(a.date) - new Date(b.date));

  if (chartData.length === 0) {
    return (
      <div className="chart-placeholder">
        <h4>Biểu đồ tiến độ</h4>
        <p>Chưa có đủ dữ liệu để vẽ biểu đồ. Hãy hoàn thành một bài test để xem tiến độ của bạn!</p>
      </div>
    );
  }

  return (
    <div className="progress-chart-container">
      <h4>Tiến độ luyện tập</h4>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart
          data={chartData}
          margin={{
            top: 5,
            right: 30,
            left: 0,
            bottom: 5,
          }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
          <XAxis dataKey="date" tick={{ fill: '#666', fontSize: 12 }} />
          <YAxis domain={[0, 9]} tick={{ fill: '#666', fontSize: 12 }} />
          <Tooltip content={<CustomTooltip />} />
          <Legend wrapperStyle={{ color: '#333' }} />
          <Line
            type="monotone"
            dataKey="bandScore"
            name="Band Score"
            stroke="#8c52ff"
            strokeWidth={2}
            activeDot={{ r: 8 }}
            dot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default ProgressChart;
