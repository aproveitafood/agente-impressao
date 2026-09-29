package qz.ui.tray;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.awt.Point;
import java.awt.Rectangle;

import static org.testng.Assert.assertEquals;
import static qz.ui.tray.TrayActivationGate.*;

public class TrayActivationGateTests {
    /** The menu window sits next to the tray icon, so the pointer is inside it while browsing rows. */
    private static final Rectangle MENU_BOUNDS = new Rectangle(1900, 900, 300, 400);
    private static final Point TRAY_ICON = new Point(1980, 30);
    private static final long T0 = 1_000_000L;

    @Test
    public void pressAndReleaseTogglesTheMenu() {
        TrayActivationGate gate = new TrayActivationGate();

        assertEquals(gate.onEvent(Event.PRESS, null, false, null, T0), Decision.IGNORE);
        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, false, MENU_BOUNDS, T0 + 300), Decision.TOGGLE);
    }

    @Test
    public void pressAndReleaseClosesAnOpenMenu() {
        TrayActivationGate gate = new TrayActivationGate();

        gate.onEvent(Event.PRESS, null, false, null, T0);
        gate.onEvent(Event.RELEASE, TRAY_ICON, false, MENU_BOUNDS, T0 + 300);

        gate.onEvent(Event.PRESS, null, true, MENU_BOUNDS, T0 + 5000);
        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, true, MENU_BOUNDS, T0 + 5300), Decision.TOGGLE);
    }

    /**
     * The regression this class exists for: a tray host reports an extra release as the pointer
     * leaves the icon, with no press.  With the menu open it must not close the window.
     */
    @Test
    public void releaseWithoutPressDoesNotCloseAnOpenMenu() {
        TrayActivationGate gate = new TrayActivationGate();

        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, true, MENU_BOUNDS, T0), Decision.IGNORE);
        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, true, MENU_BOUNDS, T0 + 5000), Decision.IGNORE);
    }

    /** Tray hosts that report no press at all must still be able to open the menu. */
    @Test
    public void releaseWithoutPressOpensAClosedMenu() {
        TrayActivationGate gate = new TrayActivationGate();

        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, false, MENU_BOUNDS, T0), Decision.TOGGLE);
    }

    @Test
    public void releaseInsideTheMenuBoundsIsIgnored() {
        TrayActivationGate gate = new TrayActivationGate();
        Point insideMenu = new Point(MENU_BOUNDS.x + 20, MENU_BOUNDS.y + 20);

        assertEquals(gate.onEvent(Event.PRESS, null, true, MENU_BOUNDS, T0), Decision.IGNORE);
        assertEquals(gate.onEvent(Event.RELEASE, insideMenu, true, MENU_BOUNDS, T0 + 300), Decision.IGNORE);
    }

    @Test
    public void releaseInsideTheMenuBoundsIsIgnoredEvenWithAMenuClosed() {
        TrayActivationGate gate = new TrayActivationGate();
        Point insideMenu = new Point(MENU_BOUNDS.x + 20, MENU_BOUNDS.y + 20);

        assertEquals(gate.onEvent(Event.RELEASE, insideMenu, false, MENU_BOUNDS, T0), Decision.IGNORE);
    }

    @DataProvider(name = "rapidFollowUps")
    public Object[][] rapidFollowUps() {
        return new Object[][] {
                {T0 + 1L}, {T0 + 249L},
        };
    }

    @Test(dataProvider = "rapidFollowUps")
    public void releaseInsideTheDebounceWindowIsIgnored(long nowMillis) {
        TrayActivationGate gate = new TrayActivationGate();

        gate.onEvent(Event.PRESS, null, false, null, T0);
        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, false, MENU_BOUNDS, T0), Decision.TOGGLE);

        gate.onEvent(Event.PRESS, null, false, null, nowMillis);
        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, false, MENU_BOUNDS, nowMillis), Decision.IGNORE);
    }

    @Test
    public void otherEventsAreIgnored() {
        TrayActivationGate gate = new TrayActivationGate();

        assertEquals(gate.onEvent(Event.OTHER, TRAY_ICON, true, MENU_BOUNDS, T0), Decision.IGNORE);
    }

    @Test
    public void unknownPointerAndBoundsAreTolerated() {
        TrayActivationGate gate = new TrayActivationGate();

        gate.onEvent(Event.PRESS, null, true, null, T0);
        assertEquals(gate.onEvent(Event.RELEASE, null, true, null, T0 + 300), Decision.TOGGLE);
    }

    @Test
    public void aStalePressCannotCloseAnOpenMenu() {
        TrayActivationGate gate = new TrayActivationGate();

        // A press the tray host swallowed, with the matching release consumed elsewhere
        gate.onEvent(Event.PRESS, null, false, null, T0);
        gate.reset();

        assertEquals(gate.onEvent(Event.RELEASE, TRAY_ICON, true, MENU_BOUNDS, T0 + 5000), Decision.IGNORE);
    }
}
