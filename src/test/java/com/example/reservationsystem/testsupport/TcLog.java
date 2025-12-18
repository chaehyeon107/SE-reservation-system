package com.example.reservationsystem.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcLog {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    // ✅ 네가 원하는 파일: build/test-output.txt
    private static final Path OUT = Paths.get("build", "test-output.txt");

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TcLog() {}

    private static void initIfNeeded() {
        if (INITIALIZED.compareAndSet(false, true)) {
            try {
                Files.createDirectories(OUT.getParent());
                // 매 실행마다 새로 시작(원하면 APPEND 유지로 바꿀 수 있음)
                Files.writeString(OUT, "", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to init test-output file: " + OUT, e);
            }
        }
    }

    private static void writeLine(String msg) {
        initIfNeeded();
        String ts = LocalDateTime.now().format(FMT);
        String full = ts + " " + msg + System.lineSeparator();

        // 콘솔에도 찍기 (Gradle/IDE에서도 보임)
        System.out.print(full);

        // 파일에도 append
        try {
            Files.writeString(OUT, full, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write test log: " + OUT, e);
        }
    }

    // ✅ 자동 PASS/FAIL 한 줄 요약
    public static void pass(String displayName) {
        writeLine("[RESULT=PASS] " + displayName);
    }

    public static void fail(String displayName, Throwable cause) {
        writeLine("[RESULT=FAIL] " + displayName
                + " [ERROR=" + (cause == null ? "null" : cause.getClass().getSimpleName()) + "]"
                + " [MESSAGE=" + safeMsg(cause) + "]");
    }

    private static String safeMsg(Throwable t) {
        if (t == null) return "null";
        return (t.getMessage() == null) ? "null" : t.getMessage();
    }
}
