package com.bwdesigngroup.ignition.tag_cicd.designer;

import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDConstants;
import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDRPC;

import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.designer.gui.import_export.ProjectTreeRenderer;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.inductiveautomation.ignition.common.BundleUtil.i18n;

public class TagConfigSelectionDialog extends JDialog {
    private final DesignerContext context;
    private final String operationType;
    private final JsonArray configs;
    private final DefaultTreeModel treeModel;
    private final JTree configTree;
    private final Map<DefaultMutableTreeNode, JsonObject> nodeToConfigMap = new HashMap<>();
    private final Map<DefaultMutableTreeNode, String> nodeToStatusMap = new HashMap<>();
    private final List<JsonObject> selectedConfigs = new ArrayList<>();
    private boolean confirmed = false;
    private JLabel selectAllLabel;
    private JLabel selectNoneLabel;
    private JLabel statusLabel;
    private JButton confirmButton;
    private JButton cancelButton;
    private final Gson gson = new Gson();
    private static final int RPC_TIMEOUT_SECONDS = 30;

    public TagConfigSelectionDialog(DesignerContext context, String operationType, JsonArray configs) {
        super(context.getFrame(), true); // Modal dialog
        this.context = context;
        this.operationType = operationType;
        this.configs = configs;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        buildTreeModel(root, configs);
        treeModel = new DefaultTreeModel(root);
        configTree = new JTree(treeModel);
        configTree.setCellRenderer(new TagConfigTreeRenderer(context));
        configTree.setShowsRootHandles(true);
        configTree.setRootVisible(false);
        configTree.setToggleClickCount(0); // Disable expand/collapse on double-click

        // Expand all provider nodes by default
        for (int i = 0; i < root.getChildCount(); i++) {
            TreePath path = new TreePath(new Object[] { root, root.getChildAt(i) });
            configTree.expandPath(path);
        }

        // Enable checkbox functionality
        configTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!configTree.isEnabled())
                    return; // Ignore clicks if tree is disabled
                int selRow = configTree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    TreePath selPath = configTree.getPathForLocation(e.getX(), e.getY());
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    if (node.getUserObject() instanceof TagConfigNode) {
                        TagConfigNode configNode = (TagConfigNode) node.getUserObject();
                        configNode.setSelected(!configNode.isSelected());
                        // Update children if this is a provider node
                        if (node.getChildCount() > 0) {
                            for (int i = 0; i < node.getChildCount(); i++) {
                                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                                TagConfigNode childConfig = (TagConfigNode) child.getUserObject();
                                childConfig.setSelected(configNode.isSelected());
                            }
                        }
                        // Update parent if this is a baseTagPath node
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        if (parent != null && parent.getUserObject() instanceof TagConfigNode) {
                            TagConfigNode parentConfig = (TagConfigNode) parent.getUserObject();
                            boolean allChildrenSelected = true;
                            for (int i = 0; i < parent.getChildCount(); i++) {
                                DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
                                TagConfigNode childConfig = (TagConfigNode) child.getUserObject();
                                if (!childConfig.isSelected()) {
                                    allChildrenSelected = false;
                                    break;
                                }
                            }
                            parentConfig.setSelected(allChildrenSelected);
                        }
                        configTree.repaint();
                    }
                }
            }
        });

        // Add tooltip support for source path
        configTree.setToolTipText("");
        configTree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                TreePath path = configTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (nodeToConfigMap.containsKey(node)) {
                        JsonObject config = nodeToConfigMap.get(node);
                        configTree.setToolTipText("Source Path: " + config.get("sourcePath").getAsString());
                    } else {
                        configTree.setToolTipText(null);
                    }
                } else {
                    configTree.setToolTipText(null);
                }
            }
        });

        initComponents();
        centerOnOwner(context.getFrame());
    }

    private void buildTreeModel(DefaultMutableTreeNode root, JsonArray configs) {
        Map<String, DefaultMutableTreeNode> providerNodes = new HashMap<>();

        for (int i = 0; i < configs.size(); i++) {
            JsonObject config = configs.get(i).getAsJsonObject();
            String provider = config.get("provider").getAsString();
            String baseTagPath = config.get("baseTagPath").getAsString();

            // Find or create the provider node
            DefaultMutableTreeNode providerNode = providerNodes.computeIfAbsent(provider, k -> {
                TagConfigNode providerConfigNode = new TagConfigNode(provider, true);
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(providerConfigNode);
                root.add(newNode);
                return newNode;
            });

            // Create the baseTagPath node
            String displayText = baseTagPath.isEmpty() ? "[Provider Root]" : baseTagPath;
            TagConfigNode configNode = new TagConfigNode(displayText, false);
            DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(configNode);
            providerNode.add(tagNode);
            nodeToConfigMap.put(tagNode, config);
        }

        // Set initial selection state
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) root.getChildAt(i);
            TagConfigNode providerConfigNode = (TagConfigNode) providerNode.getUserObject();
            providerConfigNode.setSelected(true);
            for (int j = 0; j < providerNode.getChildCount(); j++) {
                DefaultMutableTreeNode tagNode = (DefaultMutableTreeNode) providerNode.getChildAt(j);
                TagConfigNode tagConfigNode = (TagConfigNode) tagNode.getUserObject();
                tagConfigNode.setSelected(true);
            }
        }
    }

    private void initComponents() {
        setSize(350, 470); // Adjusted size to match the example dialog
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());

        // Header
        JLabel headerLabel = new JLabel("Select Configurations to " + operationType);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(headerLabel, BorderLayout.NORTH);

        // Tree panel with border and margin
        JScrollPane scrollPane = new JScrollPane(configTree);
        scrollPane.setBorder(new CompoundBorder(
                new EmptyBorder(5, 5, 5, 5), // Margin outside the border
                new LineBorder(new Color(200, 200, 200), 1) // Subtle border
        ));
        scrollPane.setPreferredSize(new Dimension(350, 150)); // Reduced height to match example
        add(scrollPane, BorderLayout.CENTER);

        // South panel to hold selection labels, status, and action buttons
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Selection labels panel
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        selectionPanel.setBorder(new EmptyBorder(0, 0, 5, 0)); // Tighter spacing below

        selectAllLabel = new JLabel("Select All");
        styleLinkLabel(selectAllLabel);
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectAllLabel.isEnabled()) {
                    setAllSelected(true);
                    configTree.repaint();
                }
            }
        });

        selectNoneLabel = new JLabel("None");
        styleLinkLabel(selectNoneLabel);
        selectNoneLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectNoneLabel.isEnabled()) {
                    setAllSelected(false);
                    configTree.repaint();
                }
            }
        });

        selectionPanel.add(selectAllLabel);
        selectionPanel.add(selectNoneLabel);
        southPanel.add(selectionPanel, BorderLayout.NORTH);

        // Status label
        statusLabel = new JLabel("");
        statusLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        southPanel.add(statusLabel, BorderLayout.CENTER);

        // Action buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); // Increased spacing between buttons
        confirmButton = new JButton(i18n("tagcicd.Dialog.ConfirmButton"));
        confirmButton.addActionListener(e -> {
            confirmed = true;
            selectedConfigs.clear();
            for (int i = 0; i < treeModel.getChildCount(treeModel.getRoot()); i++) {
                DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) treeModel.getChild(treeModel.getRoot(),
                        i);
                for (int j = 0; j < providerNode.getChildCount(); j++) {
                    DefaultMutableTreeNode tagNode = (DefaultMutableTreeNode) providerNode.getChildAt(j);
                    TagConfigNode configNode = (TagConfigNode) tagNode.getUserObject();
                    if (configNode.isSelected()) {
                        selectedConfigs.add(nodeToConfigMap.get(tagNode));
                    }
                }
            }

            if (selectedConfigs.isEmpty()) {
                JOptionPane.showMessageDialog(context.getFrame(),
                        "No configurations selected for " + operationType.toLowerCase() + ".",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            confirmButton.setEnabled(false);
            cancelButton.setEnabled(false); // Disable Close button during processing
            cancelButton.setText("Close");
            configTree.setEnabled(false);
            selectAllLabel.setEnabled(false);
            selectNoneLabel.setEnabled(false);
            statusLabel.setText("Processing...");

            // Start the appropriate process based on operationType
            if (operationType.equals("Export")) {
                startExport();
            } else if (operationType.equals("Import")) {
                startImport();
            }
        });

        cancelButton = new JButton(i18n("tagcicd.Dialog.CancelButton"));
        cancelButton.addActionListener(e -> setVisible(false));

        confirmButton.setFocusPainted(false);
        cancelButton.setFocusPainted(false);
        confirmButton.setBackground(new Color(20, 176, 237));
        confirmButton.setForeground(Color.WHITE);
        cancelButton.setBackground(new Color(249, 251, 252));
        cancelButton.setForeground(new Color(111, 117, 123));

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);
    }

    private void startExport() {
        SwingWorker<Void, Void> exportWorker = new SwingWorker<Void, Void>() {
            private JsonObject exportResults = new JsonObject();

            @Override
            protected Void doInBackground() {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    TagCICDRPC rpc = ModuleRPCFactory.create(TagCICDConstants.MODULE_ID, TagCICDRPC.class);
                    for (JsonObject config : selectedConfigs) {
                        String filePath = config.get("sourcePath").getAsString();
                        String provider = config.get("provider").getAsString();
                        String baseTagPath = config.get("baseTagPath").getAsString();
                        boolean individualFilesPerObject = config.get("individualFilesPerObject").getAsBoolean();
                        boolean excludeUdtDefinitions = config.has("excludeUdtDefinitions")
                                ? config.get("excludeUdtDefinitions").getAsBoolean()
                                : false;

                        // Explicitly use Callable<String> to ensure the Future is typed correctly
                        Callable<String> exportTask = () -> rpc.exportTags(provider, baseTagPath, filePath, true, false,
                                individualFilesPerObject, true, excludeUdtDefinitions);
                        Future<String> future = executor.submit(exportTask);

                        String result;
                        try {
                            result = future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (TimeoutException te) {
                            exportResults.addProperty(filePath,
                                    "Failed: Operation timed out after " + RPC_TIMEOUT_SECONDS + " seconds");
                            continue;
                        }
                        JsonObject exportResult = gson.fromJson(result, JsonObject.class);
                        if (exportResult.get("success").getAsBoolean()) {
                            exportResults.addProperty(filePath, "Exported successfully");
                        } else {
                            exportResults.addProperty(filePath, "Failed: " + exportResult.get("error").getAsString());
                        }
                    }
                } catch (Exception ex) {
                    for (JsonObject config : selectedConfigs) {
                        String filePath = config.get("sourcePath").getAsString();
                        exportResults.addProperty(filePath, "Failed: " + ex.getMessage());
                    }
                } finally {
                    executor.shutdown();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Ensure any exceptions in doInBackground are thrown here
                    updateStatus(exportResults);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(context.getFrame(),
                            "Error during export: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setVisible(false); // Close the dialog after export completes
                }
            }
        };
        exportWorker.execute();
    }

    private void startImport() {
        SwingWorker<Void, Void> importWorker = new SwingWorker<Void, Void>() {
            private JsonObject importResults = new JsonObject();

            @Override
            protected Void doInBackground() {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    TagCICDRPC rpc = ModuleRPCFactory.create(TagCICDConstants.MODULE_ID, TagCICDRPC.class);
                    for (JsonObject config : selectedConfigs) {
                        String filePath = config.get("sourcePath").getAsString();
                        String provider = config.get("provider").getAsString();
                        String baseTagPath = config.get("baseTagPath").getAsString();
                        String collisionPolicy = config.get("collisionPolicy").getAsString();
                        boolean individualFilesPerObject = config.get("individualFilesPerObject").getAsBoolean();

                        // Explicitly use Callable<String> to ensure the Future is typed correctly
                        Callable<String> importTask = () -> rpc.importTags(provider, baseTagPath, filePath,
                                collisionPolicy, individualFilesPerObject);
                        Future<String> future = executor.submit(importTask);

                        String result;
                        try {
                            result = future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (TimeoutException te) {
                            importResults.addProperty(filePath,
                                    "Failed: Operation timed out after " + RPC_TIMEOUT_SECONDS + " seconds");
                            continue;
                        }
                        JsonObject importResult = gson.fromJson(result, JsonObject.class);
                        if (importResult.has("error")) {
                            importResults.addProperty(filePath, "Failed: " + importResult.get("error").getAsString());
                        } else {
                            importResults.add(filePath, importResult);
                        }
                    }
                } catch (Exception ex) {
                    for (JsonObject config : selectedConfigs) {
                        String filePath = config.get("sourcePath").getAsString();
                        importResults.addProperty(filePath, "Failed: " + ex.getMessage());
                    }
                } finally {
                    executor.shutdown();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Ensure any exceptions in doInBackground are thrown here
                    updateStatus(importResults);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(context.getFrame(),
                            "Error during import: " + ex.getMessage(),
                            "Import Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setVisible(false); // Close the dialog after import completes
                }
            }
        };
        importWorker.execute();
    }

    private void styleLinkLabel(JLabel label) {
        label.setForeground(new Color(0, 102, 204)); // Blue color for links
        label.setFont(label.getFont().deriveFont(11f)); // Smaller font size to match example
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

    private void setAllSelected(boolean selected) {
        for (int i = 0; i < treeModel.getChildCount(treeModel.getRoot()); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) treeModel.getChild(treeModel.getRoot(), i);
            TagConfigNode providerConfigNode = (TagConfigNode) providerNode.getUserObject();
            providerConfigNode.setSelected(selected);
            for (int j = 0; j < providerNode.getChildCount(); j++) {
                DefaultMutableTreeNode tagNode = (DefaultMutableTreeNode) providerNode.getChildAt(j);
                TagConfigNode tagConfigNode = (TagConfigNode) tagNode.getUserObject();
                tagConfigNode.setSelected(selected);
            }
        }
    }

    private void centerOnOwner(Frame owner) {
        if (owner != null) {
            GraphicsConfiguration gc = owner.getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Rectangle ownerBounds = owner.getBounds();
            Dimension dialogSize = getSize();
            int x = ownerBounds.x + (ownerBounds.width - dialogSize.width) / 2;
            int y = ownerBounds.y + (ownerBounds.height - dialogSize.height) / 2;
            x = Math.max(screenBounds.x, Math.min(x, screenBounds.x + screenBounds.width - dialogSize.width));
            y = Math.max(screenBounds.y, Math.min(y, screenBounds.y + screenBounds.height - dialogSize.height));
            setLocation(x, y);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<JsonObject> getSelectedConfigs() {
        return selectedConfigs;
    }

    public void updateStatus(JsonObject results) {
        statusLabel.setText(""); // Clear the "Processing..." message
        for (int i = 0; i < treeModel.getChildCount(treeModel.getRoot()); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) treeModel.getChild(treeModel.getRoot(), i);
            for (int j = 0; j < providerNode.getChildCount(); j++) {
                DefaultMutableTreeNode tagNode = (DefaultMutableTreeNode) providerNode.getChildAt(j);
                JsonObject config = nodeToConfigMap.get(tagNode);
                String sourcePath = config.get("sourcePath").getAsString();
                if (results.has(sourcePath)) {
                    JsonElement result = results.get(sourcePath);
                    String status;
                    Color statusColor;
                    if (operationType.equals("Export")) {
                        status = result.getAsString();
                        statusColor = status.contains("success") ? Color.GREEN : Color.RED;
                    } else {
                        JsonObject importResult = result.getAsJsonObject();
                        if (importResult.has("error")) {
                            status = "Failed: " + importResult.get("error").getAsString();
                            statusColor = Color.RED;
                        } else {
                            int createdCount = importResult.has("created_tags")
                                    ? importResult.get("created_tags").getAsJsonObject().size()
                                    : 0;
                            int deletedCount = importResult.has("deleted_tags")
                                    ? importResult.get("deleted_tags").getAsJsonObject().size()
                                    : 0;
                            status = "Created: " + createdCount + ", Deleted: " + deletedCount;
                            statusColor = (createdCount > 0 || deletedCount > 0) ? Color.GREEN : Color.RED;
                        }
                    }
                    nodeToStatusMap.put(tagNode, status);
                    TagConfigNode configNode = (TagConfigNode) tagNode.getUserObject();
                    configNode.setStatus(status);
                    configNode.setStatusColor(statusColor);
                }
            }
        }
        configTree.repaint();
    }

    // Inner class to hold node data
    private static class TagConfigNode {
        private final String displayText;
        private final boolean isProvider;
        private boolean selected;
        private String status = "";
        private Color statusColor = Color.BLACK;

        public TagConfigNode(String displayText, boolean isProvider) {
            this.displayText = displayText;
            this.isProvider = isProvider;
            this.selected = false;
        }

        public boolean isProvider() {
            return isProvider;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Color getStatusColor() {
            return statusColor;
        }

        public void setStatusColor(Color statusColor) {
            this.statusColor = statusColor;
        }

        @Override
        public String toString() {
            return displayText + (status.isEmpty() ? "" : " - " + status);
        }
    }

    // Custom renderer for the tree
    private class TagConfigTreeRenderer extends DefaultTreeCellRenderer {
        private final ProjectTreeRenderer projectRenderer;

        public TagConfigTreeRenderer(DesignerContext context) {
            projectRenderer = new ProjectTreeRenderer(context);
            setOpaque(true);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (!(userObject instanceof TagConfigNode)) {
                return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            }

            TagConfigNode configNode = (TagConfigNode) userObject;

            // Create a panel to hold the checkbox and label
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panel.setOpaque(false);

            // Add the checkbox
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(configNode.isSelected());
            checkBox.setOpaque(false);
            checkBox.setEnabled(tree.isEnabled());
            panel.add(checkBox);

            // Use ProjectTreeRenderer to get the icon and text
            JLabel label = (JLabel) projectRenderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                    hasFocus);
            label.setText(configNode.toString());
            label.setForeground(configNode.getStatusColor());
            panel.add(label);

            return panel;
        }
    }
}