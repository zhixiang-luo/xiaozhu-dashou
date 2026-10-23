package com.personaljarvis;

import android.content.Context;
import android.os.RemoteException;

import java.io.IOException;
import java.util.Arrays;

public final class ShizukuActionService extends IShizukuActionService.Stub {
    public ShizukuActionService() {
    }

    public ShizukuActionService(Context ignored) {
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public boolean tap(int x, int y) throws RemoteException {
        return runInput("tap", Integer.toString(x), Integer.toString(y));
    }

    @Override
    public boolean swipe(
            int fromX,
            int fromY,
            int toX,
            int toY,
            int durationMs
    ) throws RemoteException {
        return runInput(
                "swipe",
                Integer.toString(fromX),
                Integer.toString(fromY),
                Integer.toString(toX),
                Integer.toString(toY),
                Integer.toString(durationMs)
        );
    }

    @Override
    public boolean inputText(String text) throws RemoteException {
        String escaped = text
                .replace("%", "%%")
                .replace(" ", "%s");
        return runInput("text", escaped);
    }

    @Override
    public boolean keyEvent(int keyCode) throws RemoteException {
        return runInput("keyevent", Integer.toString(keyCode));
    }

    @Override
    public boolean openApp(String packageName) throws RemoteException {
        if (!packageName.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) {
            return false;
        }
        return runInput("monkey", "-p", packageName, "1");
    }

    private boolean runInput(String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "input";
        if (args.length > 0 && "monkey".equals(args[0])) {
            command[0] = "monkey";
            System.arraycopy(args, 1, command, 1, args.length - 1);
            command = Arrays.copyOf(command, args.length);
        } else {
            System.arraycopy(args, 0, command, 1, args.length);
        }

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        }
    }
}
