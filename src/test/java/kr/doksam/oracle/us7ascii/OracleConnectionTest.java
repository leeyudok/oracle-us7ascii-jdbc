package kr.doksam.oracle.us7ascii;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

/**
 * Oracle DB 실제 연결 테스트.
 *
 * <p>test-db.properties 파일에서 접속정보를 읽어 Oracle에 연결한다.
 * 파일이 없으면 테스트를 건너뛴다.</p>
 */
public class OracleConnectionTest {

    private Properties loadDbProperties() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("test-db.properties")) {
            props.load(fis);
            return props;
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    public void Oracle_직접연결_테스트() throws Exception {
        Properties dbProps = loadDbProperties();
        if (dbProps == null) {
            System.out.println("[SKIP] test-db.properties 파일 없음 — Oracle 연결 테스트 건너뜀");
            return;
        }

        String host = dbProps.getProperty("db.host");
        String port = dbProps.getProperty("db.port");
        String sid = dbProps.getProperty("db.sid");
        String user = dbProps.getProperty("db.username");
        String pass = dbProps.getProperty("db.password");
        String role = dbProps.getProperty("db.role", "");

        // Oracle JDBC 직접 연결 (래퍼 없이)
        String url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;

        Properties connProps = new Properties();
        connProps.setProperty("user", user);
        connProps.setProperty("password", pass);
        if ("sysdba".equalsIgnoreCase(role)) {
            connProps.setProperty("internal_logon", "sysdba");
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, connProps);
            assertNotNull("Connection 객체 생성", conn);
            assertFalse("Connection 열림", conn.isClosed());

            // DB 버전 확인
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("[OK] Oracle 직접 연결 성공");
            System.out.println("  DB 버전: " + meta.getDatabaseProductVersion());
            System.out.println("  드라이버: " + meta.getDriverName() + " " + meta.getDriverVersion());

            // 간단한 쿼리
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL");
            assertTrue("DUAL 조회 성공", rs.next());
            assertEquals(1, rs.getInt(1));
            rs.close();
            stmt.close();

            // NLS 캐릭터셋 확인
            stmt = conn.createStatement();
            rs = stmt.executeQuery(
                    "SELECT value FROM nls_database_parameters WHERE parameter = 'NLS_CHARACTERSET'");
            if (rs.next()) {
                System.out.println("  NLS_CHARACTERSET: " + rs.getString(1));
            }
            rs.close();
            stmt.close();

        } finally {
            if (conn != null) conn.close();
        }
    }

    @Test
    public void CharsetDriver_래핑연결_테스트() throws Exception {
        Properties dbProps = loadDbProperties();
        if (dbProps == null) {
            System.out.println("[SKIP] test-db.properties 파일 없음 — CharsetDriver 테스트 건너뜀");
            return;
        }

        String host = dbProps.getProperty("db.host");
        String port = dbProps.getProperty("db.port");
        String sid = dbProps.getProperty("db.sid");
        String user = dbProps.getProperty("db.username");
        String pass = dbProps.getProperty("db.password");
        String role = dbProps.getProperty("db.role", "");

        // CharsetDriver를 통한 연결
        Class.forName("kr.doksam.oracle.us7ascii.CharsetDriver");
        String url = "jdbc:oracle:us7ascii:thin:@" + host + ":" + port + ":" + sid;

        Properties connProps = new Properties();
        connProps.setProperty("user", user);
        connProps.setProperty("password", pass);
        if ("sysdba".equalsIgnoreCase(role)) {
            connProps.setProperty("internal_logon", "sysdba");
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, connProps);
            assertNotNull("Connection 객체 생성", conn);
            assertTrue("CharsetConnection 인스턴스", conn instanceof CharsetConnection);

            // DUAL 조회
            Statement stmt = conn.createStatement();
            assertTrue("CharsetStatement 인스턴스", stmt instanceof CharsetStatement);

            ResultSet rs = stmt.executeQuery("SELECT 'hello' FROM DUAL");
            assertTrue("CharsetResultSet 인스턴스", rs instanceof CharsetResultSet);
            assertTrue(rs.next());
            assertEquals("hello", rs.getString(1));
            rs.close();
            stmt.close();

            System.out.println("[OK] CharsetDriver 래핑 연결 성공");

        } finally {
            if (conn != null) conn.close();
        }
    }
}
