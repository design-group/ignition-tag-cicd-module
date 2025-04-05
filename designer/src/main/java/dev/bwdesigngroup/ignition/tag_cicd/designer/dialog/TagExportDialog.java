package dev.bwdesigngroup.ignition.tag_cicd.designer.dialog;

import dev.bwdesigngroup.ignition.tag_cicd.common.TagCICDRPC;
import dev.bwdesigngroup.ignition.tag_cicd.common.constants.TagCICDConstants;
import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import dev.bwdesigngroup.ignition.tag_cicd.designer.util.DialogUtilities;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

/**
 * Dialog for exporting tags.
 */
public class TagExportDialog extends JDialog {
    private final DesignerContext context;
    private final JTextField baseTagPathField;
    private final JTextField filePathField;
    private final JComboBox<String> providerComboBox;
    private final JComboBox<String> exportModeComboBox;
    private final JCheckBox recursiveCheckBox;
    private final JCheckBox deleteExistingCheckBox;
    private final JCheckBox excludeUdtDefinitionsCheckBox;

    private boolean confirmed = false;
    private String provider;
    private String baseTagPath;
    private String filePath;
    private String exportMode;
    private boolean recursive;
    private boolean deleteExisting;
    private boolean excludeUdtDefinitions;

    public TagExportDialog(DesignerContext context) {
        super(context.getFrame(), "Export Tags", true);
        this.context = context;

        // Create components
        JLabel providerLabel = new JLabel("Provider:");
        String[] providers = getTagProviders();
        providerComboBox = new JComboBox<>(providers);
        providerComboBox.setSelectedIndex(0);

        JLabel baseTagPathLabel = new JLabel("Base Tag Path:");
        baseTagPathField = new JTextField(20);

        JLabel exportModeLabel = new JLabel("Export Mode:");
        String[] exportModes = Arrays.stream(ExportMode.values())
                .map(mode -> mode.getDisplayName())
                .toArray(String[]::new);
        exportModeComboBox = new JComboBox<>(exportModes);
        exportModeComboBox.setSelectedItem(ExportMode.INDIVIDUAL_FILES.getDisplayName());

        JLabel filePathLabel = new JLabel("File Path:");
        filePathField = new JTextField(20);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browsePath());

        recursiveCheckBox = new JCheckBox("Recursive", true);
        deleteExistingCheckBox = new JCheckBox("Delete Existing Files", true);
        excludeUdtDefinitionsCheckBox = new JCheckBox("Exclude UDT Definitions", false);

        // Set up export mode help tooltip
        exportModeComboBox.setToolTipText(
                "<html>" +
                        "<b>Single File:</b> Export all tags as a single JSON file<br>" +
                        "<b>Individual Files:</b> Export each tag, folder, and UDT as individual files<br>" +
                        "<b>Structured Files:</b> Export using a folder structure with tags.json and udts.json files" +
                        "</html>");

        // Create the content panel using DialogUtilities
        JPanel contentPanel = DialogUtilities.createContentPanel();

        // Add custom header
        JPanel headerPanel = DialogUtilities.createHeaderPanel("Export Tags");
        contentPanel.add(headerPanel, BorderLayout.NORTH);

        // Create the form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = DialogUtilities.createGridBagConstraints();

        // Provider row
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(providerLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(providerComboBox, gbc);

        // Base Tag Path row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(baseTagPathLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(baseTagPathField, gbc);

        // Export Mode row
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(exportModeLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(exportModeComboBox, gbc);

        // File Path row
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(filePathLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        formPanel.add(filePathField, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(browseButton, gbc);

        // Checkbox row
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkBoxPanel.add(recursiveCheckBox);
        checkBoxPanel.add(deleteExistingCheckBox);
        checkBoxPanel.add(excludeUdtDefinitionsCheckBox);
        formPanel.add(checkBoxPanel, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = DialogUtilities.createButtonPanel(
                this::confirmAction,
                this::cancelAction,
                "Export",
                "Cancel");
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
        pack();
        DialogUtilities.centerOnOwner(this, context.getFrame());
    }

    private void browsePath() {
        String path = DialogUtilities.browseForFile(
                this,
                "Select File or Directory",
                JFileChooser.FILES_AND_DIRECTORIES,
                filePathField.getText().trim());

        if (path != null) {
            filePathField.setText(path);
        }
    }

    private void confirmAction() {
        if (validateInputs()) {
            confirmed = true;
            provider = (String) providerComboBox.getSelectedItem();
            baseTagPath = baseTagPathField.getText().trim();
            filePath = filePathField.getText().trim();
            recursive = recursiveCheckBox.isSelected();
            deleteExisting = deleteExistingCheckBox.isSelected();
            excludeUdtDefinitions = excludeUdtDefinitionsCheckBox.isSelected();

            // Get the export mode code
            String modeName = (String) exportModeComboBox.getSelectedItem();
            for (ExportMode mode : ExportMode.values()) {
                if (mode.getDisplayName().equals(modeName)) {
                    exportMode = mode.getCode();
                    break;
                }
            }

            dispose();
        }
    }

    private void cancelAction() {
        dispose();
    }

    private boolean validateInputs() {
        if (providerComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select a tag provider", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a file path", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * Gets available tag providers from the Ignition tag system.
     * 
     * @return An array of available tag provider names
     */
    private String[] getTagProviders() {
        try {
            // Get tag providers via RPC
            TagCICDRPC rpc = ModuleRPCFactory.create(TagCICDConstants.MODULE_ID, TagCICDRPC.class);
            String providersStr = rpc.getTagProviders();
            String[] providers = new Gson().fromJson(providersStr, String[].class);
            
            return providers != null ? providers : new String[0];
        } catch (Exception e) {
            return new String[0];
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getProvider() {
        return provider;
    }

    public String getBaseTagPath() {
        return baseTagPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getExportMode() {
        return exportMode;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isDeleteExisting() {
        return deleteExisting;
    }

    public boolean isExcludeUdtDefinitions() {
        return excludeUdtDefinitions;
    }
}