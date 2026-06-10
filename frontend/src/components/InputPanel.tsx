import { useState } from 'react';
import type { UserInput, Platform } from '../types';

interface Props {
  onStart: (users: UserInput[]) => void;
  disabled: boolean;
}

function InputPanel({ onStart, disabled }: Props) {
  const [handle, setHandle] = useState('');
  const [platform, setPlatform] = useState<Platform>('CODEFORCES');
  const [users, setUsers] = useState<UserInput[]>([]);

  const addUser = () => {
    const trimmed = handle.trim();
    if (!trimmed) return;
    setUsers(prev => [...prev, { handle: trimmed, platform }]);
    setHandle('');
  };

  const removeUser = (idx: number) => {
    setUsers(prev => prev.filter((_, i) => i !== idx));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') addUser();
  };

  return (
    <div style={styles.panel}>
      <h3 style={styles.heading}>添加用户</h3>
      <div style={styles.row}>
        <input
          style={styles.input}
          placeholder="用户名 handle"
          value={handle}
          onChange={e => setHandle(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
        />
        <select
          style={styles.select}
          value={platform}
          onChange={e => setPlatform(e.target.value as Platform)}
          disabled={disabled}
        >
          <option value="CODEFORCES">Codeforces</option>
          <option value="LUOGU">Luogu</option>
        </select>
        <button style={styles.addBtn} onClick={addUser} disabled={disabled}>
          添加
        </button>
      </div>
      {users.length > 0 && (
        <div style={styles.userList}>
          {users.map((u, i) => (
            <span key={i} style={styles.tag}>
              {u.handle} ({u.platform === 'CODEFORCES' ? 'CF' : 'LG'})
              <button style={styles.removeBtn} onClick={() => removeUser(i)}>
                ×
              </button>
            </span>
          ))}
        </div>
      )}
      <button
        style={{
          ...styles.startBtn,
          ...(disabled || users.length === 0 ? styles.startBtnDisabled : {}),
        }}
        onClick={() => onStart(users)}
        disabled={disabled || users.length === 0}
      >
        {disabled ? '爬取中...' : '▶ 开始爬取'}
      </button>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    background: '#fff',
    borderRadius: 8,
    padding: 20,
    boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
  },
  heading: {
    fontSize: 14,
    fontWeight: 600,
    color: '#333',
    marginBottom: 12,
  },
  row: {
    display: 'flex',
    gap: 8,
    marginBottom: 12,
  },
  input: {
    flex: 1,
    padding: '8px 12px',
    border: '1px solid #d9d9d9',
    borderRadius: 6,
    fontSize: 14,
    outline: 'none',
  },
  select: {
    padding: '8px 12px',
    border: '1px solid #d9d9d9',
    borderRadius: 6,
    fontSize: 14,
    background: '#fff',
    cursor: 'pointer',
  },
  addBtn: {
    padding: '8px 16px',
    background: '#1a73e8',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    cursor: 'pointer',
    fontWeight: 500,
  },
  userList: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  tag: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 4,
    padding: '4px 10px',
    background: '#e8f0fe',
    color: '#1a73e8',
    borderRadius: 16,
    fontSize: 13,
  },
  removeBtn: {
    background: 'none',
    border: 'none',
    color: '#999',
    cursor: 'pointer',
    fontSize: 16,
    lineHeight: 1,
    padding: '0 2px',
  },
  startBtn: {
    width: '100%',
    padding: '10px 0',
    background: '#2ea043',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 16,
    fontWeight: 600,
    cursor: 'pointer',
  },
  startBtnDisabled: {
    background: '#94d3a2',
    cursor: 'not-allowed',
  },
};

export default InputPanel;
