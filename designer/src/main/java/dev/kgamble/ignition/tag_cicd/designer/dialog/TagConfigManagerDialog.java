package dev.kgamble.ignition.tag_cicd.designer.dialog;

import dev.kgamble.ignition.tag_cicd.common.model.ExportMode;
import dev.kgamble.ignition.tag_cicd.designer.model.TagConfigManager;
import dev.kgamble.ignition.tag_cicd.designer.util.DialogUtilities;
import com.inductiveautomation.ignition.client.icons.VectorIcons;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.GsonBuilder;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for managing Tag CICD configurations.
 * This dialog allows users to view, add, edit, delete, and reorder tag
 * export/import
 * configurations, as well as execute operations directly from the UI.
 */
public class TagConfigManagerDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(TagConfigManagerDialog.class.getName());
    private static final String[] TABLE_COLUMNS = {
            "Provider", "Base Tag Path", "Export Path", "Export Mode", "Collision Policy", "Include UDT Defs"
    };

    private final DesignerContext context;
    private final TagConfigManager configManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private JTable configTable;
    private DefaultTableModel tableModel;
    private Map<Integer, Boolean> pathOverlapMap = new HashMap<>();
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton exportButton;
    private JButton importButton;
    private JButton exportConfigButton;
    private JButton importConfigButton;
    private JButton editOrderButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private boolean isEditOrderMode = false;
    private boolean hasShownOrderConfirmation = false;

    public TagConfigManagerDialog(DesignerContext context) {
        super(context.getFrame(), "Tag CICD Configuration Manager", true);
        this.context = context;
        this.configManager = new TagConfigManager();

        initComponents();
        loadConfigurations();

        setSize(900, 500);
        setMinimumSize(new Dimension(800, 400));
        DialogUtilities.centerOnOwner(this, context.getFrame());
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Create the header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Create the table panel
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Create the button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 5, 10));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JLabel titleLabel = new JLabel("Manage Tag Export/Import Configurations");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        leftPanel.add(titleLabel);

        // Add documentation button
        JButton docsButton = new JButton();
        docsButton.setIcon(VectorIcons.getInteractive("help"));
        docsButton.setToolTipText("Open documentation");
        docsButton.setPreferredSize(new Dimension(25, 25));
        docsButton.setFocusPainted(false);
        docsButton.addActionListener(e -> openDocumentation());
        leftPanel.add(docsButton);

        panel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        exportConfigButton = new JButton("Export Config");
        exportConfigButton.setIcon(VectorIcons.getInteractive("export"));
        exportConfigButton.addActionListener(e -> exportConfigFile());

        importConfigButton = new JButton("Import Config");
        importConfigButton.setIcon(VectorIcons.getInteractive("import"));
        importConfigButton.addActionListener(e -> importConfigFile());

        rightPanel.add(exportConfigButton);
        rightPanel.add(importConfigButton);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private void openDocumentation() {
        try {
            Desktop.getDesktop()
                    .browse(new URI("https://keith-gamble.github.io/ignition-tag-cicd-module/"));
        } catch (Exception e) {
            logger.error("Error opening documentation URL", e);
            JOptionPane.showMessageDialog(this,
                    "Error opening documentation: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Create table model
        tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 5 ? Boolean.class : String.class;
            }
        };

        // Create table
        configTable = new JTable(tableModel);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.setRowHeight(24);
        configTable.getTableHeader().setReorderingAllowed(false);

        // Configure drag-and-drop support
        configTable.setDragEnabled(false);
        configTable.setDropMode(DropMode.INSERT_ROWS);
        configTable.setTransferHandler(new TableRowTransferHandler());

        // Configure table selection listener
        configTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        // Configure table column widths
        configTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Provider
        configTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Base Tag Path
        configTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Export Path
        configTable.getColumnModel().getColumn(2).setCellRenderer(new PathOverlapWarningRenderer()); // Add renderer to
                                                                                                     // Export Path
        configTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Export Mode
        configTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Collision Policy
        configTable.getColumnModel().getColumn(5).setPreferredWidth(80); // Include UDT Defs

        // Double-click to edit
        configTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && configTable.getSelectedRow() != -1 && !isEditOrderMode) {
                    editConfiguration();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(configTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout(5, 0));
        statusPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        statusLabel = new JLabel("");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.EAST);

        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        addButton = new JButton("Add");
        addButton.setIcon(VectorIcons.getInteractive("add"));
        addButton.addActionListener(e -> addConfiguration());

        editButton = new JButton("Edit");
        editButton.setIcon(VectorIcons.getInteractive("edit"));
        editButton.setEnabled(false);
        editButton.addActionListener(e -> editConfiguration());

        deleteButton = new JButton("Delete");
        deleteButton.setIcon(VectorIcons.getInteractive("delete"));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteConfiguration());

        exportButton = new JButton("Export Selected");
        exportButton.setIcon(VectorIcons.getInteractive("export"));
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportTags());

        importButton = new JButton("Import Selected");
        importButton.setIcon(VectorIcons.getInteractive("import"));
        importButton.setEnabled(false);
        importButton.addActionListener(e -> importTags());

        editOrderButton = new JButton("Edit Order");
        editOrderButton.setIcon(VectorIcons.getInteractive("sort-ascending"));
        editOrderButton.addActionListener(e -> toggleEditOrderMode());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(exportButton);
        panel.add(importButton);
        panel.add(editOrderButton);
        panel.add(closeButton);

        return panel;
    }

    private void loadConfigurations() {
        try {
            configManager.loadConfigurations();
            updateTableModel();

            int count = configManager.getConfigCount();
            statusLabel.setText(String.format("Loaded %d configurations", count));
        } catch (Exception e) {
            logger.error("Failed to load configurations", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to load configurations: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading configurations");
        }
    }

    private void updateTableModel() {
        tableModel.setRowCount(0);
        pathOverlapMap.clear(); // Clear the map

        JsonArray configArray = configManager.getConfigArray();
        for (int i = 0; i < configArray.size(); i++) {
            JsonObject config = configArray.get(i).getAsJsonObject();
            String provider = config.get("provider").getAsString();
            String baseTagPath = config.get("baseTagPath").getAsString();
            String sourcePath = config.get("sourcePath").getAsString();

            // Check for overlapping paths
            boolean hasOverlap = configManager.hasOverlappingPath(i, sourcePath);
            pathOverlapMap.put(i, hasOverlap);

            // Get export mode display name
            String exportModeCode = config.get("exportMode").getAsString();
            String exportModeDisplay = configManager.getExportModeDisplayName(exportModeCode);

            // Get collision policy display name
            String collisionPolicyCode = config.get("collisionPolicy").getAsString();
            String collisionPolicyDisplay = configManager.getCollisionPolicyDisplayName(collisionPolicyCode);

            // Get include UDT definitions flag (inverse of excludeUdtDefinitions)
            boolean includeUdtDefinitions = !config.has("excludeUdtDefinitions") ||
                    !config.get("excludeUdtDefinitions").getAsBoolean();

            Object[] rowData = {
                    provider,
                    baseTagPath.isEmpty() ? "[Provider Root]" : baseTagPath,
                    sourcePath,
                    exportModeDisplay,
                    collisionPolicyDisplay,
                    includeUdtDefinitions
            };

            tableModel.addRow(rowData);
        }
    }

    private void addConfiguration() {
        TagConfigEditorDialog dialog = new TagConfigEditorDialog(context, null, configManager);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            JsonObject newConfig = dialog.getConfigObject();
            configManager.addConfiguration(newConfig);
            updateTableModel();

            // Select the new row
            int newRow = configTable.getRowCount() - 1;
            configTable.setRowSelectionInterval(newRow, newRow);
            configTable.scrollRectToVisible(configTable.getCellRect(newRow, 0, true));

            statusLabel.setText(String.format("Saved %d configurations", configManager.getConfigCount()));
        }
    }

    private void editConfiguration() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        JsonObject config = configManager.getConfiguration(selectedRow);
        TagConfigEditorDialog dialog = new TagConfigEditorDialog(context, config, configManager);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            JsonObject updatedConfig = dialog.getConfigObject();
            configManager.updateConfiguration(selectedRow, updatedConfig);
            updateTableModel();

            // Maintain selection
            configTable.setRowSelectionInterval(selectedRow, selectedRow);

            statusLabel.setText(String.format("Saved %d configurations", configManager.getConfigCount()));
        }
    }

    private void deleteConfiguration() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this configuration?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            configManager.deleteConfiguration(selectedRow);
            updateTableModel();

            statusLabel.setText("Configuration deleted");
        }
    }

    private void exportTags() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        // Disable buttons during operation
        setOperationInProgress(true);
        statusLabel.setText("Exporting tags...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return configManager.exportTags(selectedRow);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    JsonObject resultObj = configManager.parseResult(result);

                    if (resultObj.get("success").getAsBoolean()) {
                        statusLabel.setText("Tags exported successfully");
                    } else {
                        String error = resultObj.get("error").getAsString();
                        statusLabel.setText("Export failed: " + error);
                        JOptionPane.showMessageDialog(TagConfigManagerDialog.this,
                                "Export error: " + error,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.error("Error processing export result", e);
                    statusLabel.setText("Export failed: " + e.getMessage());
                } finally {
                    setOperationInProgress(false);
                }
            }
        };

        worker.execute();
    }

    private void importTags() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        // Confirm import operation
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to import tags using this configuration?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Disable buttons during operation
        setOperationInProgress(true);
        statusLabel.setText("Importing tags...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return configManager.importTags(selectedRow);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    JsonObject resultObj = configManager.parseResult(result);

                    if (resultObj.get("success").getAsBoolean()) {
                        JsonObject details = resultObj.getAsJsonObject("details");
                        int created = details.has("created_tags") ? details.getAsJsonObject("created_tags").size() : 0;
                        int deleted = details.has("deleted_tags") ? details.getAsJsonObject("deleted_tags").size() : 0;

                        statusLabel.setText(String.format("Tags imported successfully: Created: %d, Deleted: %d",
                                created, deleted));
                    } else {
                        String error = resultObj.get("error").getAsString();
                        statusLabel.setText("Import failed: " + error);
                        JOptionPane.showMessageDialog(TagConfigManagerDialog.this,
                                "Import error: " + error,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.error("Error processing import result", e);
                    statusLabel.setText("Import failed: " + e.getMessage());
                } finally {
                    setOperationInProgress(false);
                }
            }
        };

        worker.execute();
    }

    private void exportConfigFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Configuration File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("export-config.json"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                JsonArray configArray = configManager.getConfigArray();
                String jsonContent = gson.toJson(configArray);

                try (FileWriter writer = new FileWriter(selectedFile)) {
                    writer.write(jsonContent);
                }

                statusLabel.setText("Configuration exported to " + selectedFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "Configuration successfully exported to " + selectedFile.getName(),
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Error exporting configuration file", e);
                statusLabel.setText("Failed to export configuration");
                JOptionPane.showMessageDialog(this,
                        "Error exporting configuration: " + e.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importConfigFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Configuration File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(selectedFile.toPath()));
                JsonArray newConfigArray = gson.fromJson(content, JsonArray.class);

                // Validate the imported configuration
                if (newConfigArray == null || newConfigArray.size() == 0) {
                    throw new IllegalArgumentException("Imported file contains no valid configurations");
                }

                // Replace existing configurations by creating a new array
                while (configManager.getConfigArray().size() > 0) {
                    configManager.deleteConfiguration(0);
                }
                for (int i = 0; i < newConfigArray.size(); i++) {
                    configManager.addConfiguration(newConfigArray.get(i).getAsJsonObject());
                }

                updateTableModel();
                statusLabel.setText("Configuration imported from " + selectedFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "Configuration successfully imported from " + selectedFile.getName(),
                        "Import Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logger.error("Error importing configuration file", e);
                statusLabel.setText("Failed to import configuration");
                JOptionPane.showMessageDialog(this,
                        "Error importing configuration: " + e.getMessage(),
                        "Import Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private class PathOverlapWarningRenderer extends DefaultTableCellRenderer {
    private final Color warningColor = new Color(255, 204, 0);
    private final Icon warningIcon = VectorIcons.getInteractive("warning", warningColor);
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component comp = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        
        // Only add warning icon to the export path column (column 2)
        if (column == 2 && pathOverlapMap.containsKey(row) && pathOverlapMap.get(row)) {
            setIcon(warningIcon);
            setToolTipText("Warning: This path overlaps with another configuration's path");
        } else {
            setIcon(null);
            setToolTipText(null);
        }
        
        return comp;
    }
}

    private void toggleEditOrderMode() {
        if (!isEditOrderMode) {
            if (!hasShownOrderConfirmation) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "The order of configurations determines the sequence of tag imports.\n" +
                                "Drag rows to reorder. Do you want to proceed?",
                        "Edit Order Mode",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                hasShownOrderConfirmation = true;
            }
            isEditOrderMode = true;
            editOrderButton.setText("Save Order");
            configTable.setDragEnabled(true);
            configTable.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            statusLabel.setText("Edit Order Mode: Drag rows to reorder");
        } else {
            isEditOrderMode = false;
            editOrderButton.setText("Edit Order");
            configTable.setDragEnabled(false);
            configTable.setCursor(Cursor.getDefaultCursor());
            try {
                configManager.saveConfigurations();
                statusLabel.setText("Order saved successfully");
            } catch (Exception e) {
                logger.error("Failed to save configuration order", e);
                statusLabel.setText("Failed to save order: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Error saving order: " + e.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = configTable.getSelectedRow() != -1;
        boolean operationInProgress = progressBar.isVisible();

        addButton.setEnabled(!isEditOrderMode && !operationInProgress);
        editButton.setEnabled(!isEditOrderMode && !operationInProgress && hasSelection);
        deleteButton.setEnabled(!isEditOrderMode && !operationInProgress && hasSelection);
        exportButton.setEnabled(!isEditOrderMode && !operationInProgress && hasSelection);
        importButton.setEnabled(!isEditOrderMode && !operationInProgress && hasSelection);
        exportConfigButton.setEnabled(!isEditOrderMode && !operationInProgress);
        importConfigButton.setEnabled(!isEditOrderMode && !operationInProgress);
        editOrderButton.setEnabled(!operationInProgress);
        configTable.setEnabled(!operationInProgress);
    }

    private void setOperationInProgress(boolean inProgress) {
        progressBar.setVisible(inProgress);
        progressBar.setIndeterminate(inProgress);
        updateButtonStates();
    }

    @Override
    public void dispose() {
        configManager.shutdown();
        super.dispose();
    }

    // TransferHandler for drag-and-drop row reordering
    private class TableRowTransferHandler extends TransferHandler {
        private final DataFlavor rowFlavor = new DataFlavor(Integer.class, "RowIndex");

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTable table = (JTable) c;
            int row = table.getSelectedRow();
            if (row != -1) {
                return new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[] { rowFlavor };
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return rowFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) {
                        return row;
                    }
                };
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return isEditOrderMode && support.isDrop() && support.isDataFlavorSupported(rowFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            int dropRow = dl.getRow();

            try {
                int sourceRow = (Integer) support.getTransferable().getTransferData(rowFlavor);
                if (sourceRow == dropRow || sourceRow + 1 == dropRow) {
                    return false; // No change needed
                }

                // Adjust for insertion point
                int adjustedDropRow = dropRow > sourceRow ? dropRow - 1 : dropRow;

                // Move row in table model
                tableModel.moveRow(sourceRow, sourceRow, adjustedDropRow);

                // Update configArray in TagConfigManager
                JsonArray configArray = configManager.getConfigArray();
                JsonElement element = configArray.remove(sourceRow);

                // Create a new JsonArray to preserve order
                JsonArray newArray = new JsonArray();
                for (int i = 0; i < configArray.size(); i++) {
                    if (i == adjustedDropRow) {
                        newArray.add(element);
                    }
                    newArray.add(configArray.get(i));
                }
                if (adjustedDropRow == configArray.size()) {
                    newArray.add(element); // Add at the end if drop is at the last position
                }

                // Replace the old array (since JsonArray doesn't support direct assignment, we
                // need to clear and addAll)
                while (configArray.size() > 0) {
                    configArray.remove(0);
                }
                configArray.addAll(newArray);

                // Update selection
                configTable.setRowSelectionInterval(adjustedDropRow, adjustedDropRow);

                statusLabel.setText("Moved row from position " + (sourceRow + 1) + " to " + (adjustedDropRow + 1));
                return true;
            } catch (Exception e) {
                logger.error("Error during drag-and-drop", e);
                statusLabel.setText("Error reordering: " + e.getMessage());
                return false;
            }
        }
    }
}