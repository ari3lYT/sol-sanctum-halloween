package ru.d2.elysium;

public interface Canvas {
    int width();
    int height();
    void fill(int left, int top, int right, int bottom, int color);
    void text(String text, int x, int y, int color);
    int textWidth(String text);
    default void fitText(String text, int x, int y, int maxWidth, int color) {
        String result = text;
        while (textWidth(result) > maxWidth && result.length() > 1) result = result.substring(0, result.length() - 1);
        text(result, x, y, color);
    }
}
