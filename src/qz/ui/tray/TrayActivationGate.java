package qz.ui.tray;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Decides whether a mouse event observed on a tray icon is a real user activation.
 *
 * Desktop tray hosts (GNOME/AppIndicator in particular) can deliver an extra {@code MOUSE_RELEASED}
 * for the tray icon as the pointer leaves the icon's area, without a preceding press. Because the
 * tray menu is positioned at the pointer, that release arrives right as the cursor moves toward
 * the menu, and a plain toggle would close the window again.
 *
 * A release is only honored when it either follows a press on this icon, or arrives while the menu
 * is still closed, so desktops that deliver no press at all can always open the menu. Releases
 * landing inside the menu's own screen bounds, or arriving too soon after the previous toggle, are
 * dropped.
 */
public class TrayActivationGate {
    private static final long DEFAULT_DEBOUNCE_MS = 250L;

    private final long debounceMs;

    private boolean pressSeen;
    private boolean hasToggled;
    private long lastToggleMs;

    public TrayActivationGate() {
        this(DEFAULT_DEBOUNCE_MS);
    }

    public TrayActivationGate(long debounceMs) {
        this.debounceMs = debounceMs;
        reset();
    }

    /** The kind of tray event worth handing to the gate. */
    public enum Event {
        PRESS,
        RELEASE,
        OTHER
    }

    public enum Decision {
        TOGGLE,
        IGNORE
    }

    /** Forgets any pending press and the toggle rate limit. */
    public void reset() {
        pressSeen = false;
        hasToggled = false;
        lastToggleMs = 0L;
    }

    /**
     * @param event         kind of event seen on the tray icon
     * @param pointer       pointer position on screen, or {@code null} when unknown
     * @param menuVisible   whether the tray menu is currently on screen
     * @param menuBounds    menu bounds on screen, or {@code null} when unknown
     * @param nowMillis     current time, used for the toggle rate limit
     * @return whether the event should toggle the tray menu
     */
    public Decision onEvent(Event event, Point pointer, boolean menuVisible, Rectangle menuBounds, long nowMillis) {
        if (event == Event.PRESS) {
            pressSeen = true;
            return Decision.IGNORE;
        }
        if (event != Event.RELEASE) {
            return Decision.IGNORE;
        }

        boolean activated = pressSeen || !menuVisible;
        pressSeen = false;
        if (!activated || isInside(menuBounds, pointer)) {
            return Decision.IGNORE;
        }
        if (hasToggled && nowMillis - lastToggleMs < debounceMs) {
            return Decision.IGNORE;
        }

        hasToggled = true;
        lastToggleMs = nowMillis;
        return Decision.TOGGLE;
    }

    private static boolean isInside(Rectangle bounds, Point pointer) {
        return bounds != null && pointer != null && bounds.contains(pointer);
    }
}
