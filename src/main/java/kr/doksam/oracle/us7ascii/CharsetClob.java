package kr.doksam.oracle.us7ascii;

import java.io.*;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

/**
 * US7ASCII Oracle DB에서 EUC-KR 한글 처리를 위한 Clob 래퍼.
 *
 * <p>Oracle JDBC가 반환하는 Clob의 문자열 데이터(Oracle US7ASCII 매핑)를
 * {@link CharsetUtils#toApp(String)}으로 EUC-KR 한글로 변환하여 반환한다.</p>
 *
 * <p>변환 대상 메서드:</p>
 * <ul>
 *   <li>{@link #getSubString(long, int)} — 부분 문자열 조회 시 toApp() 적용</li>
 *   <li>{@link #getCharacterStream()} — Reader를 toApp() 변환 Reader로 래핑</li>
 *   <li>{@link #getCharacterStream(long, long)} — 위치 기반 Reader도 동일</li>
 * </ul>
 *
 * @author 덕삼이
 * @see CharsetUtils#toApp(String)
 */
public class CharsetClob implements NClob {

    /** 실제 Oracle JDBC Clob */
    private final Clob delegate;

    /**
     * 래퍼 Clob을 생성한다.
     *
     * @param delegate 실제 Oracle JDBC Clob
     */
    public CharsetClob(Clob delegate) {
        this.delegate = delegate;
    }

    @Override
    public long length() throws SQLException {
        // Oracle에서 반환하는 length는 Oracle 내부 문자 수 (= 바이트 수, US7ASCII)
        // EUC-KR 한글은 2바이트이므로 실제 Java 문자 수와 다를 수 있지만,
        // getSubString/getCharacterStream에서 변환하므로 delegate 값을 그대로 사용
        return delegate.length();
    }

    /**
     * CLOB의 부분 문자열을 EUC-KR로 변환하여 반환한다.
     *
     * @param pos 시작 위치 (1부터)
     * @param length 가져올 길이
     * @return EUC-KR로 복원된 문자열
     */
    @Override
    public String getSubString(long pos, int length) throws SQLException {
        String raw = delegate.getSubString(pos, length);
        return CharsetUtils.toApp(raw);
    }

    /**
     * CLOB 전체 내용을 EUC-KR로 변환하는 Reader를 반환한다.
     */
    @Override
    public Reader getCharacterStream() throws SQLException {
        String raw = delegate.getSubString(1, (int) delegate.length());
        String converted = CharsetUtils.toApp(raw);
        return new StringReader(converted);
    }

    @Override
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        String raw = delegate.getSubString(pos, (int) length);
        String converted = CharsetUtils.toApp(raw);
        return new StringReader(converted);
    }

    @Override
    public InputStream getAsciiStream() throws SQLException {
        // ASCII 스트림은 EUC-KR 바이트로 변환하여 반환
        String raw = delegate.getSubString(1, (int) delegate.length());
        String converted = CharsetUtils.toApp(raw);
        try {
            return new ByteArrayInputStream(converted.getBytes("EUC-KR"));
        } catch (UnsupportedEncodingException e) {
            throw new SQLException("EUC-KR encoding error", e);
        }
    }

    @Override
    public long position(String searchstr, long start) throws SQLException {
        return delegate.position(CharsetUtils.toDb(searchstr), start);
    }

    @Override
    public long position(Clob searchstr, long start) throws SQLException {
        return delegate.position(searchstr, start);
    }

    // ── 쓰기 메서드: 앱→DB 방향 변환 ──

    @Override
    public int setString(long pos, String str) throws SQLException {
        return delegate.setString(pos, CharsetUtils.toDb(str));
    }

    @Override
    public int setString(long pos, String str, int offset, int len) throws SQLException {
        return delegate.setString(pos, CharsetUtils.toDb(str), offset, len);
    }

    @Override
    public OutputStream setAsciiStream(long pos) throws SQLException {
        return delegate.setAsciiStream(pos);
    }

    @Override
    public Writer setCharacterStream(long pos) throws SQLException {
        return delegate.setCharacterStream(pos);
    }

    @Override
    public void truncate(long len) throws SQLException {
        delegate.truncate(len);
    }

    @Override
    public void free() throws SQLException {
        delegate.free();
    }
}
