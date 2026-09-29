package qz.ui.tray;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Decides where the tray menu window goes, given the tray icon position.
 *
 * <p>Pure geometry on purpose: the window manager and {@code pack()} cannot run headless, so the
 * decision is kept separate from {@link TaskbarTrayIcon} and unit tested.
 */
public final class TrayMenuPlacement {

    /** Space kept between the menu and the tray icon, so the icon stays clickable. */
    public static final int GAP = 4;

    private TrayMenuPlacement() {}

    /**
     * Top-left corner for the menu.
     *
     * <p>The menu is pinned to the tray icon: it goes above the icon when there is room, below it
     * when there is more room below, and the result is clamped so the menu cannot leave the
     * display.  It is deliberately not clamped to the work area, because the icon itself lives in
     * the panel at the screen edge, and a work-area clamp would float the menu far away from it.
     *
     * @param anchor tray icon position on screen, never {@code null}
     * @param menu   current menu size, never {@code null}
     * @param screen bounds of the monitor holding the anchor, never {@code null}
     */
    public static Point topLeft(Point anchor, Dimension menu, Rectangle screen) {
        int left = screen.x;
        int right = screen.x + screen.width;
        int top = screen.y;
        int bottom = screen.y + screen.height;

        int roomAbove = anchor.y - top;
        int roomBelow = bottom - anchor.y;
        boolean fitsAbove = roomAbove >= menu.height + GAP;

        int y = (fitsAbove || roomAbove >= roomBelow) ? anchor.y - menu.height - GAP : anchor.y + GAP;
        int x = anchor.x > left + (right - left) / 2 ? anchor.x - menu.width : anchor.x;

        return new Point(
                clamp(x, left, right - menu.width),
                clamp(y, top, bottom - menu.height));
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            // Menu is bigger than the screen on this axis; pin to the leading edge, never invert.
            return min;
        }
        return Math.max(min, Math.min(value, max));
    }
}
