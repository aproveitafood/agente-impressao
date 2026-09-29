package qz.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.ui.component.LinkLabel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * The look-and-feel paints the hover state from its own style, ignoring {@code setForeground}, so a
 * brand color used to turn light and disappear on the branded cream surfaces. These tests assert the
 * pixels actually painted, because the client properties alone do not protect the color.
 */
public class ThemeUtilitiesTests {

    /** Enough glyph pixels for a legible word, tolerant of font and hinting differences. */
    private static final int GLYPH_PIXELS = 15;

    private static final Color CREAM = Constants.BRAND_CREAM_COLOR;
    private static final Color COCOA = Constants.BRAND_COCOA_COLOR;
    private static final Color PAPRIKA = Constants.BRAND_PRIMARY_COLOR;

    private LookAndFeel previous;

    @BeforeMethod
    public void rememberLookAndFeel() {
        previous = UIManager.getLookAndFeel();
    }

    @AfterMethod
    public void restoreLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(previous);
    }

    @DataProvider(name = "lafs")
    public Object[][] lafs() {
        return new Object[][] {{"darcula"}, {"light"}};
    }

    /** The tray menu only ever exists with the brand table applied, so the tests reproduce that. */
    private void install(String laf) throws Exception {
        UIManager.setLookAndFeel("darcula".equals(laf)? new FlatDarculaLaf(): new FlatIntelliJLaf());
        ThemeUtilities.applyBrandTheme();
    }

    @Test(dataProvider = "lafs")
    public void linkKeepsTheBrandColorWhenHovered(String laf) throws Exception {
        install(laf);
        LinkLabel link = new LinkLabel("Aproveita Food");
        link.setForeground(PAPRIKA);

        String style = String.valueOf(link.getClientProperty(FlatClientProperties.STYLE));
        assertTrue(style.contains("hoverForeground:#BC361C"),
                "the hover foreground has to be pinned in the component style, got " + style);

        enter(link);
        assertEquals(link.getForeground(), PAPRIKA, "hover must not change the link color");
        assertTrue(painted(link, CREAM, PAPRIKA) >= GLYPH_PIXELS,
                "the link text has to stay paprika on hover, found "
                        + painted(link, CREAM, PAPRIKA) + " pixels of " + hex(PAPRIKA));
        leave(link);

        assertEquals(link.getForeground(), PAPRIKA, "the resting color has to come back");
    }

    @Test(dataProvider = "lafs")
    public void trayRowKeepsCocoaTextOnHover(String laf) throws Exception {
        install(laf);
        JButton row = new JButton("Recarregar");
        TrayManager.styleTrayRow(row);

        assertEquals(row.getBackground(), Constants.BRAND_CARD_COLOR);
        assertEquals(row.getForeground(), COCOA);

        enter(row);
        assertEquals(row.getForeground(), COCOA, "the tray row text has to stay cocoa on hover");
        assertEquals(row.getBackground(), ThemeUtilities.mix(Constants.BRAND_CARD_COLOR, PAPRIKA, 0.12f));
        assertTrue(painted(row, row.getBackground(), COCOA) >= GLYPH_PIXELS,
                "the row text has to be painted in cocoa, found "
                        + painted(row, row.getBackground(), COCOA) + " pixels");
        leave(row);

        assertEquals(row.getForeground(), COCOA);
        assertEquals(row.getBackground(), Constants.BRAND_CARD_COLOR, "the row background has to come back");
    }

    @Test(dataProvider = "lafs")
    public void transparentControlKeepsItsBackgroundUntouched(String laf) throws Exception {
        install(laf);
        JButton glyph = new JButton();
        glyph.setContentAreaFilled(false);
        glyph.setOpaque(false);
        glyph.setForeground(COCOA);
        ThemeUtilities.applyBrandHover(glyph, COCOA, null, COCOA, null);

        Color before = glyph.getBackground();
        enter(glyph);
        assertEquals(glyph.getForeground(), COCOA);
        assertSame(glyph.getBackground(), before, "a null background must never be touched");
        leave(glyph);
    }

    @Test
    public void mixBlendsAndToleratesMissingColors() {
        assertEquals(ThemeUtilities.mix(null, PAPRIKA, 0.5f), PAPRIKA);
        assertEquals(ThemeUtilities.mix(PAPRIKA, null, 0.5f), PAPRIKA);
        assertEquals(ThemeUtilities.mix(Constants.BRAND_CARD_COLOR, PAPRIKA, 0.12f),
                new Color(0xF7, 0xE7, 0xE4), "the tray row hover tint is part of the visual contract");
    }

    @Test
    public void themeDefinesTheHoverForegroundDefault() {
        // Without this default the look-and-feel keeps its own light hover text.
        ThemeUtilities.applyBrandTheme();
        assertNotNull(UIManager.getColor("Button.hoverForeground"), "Button.hoverForeground is required");
        assertNotNull(UIManager.getColor("Button.focusedForeground"), "Button.focusedForeground is required");
    }

    private static void enter(JComponent component) {
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_ENTERED, 0L, 0, 5, 5, 0, false, MouseEvent.NOBUTTON));
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_MOVED, 0L, 0, 6, 6, 0, false, MouseEvent.NOBUTTON));
    }

    private static void leave(JComponent component) {
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_EXITED, 0L, 0, 5, 5, 0, false, MouseEvent.NOBUTTON));
    }

    /**
     * Counts the pixels painted close to {@code color} over {@code surface}. Antialiasing is turned
     * off so the glyph core is measured, and the border is skipped so the focus ring, which is a
     * different color on purpose, is not counted as text.
     */
    private static int painted(JComponent component, Color surface, Color color) {
        int width = Math.max(component.getPreferredSize().width, 220);
        int height = Math.max(component.getPreferredSize().height, 40);
        component.setSize(width, height);
        component.doLayout();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(surface);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        component.paint(graphics);
        graphics.dispose();

        int found = 0;
        for(int y = 8; y < height - 8; y++) {
            for(int x = 8; x < width - 8; x++) {
                if(close(new Color(image.getRGB(x, y) & 0xFFFFFF), color, 40)) {
                    found++;
                }
            }
        }
        return found;
    }

    private static boolean close(Color first, Color second, int tolerance) {
        if(first == null || second == null) {
            return false;
        }
        return Math.abs(first.getRed() - second.getRed()) <= tolerance
                && Math.abs(first.getGreen() - second.getGreen()) <= tolerance
                && Math.abs(first.getBlue() - second.getBlue()) <= tolerance;
    }

    private static String hex(Color color) {
        return color == null? "null": String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
