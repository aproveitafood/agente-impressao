package qz.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.FontFormatException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThemeUtilities {

    private static final String HOVER_GUARD = "qz.brandHoverGuard";
    private static final String HOVER_REST_FOREGROUND = "qz.brandRestForeground";
    private static final String HOVER_REST_BACKGROUND = "qz.brandRestBackground";
    private static final String HOVER_ACTIVE_FOREGROUND = "qz.brandActiveForeground";
    private static final String HOVER_ACTIVE_BACKGROUND = "qz.brandActiveBackground";
    private static final Logger log = LogManager.getLogger(ThemeUtilities.class);
    private static final String RESOURCE_PATH = "/qz/ui/resources/fonts/";
    private static Font bodyFont;
    private static Font headingFont;

    public static void applyBrandTheme() {
        loadBrandFonts();

        boolean dark = SystemUtilities.isDarkDesktop();
        Color background = dark ? Color.decode("#2C211C") : Color.decode("#FFF3E7");
        Color surface = dark ? Color.decode("#382A23") : Color.decode("#FFFAF4");
        Color card = dark ? Color.decode("#49362D") : Color.WHITE;
        Color foreground = dark ? Color.decode("#FFF3E7") : Color.decode("#4A2B1F");
        Color muted = dark ? Color.decode("#D1BFB3") : Color.decode("#765F55");
        Color border = dark ? Color.decode("#624A3F") : Color.decode("#E8D9CC");
        Color selection = Constants.BRAND_PRIMARY_COLOR;
        Color selectionForeground = Color.WHITE;
        FontUIResource body = new FontUIResource((bodyFont == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 13) : bodyFont)
                .deriveFont(Font.PLAIN, 13f));
        FontUIResource bodyBold = new FontUIResource(body.deriveFont(Font.BOLD));
        FontUIResource heading = new FontUIResource((headingFont == null ? body : headingFont)
                .deriveFont(Font.BOLD, 13f));

        Object[][] defaults = {
                {"defaultFont", body}, {"Label.font", body}, {"Button.font", bodyBold},
                {"ToggleButton.font", bodyBold}, {"CheckBox.font", body}, {"RadioButton.font", body},
                {"Menu.font", body}, {"MenuItem.font", body}, {"CheckBoxMenuItem.font", body},
                {"RadioButtonMenuItem.font", body}, {"TextField.font", body}, {"PasswordField.font", body},
                {"TextArea.font", body}, {"TextPane.font", body}, {"EditorPane.font", body},
                {"ComboBox.font", body}, {"List.font", body}, {"Table.font", body},
                {"TableHeader.font", bodyBold}, {"TabbedPane.font", bodyBold},
                {"TitledBorder.font", heading}, {"OptionPane.messageFont", body},
                {"OptionPane.buttonFont", bodyBold}, {"Panel.background", background},
                {"Dialog.background", background}, {"OptionPane.background", background},
                {"Button.background", card}, {"Button.foreground", foreground},
                {"ToggleButton.background", card}, {"ToggleButton.foreground", foreground},
                {"Label.foreground", foreground}, {"CheckBox.foreground", foreground},
                {"Label.disabledForeground", muted},
                {"RadioButton.foreground", foreground}, {"Menu.foreground", foreground},
                {"MenuItem.foreground", foreground}, {"CheckBoxMenuItem.foreground", foreground},
                {"MenuItem.background", card}, {"CheckBoxMenuItem.background", card},
                {"RadioButtonMenuItem.foreground", foreground}, {"TextField.background", card},
                {"TextField.foreground", foreground}, {"TextArea.background", card},
                {"TextArea.foreground", foreground}, {"TextPane.background", card},
                {"TextPane.foreground", foreground}, {"EditorPane.background", card},
                {"EditorPane.foreground", foreground}, {"ComboBox.background", card},
                {"ComboBox.foreground", foreground}, {"List.background", card},
                {"List.foreground", foreground}, {"Table.background", card},
                {"Table.foreground", foreground}, {"Table.selectionBackground", selection},
                {"Table.selectionForeground", selectionForeground}, {"TableHeader.background", surface},
                {"TableHeader.foreground", foreground}, {"TabbedPane.background", background},
                {"TabbedPane.foreground", muted}, {"TabbedPane.selectedBackground", card},
                {"TabbedPane.selectedForeground", Constants.BRAND_PRIMARY_COLOR},
                {"Separator.foreground", border}, {"Separator.background", border},
                {"ScrollPane.background", background}, {"Viewport.background", card},
                {"PopupMenu.background", card}, {"PopupMenu.foreground", foreground},
                {"MenuItem.selectionBackground", selection}, {"MenuItem.selectionForeground", selectionForeground},
                {"MenuItem.hoverBackground", selection}, {"MenuItem.hoverForeground", selectionForeground},
                {"CheckBoxMenuItem.selectionBackground", selection}, {"CheckBoxMenuItem.selectionForeground", selectionForeground},
                {"CheckBoxMenuItem.hoverBackground", selection}, {"CheckBoxMenuItem.hoverForeground", selectionForeground},
                {"ToolBar.background", surface}, {"ToolBar.foreground", foreground},
                {"ToolTip.background", foreground}, {"ToolTip.foreground", background},
                {"Component.arc", 12}, {"Button.arc", 10}, {"TextComponent.arc", 8},
                {"Component.focusWidth", 2}, {"Component.focusColor", selection},
                {"Component.focusedBorderColor", selection}, {"Button.focusedBorderColor", selection},
                {"Button.focusedBackground", selection}, {"Button.hoverBackground", selection},
                // Without these the look-and-feel keeps its own hover/focus foreground, which is
                // light on the dark themes and disappears on the branded light surfaces.
                {"Button.hoverForeground", foreground}, {"Button.focusedForeground", foreground},
                {"Button.pressedForeground", foreground}, {"Button.selectedForeground", foreground},
                {"Button.pressedBackground", selection},
                {"Component.borderColor", border}, {"Button.margin", new Insets(8, 15, 8, 15)},
                {"MenuItem.margin", new Insets(7, 12, 7, 12)},
                {"CheckBoxMenuItem.margin", new Insets(7, 12, 7, 12)},
                {"ScrollBar.width", 12}, {"ScrollBar.thumbArc", 999},
                {"Table.rowHeight", 30}, {"Table.intercellSpacing", new Dimension(0, 1)},
                {"TableHeader.height", 34}, {"TabbedPane.tabHeight", 38},
                {"TabbedPane.showTabSeparators", false}
        };
        for (Object[] entry : defaults) {
            UIManager.put(entry[0], entry[1]);
        }
    }

    /**
     * Blends <code>over</code> on top of <code>base</code> by <code>ratio</code>, used to derive the
     * subtle hover tints of the branded surfaces.
     */
    public static Color mix(Color base, Color over, float ratio) {
        if(base == null) {
            return over;
        }
        if(over == null) {
            return base;
        }
        float keep = 1f - ratio;
        return new Color(Math.round(base.getRed() * keep + over.getRed() * ratio),
                         Math.round(base.getGreen() * keep + over.getGreen() * ratio),
                         Math.round(base.getBlue() * keep + over.getBlue() * ratio));
    }

    /**
     * Keeps a component's colors under the pointer, so the look-and-feel cannot replace them with
     * its own hover palette. Client properties are not enough on their own: the look-and-feel
     * resolves the hover foreground from its style when the rollover state changes, which puts light
     * text on the branded cream surfaces. The guard is installed once per component and reads the
     * colors from the client properties, so a later color change only needs another call.
     *
     * <p>A <code>null</code> background is left untouched, which is what the transparent controls
     * (links, the tray minimize glyph) need.
     */
    public static void applyBrandHover(JComponent component, Color restForeground, Color restBackground, Color hoverForeground, Color hoverBackground) {
        if(component == null || restForeground == null) {
            return;
        }
        component.putClientProperty(HOVER_REST_FOREGROUND, restForeground);
        component.putClientProperty(HOVER_REST_BACKGROUND, restBackground);
        component.putClientProperty(HOVER_ACTIVE_FOREGROUND, hoverForeground != null? hoverForeground: restForeground);
        component.putClientProperty(HOVER_ACTIVE_BACKGROUND, hoverBackground);
        // The look-and-feel paints the hover state from its own style, so the component foreground
        // alone is not enough; the style below is what actually reaches the screen.
        if(UIManager.getLookAndFeel() instanceof FlatLaf) {
            StringBuilder style = new StringBuilder();
            if(component instanceof JMenuItem) {
                // FlatMenuItemUI has no hover* style keys; its hover state is the "selection" state
                styleProperty(style, "selectionForeground", hoverForeground != null? hoverForeground: restForeground);
                styleProperty(style, "selectionBackground", hoverBackground);
            } else {
                styleProperty(style, "hoverForeground", hoverForeground != null? hoverForeground: restForeground);
                styleProperty(style, "focusedForeground", restForeground);
                styleProperty(style, "hoverBackground", hoverBackground);
            }
            component.putClientProperty(FlatClientProperties.STYLE, style.toString());
        }
        if(component.getClientProperty(HOVER_GUARD) != null) {
            return;
        }
        component.putClientProperty(HOVER_GUARD, Boolean.TRUE);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                swap(mouseEvent.getComponent(), true);
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                swap(mouseEvent.getComponent(), true);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                swap(mouseEvent.getComponent(), false);
            }

            private void swap(Component source, boolean hovered) {
                if(!(source instanceof JComponent target)) {
                    return;
                }
                Color foreground = color(target, hovered? HOVER_ACTIVE_FOREGROUND: HOVER_REST_FOREGROUND);
                Color background = color(target, hovered? HOVER_ACTIVE_BACKGROUND: HOVER_REST_BACKGROUND);
                if(foreground != null && !foreground.equals(target.getForeground())) {
                    target.setForeground(foreground);
                }
                if(background != null && !background.equals(target.getBackground())) {
                    target.setBackground(background);
                }
            }
        });
    }

    private static void styleProperty(StringBuilder style, String key, Color color) {
        if(color == null) {
            return;
        }
        if(style.length() > 0) {
            style.append(';');
        }
        style.append(key).append(':')
             .append(String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
    }

    private static Color color(JComponent component, String key) {
        Object value = component.getClientProperty(key);
        return value instanceof Color color? color: null;
    }

    private static void loadBrandFonts() {
        if (bodyFont != null && headingFont != null) {
            return;
        }

        bodyFont = loadBrandFont("inter.ttf");
        headingFont = loadBrandFont("sora.ttf");
    }

    private static Font loadBrandFont(String fileName) {
        String path = RESOURCE_PATH + fileName;
        try (InputStream stream = ThemeUtilities.class.getResourceAsStream(path)) {
            if (stream == null) {
                log.error("Missing bundled brand font: {}", path);
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (FontFormatException | IOException e) {
            log.error("Unable to load bundled brand font: {}", path, e);
            return null;
        }
    }

    /**
     * Polling thread to check if a meaningful Desktop theme has changed
     */
    public static class ThemeMonitor {
        private static final Logger log = LogManager.getLogger(ThemeMonitor.class);
        private static final String LOG_TEMPLATE = "%s theme changed from '%s' to '%s'";
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        private Runnable refreshAction;

        private boolean isDarkDesktop;
        private boolean isDarkTaskbar;

        public ThemeMonitor() {
            this.isDarkDesktop = SystemUtilities.isDarkDesktop(false);
            this.isDarkTaskbar = SystemUtilities.isDarkTaskbar(false);
        }

        public void onChange(Runnable refreshAction) {
            this.refreshAction = refreshAction;
        }

        public ThemeMonitor startPolling(long intervalMs) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    boolean isDarkDesktop = SystemUtilities.isDarkDesktop(true);
                    boolean isDarkTaskbar = SystemUtilities.isDarkTaskbar(true);

                    if(this.isDarkDesktop != isDarkDesktop || this.isDarkTaskbar != isDarkTaskbar) {
                        String desktopMessage = format("Desktop", this.isDarkDesktop, isDarkDesktop);
                        String taskbarMessage = format("Taskbar", this.isDarkTaskbar, isDarkTaskbar);
                        if(!desktopMessage.isEmpty()) {
                            log.info(desktopMessage);
                        }
                        if(!taskbarMessage.isEmpty()) {
                            log.info(taskbarMessage);
                        }

                        this.isDarkDesktop = isDarkDesktop;
                        this.isDarkTaskbar = isDarkTaskbar;
                        if(refreshAction != null) {
                            this.refreshAction.run();
                        }
                    }
                } catch (Throwable t) {
                    log.warn("An error occurred polling the light/dark theme: {}", t.getMessage());
                }
            }, 0, intervalMs, TimeUnit.MILLISECONDS);
            return this;
        }

        public static String format(String desktopComponent, boolean wasDark, boolean isDark) {
            if(wasDark == isDark) {
                return "";
            }
            return String.format(LOG_TEMPLATE, desktopComponent, wasDark? "dark":"light", isDark? "dark":"light");
        }
    }

    public static void refreshAll(Container container, Component ... orphans) {
        // Handle orphaned UI objects (e.g. Component added to a message dialog)
        for(Component orphan : orphans) {
            recurseOrphanedComponents(orphan);
        }
        refreshAll(ArrayUtils.addAll(container.getComponents(), orphans));
    }

    private static void refreshAll(Component ... components) {
        for(Component c : components) {
            if (c instanceof Themeable) {
                ((Themeable)c).refresh();
            }
            if (c instanceof Container) {
                refreshAll((Container)c);
            }
        }
    }

    /**
     * Inefficient yet effective way to recurse orphaned component's UI changes
     */
    private static Container recurseOrphanedComponents(Component c) {
        if (c != null) {
            SwingUtilities.updateComponentTreeUI(c);
            if (c instanceof JRootPane) {
                return (Container)c;
            }
            return recurseOrphanedComponents(c.getParent());
        }
        return null;
    }
}
