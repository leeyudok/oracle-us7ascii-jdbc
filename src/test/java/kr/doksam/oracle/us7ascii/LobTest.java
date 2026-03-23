package kr.doksam.oracle.us7ascii;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * LOB (CLOB/BLOB) 데이터 한글 인코딩 통합 테스트.
 */
public class LobTest {

    private static Properties dbProps;
    private static boolean skip = false;
    private Connection conn;

    @BeforeClass
    public static void loadConfig() {
        dbProps = new Properties();
        try (FileInputStream fis = new FileInputStream("test-db.properties")) {
            dbProps.load(fis);
        } catch (Exception e) {
            skip = true;
        }
    }

    @Before
    public void setUp() throws Exception {
        if (skip) return;
        Class.forName("kr.doksam.oracle.us7ascii.CharsetDriver");

        String url = "jdbc:oracle:us7ascii:thin:@"
                + dbProps.getProperty("db.host") + ":"
                + dbProps.getProperty("db.port") + ":"
                + dbProps.getProperty("db.sid");
        Properties connProps = new Properties();
        connProps.setProperty("user", dbProps.getProperty("db.username"));
        connProps.setProperty("password", dbProps.getProperty("db.password"));
        String role = dbProps.getProperty("db.role", "");
        if ("sysdba".equalsIgnoreCase(role)) {
            connProps.setProperty("internal_logon", "sysdba");
        }

        conn = DriverManager.getConnection(url, connProps);
        conn.setAutoCommit(false);

        Statement stmt = conn.createStatement();
        try { stmt.executeUpdate("DROP TABLE lob_test PURGE"); } catch (SQLException ignored) {}
        stmt.executeUpdate(
            "CREATE TABLE lob_test ("
            + "  id NUMBER PRIMARY KEY,"
            + "  text_clob CLOB,"
            + "  bin_blob BLOB"
            + ")"
        );
        conn.commit();
        stmt.close();
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            try { conn.commit(); } finally { conn.close(); }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // CLOB — PS setString → getString
    // ════════════════════════════════════════════════════════════════

    @Test
    public void CLOB_setString_getString_왕복() throws Exception {
        if (skip) return;

        String korean = "한글 CLOB 테스트입니다. 大韓民國 αβγδ";

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 1);
        ps.setString(2, korean);
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 1);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());
        assertEquals(korean, rs.getString(1));
        rs.close();
        sel.close();
    }

    @Test
    public void CLOB_대용량_한글() throws Exception {
        if (skip) return;

        // 10,000자 한글 (EUC-KR 20,000 bytes)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("가나다라마");
        }
        String largeKorean = sb.toString(); // 50,000자

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 2);
        ps.setString(2, largeKorean);
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 2);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());
        String result = rs.getString(1);
        assertEquals("길이 일치", largeKorean.length(), result.length());
        assertEquals("내용 일치", largeKorean, result);
        rs.close();
        sel.close();
    }

    // ════════════════════════════════════════════════════════════════
    // CLOB — getClob() 객체로 읽기
    // ════════════════════════════════════════════════════════════════

    @Test
    public void CLOB_getClob_getSubString() throws Exception {
        if (skip) return;

        String korean = "한글 CLOB getSubString 테스트";

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 10);
        ps.setString(2, korean);
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 10);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());

        Clob clob = rs.getClob(1);
        assertNotNull(clob);
        String sub = clob.getSubString(1, (int) clob.length());
        assertEquals(korean, sub);

        rs.close();
        sel.close();
    }

    @Test
    public void CLOB_getClob_getCharacterStream() throws Exception {
        if (skip) return;

        String korean = "한글 CharacterStream 읽기 테스트";

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 11);
        ps.setString(2, korean);
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 11);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());

        Clob clob = rs.getClob(1);
        Reader reader = clob.getCharacterStream();
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        reader.close();
        assertEquals(korean, sb.toString());

        rs.close();
        sel.close();
    }

    // ════════════════════════════════════════════════════════════════
    // CLOB — setClob() 으로 쓰기
    // ════════════════════════════════════════════════════════════════

    @Test
    public void CLOB_setClob_Reader() throws Exception {
        if (skip) return;

        String korean = "Reader로 CLOB에 한글 쓰기";

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 20);
        ps.setClob(2, new StringReader(korean));
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 20);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());
        assertEquals(korean, rs.getString(1));
        rs.close();
        sel.close();
    }

    @Test
    public void CLOB_setCharacterStream() throws Exception {
        if (skip) return;

        String korean = "CharacterStream으로 CLOB 쓰기 테스트";

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 21);
        ps.setCharacterStream(2, new StringReader(korean), korean.length());
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 21);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());
        assertEquals(korean, rs.getString(1));
        rs.close();
        sel.close();
    }

    // ════════════════════════════════════════════════════════════════
    // BLOB — 바이너리는 변환 없이 통과
    // ════════════════════════════════════════════════════════════════

    @Test
    public void BLOB_바이너리_왕복() throws Exception {
        if (skip) return;

        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) data[i] = (byte) i;

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, bin_blob) VALUES (?, ?)");
        ps.setInt(1, 30);
        ps.setBlob(2, new ByteArrayInputStream(data));
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT bin_blob FROM lob_test WHERE id = ?");
        sel.setInt(1, 30);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());

        Blob blob = rs.getBlob(1);
        byte[] result = blob.getBytes(1, (int) blob.length());
        assertArrayEquals(data, result);

        rs.close();
        sel.close();
    }

    // ════════════════════════════════════════════════════════════════
    // ResultSet getCharacterStream — CLOB/VARCHAR2 공통
    // ════════════════════════════════════════════════════════════════

    @Test
    public void RS_getCharacterStream_CLOB() throws Exception {
        if (skip) return;

        String korean = "getCharacterStream으로 CLOB 직접 읽기";

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lob_test (id, text_clob) VALUES (?, ?)");
        ps.setInt(1, 40);
        ps.setString(2, korean);
        ps.executeUpdate();
        ps.close();

        PreparedStatement sel = conn.prepareStatement(
                "SELECT text_clob FROM lob_test WHERE id = ?");
        sel.setInt(1, 40);
        ResultSet rs = sel.executeQuery();
        assertTrue(rs.next());

        Reader reader = rs.getCharacterStream(1);
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        reader.close();
        assertEquals(korean, sb.toString());

        rs.close();
        sel.close();
    }
}
