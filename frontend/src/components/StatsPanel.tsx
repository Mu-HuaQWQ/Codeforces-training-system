import type { UserStatsResponse } from '../types';

interface Props {
  results: UserStatsResponse[];
}

function StatsPanel({ results }: Props) {
  if (!results || results.length === 0) {
    return (
      <div style={styles.empty}>
        <p>统计数据将在爬取完成后显示</p>
      </div>
    );
  }

  return (
    <div style={styles.wrapper}>
      <table style={styles.table}>
        <thead>
          <tr>
            <th>用户</th>
            <th>平台</th>
            <th>Rating</th>
            <th>总提交</th>
            <th>AC 数</th>
            <th>通过率</th>
            <th>连续 AC</th>
          </tr>
        </thead>
        <tbody>
          {results.map(r => {
            if (r.failed) {
              return (
                <tr key={r.handle} style={styles.failedRow}>
                  <td>{r.handle}</td>
                  <td>{r.platform}</td>
                  <td colSpan={5} style={{ color: '#f44336' }}>
                    失败: {r.error}
                  </td>
                </tr>
              );
            }
            const s = r.stats!;
            const p = r.profile;
            return (
              <tr key={r.handle}>
                <td style={styles.handle}>{r.handle}</td>
                <td>{r.platform}</td>
                <td>{p?.rating ?? '-'}</td>
                <td>{s.totalSubmissions}</td>
                <td style={{ color: '#2ea043', fontWeight: 600 }}>
                  {s.acceptedCount}
                </td>
                <td>{s.acceptanceRate.toFixed(1)}%</td>
                <td>{s.maxStreak} 天</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    overflowX: 'auto',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: 14,
  },
  empty: {
    textAlign: 'center',
    padding: 60,
    color: '#999',
    fontSize: 15,
  },
  handle: {
    fontWeight: 600,
    color: '#1a73e8',
  },
  failedRow: {
    background: '#fff5f5',
  },
};

export default StatsPanel;
