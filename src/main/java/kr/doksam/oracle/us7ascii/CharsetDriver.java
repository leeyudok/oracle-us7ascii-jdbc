package kr.doksam.oracle.us7ascii;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * US7ASCII Oracle DB에서 EUC-KR 한글 처리를 위한 JDBC 드라이버 래퍼.
 *
 * <p>{@code jdbc:oracle:us7ascii:} 접두어가 붙은 URL을 가로채서,
 * 실제 Oracle JDBC 드라이버로 연결한 뒤 {@link CharsetConnection}으로 래핑하여 반환한다.</p>
 *
 * <h3>사용법</h3>
 * <pre>
 * Class.forName("kr.doksam.oracle.us7ascii.CharsetDriver");
 * Connection conn = DriverManager.getConnection("jdbc:oracle:us7ascii:thin:@host:1521:SID", user, pass);
 * </pre>
 *
 * @author 덕삼이
 * @see CharsetConnection
 */
public class CharsetDriver implements Driver {

    /** 이 드라이버가 처리하는 JDBC URL 접두어 */
    private static final String URL_PREFIX = "jdbc:oracle:us7ascii:";

    /** 실제 Oracle JDBC 드라이버에 전달할 URL 접두어 (us7ascii: 제거 후) */
    private static final String REAL_URL_PREFIX = "jdbc:oracle:";

    /** 클래스 로딩 시 DriverManager에 자동 등록 */
    static {
        try {
            java.sql.DriverManager.registerDriver(new CharsetDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Can't register driver!");
        }
    }

    /**
     * 지정된 URL로 Oracle DB에 연결하고, {@link CharsetConnection}으로 래핑하여 반환한다.
     *
     * <p>URL이 {@code jdbc:oracle:us7ascii:}로 시작하지 않으면 {@code null}을 반환하여
     * DriverManager가 다른 드라이버를 시도하도록 한다.</p>
     *
     * @param url  JDBC URL ({@code jdbc:oracle:us7ascii:thin:@host:port:SID})
     * @param info 연결 속성 (user, password 등)
     * @return 래핑된 Connection, 또는 URL이 맞지 않으면 {@code null}
     * @throws SQLException 연결 실패 시
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        String realUrl = url.replace(URL_PREFIX, REAL_URL_PREFIX);
        Connection realConn = DriverManager.getConnection(realUrl, info);
        return new CharsetConnection(realConn);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        String realUrl = url.replace(URL_PREFIX, REAL_URL_PREFIX);
        Driver realDriver = DriverManager.getDriver(realUrl);
        return realDriver.getPropertyInfo(realUrl, info);
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("CharsetDriver");
    }
}
