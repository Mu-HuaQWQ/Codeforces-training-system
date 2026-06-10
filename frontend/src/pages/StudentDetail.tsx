import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { StudentDetail as StudentDetailType } from '../types';
import { getStudentDetail } from '../api';
import RadarPanel from '../components/RadarPanel';
import StatsPanel from '../components/StatsPanel';
import RatingChart from '../components/RatingChart';

function StudentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<StudentDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<'contests' | 'radar' | 'stats'>('contests');
  const [contestPage, setContestPage] = useState(0);
  const PAGE_SIZE = 15;

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setContestPage(0);
    getStudentDetail(Number(id))
      .then(d => { setDetail(d); setLoading(false); })
      .catch(e => { setError('加载失败: ' + e); setLoading(false); });
  }, [id]);

  if (loading) return <div style={styles.center}>加载中...</div>;
  if (error) return <div style={{ ...styles.center, color: '#e74c3c' }}>{error}</div>;
  if (!detail) return <div style={styles.center}>无数据</div>;

  const tabs = [
    { key: 'contests' as const, label: '最近比赛' },
    { key: 'radar' as const, label: '雷达图' },
    { key: 'stats' as const, label: '统计' },
  ];

  const totalPages = Math.max(1, Math.ceil(detail.contests.length / PAGE_SIZE));
  const displayedContests = detail.contests.slice(
    contestPage * PAGE_SIZE, (contestPage + 1) * PAGE_SIZE,
  );

  return (
    <div style={styles.wrapper}>
      <button style={styles.backBtn} onClick={() => navigate('/')}>← 返回</button>

      {/* Header */}
      <div style={styles.header}>
        <h2 style={styles.name}>{detail.name}</h2>
        <span style={styles.handle}>{detail.handle}</span>
        <span style={detail.platform === 'CODEFORCES' ? styles.cfBadge : styles.lgBadge}>
          {detail.platform === 'CODEFORCES' ? 'Codeforces' : 'Luogu'}
        </span>
        {detail.profile && (
          <span style={styles.rating}>
            Rating: {detail.profile.rating ?? '-'} (max {detail.profile.maxRating ?? '-'})
          </span>
        )}
      </div>

      {/* Rating Chart */}
      <div style={styles.section}>
        <RatingChart data={detail.ratingHistory} />
      </div>

      {/* Tabs */}
      <div style={styles.tabBar}>
        {tabs.map(t => (
          <button key={t.key}
            style={{ ...styles.tab, ...(tab === t.key ? styles.tabActive : {}) }}
            onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'contests' && (
        <div style={styles.section}>
          {detail.contests.length === 0 ? (
            <div style={styles.emptyText}>暂无比赛记录</div>
          ) : (
            <>
              <table style={styles.table}>
                <thead>
                  <tr><th>比赛名称</th><th>排名</th><th>过题数</th><th>Rating 变化</th><th>日期</th></tr>
                </thead>
                <tbody>
                  {displayedContests.map(c => (
                    <tr key={c.id}>
                      <td style={{ fontWeight: 500 }}>{c.contestName}</td>
                      <td>{c.rank != null ? `#${c.rank}` : '-'}</td>
                      <td>{c.solvedCount != null ? c.solvedCount : '-'}</td>
                      <td style={{
                        color: (c.ratingChange ?? 0) >= 0 ? '#2ea043' : '#e74c3c',
                        fontWeight: 600,
                      }}>
                        {c.ratingChange != null
                          ? `${c.ratingChange >= 0 ? '+' : ''}${c.ratingChange}`
                          : '-'}
                        ({c.oldRating} → {c.newRating})
                      </td>
                      <td style={styles.dateCell}>
                        {new Date(c.contestDate).toLocaleDateString('zh-CN', {
                          year: 'numeric', month: '2-digit', day: '2-digit',
                        })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {totalPages > 1 && (
                <div style={styles.pagination}>
                  <button style={styles.pageBtn} disabled={contestPage === 0}
                    onClick={() => setContestPage(0)}>««</button>
                  <button style={styles.pageBtn} disabled={contestPage === 0}
                    onClick={() => setContestPage(p => p - 1)}>«</button>
                  <span style={styles.pageInfo}>
                    第 {contestPage + 1} / {totalPages} 页（共 {detail.contests.length} 场）
                  </span>
                  <button style={styles.pageBtn} disabled={contestPage >= totalPages - 1}
                    onClick={() => setContestPage(p => p + 1)}>»</button>
                  <button style={styles.pageBtn} disabled={contestPage >= totalPages - 1}
                    onClick={() => setContestPage(totalPages - 1)}>»»</button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {tab === 'radar' && (
        <div style={styles.section}>
          {detail.radarData.length > 0 ? (
            <RadarPanel data={detail.radarData} />
          ) : (
            <div style={styles.emptyText}>暂无雷达图数据</div>
          )}
        </div>
      )}

      {tab === 'stats' && detail.stats && (
        <div style={styles.section}>
          <StatsPanel results={[{
            handle: detail.handle, platform: detail.platform,
            profile: detail.profile, stats: detail.stats,
            failed: false, error: null,
          }]} />
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: { maxWidth: 1400, margin: '0 auto' },
  center: { textAlign: 'center', padding: 60 },
  backBtn: { padding: '6px 16px', background: '#f5f5f5', border: 'none', borderRadius: 6, cursor: 'pointer', marginBottom: 16, fontSize: 14 },
  header: { display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', marginBottom: 20 },
  name: { fontSize: 24, fontWeight: 700, color: '#333' },
  handle: { fontFamily: 'monospace', fontSize: 15, color: '#666' },
  rating: { fontWeight: 600, color: '#2ea043', fontSize: 15 },
  cfBadge: { padding: '2px 10px', borderRadius: 4, background: '#fff0e6', color: '#e67e22', fontSize: 13, fontWeight: 600 },
  lgBadge: { padding: '2px 10px', borderRadius: 4, background: '#e8f0fe', color: '#1a73e8', fontSize: 13, fontWeight: 600 },
  section: { background: '#fff', borderRadius: 8, padding: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.08)', marginBottom: 16 },
  tabBar: { display: 'flex', gap: 4, marginBottom: 16 },
  tab: { padding: '8px 20px', background: '#f5f5f5', border: 'none', borderRadius: '6px 6px 0 0', cursor: 'pointer', fontSize: 14 },
  tabActive: { background: '#fff', color: '#1a73e8', fontWeight: 600 },
  table: { width: '100%', borderCollapse: 'collapse' },
  dateCell: { color: '#888', fontSize: 13, whiteSpace: 'nowrap' },
  emptyText: { textAlign: 'center', padding: 40, color: '#999' },
  pagination: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    gap: 8, marginTop: 16, paddingTop: 12, borderTop: '1px solid #f0f0f0',
  },
  pageBtn: {
    padding: '4px 12px', background: '#fff', border: '1px solid #d9d9d9',
    borderRadius: 4, fontSize: 13, cursor: 'pointer', color: '#333',
  },
  pageInfo: { fontSize: 13, color: '#888', margin: '0 8px' },
};

export default StudentDetail;
