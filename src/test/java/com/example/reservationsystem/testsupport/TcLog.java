package com.example.reservationsystem.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;



public final class TcLog {

    public static void expectedError(String tcId, String expectedError, String expectedMessage) {
        line("[TC=" + tcId + "] [EXPECTED_ERROR=" + expectedError + "] [EXPECTED_MESSAGE=" + expectedMessage + "]");
    }
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    // ✅ 한 파일로 모으기: build/test-output.txt
    private static final Path OUT = Paths.get("build", "test-output.txt");

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TcLog() {}

    private static void initIfNeeded() {
        if (INITIALIZED.compareAndSet(false, true)) {
            try {
                Files.createDirectories(OUT.getParent());
                Files.writeString(OUT, "", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to init test-output file: " + OUT, e);
            }
        }
    }

    public static void line(String msg) {
        initIfNeeded();
        String ts = LocalDateTime.now().format(FMT);
        String full = ts + " " + msg + System.lineSeparator();

        // 콘솔에도 남기기
        System.out.print(full);

        // 파일에도 append
        try {
            Files.writeString(OUT, full, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write test log: " + OUT, e);
        }
    }

    public static void pass(String displayName) {
        line("[RESULT=PASS] " + displayName);
    }

    public static void fail(String displayName, Throwable cause) {
        line("[RESULT=FAIL] " + displayName
                + " [ERROR=" + (cause == null ? "null" : cause.getClass().getSimpleName()) + "]"
                + " [MESSAGE=" + safeMsg(cause) + "]");
    }

    private static String safeMsg(Throwable t) {
        if (t == null) return "null";
        return (t.getMessage() == null) ? "null" : t.getMessage();
    }
}
