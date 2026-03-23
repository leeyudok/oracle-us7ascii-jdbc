package kr.doksam.oracle.us7ascii;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

/**
 * US7ASCII Oracle DB 환경에서 한글 인코딩 변환 통합 테스트.
 *
 * <p>test-db.properties 파일이 없으면 모든 테스트를 건너뛴다.
 * 실제 Oracle DB(US7ASCII)에서 한글 INSERT/SELECT/UPDATE/DELETE,
 * Statement/PreparedStatement/CallableStatement, COMMENT ON 등
 * 모든 경로의 한글 왕복 변환을 검증한다.</p>
 */
public class IntegrationTest {

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
            System.out.println("[SKIP] test-db.properties 없음 — 통합 테스트 전체 건너뜀");
        }
    }

    @Before
    public void setUp() throws Exception {
        if (skip) return;

        Class.forName("kr.doksam.oracle.us7ascii.CharsetDriver");

        String host = dbProps.getProperty("db.host");
        String port = dbProps.getProperty("db.port");
        String sid = dbProps.getProperty("db.sid");
        String user = dbProps.getProperty("db.username");
        String pass = dbProps.getProperty("db.password");
        String role = dbProps.getProperty("db.role", "");

        String url = "jdbc:oracle:us7ascii:thin:@" + host + ":" + port + ":" + sid;

        Properties connProps = new Properties();
        connProps.setProperty("user", user);
        connProps.setProperty("password", pass);
        if ("sysdba".equalsIgnoreCase(role)) {
            connProps.setProperty("internal_logon", "sysdba");
        }

        conn = DriverManager.getConnection(url, connProps);
        conn.setAutoCommit(false);

        // 테스트 테이블 생성
        Statement stmt = conn.createStatement();
        try { stmt.executeUpdate("DROP TABLE charset_test PURGE"); } catch (SQLException ignored) {}
        stmt.executeUpdate(
            "CREATE TABLE charset_test ("
            + "  id NUMBER PRIMARY KEY,"
            + "  name VARCHAR2(200),"
            + "  memo VARCHAR2(4000),"
            + "  created_at DATE DEFAULT SYSDATE"
            + ")"
        );
        conn.commit();
        stmt.close();
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            try {
                conn.commit();
                // 테이블/SP/함수 삭제하지 않음 — 테스트 후 검증용으로 유지
            } finally {
                conn.close();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 1. PreparedStatement — 핵심 경로 (setAsciiStream 우회)
    // ════════════════════════════════════════════════════════════════

    @Test
    public void PS_한글_INSERT_SELECT_왕복변환() throws Exception {
        if (skip) return;

        String korean = "한글테스트입니다";

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, korean);
        assertEquals(1, pstmt.executeUpdate());
        pstmt.close();

        PreparedStatement qstmt = conn.prepareStatement(
                "SELECT name FROM charset_test WHERE id = ?");
        qstmt.setInt(1, 1);
        ResultSet rs = qstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals(korean, rs.getString(1));
        assertEquals(korean, rs.getString("name"));
        rs.close();
        qstmt.close();
    }

    @Test
    public void PS_영문_한글_혼합() throws Exception {
        if (skip) return;

        String mixed = "Name: 홍길동, Age: 30, 부서: 개발팀";

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, mixed);
        pstmt.executeUpdate();
        pstmt.close();

        PreparedStatement qstmt = conn.prepareStatement(
                "SELECT name FROM charset_test WHERE id = ?");
        qstmt.setInt(1, 1);
        ResultSet rs = qstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals(mixed, rs.getString(1));
        rs.close();
        qstmt.close();
    }

    @Test
    public void PS_NULL_처리() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, null);
        pstmt.executeUpdate();
        pstmt.close();

        PreparedStatement qstmt = conn.prepareStatement(
                "SELECT name FROM charset_test WHERE id = ?");
        qstmt.setInt(1, 1);
        ResultSet rs = qstmt.executeQuery();
        assertTrue(rs.next());
        assertNull(rs.getString(1));
        assertTrue(rs.wasNull());
        rs.close();
        qstmt.close();
    }

    @Test
    public void PS_빈문자열() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, "");
        pstmt.executeUpdate();
        pstmt.close();

        PreparedStatement qstmt = conn.prepareStatement(
                "SELECT name FROM charset_test WHERE id = ?");
        qstmt.setInt(1, 1);
        ResultSet rs = qstmt.executeQuery();
        assertTrue(rs.next());
        // Oracle은 빈 문자열을 NULL로 저장
        assertNull(rs.getString(1));
        rs.close();
        qstmt.close();
    }

    @Test
    public void PS_setObject_String_변환() throws Exception {
        if (skip) return;

        String korean = "오브젝트변환테스트";

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setObject(2, korean);
        pstmt.executeUpdate();
        pstmt.close();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test WHERE id = 1");
        assertTrue(rs.next());
        assertEquals(korean, rs.getString(1));
        rs.close();
        stmt.close();
    }

    @Test
    public void PS_배치_INSERT() throws Exception {
        if (skip) return;

        String[] names = {"김철수", "이영희", "박민수", "정다은", "최준혁"};

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        for (int i = 0; i < names.length; i++) {
            pstmt.setInt(1, i + 1);
            pstmt.setString(2, names[i]);
            pstmt.addBatch();
        }
        int[] results = pstmt.executeBatch();
        assertEquals(names.length, results.length);
        pstmt.close();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test ORDER BY id");
        for (String name : names) {
            assertTrue(rs.next());
            assertEquals(name, rs.getString(1));
        }
        assertFalse(rs.next());
        rs.close();
        stmt.close();
    }

    @Test
    public void PS_UPDATE_한글() throws Exception {
        if (skip) return;

        // INSERT
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, "변경전");
        pstmt.executeUpdate();
        pstmt.close();

        // UPDATE
        pstmt = conn.prepareStatement(
                "UPDATE charset_test SET name = ? WHERE id = ?");
        pstmt.setString(1, "변경후한글");
        pstmt.setInt(2, 1);
        assertEquals(1, pstmt.executeUpdate());
        pstmt.close();

        // SELECT
        PreparedStatement qstmt = conn.prepareStatement(
                "SELECT name FROM charset_test WHERE id = ?");
        qstmt.setInt(1, 1);
        ResultSet rs = qstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals("변경후한글", rs.getString(1));
        rs.close();
        qstmt.close();
    }

    @Test
    public void PS_한글_WHERE_조건_검색() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, "검색대상이름");
        pstmt.executeUpdate();
        pstmt.setInt(1, 2);
        pstmt.setString(2, "다른이름");
        pstmt.executeUpdate();
        pstmt.close();

        // 한글로 WHERE 검색
        pstmt = conn.prepareStatement(
                "SELECT id FROM charset_test WHERE name = ?");
        pstmt.setString(1, "검색대상이름");
        ResultSet rs = pstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        assertFalse(rs.next());
        rs.close();
        pstmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 2. Statement — SQL 리터럴 UTL_RAW 변환
    // ════════════════════════════════════════════════════════════════

    @Test
    public void STMT_한글_리터럴_INSERT_SELECT() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO charset_test (id, name) VALUES (1, '스테이트먼트한글')");

        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test WHERE id = 1");
        assertTrue(rs.next());
        assertEquals("스테이트먼트한글", rs.getString(1));
        rs.close();
        stmt.close();
    }

    @Test
    public void STMT_영문_한글_혼합_리터럴() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(
                "INSERT INTO charset_test (id, name, memo) VALUES (1, 'hello', '메모내용입니다')");

        ResultSet rs = stmt.executeQuery("SELECT name, memo FROM charset_test WHERE id = 1");
        assertTrue(rs.next());
        assertEquals("hello", rs.getString("name"));
        assertEquals("메모내용입니다", rs.getString("memo"));
        rs.close();
        stmt.close();
    }

    @Test
    public void STMT_여러_한글_리터럴() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(
                "INSERT INTO charset_test (id, name, memo) VALUES (1, '이름값', '메모값')");

        ResultSet rs = stmt.executeQuery("SELECT name, memo FROM charset_test WHERE id = 1");
        assertTrue(rs.next());
        assertEquals("이름값", rs.getString("name"));
        assertEquals("메모값", rs.getString("memo"));
        rs.close();
        stmt.close();
    }

    @Test
    public void STMT_이스케이프_작은따옴표() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(
                "INSERT INTO charset_test (id, name) VALUES (1, 'It''s 한글')");

        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test WHERE id = 1");
        assertTrue(rs.next());
        assertEquals("It's 한글", rs.getString(1));
        rs.close();
        stmt.close();
    }

    @Test
    public void STMT_addBatch_한글() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.addBatch("INSERT INTO charset_test (id, name) VALUES (1, '배치1')");
        stmt.addBatch("INSERT INTO charset_test (id, name) VALUES (2, '배치2')");
        stmt.addBatch("INSERT INTO charset_test (id, name) VALUES (3, '배치3')");
        int[] results = stmt.executeBatch();
        assertEquals(3, results.length);

        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test ORDER BY id");
        assertTrue(rs.next()); assertEquals("배치1", rs.getString(1));
        assertTrue(rs.next()); assertEquals("배치2", rs.getString(1));
        assertTrue(rs.next()); assertEquals("배치3", rs.getString(1));
        rs.close();
        stmt.close();
    }

    @Test
    public void STMT_getResultSet_래핑확인() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO charset_test (id, name) VALUES (1, '래핑테스트')");

        boolean hasRs = stmt.execute("SELECT name FROM charset_test WHERE id = 1");
        assertTrue(hasRs);
        ResultSet rs = stmt.getResultSet();
        assertNotNull(rs);
        assertTrue("CharsetResultSet 인스턴스", rs instanceof CharsetResultSet);
        assertTrue(rs.next());
        assertEquals("래핑테스트", rs.getString(1));
        rs.close();
        stmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 3. COMMENT ON — PL/SQL 블록 변환
    // ════════════════════════════════════════════════════════════════

    @Test
    public void COMMENT_ON_TABLE_한글() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate("COMMENT ON TABLE charset_test IS '한글테이블설명'");

        ResultSet rs = stmt.executeQuery(
                "SELECT comments FROM all_tab_comments "
                + "WHERE table_name = 'CHARSET_TEST' AND owner = USER");
        assertTrue(rs.next());
        assertEquals("한글테이블설명", rs.getString(1));
        rs.close();
        stmt.close();
    }

    @Test
    public void COMMENT_ON_COLUMN_한글() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate("COMMENT ON COLUMN charset_test.name IS '사용자이름'");
        stmt.executeUpdate("COMMENT ON COLUMN charset_test.memo IS '메모내용입니다'");

        ResultSet rs = stmt.executeQuery(
                "SELECT column_name, comments FROM all_col_comments "
                + "WHERE table_name = 'CHARSET_TEST' AND owner = USER "
                + "AND comments IS NOT NULL ORDER BY column_name");
        assertTrue(rs.next());
        assertEquals("MEMO", rs.getString("column_name"));
        assertEquals("메모내용입니다", rs.getString("comments"));
        assertTrue(rs.next());
        assertEquals("NAME", rs.getString("column_name"));
        assertEquals("사용자이름", rs.getString("comments"));
        rs.close();
        stmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 4. CallableStatement — SP IN/OUT 한글 변환
    // ════════════════════════════════════════════════════════════════

    @Test
    public void CS_Procedure_IN_OUT_한글() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        // SP 생성: 입력 한글을 그대로 OUT 파라미터로 반환
        stmt.executeUpdate(
            "CREATE OR REPLACE PROCEDURE charset_test_proc("
            + "  p_in  IN  VARCHAR2,"
            + "  p_out OUT VARCHAR2"
            + ") AS BEGIN"
            + "  p_out := p_in;"
            + " END;"
        );
        stmt.close();

        CallableStatement cstmt = conn.prepareCall("{call charset_test_proc(?, ?)}");
        cstmt.setString(1, "프로시저한글");
        cstmt.registerOutParameter(2, Types.VARCHAR);
        cstmt.execute();

        assertEquals("프로시저한글", cstmt.getString(2));
        cstmt.close();
    }

    @Test
    public void CS_Function_한글_반환() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(
            "CREATE OR REPLACE FUNCTION charset_test_func("
            + "  p_in VARCHAR2"
            + ") RETURN VARCHAR2 AS BEGIN"
            + "  RETURN '결과:' || p_in;"
            + " END;"
        );
        stmt.close();

        CallableStatement cstmt = conn.prepareCall("{? = call charset_test_func(?)}");
        cstmt.registerOutParameter(1, Types.VARCHAR);
        cstmt.setString(2, "함수입력");
        cstmt.execute();

        assertEquals("결과:함수입력", cstmt.getString(1));
        cstmt.close();
    }

    @Test
    public void CS_NULL_IN_OUT() throws Exception {
        if (skip) return;

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(
            "CREATE OR REPLACE PROCEDURE charset_test_proc("
            + "  p_in  IN  VARCHAR2,"
            + "  p_out OUT VARCHAR2"
            + ") AS BEGIN"
            + "  p_out := p_in;"
            + " END;"
        );
        stmt.close();

        CallableStatement cstmt = conn.prepareCall("{call charset_test_proc(?, ?)}");
        cstmt.setString(1, null);
        cstmt.registerOutParameter(2, Types.VARCHAR);
        cstmt.execute();

        assertNull(cstmt.getString(2));
        cstmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 5. getObject — String 타입 자동 변환
    // ════════════════════════════════════════════════════════════════

    @Test
    public void RS_getObject_String_자동변환() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, "오브젝트조회");
        pstmt.executeUpdate();
        pstmt.close();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test WHERE id = 1");
        assertTrue(rs.next());

        Object obj = rs.getObject(1);
        assertTrue("String 타입", obj instanceof String);
        assertEquals("오브젝트조회", obj);

        Object obj2 = rs.getObject("name");
        assertEquals("오브젝트조회", obj2);
        rs.close();
        stmt.close();
    }

    @Test
    public void RS_getObject_숫자는_변환안함() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 42);
        pstmt.setString(2, "test");
        pstmt.executeUpdate();
        pstmt.close();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id FROM charset_test WHERE id = 42");
        assertTrue(rs.next());
        assertEquals(42, rs.getInt(1));
        rs.close();
        stmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 6. 경계값 + 특수문자 + 대량 데이터
    // ════════════════════════════════════════════════════════════════

    @Test
    public void 특수문자_한글_조합() throws Exception {
        if (skip) return;

        // EUC-KR에 있는 특수문자들
        String[] testCases = {
            "㈜한국은행",
            "금액: 1,000,000원",
            "성명(홍길동)",
            "계좌번호: 123-456-789",
            "2026년 03월 23일",
            "가나다라마바사아자차카타파하",
            "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ",
            "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣ",
            "가격 100% 할인! (특별)",
            "주소: 서울시 강남구 테헤란로 123"
        };

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        for (int i = 0; i < testCases.length; i++) {
            pstmt.setInt(1, i + 1);
            pstmt.setString(2, testCases[i]);
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        pstmt.close();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, name FROM charset_test ORDER BY id");
        for (int i = 0; i < testCases.length; i++) {
            assertTrue("행 " + (i+1) + " 존재", rs.next());
            assertEquals("행 " + (i+1) + " 값 일치", testCases[i], rs.getString("name"));
        }
        assertFalse(rs.next());
        rs.close();
        stmt.close();
    }

    @Test
    public void VARCHAR2_최대길이_한글() throws Exception {
        if (skip) return;

        // EUC-KR에서 한글 1자 = 2바이트. VARCHAR2(200) = 한글 최대 100자
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("가");
        }
        String maxKorean = sb.toString();
        assertEquals(100, maxKorean.length());

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, maxKorean);
        pstmt.executeUpdate();
        pstmt.close();

        PreparedStatement qstmt = conn.prepareStatement(
                "SELECT name FROM charset_test WHERE id = ?");
        qstmt.setInt(1, 1);
        ResultSet rs = qstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals(maxKorean, rs.getString(1));
        rs.close();
        qstmt.close();
    }

    @Test
    public void 대량_INSERT_SELECT_100건() throws Exception {
        if (skip) return;

        int count = 100;
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name, memo) VALUES (?, ?, ?)");
        for (int i = 1; i <= count; i++) {
            pstmt.setInt(1, i);
            pstmt.setString(2, "사용자" + i);
            pstmt.setString(3, "메모내용_" + i + "_한글데이터");
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        pstmt.close();

        // 전체 건수 확인
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM charset_test");
        assertTrue(rs.next());
        assertEquals(count, rs.getInt(1));
        rs.close();

        // 전체 데이터 정합성 확인
        rs = stmt.executeQuery("SELECT id, name, memo FROM charset_test ORDER BY id");
        for (int i = 1; i <= count; i++) {
            assertTrue(rs.next());
            assertEquals(i, rs.getInt("id"));
            assertEquals("사용자" + i, rs.getString("name"));
            assertEquals("메모내용_" + i + "_한글데이터", rs.getString("memo"));
        }
        assertFalse(rs.next());
        rs.close();
        stmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 7. 래퍼 타입 + 네비게이션 검증
    // ════════════════════════════════════════════════════════════════

    @Test
    public void 래퍼_타입_체인_검증() throws Exception {
        if (skip) return;

        assertTrue("CharsetConnection", conn instanceof CharsetConnection);

        Statement stmt = conn.createStatement();
        assertTrue("CharsetStatement", stmt instanceof CharsetStatement);

        // Statement → Connection 역참조
        Connection backRef = stmt.getConnection();
        assertSame("getConnection() 동일 객체", conn, backRef);

        PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM DUAL");
        assertTrue("CharsetPreparedStatement", pstmt instanceof CharsetPreparedStatement);
        assertSame("PS.getConnection() 동일 객체", conn, pstmt.getConnection());

        CallableStatement cstmt = conn.prepareCall("{call DBMS_OUTPUT.ENABLE(NULL)}");
        assertTrue("CharsetCallableStatement", cstmt instanceof CharsetCallableStatement);
        assertSame("CS.getConnection() 동일 객체", conn, cstmt.getConnection());

        // ResultSet → Statement 역참조
        stmt.executeUpdate("INSERT INTO charset_test (id, name) VALUES (1, '역참조')");
        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test WHERE id = 1");
        assertTrue("CharsetResultSet", rs instanceof CharsetResultSet);
        assertSame("RS.getStatement() 동일 객체", stmt, rs.getStatement());

        rs.close();
        cstmt.close();
        pstmt.close();
        stmt.close();
    }

    @Test
    public void unwrap_isWrapperFor_커넥션풀_호환() throws Exception {
        if (skip) return;

        // CharsetConnection 자기 타입
        assertTrue(conn.isWrapperFor(CharsetConnection.class));
        assertTrue(conn.isWrapperFor(Connection.class));
        CharsetConnection cc = conn.unwrap(CharsetConnection.class);
        assertSame(conn, cc);

        Statement stmt = conn.createStatement();
        assertTrue(stmt.isWrapperFor(CharsetStatement.class));
        assertTrue(stmt.isWrapperFor(Statement.class));
        stmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // 8. 트랜잭션 안전성
    // ════════════════════════════════════════════════════════════════

    @Test
    public void 트랜잭션_ROLLBACK_한글_데이터() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, "롤백될데이터");
        pstmt.executeUpdate();
        pstmt.close();

        conn.rollback();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM charset_test");
        assertTrue(rs.next());
        assertEquals("롤백 후 0건", 0, rs.getInt(1));
        rs.close();
        stmt.close();
    }

    @Test
    public void 트랜잭션_COMMIT_한글_데이터() throws Exception {
        if (skip) return;

        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO charset_test (id, name) VALUES (?, ?)");
        pstmt.setInt(1, 1);
        pstmt.setString(2, "커밋된데이터");
        pstmt.executeUpdate();
        pstmt.close();

        conn.commit();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name FROM charset_test WHERE id = 1");
        assertTrue(rs.next());
        assertEquals("커밋된데이터", rs.getString(1));
        rs.close();
        stmt.close();
    }
}
