/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.common;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import qz.App;
import qz.auth.Certificate;
import qz.auth.Request;
import qz.build.provision.params.Os;
import qz.installer.shortcut.ShortcutCreator;
import qz.printer.PrintServiceMatcher;
import qz.printer.action.html.WebApp;
import qz.ui.*;
import qz.ui.component.IconCache;
import qz.ui.tray.TrayType;
import qz.utils.*;
import qz.utils.linux.LinuxUtilities;
import qz.ws.PrintSocketServer;
import qz.ws.SingleInstanceChecker;
import qz.ws.WebsocketPorts;
import qz.ws.substitutions.Substitutions;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static qz.ui.component.IconCache.Icon.*;
import static qz.ui.ThemeUtilities.*;
import static qz.utils.ArgValue.*;

import static qz.App.*;

/**
 * Manages the icons and actions associated with the TrayIcon
 *
 * @author Tres Finocchiaro
 */
public class TrayManager {

    private static final Logger log = LogManager.getLogger(TrayManager.class);

    // The cached icons
    private final IconCache iconCache;

    // Custom swing pop-up menu
    private TrayType tray;

    private ConfirmDialog confirmDialog;
    private final GatewayDialog gatewayDialog;
    private AboutDialog aboutDialog;
    private LogDialog logDialog;
    private SiteManagerDialog sitesDialog;
    private ArrayList<Component> componentList;
    private IconCache.Icon shownIcon;

    // Need a class reference to this so we can set it from the request dialog window
    private JCheckBoxMenuItem anonymousItem;

    // The name this UI component will use, i.e "QZ Print 1.9.0"
    private final String name;

    // The shortcut and startup helper
    private final ShortcutCreator shortcutCreator;

    // Action to run when reload is triggered
    private Thread reloadThread;

    // Actions to run if idle after startup
    private java.util.Timer idleTimer = new java.util.Timer();

    /**
     * Create a AutoHideJSystemTray with the specified name/text
     */
    public TrayManager() {
        name = Constants.APP_DISPLAY_NAME;

        // Set strict certificate mode preference
        Certificate.setTrustBuiltIn(!getPref(TRAY_STRICTMODE));

        // Configures JSON websocket messages
        Substitutions.getInstance();

        // Set FileIO security
        FileUtilities.setFileIoEnabled(getPref(SECURITY_FILE_ENABLED));
        FileUtilities.setFileIoStrict(getPref(SECURITY_FILE_STRICT));

        if (isHeadless()) {
            log.info("Running in headless mode");
        }

        // Set up the shortcut name so that the UI components can use it
        shortcutCreator = ShortcutCreator.getInstance();

        SystemUtilities.setSystemLookAndFeel();
        iconCache = new IconCache();

        if (SystemUtilities.isSystemTraySupported()) { // UI mode with tray
            switch(SystemUtilities.getOs()) {
                case WINDOWS:
                    tray = TrayType.JX.init(iconCache);
                    // Undocumented HiDPI behavior
                    tray.setImageAutoSize(true);
                    break;
                case MAC:
                    tray = TrayType.CLASSIC.init(iconCache);
                    break;
                default:
                    tray = TrayType.MODERN.init(iconCache);
            }

            // OS-specific tray icon handling
            if (SystemTray.isSupported()) {
                iconCache.fixTrayIcons(SystemUtilities.isDarkTaskbar());
            }

            // Iterates over all images denoted by IconCache.getTypes() and caches them
            tray.setIcon(DANGER_ICON);
            tray.setToolTip(name);

            try {
                SystemTray.getSystemTray().add(tray.tray());
            }
            catch(AWTException awt) {
                log.error("Could not attach tray, forcing headless mode", awt);
                setHeadless(true);
            }
        } else if (!isHeadless()) { // UI mode without tray
            tray = TrayType.TASKBAR.init(exitListener, iconCache);
            tray.setIcon(DANGER_ICON);
            tray.setToolTip(name);
            tray.showTaskbar();
        }

        // TODO: Remove when fixed upstream.  See issue #393
        if (SystemUtilities.isUnix() && !isHeadless()) {
            // Update printer list in CUPS immediately (normally 2min)
            System.setProperty("sun.java2d.print.polling", "false");
        }

        gatewayDialog = new GatewayDialog(isHeadless(), null,"Ação necessária", iconCache);

        if(isHeadless()) {
            // If isHeadless(), look for a location to forward message dialogs to
            gatewayDialog.setEndpoint(PrefsSearch.getString(TRAY_DIALOG_ENDPOINT, getUserPrefs(), App.getTrayProperties()));
        } else {
            componentList = new ArrayList<>();
            componentList.add(gatewayDialog.getDialog());

            // The ok/cancel dialog
            confirmDialog = new ConfirmDialog(null, "Confirmação", iconCache);
            componentList.add(confirmDialog);

            // Detect theme changes
            new ThemeMonitor().startPolling(1000).onChange(this::refreshTheme);
        }

        if (tray != null) {
            addMenuItems();
        }

        // Initialize idle actions
        // Slow to start JavaFX the first time
        if (getPref(TRAY_IDLE_JAVAFX)) {
            performIfIdle((int)TimeUnit.SECONDS.toMillis(60), evt -> {
                log.debug("IDLE: Starting up JFX for HTML printing");
                try {
                    WebApp.initialize();
                }
                catch(IOException e) {
                    log.error("Idle runner failed to preemptively start JavaFX service");
                }
            });
        }
        // Slow to find printers the first time if a lot of printers are installed
        // Must run after JavaFX per https://github.com/qzind/tray/issues/924
        if (getPref(TRAY_IDLE_PRINTERS)) {
            performIfIdle((int)TimeUnit.SECONDS.toMillis(120), evt -> {
                log.debug("IDLE: Performing first run of find printers");
                PrintServiceMatcher.getNativePrinterList(false, true);
            });
        }
    }

    public void refreshTheme() {
        iconCache.fixTrayIcons(SystemUtilities.isDarkTaskbar());
        refreshIcon(null);
        // TODO: Merge into ThemeUtilities
        SwingUtilities.invokeLater(() -> {
            SystemUtilities.setSystemLookAndFeel();
            for(Component c : componentList) {
                SwingUtilities.updateComponentTreeUI(c);
                if (c instanceof Themeable) {
                    ((Themeable)c).refresh();
                }
                if (c instanceof JDialog) {
                    ((JDialog)c).pack();
                } else if (c instanceof JPopupMenu) {
                    styleTrayPopup((JPopupMenu)c);
                    ((JPopupMenu)c).pack();
                }
            }
        });
    }

    private void styleTrayPopup(JPopupMenu popup) {
        // The tray menu is a branded surface: cream background, never the desktop's dark palette
        popup.setBackground(Constants.BRAND_CREAM_COLOR);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Constants.BRAND_BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        if (popup.getComponentCount() > 0 && popup.getComponent(0) instanceof JPanel) {
            popup.getComponent(0).setBackground(popup.getBackground());
        }
        styleTrayRows(popup);
    }

    public static void styleTrayRows(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton) {
                styleTrayRow((AbstractButton)component);
            }
            if (component instanceof Container) {
                styleTrayRows((Container)component);
            }
        }
    }

    /**
     * Applies the tray row colors: a white row on the cream surface, so each row is distinguishable
     * from the panel behind it, with cocoa text.
     *
     * <p>Public and static because {@link qz.ui.tray.TaskbarTrayIcon} re-applies the same rules when
     * the menu opens and closes; the tray menu is never shown as a {@link JPopupMenu} in MODERN and
     * TASKBAR modes, so the popup listener alone is not enough to restore the rows.
     *
     * <p>Opacity is left untouched on purpose: forcing it opaque would drop the look-and-feel's
     * rounded corners and focus ring.
     */
    public static void styleTrayRow(AbstractButton item) {
        item.setBackground(Constants.BRAND_CARD_COLOR);
        item.setForeground(Constants.BRAND_COCOA_COLOR);
        ThemeUtilities.applyBrandHover(item, Constants.BRAND_COCOA_COLOR, Constants.BRAND_CARD_COLOR,
                                       Constants.BRAND_COCOA_COLOR,
                                       ThemeUtilities.mix(Constants.BRAND_CARD_COLOR, Constants.BRAND_PRIMARY_COLOR, 0.12f));
    }

    /**
     * Stand-alone invocation of TrayManager
     *
     * @param args arguments to pass to main
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(TrayManager::new);
    }

    /**
     * Builds the Swing pop-up menu with the specified items.
     */
    private void addMenuItems() {
        JPopupMenu popup = new JPopupMenu();
        componentList.add(popup);

        JPanel menuContent = new JPanel();
        menuContent.setLayout(new BoxLayout(menuContent, BoxLayout.Y_AXIS));
        componentList.add(menuContent);

        JPanel brandHeader = new JPanel();
        brandHeader.setLayout(new BoxLayout(brandHeader, BoxLayout.Y_AXIS));
        brandHeader.setOpaque(false);
        brandHeader.setBackground(Constants.BRAND_CREAM_COLOR);
        brandHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        brandHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandHeader.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));
        JPanel menuActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        menuActions.setOpaque(false);
        menuActions.setMaximumSize(new Dimension(280, 30));
        JButton minimizeButton = new JButton(iconCache.getIcon(MINIMIZE_ICON));
        minimizeButton.setName("tray-minimize");
        minimizeButton.setToolTipText("Minimizar menu");
        minimizeButton.getAccessibleContext().setAccessibleName("Minimizar menu");
        minimizeButton.setPreferredSize(new Dimension(30, 26));
        minimizeButton.setMinimumSize(new Dimension(30, 26));
        minimizeButton.setMaximumSize(new Dimension(30, 26));
        minimizeButton.setMargin(new Insets(0, 0, 0, 0));
        minimizeButton.setHorizontalAlignment(SwingConstants.CENTER);
        minimizeButton.setFocusPainted(false);
        minimizeButton.setBorderPainted(false);
        minimizeButton.setContentAreaFilled(false);
        minimizeButton.setOpaque(false);
        minimizeButton.setForeground(Constants.BRAND_COCOA_COLOR);
        ThemeUtilities.applyBrandHover(minimizeButton, Constants.BRAND_COCOA_COLOR, null, Constants.BRAND_COCOA_COLOR, null);
        menuActions.add(minimizeButton);
        brandHeader.add(menuActions);
        JLabel brandLogo = new JLabel();
        brandLogo.setHorizontalAlignment(SwingConstants.CENTER);
        java.net.URL brandLogoResource = TrayManager.class.getResource("/qz/ui/resources/aproveita-logo-horizontal.png");
        if (brandLogoResource == null) {
            log.error("Missing bundled brand logo for tray menu");
        } else {
            ImageIcon sourceLogo = new ImageIcon(brandLogoResource);
            Image scaledLogo = sourceLogo.getImage().getScaledInstance(180, 52, Image.SCALE_SMOOTH);
            brandLogo.setIcon(new ImageIcon(scaledLogo));
        }
        brandLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel versionLabel = new JLabel("Versão " + Constants.VERSION);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionLabel.setFont(UIManager.getFont("Label.font").deriveFont(11f));
        versionLabel.setForeground(Constants.BRAND_MUTED_COLOR);
        brandHeader.add(brandLogo);
        brandHeader.add(Box.createVerticalStrut(8));
        brandHeader.add(versionLabel);
        menuContent.add(brandHeader);
        addMenuSeparator(menuContent);

        JMenuItem sitesItem = new JMenuItem("Gerenciar sites...", iconCache.getIcon(SAVED_ICON));
        sitesItem.setMnemonic(KeyEvent.VK_M);
        sitesItem.addActionListener(savedListener);
        sitesDialog = new SiteManagerDialog(sitesItem, iconCache, getUserPrefs());
        componentList.add(sitesDialog);

        JPanel advancedOptions = new JPanel();
        advancedOptions.setLayout(new BoxLayout(advancedOptions, BoxLayout.Y_AXIS));
        advancedOptions.setOpaque(false);
        advancedOptions.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        if (Constants.ENABLE_DIAGNOSTICS) {
            JMenuItem browseApp = new JMenuItem("Abrir pasta do aplicativo...", iconCache.getIcon(FOLDER_ICON));
            browseApp.setToolTipText(SystemUtilities.getJarParentPath().toString());
            browseApp.setMnemonic(KeyEvent.VK_O);
            browseApp.addActionListener(e -> ShellUtilities.browseAppDirectory());
            advancedOptions.add(sectionLabel("Diagnóstico"));
            addMenuRow(advancedOptions, browseApp);

            JMenuItem browseUser = new JMenuItem("Abrir pasta do usuário...", iconCache.getIcon(FOLDER_ICON));
            browseUser.setToolTipText(FileUtilities.USER_DIR.toString());
            browseUser.setMnemonic(KeyEvent.VK_U);
            browseUser.addActionListener(e -> ShellUtilities.browseDirectory(FileUtilities.USER_DIR));
            addMenuRow(advancedOptions, browseUser);

            JMenuItem browseShared = new JMenuItem("Abrir pasta compartilhada...", iconCache.getIcon(FOLDER_ICON));
            browseShared.setToolTipText(FileUtilities.SHARED_DIR.toString());
            browseShared.setMnemonic(KeyEvent.VK_S);
            browseShared.addActionListener(e -> ShellUtilities.browseDirectory(FileUtilities.SHARED_DIR));
            addMenuRow(advancedOptions, browseShared);

            addMenuSeparator(advancedOptions);

            JCheckBoxMenuItem notificationsItem = new JCheckBoxMenuItem("Mostrar todas as notificações");
            notificationsItem.setToolTipText("Exibe avisos de conexão e desconexão.");
            notificationsItem.setMnemonic(KeyEvent.VK_S);
            notificationsItem.setState(getPref(TRAY_NOTIFICATIONS));
            notificationsItem.addActionListener(notificationsListener);
            addMenuRow(advancedOptions, notificationsItem);

            JCheckBoxMenuItem monocleItem = new JCheckBoxMenuItem("Modo compatível para impressão de páginas");
            monocleItem.setToolTipText("Altera o modo de impressão de páginas. Requer reinicialização.");
            monocleItem.setMnemonic(KeyEvent.VK_U);
            monocleItem.setState(getPref(TRAY_MONOCLE));
            if(Constants.JAVA_VERSION.getMajorVersion() <= 8) {
                log.warn("Monocle engine is not available for this Java version");
                monocleItem.setEnabled(false);
                monocleItem.setToolTipText("Este modo de impressão não está disponível.");
            }
            monocleItem.addActionListener(monocleListener);

            if (Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("11.0.0"))) {
                addMenuRow(advancedOptions, monocleItem);
            }

            addMenuSeparator(advancedOptions);

            JMenuItem logItem = new JMenuItem("Registros do aplicativo...", iconCache.getIcon(LOG_ICON));
            logItem.setMnemonic(KeyEvent.VK_L);
            logItem.addActionListener(logListener);
            addMenuRow(advancedOptions, logItem);
            logDialog = new LogDialog(logItem, iconCache, getUserPrefs());
            componentList.add(logDialog);

            JMenuItem zipLogs = new JMenuItem("Salvar registros na área de trabalho");
            zipLogs.setToolTipText("Salva um arquivo com os registros do aplicativo.");
            zipLogs.setMnemonic(KeyEvent.VK_Z);
            zipLogs.addActionListener(e -> FileUtilities.zipLogs());
            addMenuRow(advancedOptions, zipLogs);
        }

        JMenuItem desktopItem = new JMenuItem("Criar atalho na área de trabalho", iconCache.getIcon(DESKTOP_ICON));
        desktopItem.setMnemonic(KeyEvent.VK_D);
        desktopItem.addActionListener(desktopListener());
        addMenuRow(advancedOptions, desktopItem);
        addMenuRow(advancedOptions, sitesItem);

        anonymousItem = new JCheckBoxMenuItem("Bloquear conexões não verificadas");
        anonymousItem.setToolTipText("Bloqueia solicitações sem uma assinatura válida.");
        anonymousItem.setMnemonic(KeyEvent.VK_K);
        anonymousItem.setState(Certificate.UNKNOWN.isBlocked());
        anonymousItem.addActionListener(anonymousListener);
        addMenuRow(advancedOptions, anonymousItem);

        JButton advancedItem = new JButton("Avançado  +", iconCache.getIcon(SETTINGS_ICON));
        advancedItem.setName("tray-advanced");
        advancedItem.setMnemonic(KeyEvent.VK_A);
        advancedItem.addActionListener(e -> {
            boolean expanded = !advancedOptions.isVisible();
            advancedOptions.setVisible(expanded);
            advancedItem.setText(expanded ? "Avançado  -" : "Avançado  +");
            updateTrayPopupSize(popup, menuContent);
        });
        addMenuRow(menuContent, advancedItem);
        advancedOptions.setVisible(false);
        menuContent.add(advancedOptions);

        JMenuItem reloadItem = new JMenuItem("Recarregar", iconCache.getIcon(RELOAD_ICON));
        reloadItem.setMnemonic(KeyEvent.VK_R);
        reloadItem.addActionListener(reloadListener);
        addMenuRow(menuContent, reloadItem);

        JMenuItem aboutItem = new JMenuItem("Sobre o " + Constants.APP_DISPLAY_NAME, iconCache.getIcon(ABOUT_ICON));
        aboutItem.setMnemonic(KeyEvent.VK_B);
        aboutItem.addActionListener(aboutListener);
        addMenuRow(menuContent, aboutItem);
        aboutDialog = new AboutDialog(aboutItem, iconCache);
        componentList.add(aboutDialog);

        if (SystemUtilities.isMac()) {
            MacUtilities.registerAboutDialog(aboutDialog);
            MacUtilities.registerQuitHandler(this);
        }

        addMenuSeparator(menuContent);

        JCheckBoxMenuItem startupItem = new JCheckBoxMenuItem("Iniciar com o sistema");
        startupItem.setMnemonic(KeyEvent.VK_S);
        startupItem.setState(FileUtilities.isAutostart());
        startupItem.addActionListener(startupListener());
        if (!shortcutCreator.canAutoStart()) {
            startupItem.setEnabled(false);
            startupItem.setState(false);
            startupItem.setToolTipText("A inicialização automática foi desativada pelo administrador.");
        }
        addMenuRow(menuContent, startupItem);

        JMenuItem exitItem = new JMenuItem("Sair", iconCache.getIcon(EXIT_ICON));
        exitItem.addActionListener(exitListener);
        addMenuRow(menuContent, exitItem);

        popup.add(menuContent);
        styleTrayPopup(popup);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                resetTrayRows(popup);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
                resetTrayRows(popup);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
                resetTrayRows(popup);
            }
        });

        if (tray != null) {
            tray.setJPopupMenu(popup);
        }
    }

    private void resetTrayRows(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton) {
                AbstractButton item = (AbstractButton)component;
                item.getModel().setRollover(false);
                item.getModel().setArmed(false);
                styleTrayRow(item);
            }
            if (component instanceof Container) {
                resetTrayRows((Container)component);
            }
        }
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11f));
        label.setForeground(Constants.BRAND_PRIMARY_COLOR);
        label.setBorder(BorderFactory.createEmptyBorder(7, 4, 4, 4));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));
        return label;
    }

    private void addMenuSeparator(JPanel panel) {
        JSeparator separator = new JSeparator();
        separator.setPreferredSize(new Dimension(280, 2));
        separator.setMaximumSize(new Dimension(280, 2));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(separator);
    }

    private void addMenuRow(JPanel panel, JMenuItem item) {
        addMenuRow(panel, (AbstractButton)item);
    }

    private void addMenuRow(JPanel panel, AbstractButton item) {
        item.setIconTextGap(12);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setPreferredSize(new Dimension(280, 34));
        item.setMinimumSize(new Dimension(280, 34));
        item.setMaximumSize(new Dimension(280, 34));
        styleTrayRow(item);
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                item.setBackground(Constants.BRAND_PRIMARY_COLOR);
                item.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                styleTrayRow(item);
            }
        });
        panel.add(item);
    }

    private void updateTrayPopupSize(JPopupMenu popup, JPanel content) {
        content.invalidate();
        content.revalidate();
        content.repaint();
        popup.revalidate();
        tray.updateMenuSize(popup);
    }


    private final ActionListener notificationsListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            getUserPrefs().setProperty(TRAY_NOTIFICATIONS, ((JCheckBoxMenuItem)e.getSource()).getState());
        }
    };

    private final ActionListener monocleListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBoxMenuItem j = (JCheckBoxMenuItem)e.getSource();
            getUserPrefs().setProperty(TRAY_MONOCLE, j.getState());
            displayWarningMessage("Reinicie o aplicativo para aplicar esta alteração.");
        }
    };

    private final ActionListener desktopListener() {
        return e -> {
            shortcutCreator.createDesktopShortcut();
        };
    }

    private final ActionListener savedListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            sitesDialog.setVisible(true);
        }
    };

    private final ActionListener anonymousListener = e -> {
        boolean checkBoxState = true;
        if (e.getSource() instanceof JCheckBoxMenuItem) {
            checkBoxState = ((JCheckBoxMenuItem)e.getSource()).getState();
        }

        log.debug("Block unsigned: {}", checkBoxState);

        if (checkBoxState) {
            blackList(Certificate.UNKNOWN);
        } else {
            FileUtilities.deleteFromFile(Constants.BLOCK_FILE, Certificate.UNKNOWN.data(), true);
            FileUtilities.deleteFromFile(Constants.BLOCK_FILE, Certificate.UNKNOWN.data(), false);
        }
    };

    private final ActionListener logListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDialog.setVisible(true);
        }
    };

    private ActionListener startupListener() {
        return e -> {
            JCheckBoxMenuItem source = (JCheckBoxMenuItem)e.getSource();
            if (!source.getState() && !confirmDialog.prompt("Remover " + name + " da inicialização automática?")) {
                source.setState(true);
                return;
            }
            if (FileUtilities.setAutostart(source.getState())) {
                displayInfoMessage(source.getState()
                        ? "Inicialização automática ativada."
                        : "Inicialização automática desativada.");
            } else {
                displayErrorMessage("Não foi possível alterar a inicialização automática.");
            }
            source.setState(FileUtilities.isAutostart());
        };
    }

    /**
     * Sets the default reload action (in this case, <code>Thread.start()</code>) to be fired
     *
     * @param reloadThread The Thread to call when reload is clicked
     */
    public void setReloadThread(Thread reloadThread) {
        this.reloadThread = reloadThread;
    }

    private ActionListener reloadListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (reloadThread == null) {
                showErrorDialog("A opção de recarregar ainda não está disponível.");
            } else {
                reloadThread.start();
            }
        }
    };

    private final ActionListener aboutListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            aboutDialog.setVisible(true);
        }
    };

    private final ActionListener exitListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            boolean showAllNotifications = getPref(TRAY_NOTIFICATIONS);
            if (!showAllNotifications || confirmDialog.prompt("Sair do " + name + "?")) { exit(0); }
        }
    };

    public void exit(int returnCode) {
        getUserPrefs().save();
        FileUtilities.cleanup();
        System.exit(returnCode);
    }

    /**
     * Displays a basic error dialog.
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, name, JOptionPane.ERROR_MESSAGE);
    }

    public boolean showGatewayDialog(final String UID, final Request request, final String prompt, final Point position) {
        // No way to prompt, hope it's whitelisted
        if (isHeadless() && gatewayDialog.getEndpoint() == null) {
            return request.hasSavedCert();
        }

        GatewayDialog.runSafely(isHeadless(), () -> gatewayDialog.prompt(UID, "%s solicita autorização para " + prompt, request, position));

        GatewayDialog.Response response = gatewayDialog.getResponse();
        switch(response) {
            case ALWAYS_ALLOW:
                whiteList(request.getCertificate());
            case TEMPORARY_ALLOW:
                log.info("Allowed {} to {}", request.getCertName(), prompt);
                break;
            case ALWAYS_BLOCK:
                blackList(request.getCertificate());
            case TEMPORARY_BLOCK:
            case UNANSWERED:
                log.info("Denied {} to {}", request.getCertName(), prompt);
        }

        if(response.alwaysBlockAnonymous(request)) {
            // Treat as "block anonymous requests" to prevent pop-up abuse
            if(!isHeadless()) {
                anonymousItem.setState(false);
                anonymousItem.doClick();
            } else {
                // simulate the click
                anonymousListener.actionPerformed(new ActionEvent(gatewayDialog, ActionEvent.ACTION_PERFORMED, "command"));
            }
        }

        return response.isAllowed();
    }

    private void whiteList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.ALLOW_FILE, cert.data())) {
            displayInfoMessage(String.format(Constants.ALLOW_SITES_TEXT, cert.getOrganization()));
        } else {
            displayErrorMessage("Não foi possível salvar a alteração. Verifique as permissões do usuário.");
        }
    }

    private void blackList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.BLOCK_FILE, cert.data())) {
            displayInfoMessage(String.format(Constants.BLOCK_SITES_TEXT, cert.getOrganization()));
        } else {
            displayErrorMessage("Não foi possível salvar a alteração. Verifique as permissões do usuário.");
        }
    }

    public void setServer(Server server, WebsocketPorts websocketPorts) {
        if (server != null && server.getConnectors().length > 0) {
            singleInstanceCheck(websocketPorts);

            displayInfoMessage("Serviço iniciado na(s) porta(s) " + PrintSocketServer.getPorts(server));

            if (!isHeadless()) {
                aboutDialog.initComponents();
                setDefaultIcon();
            }
        } else {
            displayErrorMessage("Não foi possível iniciar o serviço local.");
        }
    }

    /**
     * Thread safe method for setting a fine status message.  Messages are suppressed unless "Show all
     * notifications" is checked.
     */
    public void displayInfoMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.INFO);
    }

    /**
     * Thread safe method for setting the default icon
     */
    public void setDefaultIcon() {
        // Workaround for JDK-8252015
        if(SystemUtilities.isMac() && Constants.MASK_TRAY_SUPPORTED && !MacUtilities.jdkSupportsTemplateIcon()) {
            setIcon(DEFAULT_ICON, () -> MacUtilities.toggleTemplateIcon(tray.tray()));
        } else {
            setIcon(DEFAULT_ICON);
        }
    }

    /** Thread safe method for setting the error status message */
    public void displayErrorMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.ERROR);
    }

    /** Thread safe method for setting the danger icon */
    public void setDangerIcon() {
        setIcon(DANGER_ICON);
    }

    /** Thread safe method for setting the warning status message */
    public void displayWarningMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.WARNING);
    }

    /** Thread safe method for setting the warning icon */
    public void setWarningIcon() {
        setIcon(WARNING_ICON);
    }

    /** Thread safe method for setting the specified icon */
    private void setIcon(final IconCache.Icon i, Runnable whenDone) {
        if (tray != null && i != shownIcon) {
            shownIcon = i;
            refreshIcon(whenDone);
        }
    }

    private void setIcon(final IconCache.Icon i) {
        setIcon(i, null);
    }

    public void refreshIcon(final Runnable whenDone) {
        SwingUtilities.invokeLater(() -> {
            tray.setIcon(shownIcon);
            if(whenDone != null) {
                whenDone.run();
            }
        });
    }

    /**
     * Thread safe method for setting the specified status message
     *
     * @param caption The title of the tray message
     * @param text    The text body of the tray message
     * @param level   The message type: Level.INFO, .WARN, .SEVERE
     */
    private void displayMessage(final String caption, final String text, final TrayIcon.MessageType level) {
        if (!isHeadless()) {
            if (tray != null) {
                SwingUtilities.invokeLater(() -> {
                    boolean showAllNotifications = getPref(TRAY_NOTIFICATIONS);
                    if (showAllNotifications || level != TrayIcon.MessageType.INFO) {
                        tray.displayMessage(caption, text, level);
                    }
                });
            }
        } else {
            log.info("{}: [{}] {}", caption, level, text);
        }
    }

    public void singleInstanceCheck(WebsocketPorts websocketPorts) {
        // Secure
        for(int port : websocketPorts.getUnusedSecurePorts()) {
            new SingleInstanceChecker(this, port, true);
        }
        // Insecure
        for(int port : websocketPorts.getUnusedInsecurePorts()) {
            new SingleInstanceChecker(this, port, false);
        }
    }

    public boolean isMonoclePreferred() {
        return getPref(TRAY_MONOCLE);
    }

    /**
     * Get boolean user pref: Searching "user", "app" and <code>System.getProperty(...)</code>.
     */
    private static boolean getPref(ArgValue argValue) {
        return PrefsSearch.getBoolean(argValue, getUserPrefs(), getTrayProperties()) ;
    }

    private void performIfIdle(int idleQualifier, ActionListener performer) {
        if (idleTimer != null) {
            idleTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    performer.actionPerformed(null);
                }
            }, idleQualifier);
        } else {
            log.warn("Idle actions have already been cleared due to activity, task not scheduled.");
        }
    }

    public void voidIdleActions() {
        if (idleTimer != null) {
            log.trace("Not idle, stopping any actions that haven't ran yet");
            idleTimer.cancel();
            idleTimer = null;
        }
    }

}
