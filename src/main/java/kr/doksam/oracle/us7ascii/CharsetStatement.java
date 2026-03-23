package kr.doksam.oracle.us7ascii;

import java.sql.*;

/**
 * US7ASCII Oracle DB에서 EUC-KR 한글 처리를 위한 Statement 래퍼.
 *
 * <p>SQL 문자열이 전달되는 모든 실행 메서드({@code executeQuery}, {@code executeUpdate},
 * {@code execute}, {@code addBatch})에서 한글 리터럴을 자동 변환한다.</p>
 *
 * <h3>변환 전략</h3>
 * <ul>
 *   <li>일반 SQL — {@link CharsetUtils#transformSql(String)}으로 리터럴을 {@code UTL_RAW} 함수 호출로 치환</li>
 *   <li>{@code COMMENT ON} 구문 — {@link CharsetUtils#transformCommentOn(String)}으로 PL/SQL 블록 래핑</li>
 * </ul>
 *
 * <p>ResultSet 반환 시 {@link CharsetResultSet}으로 래핑하여 조회 결과의 한글도 자동 복원한다.</p>
 *
 * @author 덕삼이
 * @see CharsetUtils#transformSql(String)
 * @see CharsetUtils#transformCommentOn(String)
 */
public class CharsetStatement implements Statement {

    /** 실제 Oracle JDBC Statement */
    protected final Statement delegate;

    /** 이 Statement를 생성한 래핑된 Connection (null이면 delegate에서 가져옴) */
    private final Connection wrappedConnection;

    /**
     * 래퍼 Statement를 생성한다.
     *
     * @param delegate 실제 Oracle JDBC Statement
     */
    public CharsetStatement(Statement delegate) {
        this(delegate, null);
    }

    /**
     * 래퍼 Statement를 생성한다 (Connection 참조 포함).
     *
     * @param delegate 실제 Oracle JDBC Statement
     * @param wrappedConnection 래핑된 Connection (getConnection() 시 반환)
     */
    public CharsetStatement(Statement delegate, Connection wrappedConnection) {
        this.delegate = delegate;
        this.wrappedConnection = wrappedConnection;
    }

    /**
     * SQL에 포함된 한글 리터럴을 Oracle이 처리할 수 있는 형태로 변환한다.
     *
     * <p>{@code COMMENT ON} 구문이면 PL/SQL 블록으로, 그 외에는 {@code UTL_RAW} 함수로 치환한다.</p>
     *
     * @param sql 원본 SQL
     * @return 변환된 SQL
     */
    private String transform(String sql) {
        if (sql == null)
            return null;
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("COMMENT ON")) {
            return CharsetUtils.transformCommentOn(sql);
        }
        return CharsetUtils.transformSql(sql);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return new CharsetResultSet(delegate.executeQuery(transform(sql)), this);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return delegate.executeUpdate(transform(sql));
    }

    @Override
    public void close() throws SQLException {
        delegate.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return delegate.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        delegate.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return delegate.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        delegate.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        delegate.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return delegate.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        delegate.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        delegate.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        delegate.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        delegate.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return delegate.execute(transform(sql));
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        ResultSet rs = delegate.getResultSet();
        return rs == null ? null : new CharsetResultSet(rs, this);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return delegate.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return delegate.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        delegate.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return delegate.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        delegate.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return delegate.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return delegate.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return delegate.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        delegate.addBatch(transform(sql));
    }

    @Override
    public void clearBatch() throws SQLException {
        delegate.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return delegate.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (wrappedConnection != null) {
            return wrappedConnection;
        }
        // fallback: 래핑된 Connection 참조 없으면 CharsetConnection으로 감싸서 반환
        return new CharsetConnection(delegate.getConnection());
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return delegate.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return new CharsetResultSet(delegate.getGeneratedKeys(), this);
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return delegate.executeUpdate(transform(sql), autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return delegate.executeUpdate(transform(sql), columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return delegate.executeUpdate(transform(sql), columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return delegate.execute(transform(sql), autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return delegate.execute(transform(sql), columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return delegate.execute(transform(sql), columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return delegate.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        delegate.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return delegate.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        delegate.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return delegate.isCloseOnCompletion();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }
        return delegate.isWrapperFor(iface);
    }

}
