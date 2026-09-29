package qz.ui.tray;

import org.testng.annotations.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import static org.testng.Assert.assertEquals;

public class TrayMenuPlacementTests {

    /** Primary monitor in the test desktop: 2560x1080. */
    private static final Rectangle SCREEN = new Rectangle(0, 0, 2560, 1080);

    /** Tray icon in the bottom-right panel, as reported by the real desktop. */
    private static final Point ANCHOR = new Point(2125, 1068);

    private static final Dimension MENU = new Dimension(298, 678);

    @Test
    public void bottomRightIconPlacesTheMenuAboveAndToTheLeft() {
        Point placed = TrayMenuPlacement.topLeft(ANCHOR, MENU, SCREEN);

        assertEquals(placed.x, 1827);
        // Bottom edge lands 4px above the icon, not 322px above it as the work-area clamp did
        assertEquals(placed.y, 1068 - 678 - TrayMenuPlacement.GAP);
    }

    @Test
    public void expandingTheMenuKeepsItAnchoredToTheIcon() {
        Point collapsed = TrayMenuPlacement.topLeft(ANCHOR, new Dimension(298, 340), SCREEN);
        Point expanded = TrayMenuPlacement.topLeft(ANCHOR, new Dimension(298, 678), SCREEN);

        assertEquals(expanded.x, collapsed.x);
        assertEquals(expanded.y + 678, collapsed.y + 340);
    }

    @Test
    public void bottomLeftIconPlacesTheMenuAboveAndToTheRight() {
        Point placed = TrayMenuPlacement.topLeft(new Point(40, 1068), MENU, SCREEN);

        assertEquals(placed.x, 40);
        assertEquals(placed.y, 1068 - 678 - TrayMenuPlacement.GAP);
    }

    @Test
    public void topRightIconPlacesTheMenuBelowAndToTheLeft() {
        Point placed = TrayMenuPlacement.topLeft(new Point(2125, 12), MENU, SCREEN);

        assertEquals(placed.x, 1827);
        assertEquals(placed.y, 12 + TrayMenuPlacement.GAP);
    }

    @Test
    public void aMenuThatOnlyFitsBelowIsPlacedBelow() {
        // 678px of menu needs 682px; there is only 40px above the icon and 860px below it
        Rectangle tallScreen = new Rectangle(0, 0, 2560, 900);
        Point placed = TrayMenuPlacement.topLeft(new Point(2125, 40), MENU, tallScreen);

        assertEquals(placed.y, 40 + TrayMenuPlacement.GAP);
        assertEquals(placed.y + 678, 40 + TrayMenuPlacement.GAP + 678);
    }

    @Test
    public void aMenuThatFitsNowhereIsClampedPerAxis() {
        // Taller than the screen: vertical clamps to the top edge, horizontal still clears the icon
        Point placed = TrayMenuPlacement.topLeft(ANCHOR, new Dimension(298, 2000), SCREEN);

        assertEquals(placed.y, 0);
        assertEquals(placed.x, 2125 - 298);
    }

    @Test
    public void aSecondMonitorOffsetsTheMenuWithItsOwnBounds() {
        Rectangle second = new Rectangle(2560, 0, 1366, 768);
        Point placed = TrayMenuPlacement.topLeft(new Point(3860, 756), MENU, second);

        assertEquals(placed.x, 3860 - 298);
        assertEquals(placed.y, 756 - 678 - TrayMenuPlacement.GAP);
    }
}
