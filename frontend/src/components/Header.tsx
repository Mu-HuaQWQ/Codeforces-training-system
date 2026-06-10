function Header() {
  return (
    <header style={styles.header}>
      <h1 style={styles.title}>训练管理系统</h1>
      <span style={styles.subtitle}>Codeforces & Luogu 学生训练追踪</span>
    </header>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex',
    alignItems: 'baseline',
    gap: 16,
    padding: '16px 0',
  },
  title: {
    fontSize: 24,
    fontWeight: 700,
    color: '#1a73e8',
    margin: 0,
  },
  subtitle: {
    fontSize: 14,
    color: '#999',
  },
};

export default Header;
