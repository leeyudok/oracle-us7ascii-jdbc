package kr.doksam.oracle.us7ascii;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * CharsetUtils 인코딩 변환 단위 테스트.
 *
 * <p>Oracle DB 없이 실행 가능. 핵심 변환 로직을 검증한다.</p>
 */
public class CharsetUtilsTest {

    // ── toApp / toDb 왕복 변환 ────────────────────────────────────────

    @Test
    public void toApp_null이면_null_반환() {
        assertNull(CharsetUtils.toApp(null));
    }

    @Test
    public void toDb_null이면_null_반환() {
        assertNull(CharsetUtils.toDb(null));
    }

    @Test
    public void toApp_toDb_한글_왕복변환() {
        String original = "한글테스트";
        String dbForm = CharsetUtils.toDb(original);
        String restored = CharsetUtils.toApp(dbForm);
        assertEquals(original, restored);
    }

    @Test
    public void toApp_toDb_영문은_변환없이_유지() {
        String ascii = "Hello World 123";
        String dbForm = CharsetUtils.toDb(ascii);
        String restored = CharsetUtils.toApp(dbForm);
        assertEquals(ascii, restored);
    }

    @Test
    public void toApp_toDb_한글영문_혼합() {
        String mixed = "Name: 홍길동, Age: 30";
        String dbForm = CharsetUtils.toDb(mixed);
        String restored = CharsetUtils.toApp(dbForm);
        assertEquals(mixed, restored);
    }

    @Test
    public void toApp_toDb_빈문자열() {
        String empty = "";
        assertEquals(empty, CharsetUtils.toApp(empty));
        assertEquals(empty, CharsetUtils.toDb(empty));
    }

    @Test
    public void toApp_toDb_특수문자_포함() {
        String special = "가격: \\1,000 (할인!)";
        String dbForm = CharsetUtils.toDb(special);
        String restored = CharsetUtils.toApp(dbForm);
        assertEquals(special, restored);
    }

    // ── transformSql ──────────────────────────────────────────────────

    @Test
    public void transformSql_null이면_null_반환() {
        assertNull(CharsetUtils.transformSql(null));
    }

    @Test
    public void transformSql_한글_리터럴을_UTL_RAW로_변환() {
        String sql = "INSERT INTO t VALUES ('한글')";
        String result = CharsetUtils.transformSql(sql);
        assertTrue("UTL_RAW 함수 포함", result.contains("UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW("));
        assertFalse("원본 한글 리터럴 제거", result.contains("'한글'"));
    }

    @Test
    public void transformSql_영문_리터럴은_변환하지_않음() {
        String sql = "INSERT INTO t VALUES ('hello')";
        String result = CharsetUtils.transformSql(sql);
        assertEquals(sql, result);
    }

    @Test
    public void transformSql_여러_리터럴_혼합() {
        String sql = "INSERT INTO t (a, b) VALUES ('hello', '한글')";
        String result = CharsetUtils.transformSql(sql);
        assertTrue("영문 리터럴 유지", result.contains("'hello'"));
        assertTrue("한글은 UTL_RAW 변환", result.contains("UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW("));
    }

    @Test
    public void transformSql_이스케이프된_작은따옴표_처리() {
        String sql = "INSERT INTO t VALUES ('It''s 한글')";
        String result = CharsetUtils.transformSql(sql);
        assertTrue("UTL_RAW 변환", result.contains("UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW("));
    }

    @Test
    public void transformSql_리터럴_없는_SQL은_그대로() {
        String sql = "SELECT * FROM t WHERE id = 1";
        String result = CharsetUtils.transformSql(sql);
        assertEquals(sql, result);
    }

    @Test
    public void transformSql_한글_hex값_검증() {
        // "한글" EUC-KR = C7D1 B1DB
        String sql = "SELECT * FROM t WHERE name = '한글'";
        String result = CharsetUtils.transformSql(sql);
        assertTrue("EUC-KR hex 포함", result.contains("C7D1B1DB"));
    }

    @Test
    public void transformSql_미닫힌_따옴표_원본_반환() {
        String sql = "INSERT INTO t VALUES ('미닫힌";
        String result = CharsetUtils.transformSql(sql);
        assertEquals("미닫힌 따옴표 시 원본 SQL 반환", sql, result);
    }

    // ── transformCommentOn ────────────────────────────────────────────

    @Test
    public void transformCommentOn_한글이면_PL_SQL_블록_생성() {
        String sql = "COMMENT ON TABLE t IS '한글설명'";
        String result = CharsetUtils.transformCommentOn(sql);
        assertTrue("BEGIN 포함", result.startsWith("BEGIN"));
        assertTrue("EXECUTE IMMEDIATE 포함", result.contains("EXECUTE IMMEDIATE"));
        assertTrue("END 포함", result.endsWith("END;"));
        assertTrue("HEXTORAW 포함", result.contains("HEXTORAW("));
    }

    @Test
    public void transformCommentOn_영문이면_원본_반환() {
        String sql = "COMMENT ON TABLE t IS 'english comment'";
        String result = CharsetUtils.transformCommentOn(sql);
        assertEquals(sql, result);
    }

    @Test
    public void transformCommentOn_IS_없으면_원본_반환() {
        String sql = "COMMENT ON TABLE t";
        String result = CharsetUtils.transformCommentOn(sql);
        assertEquals(sql, result);
    }

    @Test
    public void transformCommentOn_컬럼_코멘트도_변환() {
        String sql = "COMMENT ON COLUMN t.c IS '컬럼설명'";
        String result = CharsetUtils.transformCommentOn(sql);
        assertTrue("PL/SQL 블록 생성", result.startsWith("BEGIN"));
    }
}
