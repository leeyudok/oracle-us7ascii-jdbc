package kr.doksam.oracle.us7ascii;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * US7ASCII Oracle DB에서 EUC-KR 한글 처리를 위한 Connection 래퍼.
 *
 * <p>Statement, PreparedStatement, CallableStatement 생성 시 각각의 래퍼 클래스로 감싸서 반환하며,
 * SQL 문자열이 포함된 메서드({@code prepareStatement}, {@code prepareCall}, {@code nativeSQL})에서는
 * {@link CharsetUtils#toDb(String)}로 인코딩 변환을 적용한다.</p>
 *
 * @author 덕삼이
 * @see CharsetStatement
 * @see CharsetPreparedStatement
 * @see CharsetCallableStatement
 */
public class CharsetConnection implements Connection {

    /** 실제 Oracle JDBC Connection */
    private final Connection delegate;

    /**
     * 래퍼 Connection을 생성한다.
     *
     * @param delegate 실제 Oracle JDBC Connection
     */
    public CharsetConnection(Connection delegate) {
        this.delegate = delegate;
    }

    /** Statement 생성 시 {@link CharsetStatement}로 래핑하여 반환한다. */
    @Override
    public Statement createStatement() throws SQLException {
        return new CharsetStatement(delegate.createStatement(), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new CharsetPreparedStatement(delegate.prepareStatement(transformForDriver(sql)), this);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return new CharsetCallableStatement(delegate.prepareCall(transformForDriver(sql)), this);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return delegate.nativeSQL(transformForDriver(sql));
    }

    /**
     * SQL 문자열을 Oracle JDBC 드라이버에 전달하기 전에 변환한다.
     *
     * <p>한글 리터럴이 포함된 경우 {@code UTL_RAW} 함수 호출로 치환하여,
     * Oracle JDBC의 US7ASCII 캐릭터셋 변환에 의한 데이터 손실을 방지한다.
     * ASCII만 포함된 SQL은 변환 없이 그대로 반환한다.</p>
     *
     * @param sql 원본 SQL
     * @return 변환된 SQL (한글 리터럴 → UTL_RAW)
     */
    private String transformForDriver(String sql) {
        if (sql == null) return null;
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("COMMENT ON")) {
            return CharsetUtils.transformCommentOn(sql);
        }
        return CharsetUtils.transformSql(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return delegate.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        delegate.rollback();
    }

    @Override
    public void close() throws SQLException {
        delegate.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return delegate.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return delegate.getTransactionIsolation();
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
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new CharsetStatement(delegate.createStatement(resultSetType, resultSetConcurrency), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return new CharsetPreparedStatement(
                delegate.prepareStatement(transformForDriver(sql), resultSetType, resultSetConcurrency), this);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new CharsetCallableStatement(
                delegate.prepareCall(transformForDriver(sql), resultSetType, resultSetConcurrency), this);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        delegate.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return delegate.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return new CharsetStatement(
                delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return new CharsetPreparedStatement(delegate.prepareStatement(transformForDriver(sql), resultSetType,
                resultSetConcurrency, resultSetHoldability), this);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return new CharsetCallableStatement(
                delegate.prepareCall(transformForDriver(sql), resultSetType, resultSetConcurrency, resultSetHoldability), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new CharsetPreparedStatement(delegate.prepareStatement(transformForDriver(sql), autoGeneratedKeys), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new CharsetPreparedStatement(delegate.prepareStatement(transformForDriver(sql), columnIndexes), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new CharsetPreparedStatement(delegate.prepareStatement(transformForDriver(sql), columnNames), this);
    }

    @Override
    public Clob createClob() throws SQLException {
        return delegate.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return delegate.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return delegate.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return delegate.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return delegate.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return delegate.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return delegate.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return delegate.getNetworkTimeout();
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
