package kr.doksam.oracle.us7ascii;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * US7ASCII Oracle DB에서 EUC-KR 한글 처리를 위한 CallableStatement 래퍼.
 *
 * <p>Stored Procedure/Function 호출 시 IN/OUT 파라미터의 한글 인코딩을 자동 변환한다.</p>
 *
 * <h3>변환 처리</h3>
 * <ul>
 *   <li><b>IN 파라미터</b> (앱→DB): 인덱스 기반은 부모 클래스({@link CharsetPreparedStatement})에서 처리,
 *       이름 기반({@code setString(String, String)})은 이 클래스에서 {@code setAsciiStream} 우회</li>
 *   <li><b>OUT 파라미터</b> (DB→앱): {@code getString}, {@code getNString}, {@code getObject} 등에서
 *       {@link CharsetUtils#toApp(String)}으로 ISO-8859-1 → EUC-KR 역변환</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * CallableStatement cstmt = conn.prepareCall("{call my_proc(?, ?)}");
 * cstmt.setString(1, "한글입력");              // 자동 EUC-KR raw bytes 전송
 * cstmt.registerOutParameter(2, Types.VARCHAR);
 * cstmt.execute();
 * String result = cstmt.getString(2);          // 자동 EUC-KR 역변환
 * </pre>
 *
 * @author 덕삼이
 * @see CharsetPreparedStatement
 * @see CharsetUtils
 */
public class CharsetCallableStatement extends CharsetPreparedStatement implements CallableStatement {

    /** 실제 Oracle JDBC CallableStatement */
    private final CallableStatement delegateCs;

    /**
     * 래퍼 CallableStatement를 생성한다.
     *
     * @param delegate 실제 Oracle JDBC CallableStatement
     */
    public CharsetCallableStatement(CallableStatement delegate) {
        this(delegate, null);
    }

    /**
     * 래퍼 CallableStatement를 생성한다 (Connection 참조 포함).
     *
     * @param delegate 실제 Oracle JDBC CallableStatement
     * @param wrappedConnection 래핑된 Connection
     */
    public CharsetCallableStatement(CallableStatement delegate, java.sql.Connection wrappedConnection) {
        super(delegate, wrappedConnection);
        this.delegateCs = delegate;
    }

    // ── registerOutParameter ──────────────────────────────────────────

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        delegateCs.registerOutParameter(parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        delegateCs.registerOutParameter(parameterIndex, sqlType, scale);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        delegateCs.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        delegateCs.registerOutParameter(parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        delegateCs.registerOutParameter(parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        delegateCs.registerOutParameter(parameterName, sqlType, typeName);
    }

    // ── wasNull ───────────────────────────────────────────────────────

    @Override
    public boolean wasNull() throws SQLException {
        return delegateCs.wasNull();
    }

    // ── OUT 파라미터 조회 (인덱스 기반) — 문자열은 toApp()으로 EUC-KR 역변환 ──

    /** OUT 파라미터 문자열을 EUC-KR로 역변환하여 반환한다. */
    @Override
    public String getString(int parameterIndex) throws SQLException {
        return CharsetUtils.toApp(delegateCs.getString(parameterIndex));
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws SQLException {
        return delegateCs.getBoolean(parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex) throws SQLException {
        return delegateCs.getByte(parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex) throws SQLException {
        return delegateCs.getShort(parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex) throws SQLException {
        return delegateCs.getInt(parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex) throws SQLException {
        return delegateCs.getLong(parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex) throws SQLException {
        return delegateCs.getFloat(parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex) throws SQLException {
        return delegateCs.getDouble(parameterIndex);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return delegateCs.getBigDecimal(parameterIndex, scale);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws SQLException {
        return delegateCs.getBytes(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex) throws SQLException {
        return delegateCs.getDate(parameterIndex);
    }

    @Override
    public Time getTime(int parameterIndex) throws SQLException {
        return delegateCs.getTime(parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return delegateCs.getTimestamp(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex) throws SQLException {
        Object obj = delegateCs.getObject(parameterIndex);
        if (obj instanceof String) {
            return CharsetUtils.toApp((String) obj);
        }
        return obj;
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return delegateCs.getBigDecimal(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        Object obj = delegateCs.getObject(parameterIndex, map);
        if (obj instanceof String) {
            return CharsetUtils.toApp((String) obj);
        }
        return obj;
    }

    @Override
    public Ref getRef(int parameterIndex) throws SQLException {
        return delegateCs.getRef(parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex) throws SQLException {
        return delegateCs.getBlob(parameterIndex);
    }

    @Override
    public Clob getClob(int parameterIndex) throws SQLException {
        Clob clob = delegateCs.getClob(parameterIndex);
        return clob == null ? null : new CharsetClob(clob);
    }

    @Override
    public Array getArray(int parameterIndex) throws SQLException {
        return delegateCs.getArray(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return delegateCs.getDate(parameterIndex, cal);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return delegateCs.getTime(parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return delegateCs.getTimestamp(parameterIndex, cal);
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        return delegateCs.getURL(parameterIndex);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        return delegateCs.getRowId(parameterIndex);
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        return delegateCs.getNClob(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return delegateCs.getSQLXML(parameterIndex);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        return CharsetUtils.toApp(delegateCs.getNString(parameterIndex));
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return delegateCs.getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return delegateCs.getCharacterStream(parameterIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        T obj = delegateCs.getObject(parameterIndex, type);
        if (obj instanceof String) {
            return (T) CharsetUtils.toApp((String) obj);
        }
        return obj;
    }

    // ── OUT 파라미터 조회 (이름 기반) — 문자열은 toApp()으로 EUC-KR 역변환 ──

    /** OUT 파라미터 문자열을 EUC-KR로 역변환하여 반환한다. */
    @Override
    public String getString(String parameterName) throws SQLException {
        return CharsetUtils.toApp(delegateCs.getString(parameterName));
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        return delegateCs.getBoolean(parameterName);
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        return delegateCs.getByte(parameterName);
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        return delegateCs.getShort(parameterName);
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        return delegateCs.getInt(parameterName);
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        return delegateCs.getLong(parameterName);
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        return delegateCs.getFloat(parameterName);
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        return delegateCs.getDouble(parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        return delegateCs.getBytes(parameterName);
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        return delegateCs.getDate(parameterName);
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        return delegateCs.getTime(parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return delegateCs.getTimestamp(parameterName);
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        Object obj = delegateCs.getObject(parameterName);
        if (obj instanceof String) {
            return CharsetUtils.toApp((String) obj);
        }
        return obj;
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return delegateCs.getBigDecimal(parameterName);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        Object obj = delegateCs.getObject(parameterName, map);
        if (obj instanceof String) {
            return CharsetUtils.toApp((String) obj);
        }
        return obj;
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        return delegateCs.getRef(parameterName);
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        return delegateCs.getBlob(parameterName);
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        Clob clob = delegateCs.getClob(parameterName);
        return clob == null ? null : new CharsetClob(clob);
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        return delegateCs.getArray(parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return delegateCs.getDate(parameterName, cal);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return delegateCs.getTime(parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return delegateCs.getTimestamp(parameterName, cal);
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        return delegateCs.getURL(parameterName);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        return delegateCs.getRowId(parameterName);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        return delegateCs.getNClob(parameterName);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return delegateCs.getSQLXML(parameterName);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        return CharsetUtils.toApp(delegateCs.getNString(parameterName));
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return delegateCs.getNCharacterStream(parameterName);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return delegateCs.getCharacterStream(parameterName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        T obj = delegateCs.getObject(parameterName, type);
        if (obj instanceof String) {
            return (T) CharsetUtils.toApp((String) obj);
        }
        return obj;
    }

    // ── 이름 기반 파라미터 설정 — 문자열은 setAsciiStream 우회 또는 toDb() 변환 ──

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
        delegateCs.setURL(parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
        delegateCs.setNull(parameterName, sqlType);
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
        delegateCs.setBoolean(parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
        delegateCs.setByte(parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
        delegateCs.setShort(parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
        delegateCs.setInt(parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
        delegateCs.setLong(parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
        delegateCs.setFloat(parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
        delegateCs.setDouble(parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        delegateCs.setBigDecimal(parameterName, x);
    }

    /**
     * 이름 기반 문자열 파라미터를 EUC-KR raw bytes로 변환하여 {@code setAsciiStream()}으로 전송한다.
     *
     * @param parameterName 파라미터 이름
     * @param x 바인딩할 문자열. {@code null}이면 SQL NULL 설정
     * @throws SQLException 인코딩 변환 또는 파라미터 설정 실패 시
     */
    @Override
    public void setString(String parameterName, String x) throws SQLException {
        if (x == null) {
            delegateCs.setNull(parameterName, Types.VARCHAR);
        } else {
            try {
                byte[] bytes = x.getBytes("EUC-KR");
                delegateCs.setAsciiStream(parameterName, new java.io.ByteArrayInputStream(bytes), bytes.length);
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SQLException("Encoding error", e);
            }
        }
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
        delegateCs.setBytes(parameterName, x);
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
        delegateCs.setDate(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
        delegateCs.setTime(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        delegateCs.setTimestamp(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        delegateCs.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        delegateCs.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        if (x instanceof String) {
            delegateCs.setObject(parameterName, CharsetUtils.toDb((String) x), targetSqlType, scale);
        } else {
            delegateCs.setObject(parameterName, x, targetSqlType, scale);
        }
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        if (x instanceof String) {
            delegateCs.setObject(parameterName, CharsetUtils.toDb((String) x), targetSqlType);
        } else {
            delegateCs.setObject(parameterName, x, targetSqlType);
        }
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
        if (x instanceof String) {
            delegateCs.setObject(parameterName, CharsetUtils.toDb((String) x));
        } else {
            delegateCs.setObject(parameterName, x);
        }
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        delegateCs.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        delegateCs.setDate(parameterName, x, cal);
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        delegateCs.setTime(parameterName, x, cal);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        delegateCs.setTimestamp(parameterName, x, cal);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        delegateCs.setNull(parameterName, sqlType, typeName);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        delegateCs.setRowId(parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        if (value == null) {
            delegateCs.setNull(parameterName, Types.NVARCHAR);
        } else {
            delegateCs.setNString(parameterName, CharsetUtils.toDb(value));
        }
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        delegateCs.setNCharacterStream(parameterName, value, length);
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        delegateCs.setNClob(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        delegateCs.setClob(parameterName, reader, length);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        delegateCs.setBlob(parameterName, inputStream, length);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        delegateCs.setNClob(parameterName, reader, length);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        delegateCs.setSQLXML(parameterName, xmlObject);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        delegateCs.setBlob(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        delegateCs.setClob(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        delegateCs.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        delegateCs.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        delegateCs.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        delegateCs.setAsciiStream(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        delegateCs.setBinaryStream(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        delegateCs.setCharacterStream(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        delegateCs.setNCharacterStream(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
        delegateCs.setClob(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        delegateCs.setBlob(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        delegateCs.setNClob(parameterName, reader);
    }

    // ── JDBC 4.2 (Java 8) 추가 메서드 ────────────────────────────────

    @Override
    public void registerOutParameter(int parameterIndex, java.sql.SQLType sqlType) throws SQLException {
        delegateCs.registerOutParameter(parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, java.sql.SQLType sqlType, int scale) throws SQLException {
        delegateCs.registerOutParameter(parameterIndex, sqlType, scale);
    }

    @Override
    public void registerOutParameter(int parameterIndex, java.sql.SQLType sqlType, String typeName)
            throws SQLException {
        delegateCs.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, java.sql.SQLType sqlType) throws SQLException {
        delegateCs.registerOutParameter(parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, java.sql.SQLType sqlType, int scale) throws SQLException {
        delegateCs.registerOutParameter(parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, java.sql.SQLType sqlType, String typeName)
            throws SQLException {
        delegateCs.registerOutParameter(parameterName, sqlType, typeName);
    }

    /** SQLType 기반 이름 파라미터 설정. String인 경우 {@link CharsetUtils#toDb(String)}로 변환한다. */
    @Override
    public void setObject(String parameterName, Object x, java.sql.SQLType targetSqlType) throws SQLException {
        if (x instanceof String) {
            delegateCs.setObject(parameterName, CharsetUtils.toDb((String) x), targetSqlType);
        } else {
            delegateCs.setObject(parameterName, x, targetSqlType);
        }
    }

    @Override
    public void setObject(String parameterName, Object x, java.sql.SQLType targetSqlType, int scaleOrLength)
            throws SQLException {
        if (x instanceof String) {
            delegateCs.setObject(parameterName, CharsetUtils.toDb((String) x), targetSqlType, scaleOrLength);
        } else {
            delegateCs.setObject(parameterName, x, targetSqlType, scaleOrLength);
        }
    }
}
