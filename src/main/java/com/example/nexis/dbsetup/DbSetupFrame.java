package com.example.nexis.dbsetup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Swing 기반 GUI 설정 창
// JFrame: 독립적인 최상위 윈도우 컴포넌트
public class DbSetupFrame extends JFrame {

    private JComboBox<String>  dbTypeCombo;
    private JTextField         dbHostField, dbPortField, dbNameField, dbUserField;
    private JPasswordField     dbPassField; // 비밀번호 입력 시 마스킹 처리
    private JTextArea          logArea;
    private JButton            testBtn, installBtn;

    public DbSetupFrame() {
        setTitle("Nexis DB Setup");
        setSize(520, 580);
        setDefaultCloseOperation(EXIT_ON_CLOSE); // 창 닫으면 프로세스 종료
        setLocationRelativeTo(null);             // 화면 정중앙에 표시
        setResizable(false);

        // BorderLayout: 컴포넌트를 NORTH/CENTER/SOUTH 등으로 배치
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(16, 16, 16, 16)); // 안쪽 여백
        main.setBackground(Color.WHITE);

        // ── 폼 패널 (GridBagLayout: 셀 단위 배치, 가장 유연한 레이아웃) ──
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(5, 5, 5, 5); // 셀 간격
        gc.anchor  = GridBagConstraints.WEST; // 왼쪽 정렬

        // DB Type 드롭다운
        gc.gridx = 0; gc.gridy = 0;
        form.add(new JLabel("DB Type:"), gc);
        dbTypeCombo = new JComboBox<>(new String[]{"MySQL", "MariaDB", "PostgreSQL"});
        dbTypeCombo.addActionListener(e -> onDbTypeChange()); // 선택 변경 시 포트 자동 변경
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        form.add(dbTypeCombo, gc);

        // DB Host
        gc.gridx = 0; gc.gridy = 1; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        form.add(new JLabel("DB Host:"), gc);
        dbHostField = new JTextField("127.0.0.1", 20);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        form.add(dbHostField, gc);

        // DB Port
        gc.gridx = 0; gc.gridy = 2; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        form.add(new JLabel("DB Port:"), gc);
        dbPortField = new JTextField("3306", 8);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        form.add(dbPortField, gc);

        // DB Name
        gc.gridx = 0; gc.gridy = 3; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        form.add(new JLabel("DB Name:"), gc);
        dbNameField = new JTextField("nexis", 20);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        form.add(dbNameField, gc);

        // Username
        gc.gridx = 0; gc.gridy = 4; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        form.add(new JLabel("Username:"), gc);
        dbUserField = new JTextField("", 20);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        form.add(dbUserField, gc);

        // Password (JPasswordField: 입력값을 *로 마스킹)
        gc.gridx = 0; gc.gridy = 5; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        form.add(new JLabel("Password:"), gc);
        dbPassField = new JPasswordField("", 20);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        form.add(dbPassField, gc);

        // 버튼 패널
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(Color.WHITE);
        testBtn    = new JButton("Test Connection");
        installBtn = new JButton("Initialize Database");
        installBtn.setBackground(new Color(61, 141, 224)); // 파란색 강조
        installBtn.setForeground(Color.WHITE);
        installBtn.setFocusPainted(false);
        btnPanel.add(testBtn);
        btnPanel.add(installBtn);

        gc.gridx = 0; gc.gridy = 6; gc.gridwidth = 2; // 2칸 차지
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(btnPanel, gc);

        // 로그 출력 영역 (어두운 터미널 스타일)
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 255, 200)); // 연두색 텍스트
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Log"));

        main.add(form,   BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        setContentPane(main);

        // 버튼 이벤트 연결
        testBtn.addActionListener(e -> testConnection());
        installBtn.addActionListener(e -> initializeDatabase());
    }

    // DB 종류 변경 시 기본 포트 자동 설정
    private void onDbTypeChange() {
        String type = (String) dbTypeCombo.getSelectedItem();
        if ("PostgreSQL".equals(type)) {
            dbPortField.setText("5432");
        } else {
            dbPortField.setText("3306"); // MySQL / MariaDB 기본 포트
        }
    }

    // 연결 테스트 — 별도 스레드에서 실행 (UI 블로킹 방지)
    private void testConnection() {
        testBtn.setEnabled(false); // 중복 클릭 방지
        new Thread(() -> {
            log("Testing connection...");
            try {
                DbInitializer init = buildInitializer();
                if (init.testConnection()) {
                    log("✔ Connection successful!");
                } else {
                    log("✘ Connection failed.");
                }
            } catch (Exception e) {
                log("✘ Error: " + e.getMessage());
            } finally {
                // UI 업데이트는 반드시 Event Dispatch Thread(EDT)에서 해야 함
                SwingUtilities.invokeLater(() -> testBtn.setEnabled(true));
            }
        }).start();
    }

    // DB 초기화 — 별도 스레드에서 실행 (테이블 생성은 시간이 걸릴 수 있음)
    private void initializeDatabase() {
        installBtn.setEnabled(false);
        new Thread(() -> {
            log("Initializing database...");
            try {
                DbInitializer init = buildInitializer();
                init.initialize(this::log); // 메서드 참조로 log 함수 전달
                log("✔ Database initialized successfully!");
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Database initialized successfully!\nYou can now install Nexis Web and Nexis Server.",
                        "Success", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception e) {
                log("✘ Error: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> installBtn.setEnabled(true));
            }
        }).start();
    }

    // 폼 입력값으로 DbInitializer 객체 생성
    private DbInitializer buildInitializer() {
        return new DbInitializer(
            (String) dbTypeCombo.getSelectedItem(),
            dbHostField.getText().trim(),
            Integer.parseInt(dbPortField.getText().trim()),
            dbNameField.getText().trim(),
            dbUserField.getText().trim(),
            new String(dbPassField.getPassword()) // char[] → String 변환
        );
    }

    // 로그 텍스트 추가 — EDT에서 실행해 스레드 안전 보장, 항상 최신 줄로 스크롤
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // 맨 아래로 자동 스크롤
        });
    }
}