package kr.doksam.oracle.us7ascii;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

/**
 * US7ASCII Oracle DB에서 EUC-KR 한글 처리를 위한 PreparedStatement 래퍼.
 *
 * <p>파라미터 바인딩 시 Oracle JDBC의 캐릭터셋 변환을 우회하여 한글을 안전하게 전송한다.</p>
 *
 * <h3>핵심 우회 기법</h3>
 * <ul>
 *   <li>{@link #setString(int, String)} — {@code setAsciiStream()}을 사용하여 EUC-KR raw bytes를
 *       "ASCII 스트림"으로 위장해 전송. Oracle JDBC의 캐릭터셋 변환을 완전히 우회함.</li>
 *   <li>{@link #setObject(int, Object)} / {@link #setNString(int, String)} —
 *       {@link CharsetUtils#toDb(String)}로 ISO-8859-1 바이트 표현으로 변환 후 전달.</li>
 * </ul>
 *
 * @author 덕삼이
 * @see CharsetUtils#toDb(String)
 */
public class CharsetPreparedStatement extends CharsetStatement implements PreparedStatement {

    /** 실제 Oracle JDBC PreparedStatement */
    private final PreparedStatement delegatePs;

    /**
     * Reader의 전체 내용을 문자열로 읽어들인다.
     *
     * @param reader 읽을 Reader
     * @return 전체 내용 문자열
     * @throws SQLException 읽기 실패 시
     */
    private static String readAll(java.io.Reader reader) throws SQLException {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
            return sb.toString();
        } catch (java.io.IOException e) {
            throw new SQLException("Reader 읽기 실패", e);
        }
    }

    /**
     * 래퍼 PreparedStatement를 생성한다.
     *
     * @param delegate 실제 Oracle JDBC PreparedStatement
     */
    public CharsetPreparedStatement(PreparedStatement delegate) {
        this(delegate, null);
    }

    /**
     * 래퍼 PreparedStatement를 생성한다 (Connection 참조 포함).
     *
     * @param delegate 실제 Oracle JDBC PreparedStatement
     * @param wrappedConnection 래핑된 Connection
     */
    public CharsetPreparedStatement(PreparedStatement delegate, java.sql.Connection wrappedConnection) {
        super(delegate, wrappedConnection);
        this.delegatePs = delegate;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return new CharsetResultSet(delegatePs.executeQuery(), this);
    }

    @Override
    public int executeUpdate() throws SQLException {
        return delegatePs.executeUpdate();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        delegatePs.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        delegatePs.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        delegatePs.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        delegatePs.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        delegatePs.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        delegatePs.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        delegatePs.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        delegatePs.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        delegatePs.setBigDecimal(parameterIndex, x);
    }

    /**
     * 문자열 파라미터를 EUC-KR raw bytes로 변환하여 {@code setAsciiStream()}으로 전송한다.
     *
     * <p>Oracle JDBC의 {@code setString()}은 클라이언트 캐릭터셋(US7ASCII)에 맞춰
     * 변환을 시도하여 한글이 손상된다. 이를 우회하기 위해 EUC-KR 바이트를
     * ASCII 스트림으로 위장하여 raw bytes 그대로 DB에 전달한다.</p>
     *
     * @param parameterIndex 파라미터 인덱스 (1부터 시작)
     * @param x 바인딩할 문자열 (한글 포함 가능). {@code null}이면 SQL NULL 설정
     * @throws SQLException 인코딩 변환 또는 파라미터 설정 실패 시
     */
    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        if (x == null) {
            delegatePs.setNull(parameterIndex, Types.VARCHAR);
        } else {
            try {
                byte[] bytes = x.getBytes("EUC-KR");
                delegatePs.setAsciiStream(parameterIndex, new java.io.ByteArrayInputStream(bytes), bytes.length);
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SQLException("Encoding error", e);
            }
        }
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        delegatePs.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        delegatePs.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        delegatePs.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        delegatePs.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        delegatePs.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        delegatePs.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        delegatePs.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        delegatePs.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        if (x instanceof String) {
            delegatePs.setObject(parameterIndex, CharsetUtils.toDb((String) x), targetSqlType);
        } else {
            delegatePs.setObject(parameterIndex, x, targetSqlType);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        if (x instanceof String) {
            delegatePs.setObject(parameterIndex, CharsetUtils.toDb((String) x));
        } else {
            delegatePs.setObject(parameterIndex, x);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        return delegatePs.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        delegatePs.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        if (reader == null) {
            delegatePs.setNull(parameterIndex, java.sql.Types.CLOB);
        } else {
            String text = readAll(reader);
            setString(parameterIndex, text);
        }
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        delegatePs.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        delegatePs.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        if (x == null) {
            delegatePs.setNull(parameterIndex, java.sql.Types.CLOB);
        } else {
            String text = x.getSubString(1, (int) x.length());
            setString(parameterIndex, text);
        }
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        delegatePs.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return delegatePs.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        delegatePs.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        delegatePs.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        delegatePs.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        delegatePs.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        delegatePs.setURL(parameterIndex, x);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        delegatePs.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        if (value == null) {
            delegatePs.setNull(parameterIndex, Types.NVARCHAR);
        } else {
            delegatePs.setNString(parameterIndex, CharsetUtils.toDb(value));
        }
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        delegatePs.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        delegatePs.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        if (reader == null) {
            delegatePs.setNull(parameterIndex, java.sql.Types.CLOB);
        } else {
            String text = readAll(reader);
            setString(parameterIndex, text);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        delegatePs.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        delegatePs.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        delegatePs.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        if (x instanceof String) {
            delegatePs.setObject(parameterIndex, CharsetUtils.toDb((String) x), targetSqlType, scaleOrLength);
        } else {
            delegatePs.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        delegatePs.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        delegatePs.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        if (reader == null) {
            delegatePs.setNull(parameterIndex, java.sql.Types.CLOB);
        } else {
            String text = readAll(reader);
            setString(parameterIndex, text);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        delegatePs.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        delegatePs.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        if (reader == null) {
            delegatePs.setNull(parameterIndex, java.sql.Types.CLOB);
        } else {
            String text = readAll(reader);
            setString(parameterIndex, text);
        }
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        delegatePs.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        if (reader == null) {
            delegatePs.setNull(parameterIndex, java.sql.Types.CLOB);
        } else {
            String text = readAll(reader);
            setString(parameterIndex, text);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        delegatePs.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        delegatePs.setNClob(parameterIndex, reader);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return delegatePs.getParameterMetaData();
    }

    // ── JDBC 4.2 (Java 8) 추가 메서드 ────────────────────────────────

    @Override
    public long executeLargeUpdate() throws SQLException {
        return delegatePs.executeLargeUpdate();
    }

    /**
     * SQLType 기반 파라미터 설정. String인 경우 {@link CharsetUtils#toDb(String)}로 변환한다.
     *
     * @param parameterIndex 파라미터 인덱스
     * @param x 바인딩할 값
     * @param targetSqlType SQL 타입 (java.sql.SQLType)
     * @throws SQLException 설정 실패 시
     */
    @Override
    public void setObject(int parameterIndex, Object x, java.sql.SQLType targetSqlType) throws SQLException {
        if (x instanceof String) {
            delegatePs.setObject(parameterIndex, CharsetUtils.toDb((String) x), targetSqlType);
        } else {
            delegatePs.setObject(parameterIndex, x, targetSqlType);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, java.sql.SQLType targetSqlType, int scaleOrLength)
            throws SQLException {
        if (x instanceof String) {
            delegatePs.setObject(parameterIndex, CharsetUtils.toDb((String) x), targetSqlType, scaleOrLength);
        } else {
            delegatePs.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        }
    }
}
