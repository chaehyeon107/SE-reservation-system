package com.example.reservationsystem.testsupport;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class TcLogWatcher implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        TcLogger.pass(context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        TcLogger.fail(context.getDisplayName(), cause);
    }
}
