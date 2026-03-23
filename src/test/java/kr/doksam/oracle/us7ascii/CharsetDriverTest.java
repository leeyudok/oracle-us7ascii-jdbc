package kr.doksam.oracle.us7ascii;

import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * CharsetDriver URL 파싱 및 기본 동작 단위 테스트.
 *
 * <p>Oracle DB 없이 실행 가능. 드라이버의 URL 인식 로직과
 * 메타데이터 메서드를 검증한다.</p>
 */
public class CharsetDriverTest {

    private final CharsetDriver driver = new CharsetDriver();

    // ── acceptsURL ──────────────────────────────────────────────

    @Test
    public void acceptsURL_올바른_URL_true() throws SQLException {
        assertTrue(driver.acceptsURL("jdbc:oracle:us7ascii:thin:@localhost:1521:XE"));
    }

    @Test
    public void acceptsURL_thin_서비스명_true() throws SQLException {
        assertTrue(driver.acceptsURL("jdbc:oracle:us7ascii:thin:@//host:1521/service"));
    }

    @Test
    public void acceptsURL_oci_드라이버_true() throws SQLException {
        assertTrue(driver.acceptsURL("jdbc:oracle:us7ascii:oci:@TNS_NAME"));
    }

    @Test
    public void acceptsURL_일반_Oracle_URL_false() throws SQLException {
        assertFalse(driver.acceptsURL("jdbc:oracle:thin:@localhost:1521:XE"));
    }

    @Test
    public void acceptsURL_다른_DB_URL_false() throws SQLException {
        assertFalse(driver.acceptsURL("jdbc:mysql://localhost:3306/test"));
    }

    @Test
    public void acceptsURL_null_false() throws SQLException {
        assertFalse(driver.acceptsURL(null));
    }

    @Test
    public void acceptsURL_빈문자열_false() throws SQLException {
        assertFalse(driver.acceptsURL(""));
    }

    // ── connect (URL 거부 시 null 반환) ─────────────────────────

    @Test
    public void connect_잘못된_URL이면_null_반환() throws SQLException {
        assertNull(driver.connect("jdbc:oracle:thin:@localhost:1521:XE", null));
    }

    @Test
    public void connect_null_URL이면_null_반환() throws SQLException {
        assertNull(driver.connect(null, null));
    }

    // ── 메타데이터 ──────────────────────────────────────────────

    @Test
    public void getMajorVersion_1() {
        assertEquals(1, driver.getMajorVersion());
    }

    @Test
    public void getMinorVersion_0() {
        assertEquals(0, driver.getMinorVersion());
    }

    @Test
    public void jdbcCompliant_false() {
        assertFalse(driver.jdbcCompliant());
    }

    @Test
    public void getParentLogger_반환() throws SQLFeatureNotSupportedException {
        assertNotNull(driver.getParentLogger());
        assertEquals("CharsetDriver", driver.getParentLogger().getName());
    }
}
