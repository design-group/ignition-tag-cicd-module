package dev.bwdesigngroup.ignition.tag_cicd.designer.util;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import dev.bwdesigngroup.ignition.tag_cicd.designer.dialog.TagConfigManagerDialog;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Utility class for creating consistent dialog components.
 */
public class DialogUtilities {

    // Colors
    private static final Color CONFIRM_BUTTON_BG = new Color(0, 174, 239);
    private static final Color CONFIRM_BUTTON_FG = Color.WHITE;
    private static final Color CANCEL_BUTTON_BG = new Color(230, 230, 230);
    private static final Color CANCEL_BUTTON_FG = Color.DARK_GRAY;
    private static final Color LINK_COLOR = new Color(0, 102, 204);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color HEADER_BG = new Color(245, 245, 245);

    /**
     * Creates a custom header panel for dialogs.
     * 
     * @param title The header title
     * @return A JPanel configured as a header
     */
    public static JPanel createHeaderPanel(String title) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        headerPanel.setBackground(HEADER_BG);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    /**
     * Creates a standard button panel with Confirm and Cancel buttons.
     * 
     * @param confirmAction The action to perform on confirm
     * @param cancelAction  The action to perform on cancel
     * @param confirmText   The text for the confirm button
     * @param cancelText    The text for the cancel button
     * @return A JPanel containing the buttons
     */
    public static JPanel createButtonPanel(Runnable confirmAction, Runnable cancelAction,
            String confirmText, String cancelText) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton cancelButton = new JButton(cancelText);
        styleSecondaryButton(cancelButton);
        cancelButton.addActionListener(e -> cancelAction.run());

        JButton confirmButton = new JButton(confirmText);
        stylePrimaryButton(confirmButton);
        confirmButton.addActionListener(e -> confirmAction.run());

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);

        return buttonPanel;
    }

    /**
     * Creates a content panel with proper borders and layout.
     * 
     * @return A JPanel configured for content
     */
    public static JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return contentPanel;
    }

    /**
     * Creates a form panel with a grid bag layout and standard borders.
     * 
     * @return A JPanel configured for form elements
     */
    public static JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new CompoundBorder(
                new EmptyBorder(5, 5, 5, 5),
                new LineBorder(BORDER_COLOR, 1)));
        return formPanel;
    }

    /**
     * Styles a button as a primary action button.
     * 
     * @param button The button to style
     */
    public static void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(CONFIRM_BUTTON_BG);
        button.setForeground(CONFIRM_BUTTON_FG);
        button.setBorderPainted(false);
        button.setOpaque(true);
    }

    /**
     * Styles a button as a secondary action button.
     * 
     * @param button The button to style
     */
    public static void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(CANCEL_BUTTON_BG);
        button.setForeground(CANCEL_BUTTON_FG);
        button.setBorderPainted(false);
        button.setOpaque(true);
    }

    /**
     * Styles a label as a clickable link.
     * 
     * @param label The label to style
     */
    public static void styleLinkLabel(JLabel label) {
        label.setForeground(LINK_COLOR);
        label.setFont(label.getFont().deriveFont(11f));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (label.isEnabled()) {
                    label.setText("<html><u>" + label.getText() + "</u></html>");
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (label.isEnabled()) {
                    label.setText(label.getText().replaceAll("<html><u>|</u></html>", ""));
                }
            }
        });
    }

    /**
     * Creates a GridBagConstraints object with common settings.
     * 
     * @return A configured GridBagConstraints object
     */
    public static GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    /**
     * Creates a file selection dialog and returns the selected file path.
     * 
     * @param parentComponent   The parent component for the dialog
     * @param dialogTitle       The title for the dialog
     * @param fileSelectionMode The file selection mode (JFileChooser.FILES_ONLY,
     *                          etc.)
     * @param currentPath       The current path to set (if any)
     * @return The selected file path or null if canceled
     */
    public static String browseForFile(Component parentComponent, String dialogTitle,
            int fileSelectionMode, String currentPath) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(fileSelectionMode);
        fileChooser.setDialogTitle(dialogTitle);

        // Set current directory if path already has a value
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }

        int result = fileChooser.showSaveDialog(parentComponent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        }

        return null;
    }

    /**
     * Centers a dialog on its owner.
     * 
     * @param dialog The dialog to center
     * @param owner  The owner frame
     */
    public static void centerOnOwner(JDialog dialog, Frame owner) {
        if (owner != null) {
            // Get owner's screen and window bounds
            GraphicsConfiguration gc = owner.getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Rectangle ownerBounds = owner.getBounds();

            // Get our dialog's size
            Dimension dialogSize = dialog.getSize();

            // Calculate center position relative to owner window
            int x = ownerBounds.x + (ownerBounds.width - dialogSize.width) / 2;
            int y = ownerBounds.y + (ownerBounds.height - dialogSize.height) / 2;

            // Ensure dialog stays within screen bounds
            x = Math.max(screenBounds.x, Math.min(x, screenBounds.x + screenBounds.width - dialogSize.width));
            y = Math.max(screenBounds.y, Math.min(y, screenBounds.y + screenBounds.height - dialogSize.height));

            dialog.setLocation(x, y);
        }
    }
}