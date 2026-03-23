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
 * 다국어 문자 통합 테스트.
 *
 * <p>EUC-KR 인코딩이 지원하는 문자(한자, 가나, 그리스어, 키릴 등)와
 * 지원하지 않는 문자(이모지, 확장 CJK 등)의 동작을 검증한다.</p>
 *
 * <p>EUC-KR(CP949) 지원 범위:</p>
 * <ul>
 *   <li>한글 11,172자 (완성형)</li>
 *   <li>한자(CJK) 약 4,888자</li>
 *   <li>일본어 히라가나/가타카나</li>
 *   <li>그리스 대/소문자</li>
 *   <li>키릴(러시아어) 대/소문자</li>
 *   <li>라틴 확장 (ÀÁÂ 등 일부)</li>
 *   <li>특수기호 (㈜, ㎏, ㎡, ① 등)</li>
 * </ul>
 */
public class MultilingualTest {

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
        try { stmt.executeUpdate("DROP TABLE ml_test PURGE"); } catch (SQLException ignored) {}
        stmt.executeUpdate(
                "CREATE TABLE ml_test (id NUMBER PRIMARY KEY, val VARCHAR2(4000))");
        conn.commit();
        stmt.close();
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            try { conn.commit(); } finally { conn.close(); }
        }
    }

    /** PS INSERT → SELECT 왕복 헬퍼 */
    private void assertRoundTrip(int id, String input) throws Exception {
        PreparedStatement ins = conn.prepareStatement("INSERT INTO ml_test (id, val) VALUES (?, ?)");
        ins.setInt(1, id);
        ins.setString(2, input);
        ins.executeUpdate();
        ins.close();

        PreparedStatement sel = conn.prepareStatement("SELECT val FROM ml_test WHERE id = ?");
        sel.setInt(1, id);
        ResultSet rs = sel.executeQuery();
        assertTrue("id=" + id + " 조회 실패", rs.next());
        assertEquals("id=" + id + " 왕복 불일치", input, rs.getString(1));
        rs.close();
        sel.close();
    }

    /** Statement INSERT (UTL_RAW) → SELECT 왕복 헬퍼 */
    private void assertStmtRoundTrip(int id, String input) throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO ml_test (id, val) VALUES (" + id + ", '" + input + "')");

        ResultSet rs = stmt.executeQuery("SELECT val FROM ml_test WHERE id = " + id);
        assertTrue("id=" + id + " 조회 실패", rs.next());
        assertEquals("id=" + id + " 왕복 불일치", input, rs.getString(1));
        rs.close();
        stmt.close();
    }

    // ════════════════════════════════════════════════════════════════
    // EUC-KR 지원 문자 — 왕복 변환 성공해야 함
    // ════════════════════════════════════════════════════════════════

    @Test
    public void 한자_CJK_기본() throws Exception {
        if (skip) return;
        // 상용 한자
        assertRoundTrip(1, "大韓民國");
        assertRoundTrip(2, "株式會社");
        assertRoundTrip(3, "東西南北");
    }

    @Test
    public void 한자_한글_혼합() throws Exception {
        if (skip) return;
        assertRoundTrip(10, "주식회사(株式會社) 덕삼");
        assertRoundTrip(11, "서울特別市 강남區");
    }

    @Test
    public void 일본어_히라가나() throws Exception {
        if (skip) return;
        assertRoundTrip(20, "あいうえお");
        assertRoundTrip(21, "さしすせそ");
    }

    @Test
    public void 일본어_가타카나() throws Exception {
        if (skip) return;
        assertRoundTrip(30, "アイウエオ");
        assertRoundTrip(31, "カキクケコ");
    }

    @Test
    public void 일본어_한글_혼합() throws Exception {
        if (skip) return;
        assertRoundTrip(35, "こんにちは 안녕하세요");
    }

    @Test
    public void 그리스어_대문자() throws Exception {
        if (skip) return;
        assertRoundTrip(40, "ΑΒΓΔΕΖΗΘ");
    }

    @Test
    public void 그리스어_소문자() throws Exception {
        if (skip) return;
        assertRoundTrip(41, "αβγδεζηθ");
    }

    @Test
    public void 그리스어_혼합() throws Exception {
        if (skip) return;
        assertRoundTrip(42, "π=3.14 Ω저항");
    }

    @Test
    public void 키릴_러시아어() throws Exception {
        if (skip) return;
        assertRoundTrip(50, "АБВГДЕЖ");
        assertRoundTrip(51, "абвгдеж");
    }

    @Test
    public void 키릴_한글_혼합() throws Exception {
        if (skip) return;
        assertRoundTrip(52, "Москва 모스크바");
    }

    @Test
    public void 특수기호_괄호_단위() throws Exception {
        if (skip) return;
        assertRoundTrip(60, "㈜덕삼 ㎏ ㎡ ㏄");
        assertRoundTrip(61, "①②③④⑤");
    }

    @Test
    public void 특수기호_화폐_수학() throws Exception {
        if (skip) return;
        // EUC-KR에 있는 특수문자
        assertRoundTrip(62, "￦10,000 ±0.5% ×÷");
    }

    @Test
    public void 라틴확장_EUC_KR_지원문자() throws Exception {
        if (skip) return;
        // EUC-KR(KS X 1001)에 포함된 라틴 확장 문자만 테스트
        // Æ(U+00C6), Ð(U+00D0), Ø(U+00D8), Þ(U+00DE), ß(U+00DF) 등
        assertRoundTrip(70, "ÆÐØÞßæðøþ");
    }

    @Test
    public void 라틴확장_EUC_KR_미지원_확인() throws Exception {
        if (skip) return;
        // À, Á, Â 등 대부분의 라틴 악센트 문자는 EUC-KR에 없어 손실
        // 이는 인코딩 자체의 한계이며 라이브러리 버그가 아님
        String latin = "ÀÁÂÃÄÅ";
        byte[] bytes = latin.getBytes("EUC-KR");
        String restored = new String(bytes, "EUC-KR");
        System.out.println("[INFO] 라틴 악센트 'ÀÁÂÃÄÅ' → EUC-KR → 복원: '" + restored + "' (손실 예상)");
        assertNotEquals("라틴 악센트 문자는 EUC-KR에서 손실", latin, restored);
    }

    @Test
    public void 전각_영숫자() throws Exception {
        if (skip) return;
        assertRoundTrip(80, "ＡＢＣＤＥ１２３");
    }

    @Test
    public void 박스드로잉_선문자() throws Exception {
        if (skip) return;
        assertRoundTrip(85, "┌─┐│└─┘");
    }

    @Test
    public void Statement_한자_리터럴() throws Exception {
        if (skip) return;
        assertStmtRoundTrip(90, "大韓民國");
    }

    @Test
    public void Statement_일본어_리터럴() throws Exception {
        if (skip) return;
        assertStmtRoundTrip(91, "あいうえお");
    }

    @Test
    public void Statement_그리스어_리터럴() throws Exception {
        if (skip) return;
        assertStmtRoundTrip(92, "αβγδ");
    }

    @Test
    public void Statement_키릴_리터럴() throws Exception {
        if (skip) return;
        assertStmtRoundTrip(93, "АБВГ");
    }

    @Test
    public void 다국어_총집합() throws Exception {
        if (skip) return;
        // 한글 + 한자 + 히라가나 + 가타카나 + 그리스 + 키릴 + 특수기호 + ASCII
        String mega = "한글漢字ひらがなカタカナαβγδАБВГ㈜①ABC123";
        assertRoundTrip(100, mega);
    }

    @Test
    public void Statement_다국어_총집합() throws Exception {
        if (skip) return;
        String mega = "한글漢字ひらがなカタカナαβγδАБВГ㈜①ABC123";
        assertStmtRoundTrip(101, mega);
    }

    // ════════════════════════════════════════════════════════════════
    // EUC-KR 미지원 문자 — 손실 가능성 확인
    // ════════════════════════════════════════════════════════════════

    @Test
    public void EUC_KR_미지원_문자_확인() throws Exception {
        if (skip) return;

        // EUC-KR에 없는 문자는 '?' 등으로 변환됨을 확인
        // (이건 라이브러리 한계가 아니라 EUC-KR 인코딩 자체의 한계)

        // 이모지
        String emoji = "😀";
        byte[] emojiBytes = emoji.getBytes("EUC-KR");
        String restored = new String(emojiBytes, "EUC-KR");
        System.out.println("[INFO] 이모지 '😀' → EUC-KR → 복원: '" + restored + "' (손실 예상)");
        assertNotEquals("이모지는 EUC-KR에서 손실", emoji, restored);

        // 태국어
        String thai = "สวัสดี";
        byte[] thaiBytes = thai.getBytes("EUC-KR");
        String thaiRestored = new String(thaiBytes, "EUC-KR");
        System.out.println("[INFO] 태국어 'สวัสดี' → EUC-KR → 복원: '" + thaiRestored + "' (손실 예상)");
        assertNotEquals("태국어는 EUC-KR에서 손실", thai, thaiRestored);

        // 아랍어
        String arabic = "مرحبا";
        byte[] arabicBytes = arabic.getBytes("EUC-KR");
        String arabicRestored = new String(arabicBytes, "EUC-KR");
        System.out.println("[INFO] 아랍어 → EUC-KR → 복원: '" + arabicRestored + "' (손실 예상)");
        assertNotEquals("아랍어는 EUC-KR에서 손실", arabic, arabicRestored);
    }
}
