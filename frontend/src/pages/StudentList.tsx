import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Student } from '../types';
import { getStudents, deleteStudent, refreshAll } from '../api';

interface Props {
  onAddClick: () => void;
  refreshKey: number;
}

function StudentList({ onAddClick, refreshKey }: Props) {
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    getStudents().then(setStudents).catch(console.error);
  }, [refreshKey]);

  const handleRefreshAll = async () => {
    setLoading(true);
    try {
      await refreshAll();
      const list = await getStudents();
      setStudents(list);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`确定删除 ${name}？`)) return;
    await deleteStudent(id);
    setStudents(prev => prev.filter(s => s.id !== id));
  };

  return (
    <div style={styles.wrapper}>
      <div style={styles.toolbar}>
        <button style={styles.addBtn} onClick={onAddClick}>+ 添加学生</button>
        <button style={styles.refreshBtn} onClick={handleRefreshAll} disabled={loading}>
          {loading ? '更新中...' : '🔄 更新全部数据'}
        </button>
      </div>
      {students.length === 0 ? (
        <div style={styles.empty}>
          <p>暂无学生，点击「添加学生」开始</p>
        </div>
      ) : (
        <table style={styles.table}>
          <thead>
            <tr>
              <th>姓名</th>
              <th>用户名</th>
              <th>平台</th>
              <th>当前分数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {students.map(s => (
              <tr key={s.id}>
                <td style={styles.nameCell}>{s.name}</td>
                <td style={styles.mono}>{s.handle}</td>
                <td>
                  <span style={s.platform === 'CODEFORCES' ? styles.cfBadge : styles.lgBadge}>
                    {s.platform === 'CODEFORCES' ? 'CF' : 'LG'}
                  </span>
                </td>
                <td style={styles.rating}>{s.currentRating ?? '-'}</td>
                <td>
                  <button style={styles.detailBtn} onClick={() => navigate(`/${s.id}`)}>详情</button>
                  <button style={styles.delBtn} onClick={() => handleDelete(s.id, s.name)}>删除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: { background: '#fff', borderRadius: 8, padding: 20, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' },
  toolbar: { display: 'flex', gap: 12, marginBottom: 16 },
  addBtn: { padding: '8px 20px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer', fontWeight: 500 },
  refreshBtn: { padding: '8px 20px', background: '#fff', color: '#1a73e8', border: '1px solid #1a73e8', borderRadius: 6, fontSize: 14, cursor: 'pointer' },
  empty: { textAlign: 'center', padding: 60, color: '#999', fontSize: 16 },
  table: { width: '100%', borderCollapse: 'collapse' },
  nameCell: { fontWeight: 600, color: '#1a73e8' },
  mono: { fontFamily: 'monospace', fontSize: 14 },
  rating: { fontWeight: 600, color: '#2ea043' },
  cfBadge: { padding: '2px 8px', borderRadius: 4, background: '#fff0e6', color: '#e67e22', fontSize: 12, fontWeight: 600 },
  lgBadge: { padding: '2px 8px', borderRadius: 4, background: '#e8f0fe', color: '#1a73e8', fontSize: 12, fontWeight: 600 },
  detailBtn: { padding: '4px 12px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, fontSize: 13, cursor: 'pointer', marginRight: 8 },
  delBtn: { padding: '4px 12px', background: '#fff', color: '#e74c3c', border: '1px solid #e74c3c', borderRadius: 4, fontSize: 13, cursor: 'pointer' },
};

export default StudentList;
