package kr.doksam.oracle.us7ascii;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper 클래스들의 인코딩 변환 동작을 검증하는 단위 테스트.
 *
 * <p>Oracle DB 없이 실행 가능. java.lang.reflect.Proxy로 JDBC 인터페이스를 모의(Mock)하여
 * 래퍼가 올바르게 toApp/toDb 변환을 적용하는지 확인한다.</p>
 */
public class CharsetWrapperTest {

    /** "한글" 문자열을 toDb()로 변환한 결과 (DB 저장 형태) */
    private static final String KOREAN = "한글테스트";
    private static final String DB_FORM = CharsetUtils.toDb(KOREAN);

    // ════════════════════════════════════════════════════════════════
    // 헬퍼: Proxy 기반 Mock
    // ════════════════════════════════════════════════════════════════

    /**
     * 메서드 호출을 기록하고, 지정된 반환값을 돌려주는 범용 Mock 핸들러.
     */
    private static class RecordingHandler implements InvocationHandler {
        final List<String> calls = new ArrayList<>();
        final Map<String, Object> returnValues = new HashMap<>();

        void whenReturn(String methodName, Object value) {
            returnValues.put(methodName, value);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            // 인자가 있으면 기록
            StringBuilder sb = new StringBuilder(name);
            if (args != null) {
                sb.append("(");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(args[i]);
                }
                sb.append(")");
            }
            calls.add(sb.toString());

            // 미리 설정한 반환값
            if (returnValues.containsKey(name)) {
                return returnValues.get(name);
            }

            // 기본 반환값
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return false;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == float.class) return 0f;
            if (rt == double.class) return 0.0;
            if (rt == short.class) return (short) 0;
            if (rt == byte.class) return (byte) 0;
            return null;
        }

        boolean hasCalled(String prefix) {
            for (String c : calls) {
                if (c.startsWith(prefix)) return true;
            }
            return false;
        }

        String findCall(String prefix) {
            for (String c : calls) {
                if (c.startsWith(prefix)) return c;
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createMock(Class<T> iface, RecordingHandler handler) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                handler
        );
    }

    // ════════════════════════════════════════════════════════════════
    // CharsetResultSet 테스트
    // ════════════════════════════════════════════════════════════════

    @Test
    public void resultSet_getString_인덱스_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getString", DB_FORM); // DB에서 오는 값 (ISO-8859-1 형태)
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        String result = wrapper.getString(1);

        assertEquals("toApp 변환으로 원본 한글 복원", KOREAN, result);
    }

    @Test
    public void resultSet_getString_컬럼명_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getString", DB_FORM);
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        String result = wrapper.getString("col1");

        assertEquals(KOREAN, result);
    }

    @Test
    public void resultSet_getNString_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getNString", DB_FORM);
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        String result = wrapper.getNString(1);

        assertEquals(KOREAN, result);
    }

    @Test
    public void resultSet_getObject_String이면_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getObject", DB_FORM); // String 타입 반환
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        Object result = wrapper.getObject(1);

        assertEquals(KOREAN, result);
    }

    @Test
    public void resultSet_getObject_숫자면_변환없음() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getObject", Integer.valueOf(42));
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        Object result = wrapper.getObject(1);

        assertEquals(42, result);
    }

    @Test
    public void resultSet_getString_null이면_null_반환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getString", null);
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        assertNull(wrapper.getString(1));
    }

    @Test
    public void resultSet_영문은_변환없이_유지() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getString", "Hello");
        ResultSet mockRs = createMock(ResultSet.class, h);

        CharsetResultSet wrapper = new CharsetResultSet(mockRs);
        assertEquals("Hello", wrapper.getString(1));
    }

    // ════════════════════════════════════════════════════════════════
    // CharsetStatement 테스트
    // ════════════════════════════════════════════════════════════════

    @Test
    public void statement_executeQuery_한글_리터럴_UTL_RAW_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        RecordingHandler rsHandler = new RecordingHandler();
        h.whenReturn("executeQuery", createMock(ResultSet.class, rsHandler));
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        wrapper.executeQuery("SELECT * FROM t WHERE name = '한글'");

        // delegate에 전달된 SQL에는 UTL_RAW 변환이 포함되어야 함
        String call = h.findCall("executeQuery");
        assertNotNull("executeQuery 호출됨", call);
        assertTrue("UTL_RAW 변환 포함", call.contains("UTL_RAW.CAST_TO_VARCHAR2"));
        assertFalse("원본 한글 제거", call.contains("'한글'"));
    }

    @Test
    public void statement_executeUpdate_한글_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        wrapper.executeUpdate("INSERT INTO t VALUES ('테스트')");

        String call = h.findCall("executeUpdate");
        assertNotNull(call);
        assertTrue("UTL_RAW 포함", call.contains("UTL_RAW"));
    }

    @Test
    public void statement_executeUpdate_영문은_변환없음() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        wrapper.executeUpdate("INSERT INTO t VALUES ('hello')");

        String call = h.findCall("executeUpdate");
        assertTrue("영문 리터럴 그대로", call.contains("'hello'"));
    }

    @Test
    public void statement_commentOn_PL_SQL_블록_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        wrapper.executeUpdate("COMMENT ON TABLE t IS '한글설명'");

        String call = h.findCall("executeUpdate");
        assertTrue("BEGIN 포함", call.contains("BEGIN"));
        assertTrue("EXECUTE IMMEDIATE 포함", call.contains("EXECUTE IMMEDIATE"));
    }

    @Test
    public void statement_addBatch_한글_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        wrapper.addBatch("INSERT INTO t VALUES ('배치테스트')");

        String call = h.findCall("addBatch");
        assertNotNull(call);
        assertTrue("UTL_RAW 포함", call.contains("UTL_RAW"));
    }

    @Test
    public void statement_executeQuery_반환_CharsetResultSet() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        RecordingHandler rsHandler = new RecordingHandler();
        rsHandler.whenReturn("getString", DB_FORM);
        h.whenReturn("executeQuery", createMock(ResultSet.class, rsHandler));
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        ResultSet rs = wrapper.executeQuery("SELECT * FROM t");

        // 반환된 ResultSet도 CharsetResultSet이어야 함
        assertTrue("CharsetResultSet 인스턴스", rs instanceof CharsetResultSet);
        assertEquals("toApp 변환 적용", KOREAN, rs.getString(1));
    }

    // ════════════════════════════════════════════════════════════════
    // CharsetPreparedStatement 테스트
    // ════════════════════════════════════════════════════════════════

    @Test
    public void preparedStatement_setString_setAsciiStream_우회() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        wrapper.setString(1, KOREAN);

        // setString 대신 setAsciiStream이 호출되어야 함
        assertTrue("setAsciiStream 호출", h.hasCalled("setAsciiStream"));
        assertFalse("setString 미호출", h.hasCalled("setString"));
    }

    @Test
    public void preparedStatement_setString_null이면_setNull_호출() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        wrapper.setString(1, null);

        assertTrue("setNull 호출", h.hasCalled("setNull"));
    }

    @Test
    public void preparedStatement_setObject_String이면_toDb_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        wrapper.setObject(1, KOREAN);

        // setObject에 toDb 변환된 값이 전달되어야 함
        String call = h.findCall("setObject");
        assertNotNull("setObject 호출", call);
        assertTrue("toDb 변환값 포함", call.contains(DB_FORM));
    }

    @Test
    public void preparedStatement_setObject_숫자면_변환없음() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        wrapper.setObject(1, Integer.valueOf(42));

        String call = h.findCall("setObject");
        assertTrue("숫자 그대로 전달", call.contains("42"));
    }

    @Test
    public void preparedStatement_setNString_toDb_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        wrapper.setNString(1, KOREAN);

        String call = h.findCall("setNString");
        assertNotNull("setNString 호출", call);
        assertTrue("toDb 변환값 포함", call.contains(DB_FORM));
    }

    @Test
    public void preparedStatement_executeQuery_반환_CharsetResultSet() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        RecordingHandler rsHandler = new RecordingHandler();
        h.whenReturn("executeQuery", createMock(ResultSet.class, rsHandler));
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        ResultSet rs = wrapper.executeQuery();

        assertTrue("CharsetResultSet 인스턴스", rs instanceof CharsetResultSet);
    }

    // ════════════════════════════════════════════════════════════════
    // CharsetCallableStatement 테스트
    // ════════════════════════════════════════════════════════════════

    @Test
    public void callableStatement_getString_인덱스_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getString", DB_FORM);
        CallableStatement mockCs = createMock(CallableStatement.class, h);

        CharsetCallableStatement wrapper = new CharsetCallableStatement(mockCs);
        String result = wrapper.getString(1);

        assertEquals(KOREAN, result);
    }

    @Test
    public void callableStatement_getString_이름_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getString", DB_FORM);
        CallableStatement mockCs = createMock(CallableStatement.class, h);

        CharsetCallableStatement wrapper = new CharsetCallableStatement(mockCs);
        String result = wrapper.getString("out_param");

        assertEquals(KOREAN, result);
    }

    @Test
    public void callableStatement_getObject_String이면_toApp_적용() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("getObject", DB_FORM);
        CallableStatement mockCs = createMock(CallableStatement.class, h);

        CharsetCallableStatement wrapper = new CharsetCallableStatement(mockCs);
        Object result = wrapper.getObject(1);

        assertEquals(KOREAN, result);
    }

    @Test
    public void callableStatement_setString_이름_setAsciiStream_우회() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        CallableStatement mockCs = createMock(CallableStatement.class, h);

        CharsetCallableStatement wrapper = new CharsetCallableStatement(mockCs);
        wrapper.setString("param1", KOREAN);

        assertTrue("setAsciiStream 호출", h.hasCalled("setAsciiStream"));
    }

    @Test
    public void callableStatement_setString_이름_null이면_setNull_호출() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        CallableStatement mockCs = createMock(CallableStatement.class, h);

        CharsetCallableStatement wrapper = new CharsetCallableStatement(mockCs);
        wrapper.setString("param1", null);

        assertTrue("setNull 호출", h.hasCalled("setNull"));
    }

    // ════════════════════════════════════════════════════════════════
    // CharsetConnection 테스트
    // ════════════════════════════════════════════════════════════════

    @Test
    public void connection_createStatement_CharsetStatement_반환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("createStatement", createMock(Statement.class, new RecordingHandler()));
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        Statement stmt = wrapper.createStatement();

        assertTrue("CharsetStatement 인스턴스", stmt instanceof CharsetStatement);
    }

    @Test
    public void connection_prepareStatement_CharsetPreparedStatement_반환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("prepareStatement", createMock(PreparedStatement.class, new RecordingHandler()));
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        PreparedStatement pstmt = wrapper.prepareStatement("SELECT * FROM t WHERE id = ?");

        assertTrue("CharsetPreparedStatement 인스턴스", pstmt instanceof CharsetPreparedStatement);
    }

    @Test
    public void connection_prepareCall_CharsetCallableStatement_반환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("prepareCall", createMock(CallableStatement.class, new RecordingHandler()));
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        CallableStatement cstmt = wrapper.prepareCall("{call my_proc(?)}");

        assertTrue("CharsetCallableStatement 인스턴스", cstmt instanceof CharsetCallableStatement);
    }

    @Test
    public void connection_nativeSQL_한글_리터럴_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("nativeSQL", "transformed");
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        wrapper.nativeSQL("SELECT * FROM t WHERE name = '한글'");

        String call = h.findCall("nativeSQL");
        assertNotNull("nativeSQL 호출", call);
        assertTrue("UTL_RAW 변환 포함", call.contains("UTL_RAW.CAST_TO_VARCHAR2"));
        assertFalse("원본 한글 리터럴 제거", call.contains("'한글'"));
    }

    @Test
    public void connection_nativeSQL_영문만_변환없음() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("nativeSQL", "transformed");
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        wrapper.nativeSQL("SELECT * FROM t WHERE id = 1");

        String call = h.findCall("nativeSQL");
        assertTrue("영문 SQL 그대로", call.contains("SELECT * FROM t WHERE id = 1"));
    }

    @Test
    public void connection_close_위임() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        wrapper.close();

        assertTrue("close 위임", h.hasCalled("close"));
    }

    @Test
    public void connection_setAutoCommit_위임() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        wrapper.setAutoCommit(false);

        assertTrue("setAutoCommit 위임", h.hasCalled("setAutoCommit"));
    }

    // ════════════════════════════════════════════════════════════════
    // BUG 수정 검증 테스트
    // ════════════════════════════════════════════════════════════════

    // BUG-2: Connection.prepareStatement에서 한글 SQL이 UTL_RAW로 변환되는지 확인
    @Test
    public void connection_prepareStatement_한글_SQL_UTL_RAW_변환() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        h.whenReturn("prepareStatement", createMock(PreparedStatement.class, new RecordingHandler()));
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        wrapper.prepareStatement("INSERT INTO t VALUES ('한글데이터')");

        String call = h.findCall("prepareStatement");
        assertNotNull("prepareStatement 호출", call);
        assertTrue("UTL_RAW 변환 포함", call.contains("UTL_RAW.CAST_TO_VARCHAR2"));
    }

    // BUG-3: Statement.getConnection()이 래핑된 Connection 반환
    @Test
    public void statement_getConnection_래핑된_Connection_반환() throws SQLException {
        RecordingHandler connH = new RecordingHandler();
        connH.whenReturn("createStatement", createMock(Statement.class, new RecordingHandler()));
        Connection mockConn = createMock(Connection.class, connH);

        CharsetConnection connWrapper = new CharsetConnection(mockConn);
        Statement stmt = connWrapper.createStatement();

        // getConnection()이 CharsetConnection을 반환해야 함
        Connection returned = stmt.getConnection();
        assertTrue("CharsetConnection 인스턴스", returned instanceof CharsetConnection);
        assertSame("동일 CharsetConnection 반환", connWrapper, returned);
    }

    // BUG-4: isWrapperFor 자기 타입 체크
    @Test
    public void connection_isWrapperFor_자기타입_true() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        assertTrue("CharsetConnection으로 isWrapperFor", wrapper.isWrapperFor(CharsetConnection.class));
        assertTrue("Connection으로 isWrapperFor", wrapper.isWrapperFor(Connection.class));
    }

    @Test
    public void statement_isWrapperFor_자기타입_true() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Statement mockStmt = createMock(Statement.class, h);

        CharsetStatement wrapper = new CharsetStatement(mockStmt);
        assertTrue("CharsetStatement으로 isWrapperFor", wrapper.isWrapperFor(CharsetStatement.class));
    }

    @Test
    public void connection_unwrap_자기타입() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        Connection mockConn = createMock(Connection.class, h);

        CharsetConnection wrapper = new CharsetConnection(mockConn);
        CharsetConnection unwrapped = wrapper.unwrap(CharsetConnection.class);
        assertSame("자기 자신 반환", wrapper, unwrapped);
    }

    // BUG-5: ResultSet.getStatement() 동일성
    @Test
    public void resultSet_getStatement_동일_Statement_반환() throws SQLException {
        RecordingHandler rsH = new RecordingHandler();
        rsH.whenReturn("getString", DB_FORM);
        ResultSet mockRs = createMock(ResultSet.class, rsH);

        RecordingHandler stmtH = new RecordingHandler();
        stmtH.whenReturn("executeQuery", mockRs);
        Statement mockStmt = createMock(Statement.class, stmtH);

        CharsetStatement stmtWrapper = new CharsetStatement(mockStmt);
        ResultSet rs = stmtWrapper.executeQuery("SELECT 1");

        assertSame("동일 Statement 반환", stmtWrapper, rs.getStatement());
    }

    // BUG-7: setNString(null) → setNull 호출
    @Test
    public void preparedStatement_setNString_null이면_setNull_호출() throws SQLException {
        RecordingHandler h = new RecordingHandler();
        PreparedStatement mockPs = createMock(PreparedStatement.class, h);

        CharsetPreparedStatement wrapper = new CharsetPreparedStatement(mockPs);
        wrapper.setNString(1, null);

        assertTrue("setNull 호출", h.hasCalled("setNull"));
        assertFalse("setNString 미호출", h.hasCalled("setNString"));
    }
}
