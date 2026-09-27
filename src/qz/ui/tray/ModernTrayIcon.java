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

import org.jdesktop.swinghelper.tray.JXTrayIcon;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author A. Tres Finocchiaro
 */
public class ModernTrayIcon extends JXTrayIcon {
    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private JFrame invisibleFrame;
    private JPopupMenu popup;

    public ModernTrayIcon(Image image) {
        super(image);
    }

    @Override
    public void setJPopupMenu(final JPopupMenu popup) {
        this.popup = popup;

        invisibleFrame = new JFrame();
        invisibleFrame.setAlwaysOnTop(true);
        invisibleFrame.setUndecorated(true);
        invisibleFrame.setBackground(Color.BLACK);
        invisibleFrame.pack();
        invisibleFrame.setSize(1, 1);
        invisibleFrame.setFocusableWindowState(true);

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { invisibleFrame.setVisible(false); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { invisibleFrame.setVisible(false); }
        });

        invisibleFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent we) {
                popup.setInvoker(invisibleFrame);
                int popupY = invisibleFrame.getY() > screenSize.getHeight() / 2 ? -popup.getPreferredSize().height : 0;
                popup.show(invisibleFrame, 0, popupY);
                popup.requestFocus();
            }
        });

        addTrayListener();
    }

    @Override
    public void setImage(Image image) {
        super.setImage(image);
        if (invisibleFrame != null) {
            invisibleFrame.setIconImage(image);
        }
    }

    public JPopupMenu getJPopupMenu() {
        return popup;
    }

    /**
     * Functional equivalent of a <code>MouseAdapter</code>, but accommodates an edge-case in Gnome3 where the tray
     * icon cannot listen on mouse events.
     */
    private void addTrayListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent e) {
                Point p = isTrayEvent(e);
                if (p != null) {
                    SwingUtilities.invokeLater(() -> {
                        invisibleFrame.setLocation(p);
                        invisibleFrame.setVisible(true);
                        invisibleFrame.toFront();
                        invisibleFrame.requestFocus();
                    });
                }
            }
        }, MouseEvent.MOUSE_EVENT_MASK);
    }

    /**
     * Determines if TrayIcon event is detected
     * @param e An AWTEvent
     * @return A Point on the screen which the tray event occurred, or null if none is found
     */
    private static Point isTrayEvent(AWTEvent e) {
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)e;

            if (me.getID() == MouseEvent.MOUSE_RELEASED && me.getSource() != null) {
                if (me.getSource().getClass().getName().contains("TrayIcon")) {
                    return me.getLocationOnScreen();
                }
            }
        }
        return null;
    }
}