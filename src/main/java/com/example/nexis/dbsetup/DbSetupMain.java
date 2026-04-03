package com.example.nexis.dbsetup;

import java.util.HashMap;
import java.util.Map;

// 진입점 — args 유무에 따라 GUI 모드 / CLI 모드로 분기
public class DbSetupMain {

    public static void main(String[] args) {

        // args 없으면 GUI 모드로 실행
        if (args.length == 0) {
            // SwingUtilities.invokeLater: Swing 컴포넌트는 반드시 EDT(Event Dispatch Thread)에서 생성해야 함
            javax.swing.SwingUtilities.invokeLater(() -> {
                DbSetupFrame frame = new DbSetupFrame();
                frame.setVisible(true);
            });
            return;
        }

        // args 있으면 CLI 모드 — 서버 환경(헤드리스)이나 자동화 스크립트에서 사용
        // 사용 예: --type=MySQL --host=localhost --port=3306 --name=nexis --user=root --pass=1234
        Map<String, String> params = parseArgs(args);

        String type = params.getOrDefault("type", "MySQL");
        String host = params.getOrDefault("host", "127.0.0.1");
        int    port = Integer.parseInt(params.getOrDefault("port", "3306"));
        String name = params.getOrDefault("name", "nexis");
        String user = params.get("user");
        String pass = params.getOrDefault("pass", "");

        // --user는 필수 파라미터
        if (user == null || user.isBlank()) {
            System.err.println("[ERROR] --user is required.");
            System.exit(1);
        }

        // 시작 정보 출력
        System.out.println("[nexis-db-setup] Starting database initialization...");
        System.out.println("[nexis-db-setup] Type: " + type);
        System.out.println("[nexis-db-setup] Host: " + host + ":" + port);
        System.out.println("[nexis-db-setup] DB  : " + name);
        System.out.println("[nexis-db-setup] User: " + user);

        DbInitializer init = new DbInitializer(type, host, port, name, user, pass);

        try {
            // 1단계: 연결 테스트
            if (!init.testConnection()) {
                System.err.println("[ERROR] Cannot connect to database. Please check your settings.");
                System.exit(1);
            }
            System.out.println("[nexis-db-setup] Connection successful.");

            // 2단계: 테이블 초기화 (로그는 System.out으로 출력)
            init.initialize(msg -> System.out.println("[nexis-db-setup] " + msg));

            System.out.println("[nexis-db-setup] Database initialized successfully.");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(1);
        }
    }

    // "--key=value" 형식의 args를 Map으로 파싱
    // 예: "--host=localhost" → { "host": "localhost" }
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2); // "="로 최대 2개 분리
                if (parts.length == 2) {
                    map.put(parts[0], parts[1]);
                }
            }
        }
        return map;
    }
}