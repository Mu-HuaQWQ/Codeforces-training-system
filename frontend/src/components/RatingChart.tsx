import { useState, useMemo } from 'react';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement,
  Tooltip, Legend, Filler,
} from 'chart.js';
import type { RatingRecord } from '../types';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler);

type Range = '1M' | '3M' | '6M' | '1Y' | 'ALL';

interface Props {
  data: RatingRecord[];
}

function RatingChart({ data }: Props) {
  const [range, setRange] = useState<Range>('ALL');

  const filtered = useMemo(() => {
    if (range === 'ALL' || !data || data.length === 0) return data;
    const now = Date.now();
    const cutoff = {
      '1M': now - 30 * 86400000,
      '3M': now - 90 * 86400000,
      '6M': now - 180 * 86400000,
      '1Y': now - 365 * 86400000,
    }[range];
    return data.filter(r => new Date(r.recordedAt).getTime() >= cutoff);
  }, [data, range]);

  if (!data || data.length === 0) {
    return <div style={styles.empty}>暂无 rating 历史数据</div>;
  }

  const displayData = filtered.length > 0 ? filtered : data;

  const chartData = {
    labels: displayData.map(r => {
      const d = new Date(r.recordedAt);
      return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`;
    }),
    datasets: [{
      label: 'Rating',
      data: displayData.map(r => r.rating),
      fill: true,
      borderColor: '#1a73e8',
      backgroundColor: 'rgba(26, 115, 232, 0.1)',
      tension: 0.3,
      pointRadius: 3,
      pointBackgroundColor: '#1a73e8',
    }],
  };

  const minRating = Math.min(...displayData.map(r => r.rating));
  const maxRating = Math.max(...displayData.map(r => r.rating));
  const padding = Math.max(50, (maxRating - minRating) * 0.15);

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { min: minRating - padding, max: maxRating + padding },
    },
  };

  const rangeOptions: { key: Range; label: string }[] = [
    { key: '1M', label: '近1月' },
    { key: '3M', label: '近3月' },
    { key: '6M', label: '近6月' },
    { key: '1Y', label: '近1年' },
    { key: 'ALL', label: '全部' },
  ];

  return (
    <div style={styles.wrapper}>
      <div style={styles.topBar}>
        <span style={{ fontSize: 13, color: '#666' }}>{displayData.length} 个数据点</span>
        <select
          style={styles.rangeSelect}
          value={range}
          onChange={e => setRange(e.target.value as Range)}
        >
          {rangeOptions.map(o => (
            <option key={o.key} value={o.key}>{o.label}</option>
          ))}
        </select>
      </div>
      <div style={{ height: 280 }}>
        <Line data={chartData} options={options} />
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: { background: '#fff', borderRadius: 8, padding: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' },
  topBar: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  rangeSelect: {
    padding: '4px 10px', border: '1px solid #d9d9d9', borderRadius: 6,
    fontSize: 13, background: '#fff', cursor: 'pointer', outline: 'none',
  },
  empty: { textAlign: 'center', padding: 40, color: '#999' },
};

export default RatingChart;
