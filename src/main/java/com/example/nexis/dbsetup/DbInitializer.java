package com.example.nexis.dbsetup;

import java.sql.*;
import java.util.function.Consumer;

// DB 연결 테스트 및 테이블 초기화를 담당하는 클래스
// GUI(DbSetupFrame)와 CLI(DbSetupMain) 양쪽에서 공통으로 사용
public class DbInitializer {

    private final String dbType;
    private final String host;
    private final int    port;
    private final String name;
    private final String user;
    private final String pass;

    public DbInitializer(String dbType, String host, int port,
                         String name, String user, String pass) {
        this.dbType = dbType;
        this.host   = host;
        this.port   = port;
        this.name   = name;
        this.user   = user;
        this.pass   = pass;
    }

    // DB 연결이 정상인지 확인 (3초 타임아웃)
    public boolean testConnection() throws Exception {
        try (Connection conn = getConnection()) {
            return conn.isValid(3);
        }
    }

    // 모든 테이블을 CREATE TABLE IF NOT EXISTS로 생성
    // Consumer<String> log: 로그 출력 방식을 외부에서 주입 (GUI면 텍스트 영역, CLI면 System.out)
    public void initialize(Consumer<String> log) throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // DB 종류에 따라 문법이 다른 부분을 변수로 분리
            boolean isPg     = dbType.equalsIgnoreCase("postgresql");
            String autoInc   = isPg ? "BIGSERIAL PRIMARY KEY" : "BIGINT AUTO_INCREMENT PRIMARY KEY";
            String textType  = "TEXT";
            String boolDef   = "BOOLEAN DEFAULT TRUE";
            String boolFalse = "BOOLEAN DEFAULT FALSE";
            String dtType    = isPg ? "TIMESTAMP" : "DATETIME";
            String dblType   = isPg ? "DOUBLE PRECISION" : "DOUBLE";
            // PostgreSQL에서 interval은 예약어이므로 큰따옴표로 이스케이프
            String intervalCol = isPg ? "\"interval\"" : "`interval`";

            log.accept("Creating table: hosts");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS hosts (
                    id                       %s,
                    hostname                 VARCHAR(255) NOT NULL UNIQUE,
                    ip_address               VARCHAR(255),
                    agent_port               INT          DEFAULT 10050,
                    os                       VARCHAR(255),
                    description              %s,
                    enabled                  %s,
                    agent_active             %s,
                    default_group_added      %s,
                    default_group_registered %s,
                    agent_last_check         %s NULL,
                    created_at               %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, textType, boolDef, boolFalse, boolFalse, boolFalse, dtType, dtType));

            log.accept("Creating table: users");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id          %s,
                    username    VARCHAR(255) NOT NULL UNIQUE,
                    full_name   VARCHAR(255),
                    password    VARCHAR(255),
                    role        VARCHAR(100) DEFAULT 'User',
                    description %s,
                    enabled     %s,
                    last_login  %s NULL,
                    created_at  %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, textType, boolDef, dtType, dtType));

            log.accept("Creating table: user_groups");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_groups (
                    id          %s,
                    name        VARCHAR(255) NOT NULL UNIQUE,
                    description %s,
                    enabled     %s,
                    created_at  %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, textType, boolDef, dtType));

            // MySQL은 인라인 UNIQUE KEY, PostgreSQL은 ALTER TABLE로 따로 추가 (문법 차이)
            log.accept("Creating table: user_group_members");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_group_members (
                    id       %s,
                    user_id  BIGINT NOT NULL,
                    group_id BIGINT NOT NULL%s
                )""".formatted(autoInc, isPg ? "" : ", UNIQUE KEY uq_user_group (user_id, group_id)"));
            if (isPg) {
                try { stmt.execute("ALTER TABLE user_group_members ADD CONSTRAINT uq_user_group UNIQUE (user_id, group_id)"); }
                catch (Exception ignored) {} // 이미 존재하면 무시
            }

            log.accept("Creating table: user_group_permissions");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_group_permissions (
                    id           %s,
                    group_id     BIGINT      NOT NULL,
                    resource     VARCHAR(50) NOT NULL,
                    access_level VARCHAR(20) NOT NULL DEFAULT 'none'%s
                )""".formatted(autoInc, isPg ? "" : ", UNIQUE KEY uq_group_resource (group_id, resource)"));
            if (isPg) {
                try { stmt.execute("ALTER TABLE user_group_permissions ADD CONSTRAINT uq_group_resource UNIQUE (group_id, resource)"); }
                catch (Exception ignored) {}
            }

            log.accept("Creating table: user_group_host_group_access");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_group_host_group_access (
                    id            %s,
                    group_id      BIGINT NOT NULL,
                    host_group_id BIGINT NOT NULL%s
                )""".formatted(autoInc, isPg ? "" : ", UNIQUE KEY uq_ug_hg (group_id, host_group_id)"));
            if (isPg) {
                try { stmt.execute("ALTER TABLE user_group_host_group_access ADD CONSTRAINT uq_ug_hg UNIQUE (group_id, host_group_id)"); }
                catch (Exception ignored) {}
            }

            // check_cpu(boolDef), check_memory(boolDef), check_disk(boolFalse),
            // check_network(boolFalse), check_process(boolFalse) — 총 5개 bool 컬럼
            log.accept("Creating table: templates");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS templates (
                    id               %s,
                    name             VARCHAR(255) NOT NULL UNIQUE,
                    description      %s,
                    os_type          VARCHAR(100) DEFAULT 'Any',
                    check_cpu        %s,
                    check_memory     %s,
                    check_disk       %s,
                    check_network    %s,
                    check_process    %s,
                    cpu_threshold    INT DEFAULT 90,
                    memory_threshold INT DEFAULT 90,
                    created_at       %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, textType, boolDef, boolDef, boolFalse, boolFalse, boolFalse, dtType));

            log.accept("Creating table: host_groups");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS host_groups (
                    id          %s,
                    name        VARCHAR(255) NOT NULL UNIQUE,
                    description %s,
                    created_at  %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, textType, dtType));

            // hostname 컬럼 없이 host_id만 사용 (정규화)
            log.accept("Creating table: host_group_members");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS host_group_members (
                    id       %s,
                    group_id BIGINT NOT NULL,
                    host_id  BIGINT NOT NULL%s
                )""".formatted(autoInc, isPg ? "" : ", UNIQUE KEY uq_hg_host (group_id, host_id)"));
            if (isPg) {
                try { stmt.execute("ALTER TABLE host_group_members ADD CONSTRAINT uq_hg_host UNIQUE (group_id, host_id)"); }
                catch (Exception ignored) {}
            }

            // MySQL과 PostgreSQL의 UNIQUE 제약 문법이 달라서 분기
            log.accept("Creating table: host_templates");
            if (isPg) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS host_templates (
                        id          %s,
                        host_id     BIGINT NOT NULL,
                        template_id BIGINT NOT NULL,
                        linked_at   %s DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE (host_id, template_id)
                    )""".formatted(autoInc, dtType));
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS host_templates (
                        id          %s,
                        host_id     BIGINT NOT NULL,
                        template_id BIGINT NOT NULL,
                        linked_at   %s DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_host_template (host_id, template_id)
                    )""".formatted(autoInc, dtType));
            }

            // interval 컬럼: 아이템별 수집 주기(초), 기본값 60초
            // HostSyncScheduler가 이 값을 기반으로 Inactive 판단 기준을 계산
            // intervalCol: PostgreSQL은 interval이 예약어이므로 "interval"(따옴표)로 이스케이프
            log.accept("Creating table: items");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id                 %s,
                    name               VARCHAR(255) NOT NULL,
                    item_key           VARCHAR(255) NOT NULL,
                    metric             VARCHAR(100) NOT NULL,
                    value_type         VARCHAR(100) NOT NULL,
                    unit               VARCHAR(50)  NOT NULL DEFAULT 'float',
                    unit_display       VARCHAR(50),
                    template_id        BIGINT NULL,
                    host_id            BIGINT NULL,
                    source_template_id BIGINT NULL,
                    enabled            %s,
                    description        %s,
                    %s                 INT          NOT NULL DEFAULT 60,
                    created_at         %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, boolDef, textType, intervalCol, dtType));

            // duration: 0이면 단순 임계값, 양수면 해당 분 동안 지속 시 발동
            log.accept("Creating table: triggers");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS triggers (
                    id                 %s,
                    name               VARCHAR(255)  NOT NULL,
                    item_id            BIGINT        NOT NULL,
                    host_id            BIGINT        NULL,
                    template_id        BIGINT        NULL,
                    source_template_id BIGINT        NULL,
                    expression         VARCHAR(1000) NULL,
                    func               VARCHAR(50)   NOT NULL DEFAULT 'last',
                    operator           VARCHAR(10)   NOT NULL DEFAULT '>',
                    threshold          %s            NOT NULL DEFAULT 0,
                    severity           VARCHAR(20)   NOT NULL DEFAULT 'Not classified',
                    enabled            %s,
                    duration           INT           NOT NULL DEFAULT 0,
                    description        %s,
                    created_at         %s DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, dblType, boolDef, textType, dtType));

            // status: PROBLEM / ACKNOWLEDGED / RESOLVED
            log.accept("Creating table: problems");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS problems (
                    id           %s,
                    trigger_id   BIGINT        NOT NULL,
                    host_id      BIGINT        NOT NULL,
                    hostname     VARCHAR(255),
                    trigger_name VARCHAR(255),
                    item_name    VARCHAR(255),
                    expression   VARCHAR(1000) NULL,
                    severity     VARCHAR(20),
                    value        VARCHAR(100),
                    status       VARCHAR(20)   NOT NULL DEFAULT 'PROBLEM',
                    started_at   %s            DEFAULT CURRENT_TIMESTAMP,
                    resolved_at  %s            NULL
                )""".formatted(autoInc, dtType, dtType));

            log.accept("Creating table: item_data");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS item_data (
                    id           %s,
                    item_id      BIGINT       NOT NULL,
                    hostname     VARCHAR(255) NOT NULL,
                    value        %s,
                    collected_at %s           DEFAULT CURRENT_TIMESTAMP
                )""".formatted(autoInc, dblType, dtType));

            // item_data 조회 성능을 위한 인덱스 생성 (이미 있으면 무시)
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_item_data_item_time ON item_data (item_id, collected_at)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_item_data_host_time ON item_data (hostname, collected_at)");
            } catch (Exception ignored) {}

            log.accept("All tables created successfully.");
        }
    }

    // DB 종류에 맞는 드라이버 로딩 + JDBC URL 생성 후 커넥션 반환
    private Connection getConnection() throws Exception {
        String url, driver;
        switch (dbType.toLowerCase()) {
            case "mariadb" -> {
                driver = "org.mariadb.jdbc.Driver";
                url = String.format("jdbc:mariadb://%s:%d/%s?useSSL=false&serverTimezone=Asia/Seoul", host, port, name);
            }
            case "postgresql" -> {
                driver = "org.postgresql.Driver";
                url = String.format("jdbc:postgresql://%s:%d/%s", host, port, name);
            }
            default -> { // MySQL 기본값
                driver = "com.mysql.cj.jdbc.Driver";
                url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true", host, port, name);
            }
        }
        Class.forName(driver); // 드라이버 클래스 동적 로딩
        return DriverManager.getConnection(url, user, pass);
    }
}