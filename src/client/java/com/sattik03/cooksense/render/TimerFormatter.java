package com.sattik03.cooksense.render;

public class TimerFormatter {

    public static final int COLOR_WHITE = 0xFFFFFF;
    public static final int COLOR_GOLD = 0xFFD700;
    public static final int COLOR_GREEN = 0x7CFC00;
    public static final int COLOR_DONE = 0x00FF88;
    public static final int COLOR_SOUL = 0x4FC3F7;

    /**
     * Formats ticks into M:SS display.
     * Rounds up so 1 tick still shows as 0:01 rather than 0:00.
     */
    public static String formatTime(int remainingTicks) {
        if (remainingTicks <= 0) {
            return "0:00";
        }
        int totalSeconds = (remainingTicks + 19) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Returns the appropriate hex RGB color based on remaining cooking time.
     */
    public static int getColor(int remainingTicks, int totalTicks, boolean isSoulCampfire, boolean soulBlueEnabled) {
        if (remainingTicks <= 0) {
            return COLOR_DONE;
        }

        if (isSoulCampfire && soulBlueEnabled) {
            return COLOR_SOUL;
        }

        float ratio = (float) remainingTicks / (float) Math.max(1, totalTicks);

        if (ratio < 0.20f || remainingTicks <= 100) { // < 20% or under 5 seconds
            return COLOR_GREEN;
        } else if (ratio <= 0.50f) {
            return COLOR_GOLD;
        } else {
            return COLOR_WHITE;
        }
    }
}
