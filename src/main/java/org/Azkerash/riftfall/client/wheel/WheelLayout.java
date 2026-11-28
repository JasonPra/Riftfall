package org.Azkerash.riftfall.client.wheel;

/** Shared hit testing: six entries per page, clockwise from twelve o'clock. */
public final class WheelLayout {
    public static final int PAGE_SIZE = 6;

    public static int hovered(double dx, double dy, double inner, double outer, int count) {
        double distance = Math.hypot(dx, dy);
        if (count == 0 || distance < inner || distance > outer) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2 + Math.PI / count;
        angle = (angle % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
        return Math.min(count - 1, (int) (angle / (Math.PI * 2) * count));
    }

    public static boolean eligible(String name, String category) {
        String both = (name + " " + category).toLowerCase(java.util.Locale.ROOT);
        return !both.contains("player_system")
                && !both.contains("playersystem")
                && !name.equals("key.riftfall.action_wheel")
                && !java.util.Set.of(
                                "key.categories.movement",
                                "key.categories.gameplay",
                                "key.categories.inventory",
                                "key.categories.creative",
                                "key.categories.multiplayer",
                                "key.categories.ui",
                                "key.categories.misc")
                        .contains(category);
    }

    private WheelLayout() {}
}
