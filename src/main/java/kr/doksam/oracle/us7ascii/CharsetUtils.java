package kr.doksam.oracle.us7ascii;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * US7ASCII Oracle DB와 EUC-KR 애플리케이션 간의 문자열 인코딩 변환 유틸리티.
 *
 * <p>Oracle JDBC 드라이버가 US7ASCII 캐릭터셋 기준으로 문자열 변환을 시도하면서
 * 한글이 손상되는 것을 방지하기 위해, 바이트 레벨에서 직접 인코딩을 제어한다.</p>
 *
 * <h3>변환 원리</h3>
 * <ul>
 *   <li>{@link #toApp(String)} — DB에서 읽은 ISO-8859-1 문자열을 EUC-KR로 역변환</li>
 *   <li>{@link #toDb(String)} — 앱의 EUC-KR 문자열을 ISO-8859-1 바이트로 변환</li>
 *   <li>{@link #transformSql(String)} — SQL 리터럴을 {@code UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW(...))} 로 치환</li>
 *   <li>{@link #transformCommentOn(String)} — COMMENT ON 구문을 PL/SQL 블록으로 감쌈</li>
 * </ul>
 *
 * @author 덕삼이
 */
public class CharsetUtils {

    /** ISO-8859-1 캐릭터셋 (JVM 필수 — 항상 존재) */
    private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    /** EUC-KR 캐릭터셋 (클래스 로딩 시 검증 — 미존재 시 즉시 실패) */
    private static final Charset EUC_KR;

    static {
        try {
            EUC_KR = Charset.forName("EUC-KR");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(
                    "EUC-KR charset is not available in this JVM. "
                    + "This library requires EUC-KR support. "
                    + "Please use a full JDK/JRE that includes East Asian charsets.");
        }
    }

    /**
     * DB → 앱 방향 변환: Oracle JDBC가 반환한 문자열을 EUC-KR로 복원한다.
     *
     * <p>Oracle JDBC thin 드라이버는 US7ASCII DB에서 읽은 바이트를 아래와 같이 매핑한다:</p>
     * <ul>
     *   <li>바이트 0x00–0x7F → U+0000–U+007F (표준 ASCII)</li>
     *   <li>바이트 0x80–0xFF → U+FF80–U+FFFF (Oracle 전용 매핑)</li>
     * </ul>
     * <p>이 메서드는 두 매핑 모두에서 원래 바이트를 추출한 뒤 EUC-KR로 재해석한다.
     * ISO-8859-1(U+0080–U+00FF) 매핑도 하위 호환을 위해 지원한다.</p>
     *
     * @param dbStr DB에서 조회된 문자열 (Oracle JDBC US7ASCII 인코딩)
     * @return EUC-KR로 복원된 한글 문자열, {@code null}이면 {@code null} 반환
     */
    public static String toApp(String dbStr) {
        if (dbStr == null) {
            return null;
        }
        int len = dbStr.length();
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            char c = dbStr.charAt(i);
            if (c <= 0x00FF) {
                // ASCII (0x00-0x7F) 또는 ISO-8859-1 (0x80-0xFF)
                bytes[i] = (byte) c;
            } else if (c >= 0xFF00) {
                // Oracle JDBC US7ASCII 매핑: U+FF00+byte → 원래 byte
                bytes[i] = (byte) (c - 0xFF00);
            } else {
                // 기타 Unicode — US7ASCII 컨텍스트에서는 발생하지 않아야 함
                bytes[i] = (byte) (c & 0xFF);
            }
        }
        return new String(bytes, EUC_KR);
    }

    /**
     * 앱 → DB 방향 변환: EUC-KR 문자열을 ISO-8859-1 바이트 표현으로 변환한다.
     *
     * <p>EUC-KR 바이트를 ISO-8859-1 문자열로 포장하면, Oracle JDBC가 "ASCII 범위 문자열"로
     * 인식하여 캐릭터셋 변환을 시도하지 않고 raw bytes 그대로 DB에 전달한다.</p>
     *
     * @param appStr 애플리케이션의 한글 문자열 (EUC-KR)
     * @return ISO-8859-1로 포장된 문자열, {@code null}이면 {@code null} 반환
     */
    public static String toDb(String appStr) {
        if (appStr == null) {
            return null;
        }
        return new String(appStr.getBytes(EUC_KR), ISO_8859_1);
    }

    /**
     * SQL 문자열 리터럴에 포함된 비ASCII 문자(한글 등)를 Oracle {@code UTL_RAW} 함수 호출로 변환한다.
     *
     * <p>Statement로 실행하는 SQL에 한글 리터럴이 포함되면 Oracle JDBC 드라이버가
     * 캐릭터셋 변환을 시도하여 한글이 깨진다. 이를 방지하기 위해 한글이 포함된 리터럴을
     * {@code UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('EUC-KR hex'))} 형태로 치환한다.</p>
     *
     * <p>예시:</p>
     * <pre>
     * 변환 전: INSERT INTO t VALUES ('한글')
     * 변환 후: INSERT INTO t VALUES (UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('C7D1B1DB')))
     * </pre>
     *
     * <p>ASCII만 포함된 리터럴은 변환하지 않고 그대로 유지한다.
     * 이스케이프된 작은따옴표({@code ''})도 올바르게 처리한다.</p>
     *
     * @param sql 원본 SQL 문자열
     * @return 한글 리터럴이 UTL_RAW 함수로 치환된 SQL, {@code null}이면 {@code null} 반환
     */
    public static String transformSql(String sql) {
        if (sql == null)
            return null;
        StringBuilder sb = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        boolean inString = false;
        int len = sql.length();

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);
            if (!inString) {
                if (c == '\'') {
                    inString = true;
                    literal.setLength(0);
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '\'') {
                    if (i + 1 < len && sql.charAt(i + 1) == '\'') {
                        literal.append('\'');
                        i++; // Skip next quote
                    } else {
                        inString = false;
                        String val = literal.toString();
                        if (hasNonAscii(val)) {
                            sb.append("UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('");
                            sb.append(toHex(val));
                            sb.append("'))");
                        } else {
                            sb.append('\'').append(val).append('\'');
                        }
                    }
                } else {
                    literal.append(c);
                }
            }
        }
        if (inString) {
            // 미닫힌 따옴표 — 원본 SQL 그대로 반환하여 DB에서 구문 오류로 감지되게 한다.
            // 변환 과정에서 데이터가 손실/변형되는 것보다 안전하다.
            return sql;
        }
        return sb.toString();
    }

    /**
     * {@code COMMENT ON} 구문의 한글 리터럴을 PL/SQL 블록으로 감싸서 변환한다.
     *
     * <p>{@code COMMENT ON} 구문은 표준 SQL과 달리 함수 호출({@code UTL_RAW})을 허용하지 않으므로,
     * {@code BEGIN EXECUTE IMMEDIATE '...' END;} PL/SQL 블록으로 감싸서 실행한다.</p>
     *
     * <p>예시:</p>
     * <pre>
     * 변환 전: COMMENT ON TABLE t IS '한글설명'
     * 변환 후: BEGIN EXECUTE IMMEDIATE 'COMMENT ON TABLE t IS ''' || UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('hex')) || ''''; END;
     * </pre>
     *
     * @param sql COMMENT ON 구문이 포함된 SQL
     * @return PL/SQL 블록으로 변환된 SQL. 비ASCII 문자가 없으면 원본 반환
     */
    public static String transformCommentOn(String sql) {
        String trimmed = sql.trim();
        int isIndex = trimmed.toUpperCase().lastIndexOf(" IS ");
        if (isIndex == -1)
            return sql; // Fallback

        String prefix = trimmed.substring(0, isIndex + 4); // Includes " IS "
        String literal = trimmed.substring(isIndex + 4).trim();

        if (literal.startsWith("'") && literal.endsWith("'")) {
            String content = literal.substring(1, literal.length() - 1);
            if (hasNonAscii(content)) {
                // Construct PL/SQL block
                // BEGIN EXECUTE IMMEDIATE 'prefix ''' ||
                // UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('hex')) || ''''; END;
                String escapedPrefix = prefix.replace("'", "''");
                return "BEGIN EXECUTE IMMEDIATE '" + escapedPrefix + "''' || UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('"
                        + toHex(content) + "')) || ''''; END;";
            }
        }
        return sql;
    }

    /**
     * 문자열에 ASCII 범위(0~127)를 벗어나는 문자가 포함되어 있는지 검사한다.
     *
     * @param s 검사할 문자열
     * @return 비ASCII 문자가 하나라도 있으면 {@code true}
     */
    private static boolean hasNonAscii(String s) {
        for (char c : s.toCharArray()) {
            if (c > 127)
                return true;
        }
        return false;
    }

    /**
     * 문자열을 EUC-KR 바이트로 변환한 뒤 16진수 문자열로 반환한다.
     *
     * <p>Oracle {@code HEXTORAW()} 함수에 전달할 hex 문자열을 생성한다.</p>
     *
     * @param s 변환할 문자열
     * @return 대문자 16진수 문자열 (예: "한글" → "C7D1B1DB")
     * @throws RuntimeException EUC-KR 인코딩 미지원 시
     */
    private static String toHex(String s) {
        byte[] bytes = s.getBytes(EUC_KR);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }
}
