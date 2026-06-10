import { useState } from 'react';
import type { Platform } from '../types';

interface Props {
  onClose: () => void;
  onAdded: () => void;
}

function AddStudentModal({ onClose, onAdded }: Props) {
  const [name, setName] = useState('');
  const [handle, setHandle] = useState('');
  const [platform, setPlatform] = useState<Platform>('CODEFORCES');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const submit = async () => {
    const trimmedName = name.trim();
    const trimmedHandle = handle.trim();
    if (!trimmedName || !trimmedHandle) {
      setError('请填写所有字段');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const { addStudent } = await import('../api');
      await addStudent({ name: trimmedName, handle: trimmedHandle, platform });
      onAdded();
    } catch (e) {
      setError('添加失败，可能该用户已存在');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={styles.overlay} onClick={onClose}>
      <div style={styles.modal} onClick={e => e.stopPropagation()}>
        <h3 style={styles.title}>添加学生</h3>
        {error && <div style={styles.error}>{error}</div>}
        <div style={styles.field}>
          <label style={styles.label}>学生姓名</label>
          <input style={styles.input} placeholder="如：张三" value={name}
            onChange={e => setName(e.target.value)} />
        </div>
        <div style={styles.field}>
          <label style={styles.label}>用户名</label>
          <input style={styles.input} placeholder="如：tourist" value={handle}
            onChange={e => setHandle(e.target.value)} />
        </div>
        <div style={styles.field}>
          <label style={styles.label}>平台</label>
          <select style={styles.select} value={platform}
            onChange={e => setPlatform(e.target.value as Platform)}>
            <option value="CODEFORCES">Codeforces</option>
            <option value="LUOGU">Luogu</option>
          </select>
        </div>
        <div style={styles.actions}>
          <button style={styles.cancelBtn} onClick={onClose}>取消</button>
          <button style={styles.submitBtn} onClick={submit} disabled={submitting}>
            {submitting ? '添加中...' : '确认添加'}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  overlay: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 },
  modal: { background: '#fff', borderRadius: 12, padding: 28, width: 400, boxShadow: '0 8px 30px rgba(0,0,0,0.15)' },
  title: { fontSize: 18, fontWeight: 600, marginBottom: 20, color: '#333' },
  error: { padding: '8px 12px', background: '#fff5f5', color: '#e74c3c', borderRadius: 6, marginBottom: 12, fontSize: 13 },
  field: { marginBottom: 16 },
  label: { display: 'block', fontSize: 13, fontWeight: 500, color: '#666', marginBottom: 6 },
  input: { width: '100%', padding: '8px 12px', border: '1px solid #d9d9d9', borderRadius: 6, fontSize: 14, outline: 'none', boxSizing: 'border-box' },
  select: { width: '100%', padding: '8px 12px', border: '1px solid #d9d9d9', borderRadius: 6, fontSize: 14, background: '#fff', boxSizing: 'border-box' },
  actions: { display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 },
  cancelBtn: { padding: '8px 20px', background: '#f5f5f5', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer' },
  submitBtn: { padding: '8px 20px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer', fontWeight: 500 },
};

export default AddStudentModal;
