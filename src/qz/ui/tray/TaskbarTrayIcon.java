package qz.ui.tray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.common.TrayManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;

public class TaskbarTrayIcon extends JFrame implements WindowListener {

    private static final Logger log = LogManager.getLogger(TaskbarTrayIcon.class);

    private Dimension iconSize;
    private Container menuContent;
    private final boolean hideOnMinimize;

    /** Set only while minimizing on the user's request, so spurious minimizes can be reverted. */
    private boolean iconifyRequested;

    /** Set while putting the window back after a minimize nobody asked for. */
    private boolean restoringFromUnexpectedMinimize;

    /**
     * Screen point the menu is pinned to, normally the tray icon.  The menu must never follow the
     * live pointer: re-anchoring on {@link MouseInfo#getPointerInfo()} makes it jump around as soon
     * as the user clicks a row, because the pointer then sits inside the menu itself.
     */
    private Point menuAnchor;

    public TaskbarTrayIcon(Image trayImage, final ActionListener exitListener) {
        this(trayImage, exitListener, false);
    }

    public TaskbarTrayIcon(Image trayImage, final ActionListener exitListener, boolean hideOnMinimize) {
        super(Constants.APP_DISPLAY_NAME);
        this.hideOnMinimize = hideOnMinimize;
        initializeComponents(trayImage, exitListener);
    }

    private void initializeComponents(Image trayImage, final ActionListener exitListener) {
        // must come first
        setUndecorated(true);
        setTaskBarTitle(getTitle());
        setSize(0, 0);
        getContentPane().setBackground(Constants.BRAND_CREAM_COLOR);
        iconSize = new Dimension(40, 40);

        setIconImage(trayImage);
        setResizable(false);
        if (exitListener != null) {
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    exitListener.actionPerformed(new ActionEvent(e.getComponent(), e.getID(), "Exit"));
                }
            });
        }
        addWindowListener(this);
    }

    // fixes Linux taskbar title per http://hg.netbeans.org/core-main/rev/5832261b8434, JDK-6528430
    public static void setTaskBarTitle(String title) {
        try {
            Class<?> toolkit = Toolkit.getDefaultToolkit().getClass();
            if ("sun.awt.X11.XToolkit".equals(toolkit.getName())) {
                final Field awtAppClassName = toolkit.getDeclaredField("awtAppClassName");
                awtAppClassName.setAccessible(true);
                awtAppClassName.set(null, title);
            }
        }
        catch(Exception ignore) {}
    }

    /**
     * Returns the "tray" icon size (not the dialog size)
     */
    public Dimension getTrayIconSize() {
        return iconSize;
    }

    public void setJPopupMenu(final JPopupMenu popup) {
        if (popup.getComponentCount() == 0 || !(popup.getComponent(0) instanceof Container)) {
            throw new IllegalArgumentException("Tray menu must contain a root panel");
        }

        menuContent = (Container)popup.getComponent(0);
        popup.remove(menuContent);
        menuContent.setBackground(Constants.BRAND_CREAM_COLOR);
        getRootPane().setBorder(popup.getBorder());
        setContentPane(menuContent);
        resetMenuRows(menuContent);
        addMenuActions(menuContent);
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) {
        int messageType;
        switch(level) {
            case WARNING:
                messageType = JOptionPane.WARNING_MESSAGE;
                break;
            case ERROR:
                messageType = JOptionPane.ERROR_MESSAGE;
                break;
            case INFO:
                messageType = JOptionPane.INFORMATION_MESSAGE;
                break;
            case NONE:
            default:
                messageType = JOptionPane.PLAIN_MESSAGE;
        }
        JOptionPane.showMessageDialog(null, text, caption, messageType);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        if (restoringFromUnexpectedMinimize) {
            restoringFromUnexpectedMinimize = false;
            log.debug("Menu window restored after an unexpected minimize");
            return;
        }

        // The user brought the window back from the taskbar; show the menu where it already lives
        showMenu(null);
    }

    /**
     * Pins the menu to a screen point, normally the tray icon.  Pass {@code null} to keep the
     * current anchor.
     */
    public void setMenuAnchor(Point point) {
        if (point != null) {
            menuAnchor = new Point(point);
        }
    }

    /** Whether the menu is on screen and not minimized */
    public boolean isMenuVisible() {
        return isVisible() && (getExtendedState() & Frame.ICONIFIED) == 0;
    }

    /** Menu bounds on screen, or {@code null} when the menu isn't on screen */
    public Rectangle getMenuScreenBounds() {
        if (!isMenuVisible()) {
            return null;
        }
        Point location = getLocationOnScreen();
        return new Rectangle(location.x, location.y, getWidth(), getHeight());
    }

    public void showMenu(Point point) {
        resetMenuRows(menuContent);
        pack();
        setMenuAnchor(point);
        positionAtAnchor();
        iconifyRequested = false;
        setExtendedState(Frame.NORMAL);
        setVisible(true);
        toFront();
        requestFocus();
        log.debug("Menu shown, anchored to {}", menuAnchor);
    }

    public void hideMenu() {
        setVisible(false);
        resetMenuRows(menuContent);
        log.debug("Menu hidden");
    }

    /**
     * Sends the window back to the taskbar.  This is how tray-less environments start and how the
     * user dismisses the menu, so the minimize is marked as requested to survive
     * {@link #windowIconified(WindowEvent)}.
     */
    public void minimizeToTaskbar() {
        log.debug("Menu minimized to the taskbar on the user's request");
        iconifyRequested = true;
        setState(Frame.ICONIFIED);
    }

    /**
     * Re-packs the menu after its content changed, for example when "Avançado" expands.
     *
     * <p>The position is recomputed from the tray icon instead of keeping the previous one, so
     * expanding and collapsing leaves the menu pinned to the icon rather than wherever the last
     * size change happened to clamp it.
     */
    public void updateMenuSize() {
        if (menuContent == null) {
            return;
        }
        pack();
        if (isMenuVisible()) {
            positionAtAnchor();
        }
    }

    private void positionAtAnchor() {
        Point anchor = menuAnchor;
        if (anchor == null) {
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            anchor = pointerInfo == null ? getLocation() : pointerInfo.getLocation();
            menuAnchor = new Point(anchor);
        }

        Rectangle screen = screenBoundsAt(anchor);
        setLocation(TrayMenuPlacement.topLeft(anchor, new Dimension(getWidth(), getHeight()), screen));
    }

    /**
     * Full bounds of the screen holding {@code p}.
     *
     * The tray icon lives in the panel on the screen edge, so the menu is clamped to the whole
     * screen rather than to the work area.  Clamping to the work area pushes the menu far away
     * from the icon whenever the window manager reserves struts the menu is not actually behind,
     * which left the menu floating hundreds of pixels above the tray.
     */
    private Rectangle screenBoundsAt(Point p) {
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            for (GraphicsConfiguration candidate : device.getConfigurations()) {
                if (candidate.getBounds().contains(p)) {
                    return candidate.getBounds();
                }
            }
        }
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        return configuration == null ? new Rectangle(0, 0, 0, 0) : configuration.getBounds();
    }

    private void addMenuActions(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton)component;
                if ("tray-minimize".equals(button.getName())) {
                    button.addActionListener(event -> {
                        if (hideOnMinimize) {
                            hideMenu();
                        } else {
                            minimizeToTaskbar();
                        }
                    });
                }
            }
            if (component instanceof Container) {
                addMenuActions((Container)component);
            }
        }
    }

    /**
     * Clears the hover/armed state left behind by a click and re-applies the tray row colors.
     *
     * <p>The colors come from {@link TrayManager#styleTrayRow} so there is a single source of truth
     * for the row palette; this class only resets the interaction state.
     */
    private void resetMenuRows(Container container) {
        if (container == null) {
            return;
        }
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton)component;
                ButtonModel model = button.getModel();
                model.setRollover(false);
                model.setArmed(false);
                TrayManager.styleTrayRow(button);
            }
            if (component instanceof Container) {
                resetMenuRows((Container)component);
            }
        }
    }

    @Override
    public void windowOpened(WindowEvent windowEvent) {}

    @Override
    public void windowClosing(WindowEvent windowEvent) {}

    @Override
    public void windowClosed(WindowEvent windowEvent) {}

    @Override
    public void windowIconified(WindowEvent windowEvent) {
        if (iconifyRequested) {
            iconifyRequested = false;
            log.debug("Menu window minimized on the user's request");
            return;
        }

        // Nothing in the menu minimizes it, so put it back rather than losing it mid-interaction
        log.warn("Menu window was minimized unexpectedly; restoring it");
        restoringFromUnexpectedMinimize = true;
        setExtendedState(Frame.NORMAL);
        setVisible(true);
    }

    @Override
    public void windowActivated(WindowEvent windowEvent) {}

    @Override
    public void windowDeactivated(WindowEvent windowEvent) {}

}
