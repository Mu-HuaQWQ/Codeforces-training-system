import { useEffect, useRef } from 'react';
import type { ProgressInfo } from '../types';

interface Props {
  progress: ProgressInfo;
  log: string[];
}

function ProgressPanel({ progress, log }: Props) {
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [log]);

  const done = progress.completed + progress.failed;
  const pct = progress.total > 0 ? Math.round((done / progress.total) * 100) : 0;

  return (
    <div style={styles.panel}>
      <div style={styles.status}>
        {progress.total === 0
          ? '等待开始...'
          : progress.done
            ? `完成: ${progress.completed} 成功, ${progress.failed} 失败`
            : `进行中: ${progress.completed} 完成, ${progress.failed} 失败, ${progress.total} 总数`}
      </div>
      <div style={styles.barWrapper}>
        <div style={styles.barTrack}>
          <div style={{ ...styles.barFill, width: `${pct}%` }} />
        </div>
        <span style={styles.barText}>{pct}%</span>
      </div>
      <div style={styles.logBox}>
        {log.length === 0 ? (
          <span style={styles.logPlaceholder}>日志将在此显示...</span>
        ) : (
          log.map((line, i) => (
            <div
              key={i}
              style={{
                ...styles.logLine,
                color: line.startsWith('[FAIL]') ? '#f44336' : '#d4d4d4',
              }}
            >
              {line}
            </div>
          ))
        )}
        <div ref={logEndRef} />
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    display: 'flex',
    flexDirection: 'column',
    gap: 16,
  },
  status: {
    fontSize: 15,
    fontWeight: 600,
    color: '#333',
  },
  barWrapper: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  },
  barTrack: {
    flex: 1,
    height: 24,
    background: '#e8e8e8',
    borderRadius: 12,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    background: 'linear-gradient(90deg, #1a73e8, #4da3ff)',
    borderRadius: 12,
    transition: 'width 0.3s ease',
  },
  barText: {
    fontSize: 13,
    color: '#666',
    minWidth: 36,
    textAlign: 'right',
  },
  logBox: {
    maxHeight: 300,
    overflowY: 'auto',
    background: '#1e1e1e',
    borderRadius: 8,
    padding: 12,
    fontFamily: 'Consolas, "Courier New", monospace',
    fontSize: 13,
  },
  logLine: {
    lineHeight: 1.7,
  },
  logPlaceholder: {
    color: '#666',
  },
};

export default ProgressPanel;
