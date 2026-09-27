package qz.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.ui.component.EmLabel;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;
import qz.ws.substitutions.Substitutions;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Tres on 2/26/2015.
 * Displays a basic about dialog
 */
public class AboutDialog extends BasicDialog implements Themeable {

    private static final Logger log = LogManager.getLogger(AboutDialog.class);
    private JLabel logo;

    private JPanel contentPanel;
    private JToolBar headerBar;
    private LinkLabel substitutionsLabel;
    private Border dropBorder;

    public AboutDialog(JMenuItem menuItem, IconCache iconCache) {
        super(menuItem, iconCache);
        setTitle("Sobre o " + Constants.APP_DISPLAY_NAME);
    }

    public void initComponents() {
        logo = new JLabel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(new EmptyBorder(4, 0, 10, 0));
        ImageIcon windowIcon = loadBrandImage("aproveita-logo-icon.png", 32, 32);
        if (windowIcon != null) {
            setIconImage(windowIcon.getImage());
        }
        refreshLogo();

        JPanel aboutPanel = new JPanel();
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.PAGE_AXIS));
        aboutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        aboutPanel.add(logo);

        JLabel lblAbout = new EmLabel("Impressão de pedidos", 1.4f, false);
        lblAbout.setAlignmentX(Component.CENTER_ALIGNMENT);
        aboutPanel.add(lblAbout);

        JLabel description = new JLabel("Apoio local às operações do " + Constants.APP_DISPLAY_NAME + ".");
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        aboutPanel.add(Box.createVerticalStrut(6));
        aboutPanel.add(description);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        contentPanel.add(aboutPanel);

        setContent(contentPanel, true);
        contentPanel.setDropTarget(createDropTarget());
        setHeader(headerBar = getHeaderBar());
        refreshHeader();
    }

    private ImageIcon loadBrandImage(String fileName, int maxWidth, int maxHeight) {
        String resourcePath = "/qz/ui/resources/" + fileName;
        java.net.URL resource = getClass().getResource(resourcePath);
        if (resource == null) {
            log.error("Missing bundled frontend brand image: {}", resourcePath);
            return null;
        }

        ImageIcon source = new ImageIcon(resource);
        if (source.getIconWidth() <= 0 || source.getIconHeight() <= 0) {
            log.error("Unable to decode frontend brand image: {}", resourcePath);
            return null;
        }

        double scale = Math.min((double) maxWidth / source.getIconWidth(), (double) maxHeight / source.getIconHeight());
        int width = (int) Math.round(source.getIconWidth() * scale);
        int height = (int) Math.round(source.getIconHeight() * scale);
        return new ImageIcon(source.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private JToolBar getHeaderBar() {
        JToolBar headerBar = new JToolBar();
        headerBar.setBorderPainted(false);
        headerBar.setLayout(new FlowLayout());
        headerBar.setOpaque(true);
        headerBar.setFloatable(false);

        substitutionsLabel = new LinkLabel("Configurações personalizadas estão ativas neste computador");
        JButton refreshButton = new JButton("", getIcon(IconCache.Icon.RELOAD_ICON));
        refreshButton.setOpaque(false);
        refreshButton.addActionListener(e -> {
            Substitutions.getInstance(true);
            refreshHeader();
        });

        substitutionsLabel.setLinkLocation(FileUtilities.SHARED_DIR.toFile());

        headerBar.add(substitutionsLabel);
        headerBar.add(refreshButton);
        return headerBar;
    }

    private DropTarget createDropTarget() {
        return new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                processDroppedFile(evt);
            }

            @Override
            public synchronized void dragEnter(DropTargetDragEvent dtde) {
                super.dragEnter(dtde);
                setDropBorder(true);
            }

            @Override
            public synchronized void dragExit(DropTargetEvent dte) {
                super.dragExit(dte);
                setDropBorder(false);
            }
        };
    }

    private void processDroppedFile(DropTargetDropEvent evt) {
        try {
            evt.acceptDrop(DnDConstants.ACTION_COPY);
            Object dropped = evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if(dropped instanceof List) {
                List<File> droppedFiles = (List<File>)dropped;
                for (File file : droppedFiles) {
                    if(file.getName().equals(Substitutions.FILE_NAME)) {
                        blinkDropBorder(true);
                        log.info("File drop accepted: {}", file);
                        Path source = file.toPath();
                        Path dest = FileUtilities.SHARED_DIR.resolve(file.getName());
                        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                        FileUtilities.inheritParentPermissions(dest);
                        Substitutions.getInstance(true);
                        refreshHeader();
                        break;
                    } else {
                        blinkDropBorder(false);
                        break;
                    }
                }
            }
            evt.dropComplete(true);
        } catch (Exception ex) {
            log.warn(ex);
        }
        setDropBorder(false);
    }

    private void setDropBorder(boolean isShown) {
        if(isShown) {
            if(contentPanel.getBorder() == null) {
                dropBorder = BorderFactory.createDashedBorder(Constants.BRAND_PRIMARY_COLOR, 3, 5, 5, true);
                contentPanel.setBorder(dropBorder);
            }
        } else {
            contentPanel.setBorder(null);
        }
    }

    private void blinkDropBorder(boolean success) {
        Color borderColor = success ? Constants.BRAND_SUCCESS_COLOR : Constants.BRAND_DANGER_COLOR;
        dropBorder = BorderFactory.createDashedBorder(borderColor, 3, 5, 5, true);
        AtomicBoolean toggled = new AtomicBoolean(true);
        int blinkCount = 3;
        int blinkDelay = 100; // ms
        for(int i = 0; i < blinkCount * 2; i++) {
            Timer timer = new Timer("blink" + i);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        contentPanel.setBorder(toggled.getAndSet(!toggled.get())? dropBorder:null);
                    });
                }
            }, i * blinkDelay);
        }
    }

    private void refreshHeader() {
        if(headerBar != null) {
            headerBar.setBackground(Constants.BRAND_PRIMARY_COLOR);
            substitutionsLabel.setForeground(Color.WHITE);
            headerBar.setVisible(Substitutions.areActive());
            pack();
        }
    }

    private void refreshLogo() {
        if(logo != null) {
            ImageIcon brandLogo = loadBrandImage(SystemUtilities.isDarkDesktop()
                    ? "aproveita-logo-light.png"
                    : "aproveita-logo-horizontal.png", 210, 80);
            logo.setIcon(brandLogo == null ? getIcon(IconCache.Icon.LOGO_ICON, SystemUtilities.isDarkDesktop()) : brandLogo);
        }
    }

    @Override
    public void refresh() {
        refreshHeader();
        refreshLogo();
        super.refresh();
    }
}
