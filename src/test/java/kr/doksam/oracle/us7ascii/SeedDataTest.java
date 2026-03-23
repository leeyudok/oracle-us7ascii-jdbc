package kr.doksam.oracle.us7ascii;

import org.junit.Test;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

/**
 * 검증용 테스트 데이터 삽입.
 *
 * <p>다국어 데이터를 삽입하고 삭제하지 않는다.
 * 수동 검증용이므로 {@code mvn test -Dtest=SeedDataTest}로 실행.</p>
 */
public class SeedDataTest {

    @Test
    public void 테스트데이터_삽입() throws Exception {
        Properties dbProps = new Properties();
        try (FileInputStream fis = new FileInputStream("test-db.properties")) {
            dbProps.load(fis);
        } catch (Exception e) {
            System.out.println("[SKIP] test-db.properties 없음");
            return;
        }

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

        Connection conn = DriverManager.getConnection(url, connProps);
        conn.setAutoCommit(false);

        Statement stmt = conn.createStatement();

        // 테이블 생성 (이미 있으면 DROP 후 재생성)
        try { stmt.executeUpdate("DROP TABLE charset_demo PURGE"); } catch (SQLException ignored) {}
        stmt.executeUpdate(
            "CREATE TABLE charset_demo ("
            + "  id NUMBER PRIMARY KEY,"
            + "  category VARCHAR2(100),"
            + "  val VARCHAR2(4000),"
            + "  memo VARCHAR2(200)"
            + ")"
        );
        conn.commit();

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO charset_demo (id, category, val, memo) VALUES (?, ?, ?, ?)");

        // ── 한글 ──
        insert(ps, 1, "한글", "안녕하세요", "기본 인사");
        insert(ps, 2, "한글", "대한민국 만세", "띄어쓰기 포함");
        insert(ps, 3, "한글", "가나다라마바사아자차카타파하", "자음 순서");
        insert(ps, 4, "한글", "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ", "자음 낱자");
        insert(ps, 5, "한글", "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣ", "모음 낱자");
        insert(ps, 6, "한글", "뷁쉛쫄딝흮", "복잡한 완성형");

        // ── 한자 ──
        insert(ps, 10, "한자", "大韓民國", "나라 이름");
        insert(ps, 11, "한자", "株式會社", "회사");
        insert(ps, 12, "한자", "東西南北春夏秋冬", "방향+계절");
        insert(ps, 13, "한자", "人山水火木金土日月", "기본 한자");

        // ── 일본어 히라가나 ──
        insert(ps, 20, "히라가나", "あいうえお", "아이우에오");
        insert(ps, 21, "히라가나", "かきくけこさしすせそ", "카키쿠케코 사시스세소");
        insert(ps, 22, "히라가나", "たちつてとなにぬねの", "타치츠테토 나니누네노");

        // ── 일본어 가타카나 ──
        insert(ps, 30, "가타카나", "アイウエオ", "아이우에오");
        insert(ps, 31, "가타카나", "カキクケコサシスセソ", "카키쿠케코 사시스세소");
        insert(ps, 32, "가타카나", "タチツテトナニヌネノ", "타치츠테토 나니누네노");

        // ── 그리스어 ──
        insert(ps, 40, "그리스어", "ΑΒΓΔΕΖΗΘΙΚΛΜ", "대문자");
        insert(ps, 41, "그리스어", "αβγδεζηθικλμ", "소문자");
        insert(ps, 42, "그리스어", "π≈3.14159", "파이 값");

        // ── 키릴(러시아어) ──
        insert(ps, 50, "키릴", "АБВГДЕЁЖЗИЙК", "대문자");
        insert(ps, 51, "키릴", "абвгдеёжзийк", "소문자");
        insert(ps, 52, "키릴", "Москва", "모스크바");

        // ── 특수기호 ──
        insert(ps, 60, "특수기호", "㈜덕삼소프트", "괄호주식회사");
        insert(ps, 61, "특수기호", "①②③④⑤⑥⑦⑧⑨⑩", "원문자 숫자");
        insert(ps, 62, "특수기호", "㎏ ㎡ ㎝ ㎞ ℃", "단위 기호");
        insert(ps, 63, "특수기호", "￦10,000 ±0.5% ×÷", "화폐 수학");
        insert(ps, 64, "특수기호", "★☆●○◆◇■□▲△▽▼", "도형 기호");
        insert(ps, 65, "특수기호", "→←↑↓↔", "화살표");
        insert(ps, 66, "특수기호", "┌─┬─┐│├─┼─┤│└─┴─┘", "박스 드로잉");

        // ── 전각 영숫자 ──
        insert(ps, 70, "전각", "ＡＢＣＤＥＦＧ", "전각 영대문자");
        insert(ps, 71, "전각", "ａｂｃｄｅｆｇ", "전각 영소문자");
        insert(ps, 72, "전각", "０１２３４５６７８９", "전각 숫자");

        // ── 혼합 텍스트 (실무 시나리오) ──
        insert(ps, 80, "혼합-실무", "주식회사(株式會社) 덕삼소프트", "회사명 한글+한자");
        insert(ps, 81, "혼합-실무", "서울特別市 강남區 역삼洞 123-45", "주소");
        insert(ps, 82, "혼합-실무", "계좌: 123-456-789012 (예금주: 홍길동)", "은행 계좌");
        insert(ps, 83, "혼합-실무", "2026-03-23 입금 ￦1,000,000 처리완료", "거래 내역");
        insert(ps, 84, "혼합-실무", "담당자: 김철수 (TEL: 02-1234-5678)", "연락처");
        insert(ps, 85, "혼합-실무", "에러코드 ERR-001: 잔액부족 (현재잔액 ￦500)", "에러 메시지");

        // ── 다국어 총집합 ──
        insert(ps, 90, "총집합", "한글漢字ひらがなカタカナαβγδАБВГ㈜①ABC123", "모든 문자 타입");
        insert(ps, 91, "총집합", "こんにちは 안녕하세요 Привет ΑΒΓΔ", "다국어 인사");
        insert(ps, 92, "총집합", "㈜덕삼 ★ BEST → 大韓民國 αΩ ①②③", "기호+문자 혼합");

        // ── 경계값 ──
        insert(ps, 95, "경계값", "", "빈 문자열");
        insert(ps, 96, "경계값", "A", "ASCII 단일 문자");
        insert(ps, 97, "경계값", "가", "한글 단일 문자");

        // 100자 한글 (VARCHAR2(4000)에 EUC-KR 2바이트×100 = 200바이트)
        StringBuilder sb100 = new StringBuilder();
        for (int i = 0; i < 100; i++) sb100.append("가");
        insert(ps, 98, "경계값", sb100.toString(), "한글 100자 (200bytes)");

        ps.close();
        conn.commit();

        // ── Statement (UTL_RAW 변환) 방식도 추가 ──
        stmt.executeUpdate("INSERT INTO charset_demo (id, category, val, memo) VALUES "
                + "(200, 'Statement', '한글 Statement 테스트', 'UTL_RAW 자동변환')");
        stmt.executeUpdate("INSERT INTO charset_demo (id, category, val, memo) VALUES "
                + "(201, 'Statement', '大韓民國 αβγδ АБВГ', '다국어 Statement')");
        stmt.executeUpdate("INSERT INTO charset_demo (id, category, val, memo) VALUES "
                + "(202, 'Statement', '㈜덕삼 ①②③ ★☆', '특수기호 Statement')");
        conn.commit();

        // COMMENT ON 테스트
        stmt.executeUpdate("COMMENT ON TABLE charset_demo IS '다국어 인코딩 테스트 테이블'");
        stmt.executeUpdate("COMMENT ON COLUMN charset_demo.val IS '테스트 값 (한글/한자/가나/그리스/키릴)'");
        conn.commit();

        // 건수 확인
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM charset_demo");
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        System.out.println("========================================");
        System.out.println(" charset_demo 테이블 데이터 삽입 완료");
        System.out.println(" 총 " + count + "건");
        System.out.println("========================================");
        System.out.println(" 확인 SQL:");
        System.out.println("   SELECT * FROM charset_demo ORDER BY id;");
        System.out.println("   SELECT id, category, DUMP(val) FROM charset_demo;");
        System.out.println("========================================");

        stmt.close();
        conn.close();
    }

    private void insert(PreparedStatement ps, int id, String category, String val, String memo)
            throws SQLException {
        ps.setInt(1, id);
        ps.setString(2, category);
        ps.setString(3, val);
        ps.setString(4, memo);
        ps.executeUpdate();
    }
}
