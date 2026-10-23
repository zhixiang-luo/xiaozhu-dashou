package com.personaljarvis;

interface IShizukuActionService {
    void destroy() = 16777114;
    boolean tap(int x, int y) = 2;
    boolean swipe(int fromX, int fromY, int toX, int toY, int durationMs) = 3;
    boolean inputText(String text) = 4;
    boolean keyEvent(int keyCode) = 5;
    boolean openApp(String packageName) = 6;
}
