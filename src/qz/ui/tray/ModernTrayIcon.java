/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */

package qz.ui.tray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swinghelper.tray.JXTrayIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

/**
 * @author A. Tres Finocchiaro
 */
public class ModernTrayIcon extends JXTrayIcon {
    private static final Logger log = LogManager.getLogger(ModernTrayIcon.class);

    private final TrayActivationGate activationGate = new TrayActivationGate();

    private TaskbarTrayIcon menuWindow;
    private JPopupMenu popup;
    private AWTEventListener trayEventListener;

    public ModernTrayIcon(Image image) {
        super(image);
    }

    @Override
    public void setJPopupMenu(final JPopupMenu popup) {
        this.popup = popup;

        if (menuWindow != null) {
            menuWindow.dispose();
        }
        activationGate.reset();
        menuWindow = new TaskbarTrayIcon(getImage(), null, true);
        menuWindow.setType(Window.Type.UTILITY);
        menuWindow.setAlwaysOnTop(true);
        menuWindow.setFocusableWindowState(true);
        menuWindow.setJPopupMenu(popup);

        addTrayListener();
    }

    @Override
    public void setImage(Image image) {
        super.setImage(image);
        if (menuWindow != null) {
            menuWindow.setIconImage(image);
        }
    }

    public JPopupMenu getJPopupMenu() {
        return popup;
    }

    public void updateMenuSize() {
        if (menuWindow != null) {
            menuWindow.updateMenuSize();
        }
    }

    /**
     * Functional equivalent of a <code>MouseAdapter</code>, but accommodates an edge-case in Gnome3 where the tray
     * icon cannot listen on mouse events.
     *
     * Every release the tray host reports for this icon is filtered through
     * {@link TrayActivationGate}, so the menu only opens or closes on a real activation.
     */
    private void addTrayListener() {
        if (trayEventListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(trayEventListener);
        }
        trayEventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent e) {
                if (!(e instanceof MouseEvent) || menuWindow == null) {
                    return;
                }

                MouseEvent mouseEvent = (MouseEvent)e;
                if (mouseEvent.getSource() != ModernTrayIcon.this) {
                    if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED && isTrayIcon(mouseEvent.getSource())) {
                        log.debug("Ignoring tray release from another icon: {}", mouseEvent.getSource().getClass().getName());
                    }
                    return;
                }

                TrayActivationGate.Event event = toTrayEvent(mouseEvent.getID());
                if (event == TrayActivationGate.Event.OTHER) {
                    return;
                }

                Point pointer = event == TrayActivationGate.Event.RELEASE ? mouseEvent.getLocationOnScreen() : null;
                SwingUtilities.invokeLater(() -> applyTrayEvent(event, pointer));
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(trayEventListener, MouseEvent.MOUSE_EVENT_MASK);
    }

    private void applyTrayEvent(TrayActivationGate.Event event, Point pointer) {
        TaskbarTrayIcon window = menuWindow;
        if (window == null) {
            return;
        }

        boolean menuVisible = window.isMenuVisible();
        TrayActivationGate.Decision decision = activationGate.onEvent(
                event, pointer, menuVisible, window.getMenuScreenBounds(), System.currentTimeMillis());
        if (decision != TrayActivationGate.Decision.TOGGLE) {
            log.debug("Ignoring tray {} at {} (menu {}): not a user activation",
                    event, pointer, menuVisible ? "open" : "closed");
            return;
        }

        log.info("Tray icon activated; {} the menu", menuVisible ? "closing" : "opening");
        if (menuVisible) {
            window.hideMenu();
        } else {
            window.showMenu(pointer);
        }
    }

    private static TrayActivationGate.Event toTrayEvent(int eventId) {
        switch(eventId) {
            case MouseEvent.MOUSE_PRESSED:
                return TrayActivationGate.Event.PRESS;
            case MouseEvent.MOUSE_RELEASED:
                return TrayActivationGate.Event.RELEASE;
            default:
                return TrayActivationGate.Event.OTHER;
        }
    }

    private static boolean isTrayIcon(Object source) {
        return source != null && source.getClass().getName().contains("TrayIcon");
    }
}
