package ru.d2.elysium.core;

public final class Config {
    public int ordinaryPoints = 1;
    public int uniquePoints = 3;
    public boolean syncWebsite = false;
    public boolean showHud = true;
    public boolean showInTab = true;
    public double hudX = 0;
    public double hudY = 0;
    public int pollSeconds = 5;

    public void validate() {
        ordinaryPoints = Math.clamp(ordinaryPoints, 0, 100000);
        uniquePoints = Math.clamp(uniquePoints, 0, 100000);
        hudX = Double.isFinite(hudX) ? Math.clamp(hudX, 0.0, 1.0) : 0;
        hudY = Double.isFinite(hudY) ? Math.clamp(hudY, 0.0, 1.0) : 0;
        pollSeconds = Math.clamp(pollSeconds, 3, 60);
    }
}
