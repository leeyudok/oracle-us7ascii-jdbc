# Oracle US7ASCII JDBC Driver Wrapper

Oracle DB 캐릭터셋이 **US7ASCII**인데 실제 데이터는 **EUC-KR(한글)**로 저장된 환경에서 발생하는 **한글 깨짐 문제**를 해결하는 JDBC 드라이버 래퍼입니다.

> 은행 폐쇄망 등 레거시 Oracle 환경에서 검증된 라이브러리입니다. 131건 테스트, 에러율 0%.

---

## 목차

- [문제 상황](#문제-상황)
- [해결 원리](#해결-원리)
- [빠른 시작 (5분)](#빠른-시작-5분)
- [주요 기능](#주요-기능)
- [다국어 지원 범위](#다국어-지원-범위)
- [호환성](#호환성)
- [빌드](#빌드)
- [테스트](#테스트)
- [DBeaver 설정](#dbeaver-설정)
- [프로젝트 구조](#프로젝트-구조)
- [보안 감사 이력](#보안-감사-이력)
- [주의 사항](#주의-사항)

---

## 문제 상황

많은 레거시 Oracle DB가 캐릭터셋을 `US7ASCII`로 설정해 놓고, 실제로는 EUC-KR 한글 데이터를 저장하고 있습니다. 이 환경에서 JDBC로 접속하면:

```
DB 저장값: C7 D1 B1 DB  (EUC-KR "한글")
JDBC 조회: ????  또는  ÇÑ±Û  (깨진 문자)
```

**원인**: Oracle JDBC 드라이버가 US7ASCII 캐릭터셋을 신뢰하고, 0x80 이상 바이트를 잘못된 유니코드로 매핑합니다.

## 해결 원리

이 래퍼는 JDBC URL에 `us7ascii:`를 추가하는 것만으로 모든 문자열 I/O를 자동 변환합니다:

```
┌─────────────┐     EUC-KR bytes      ┌──────────┐     US7ASCII 매핑      ┌──────────┐
│  Java App   │ ──── setString() ────▶ │  래퍼    │ ── setAsciiStream() ──▶│ Oracle DB│
│  "한글"     │ ◀─── getString() ──── │ (자동)   │ ◀── U+FFxx 역변환 ─── │ C7D1B1DB │
└─────────────┘                        └──────────┘                        └──────────┘
```

- **쓰기**: `setString("한글")` → EUC-KR 바이트로 변환 → `setAsciiStream()`으로 직접 전송
- **읽기**: Oracle이 반환한 U+FFxx 문자 → 바이트 추출 → EUC-KR 디코딩 → `"한글"`

---

## 빠른 시작 (5분)

### 1단계: JAR 준비

```bash
# 직접 빌드
git clone https://github.com/leeyudok/oracle-us7ascii-jdbc.git
cd oracle-us7ascii-jdbc
mvn clean package

# 산출물: target/oracle-us7ascii-jdbc-0.0.2.jar
```

### 2단계: classpath에 추가

프로젝트의 `lib/` 폴더에 2개 JAR을 넣습니다:

```
lib/
├── oracle-us7ascii-jdbc-0.0.2.jar   ← 이 래퍼
└── ojdbc8-23.26.1.0.0.jar           ← Oracle JDBC (기존에 쓰던 것)
```

Maven 프로젝트라면:

```xml
<dependency>
  <groupId>kr.doksam</groupId>
  <artifactId>oracle-us7ascii-jdbc</artifactId>
  <version>0.0.2</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/oracle-us7ascii-jdbc-0.0.2.jar</systemPath>
</dependency>
```

### 3단계: JDBC URL 변경 (딱 한 줄)

```
변경 전: jdbc:oracle:thin:@localhost:1521:XE
변경 후: jdbc:oracle:us7ascii:thin:@localhost:1521:XE
                    ↑↑↑↑↑↑↑↑↑
                    이것만 추가
```

### 4단계: 코드 변경 — 없음!

```java
// 드라이버 등록 (Java 8 이상에서는 ServiceLoader로 자동 등록되어 생략 가능)
Class.forName("kr.doksam.oracle.us7ascii.CharsetDriver");

// 기존 코드 그대로 사용
String url = "jdbc:oracle:us7ascii:thin:@localhost:1521:XE";
Connection conn = DriverManager.getConnection(url, "user", "password");

PreparedStatement pstmt = conn.prepareStatement("INSERT INTO t (col) VALUES (?)");
pstmt.setString(1, "한글테스트");  // ← 기존 코드 그대로! 내부에서 자동 변환
pstmt.executeUpdate();

ResultSet rs = stmt.executeQuery("SELECT col FROM t");
while (rs.next()) {
    String val = rs.getString("col");  // ← 자동으로 EUC-KR 복원
    System.out.println(val);           // "한글테스트" ✓
}
```

> **핵심**: 기존 Java 코드는 한 글자도 바꿀 필요 없습니다. URL만 변경하세요.

---

## 주요 기능

### 1. PreparedStatement (파라미터 바인딩)

`setString()` 호출 시 내부적으로 `setAsciiStream()`을 사용하여 Oracle JDBC의 캐릭터셋 변환을 우회하고, **EUC-KR raw bytes**를 DB에 직접 전송합니다.

### 2. Statement (SQL 리터럴)

SQL에 포함된 한글 문자열 리터럴을 감지하여 Oracle 내장 함수로 자동 변환합니다:

```sql
-- 변환 전
INSERT INTO t VALUES ('한글')

-- 변환 후 (래퍼가 자동 처리)
INSERT INTO t VALUES (UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('C7D1B1DB')))
```

### 3. COMMENT ON

`COMMENT ON` 구문은 함수 호출을 허용하지 않으므로, 자동으로 PL/SQL 블록으로 감싸서 실행합니다:

```sql
-- 원본
COMMENT ON TABLE t IS '한글 코멘트'

-- 래퍼가 자동 변환
BEGIN EXECUTE IMMEDIATE 'COMMENT ON TABLE t IS ' || UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('...')); END;
```

### 4. CallableStatement (Stored Procedure)

IN 파라미터(`setString`)는 `setAsciiStream` 우회, OUT 파라미터(`getString`)는 EUC-KR 역변환을 적용합니다.

### 5. ResultSet (조회)

DB에서 조회된 문자열을 Oracle JDBC의 US7ASCII 바이트 매핑(U+FFxx)에서 EUC-KR로 역변환하여 올바른 한글을 반환합니다.

### 6. CLOB (대용량 텍스트)

`getClob()`, `setClob()`, `getCharacterStream()`, `setCharacterStream()` 모두 자동 변환됩니다. VARCHAR2와 동일하게 EUC-KR 인코딩이 적용됩니다.

---

## 다국어 지원 범위

EUC-KR(CP949) 인코딩이 지원하는 모든 문자를 왕복 변환합니다:

| 문자 종류 | 예시 | 지원 |
| --- | --- | :---: |
| 한글 | 가나다, ㄱㄴㄷ, ㅏㅑㅓ | O |
| 한자 (CJK) | 大韓民國, 株式會社 | O |
| 일본어 히라가나 | あいうえお | O |
| 일본어 가타카나 | アイウエオ | O |
| 그리스어 | αβγδ, ΑΒΓΔ | O |
| 키릴 (러시아어) | абвг, АБВГ | O |
| 특수기호 | ㈜, ①②③, ㎏, ★☆, →← | O |
| 전각 영숫자 | ＡＢＣ, ０１２ | O |
| 박스 드로잉 | ┌─┐│└─┘ | O |
| 라틴 악센트 (ÀÁÂÃ 등) | - | **X** (EUC-KR 한계) |
| 이모지 | 😀 | **X** (EUC-KR 한계) |
| 태국어/아랍어 등 | - | **X** (EUC-KR 한계) |

> EUC-KR에 없는 문자는 인코딩 자체의 한계이며 라이브러리 버그가 아닙니다.

---

## 호환성

### Java 버전

| Java 버전 | JDBC 스펙 | 지원 | 비고 |
| --- | --- | :---: | --- |
| **Java 8** | JDBC 4.2 | **O** | 빌드 타겟 |
| Java 11 | JDBC 4.3 | O | |
| Java 17 | JDBC 4.3 | O | |
| Java 21 | JDBC 4.3 | O | |
| Java 7 이하 | JDBC 4.1 | **X** | JDBC 4.2 API 사용으로 컴파일 불가 |

### Oracle JDBC Driver

| 드라이버 | Oracle DB | Java | 비고 |
| --- | --- | --- | --- |
| ojdbc6 (11.2.x) | 11g R2 | 6+ | |
| ojdbc7 (12.1.x) | 12c R1 | 7+ | |
| **ojdbc8 (12.2.x~23.x)** | 12c R2~23ai | 8+ | **테스트 완료 (23.26.1.0.0)** |
| **ojdbc10 (19.x)** | 19c | 10+ | **테스트 완료 (19.27.0.0)** |
| **ojdbc11 (21.x~23.x)** | 21c~23ai | 11+ | **테스트 완료 (23.26.1.0.0)** |

> 표준 JDBC 인터페이스만 래핑하므로 드라이버 버전에 의존하지 않습니다.

### Oracle Database

- **필수**: `UTL_RAW` 패키지 (대부분 기본 포함)
- Oracle 10g ~ 23ai, 캐릭터셋 `US7ASCII` 환경
- **테스트 완료**: Oracle 21c XE (Docker `gvenzl/oracle-xe`, NLS_CHARACTERSET=US7ASCII)

---

## 빌드

```bash
mvn clean package              # 기본: ojdbc8
mvn clean package -Pojdbc10    # ojdbc10으로 전환
mvn clean package -Pojdbc11    # ojdbc11로 전환
```

산출물 (3개 JAR):

```text
target/oracle-us7ascii-jdbc-0.0.2.jar          # 메인 라이브러리
target/oracle-us7ascii-jdbc-0.0.2-sources.jar   # 소스
target/oracle-us7ascii-jdbc-0.0.2-javadoc.jar   # JavaDoc
```

> 래퍼 JAR 자체는 동일합니다. 프로파일은 컴파일/테스트 시 classpath에 올릴 Oracle JDBC 드라이버를 선택합니다.

---

## 테스트

### 단위 테스트 (Oracle 불필요)

```bash
mvn test    # 70건 자동 실행
```

### 통합 테스트 (Oracle DB 필요)

#### 1. Oracle DB 준비 (Docker)

US7ASCII Oracle이 없다면 Docker로 빠르게 띄울 수 있습니다:

```bash
docker run -d --name oracle-xe \
  -p 1521:1521 \
  -e ORACLE_PASSWORD=yourpassword \
  -e ORACLE_CHARACTERSET=US7ASCII \
  gvenzl/oracle-xe:21-slim
```

#### 2. 접속 정보 파일 작성

프로젝트 루트에 `test-db.properties`를 만듭니다 (`.gitignore`에 등록되어 있어 커밋되지 않음):

```properties
db.host=localhost
db.port=1521
db.sid=xe
db.username=sys
db.password=yourpassword
db.role=sysdba
```

#### 3. 통합 테스트 실행

```bash
# 통합 테스트 (한글 왕복 변환 검증)
mvn test -Dtest=IntegrationTest

# 다국어 테스트 (한자/가나/그리스/키릴/특수기호)
mvn test -Dtest=MultilingualTest

# LOB 테스트 (CLOB/BLOB)
mvn test -Dtest=LobTest

# 전체 테스트 (단위 + 통합) 한번에
mvn test -Dtest=CharsetUtilsTest,CharsetDriverTest,CharsetWrapperTest,OracleConnectionTest,IntegrationTest,MultilingualTest,LobTest

# 검증용 시드 데이터 삽입 (테이블에 데이터가 남아 DBeaver 등으로 확인 가능)
mvn test -Dtest=SeedDataTest
```

### 테스트 현황 (v0.0.2)

| 테스트 | 건수 | Oracle 필요 | 설명 |
| --- | :---: | :---: | --- |
| CharsetUtilsTest | 19 | | 인코딩 변환 유틸리티 |
| CharsetWrapperTest | 38 | | Proxy 기반 래퍼 검증 |
| CharsetDriverTest | 13 | | 드라이버 URL/메타데이터 |
| IntegrationTest | 28 | O | CRUD, 배치, SP, COMMENT ON 왕복 변환 |
| MultilingualTest | 23 | O | 한자/가나/그리스/키릴/특수기호 |
| OracleConnectionTest | 2 | O | 직접/래핑 접속 |
| LobTest | 8 | O | CLOB/BLOB 읽기/쓰기 |
| **합계** | **131** | | **0 failures (ojdbc8 / ojdbc10 / ojdbc11)** |

### 테스트 생성 DB 오브젝트

통합 테스트 실행 후 Oracle DB에 남는 오브젝트입니다. 삭제하지 않으므로 직접 확인할 수 있습니다:

| 오브젝트 | 타입 | 생성 테스트 | 설명 |
| --- | --- | --- | --- |
| `CHARSET_TEST` | TABLE | IntegrationTest | CRUD 테스트용 |
| `CHARSET_TEST_PROC` | PROCEDURE | IntegrationTest | IN/OUT 파라미터 한글 왕복 |
| `CHARSET_TEST_FUNC` | FUNCTION | IntegrationTest | VARCHAR2 반환값 한글 |
| `ML_TEST` | TABLE | MultilingualTest | 다국어 문자 검증 |
| `CHARSET_DEMO` | TABLE | SeedDataTest | 시드 데이터 (48행) |

```sql
-- 데이터 확인
SELECT * FROM charset_test ORDER BY id;
SELECT * FROM ml_test ORDER BY id;
SELECT * FROM charset_demo ORDER BY id;

-- 정리 (필요 시)
DROP TABLE charset_test PURGE;
DROP TABLE ml_test PURGE;
DROP TABLE charset_demo PURGE;
DROP PROCEDURE charset_test_proc;
DROP FUNCTION charset_test_func;
```

---

## DBeaver 설정

DBeaver에서도 이 래퍼를 사용하여 한글을 정상적으로 조회할 수 있습니다.

### 1. 드라이버 관리자 열기

메뉴: **데이터베이스** → **드라이버 관리자** → **새로 만들기**

### 2. 드라이버 설정

| 항목 | 값 |
| --- | --- |
| 드라이버 이름 | `Oracle US7ASCII` |
| 클래스 이름 | `kr.doksam.oracle.us7ascii.CharsetDriver` |
| URL 템플릿 | `jdbc:oracle:us7ascii:thin:@{host}:{port}:{server}` |

### 3. 라이브러리 추가

**라이브러리** 탭에서 **2개 JAR 모두** 추가합니다:

- `oracle-us7ascii-jdbc-0.0.2.jar` (래퍼)
- `ojdbc8-23.26.1.0.0.jar` (Oracle JDBC)

> 래퍼 JAR만 넣으면 `ClassNotFoundException: oracle.jdbc.OracleDriver` 에러가 발생합니다. 반드시 2개 모두 추가하세요.

### 4. 새 연결 만들기

새 연결에서 방금 만든 `Oracle US7ASCII` 드라이버를 선택하고 접속 정보를 입력합니다.

> **주의**: DBeaver가 URL을 `jdbc:oracle:thin:@...`으로 자동 생성하는 경우, 연결 설정 → **URL 직접 편집**을 체크하고 `us7ascii:`를 수동으로 추가해야 합니다.

---

## 프로젝트 구조

```text
oracle-us7ascii-jdbc/
├── pom.xml                          # Maven 빌드 (ojdbc8/10/11 프로파일)
├── src/main/java/kr/doksam/oracle/us7ascii/
│   ├── CharsetDriver.java           # JDBC Driver (URL 파싱, 연결 생성)
│   ├── CharsetConnection.java       # Connection 래퍼
│   ├── CharsetStatement.java        # Statement 래퍼 (SQL 리터럴 UTL_RAW 변환)
│   ├── CharsetPreparedStatement.java# PS 래퍼 (setAsciiStream 우회)
│   ├── CharsetCallableStatement.java# CS 래퍼 (SP IN/OUT 변환)
│   ├── CharsetResultSet.java        # RS 래퍼 (getString EUC-KR 복원)
│   ├── CharsetClob.java             # CLOB 래퍼 (읽기/쓰기 변환)
│   └── CharsetUtils.java           # 인코딩 변환 핵심 유틸리티
└── src/test/java/kr/doksam/oracle/us7ascii/
    ├── CharsetUtilsTest.java        # 단위: 인코딩 변환 (19건)
    ├── CharsetWrapperTest.java      # 단위: 래퍼 검증 (38건)
    ├── CharsetDriverTest.java      # 단위: 드라이버 URL (13건)
    ├── IntegrationTest.java         # 통합: CRUD/SP/COMMENT ON (28건)
    ├── MultilingualTest.java        # 통합: 다국어 (23건)
    ├── LobTest.java                 # 통합: CLOB/BLOB (8건)
    ├── OracleConnectionTest.java    # 통합: 접속 (2건)
    └── SeedDataTest.java            # 시드 데이터 삽입 (48행)
```

---

## 보안 감사 이력 (v0.0.2)

은행 폐쇄망 배포를 위한 코드 보안 감사에서 발견/수정된 항목:

| # | 항목 | 위험도 | 수정 내용 |
| --- | --- | :---: | --- |
| BUG-1 | EUC-KR 미보장 | 높음 | `Charset.forName()` + `ExceptionInInitializerError` |
| BUG-2 | `toDb()` SQL 템플릿 손상 | 높음 | `transformForDriver()` 도입 (UTL_RAW 변환) |
| BUG-3 | `getConnection()` raw 객체 반환 | 중간 | `wrappedConnection` 참조 체인 |
| BUG-4 | `unwrap/isWrapperFor` 미인식 | 낮음 | 자기 타입 검사 추가 |
| BUG-5 | `getStatement()` 타입/객체 손실 | 중간 | `wrappedStatement` 참조 체인 |
| BUG-6 | 미닫힌 따옴표 SQL 손상 | 높음 | 원본 SQL 반환으로 변경 |
| BUG-7 | `setNString(null)` NPE | 중간 | `setNull()` 분기 처리 |
| BUG-8 | `toApp()` Oracle US7ASCII 매핑 불일치 | 높음 | U+FFxx 매핑 역산 방식으로 변경 |

---

## 주의 사항

- 이 드라이버는 `US7ASCII` DB에 `EUC-KR` 데이터를 저장하는 특수한 환경("US7ASCII Hack")을 위해 제작되었습니다.
- EUC-KR에 포함되지 않는 문자(이모지, 태국어, 아랍어, 대부분의 라틴 악센트 문자 등)는 인코딩 변환 시 손실됩니다.
- `NVARCHAR`, `NCHAR` 등 유니코드 전용 컬럼에 대해서는 테스트되지 않았습니다.
- Oracle JDBC thin 드라이버의 US7ASCII 바이트 매핑(byte 0x80+ → U+FF80+)은 ojdbc8/ojdbc10/ojdbc11 23.x에서 확인되었습니다. 다른 드라이버 버전에서는 ISO-8859-1 매핑(U+0080+)일 수 있으며, `toApp()`은 두 매핑 모두 지원합니다.
