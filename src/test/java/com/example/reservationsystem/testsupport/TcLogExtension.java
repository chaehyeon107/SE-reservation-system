package com.example.reservationsystem.testsupport;

import org.junit.jupiter.api.extension.*;

public class TcLogExtension implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        TcLog.pass(context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        TcLog.fail(context.getDisplayName(), cause);
    }
}
