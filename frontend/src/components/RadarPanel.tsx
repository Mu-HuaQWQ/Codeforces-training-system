import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';
import { Radar } from 'react-chartjs-2';
import type { RadarData } from '../types';

ChartJS.register(
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend
);

interface Props {
  data: RadarData[];
}

const COLORS = [
  'rgba(26, 115, 232, 0.7)',
  'rgba(244, 67, 54, 0.7)',
  'rgba(46, 160, 67, 0.7)',
  'rgba(255, 152, 0, 0.7)',
];

function RadarPanel({ data }: Props) {
  if (!data || data.length === 0) {
    return (
      <div style={styles.empty}>
        <p>雷达图将在爬取完成后显示</p>
        <p style={styles.hint}>添加用户并开始爬取，完成后自动生成能力对比雷达图</p>
      </div>
    );
  }

  const chartData = {
    labels: data[0].labels,
    datasets: data.map((d, i) => ({
      label: d.handle,
      data: d.values,
      backgroundColor: COLORS[i % COLORS.length].replace('0.7', '0.15'),
      borderColor: COLORS[i % COLORS.length],
      borderWidth: 2,
      pointBackgroundColor: COLORS[i % COLORS.length],
      pointRadius: 3,
    })),
  };

  const maxVal = Math.max(...data.flatMap(d => d.values), 1);
  const stepSize = Math.max(1, Math.ceil(maxVal / 6));

  const options = {
    responsive: true,
    maintainAspectRatio: true,
    scales: {
      r: {
        beginAtZero: true,
        ticks: {
          stepSize,
          font: { size: 11 },
        },
        pointLabels: {
          font: { size: 13 },
        },
      },
    },
    plugins: {
      legend: {
        position: 'bottom' as const,
      },
    },
  };

  return (
    <div style={styles.wrapper}>
      <Radar data={chartData} options={options} />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    maxWidth: 600,
    margin: '0 auto',
  },
  empty: {
    textAlign: 'center',
    padding: 60,
    color: '#999',
    fontSize: 15,
  },
  hint: {
    fontSize: 13,
    color: '#bbb',
    marginTop: 8,
  },
};

export default RadarPanel;
