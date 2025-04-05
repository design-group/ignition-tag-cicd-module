package dev.bwdesigngroup.ignition.tag_cicd.designer.dialog;

import dev.bwdesigngroup.ignition.tag_cicd.common.TagCICDRPC;
import dev.bwdesigngroup.ignition.tag_cicd.common.constants.TagCICDConstants;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.inductiveautomation.ignition.common.BundleUtil.i18n;

public class TagConfigOperationDialog extends JDialog {
    private final DesignerContext context;
    private final String operationType; // "Export" or "Import"
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

    public TagConfigOperationDialog(DesignerContext context, String operationType) {
        super(context.getFrame(), true);
        this.context = context;
        this.operationType = operationType;
        this.configs = fetchTagConfigs();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        buildTreeModel(root, configs);
        treeModel = new DefaultTreeModel(root);
        configTree = new JTree(treeModel);
        configTree.setCellRenderer(new TagConfigTreeRenderer(context));
        configTree.setShowsRootHandles(true);
        configTree.setRootVisible(false);
        configTree.setToggleClickCount(0);

        for (int i = 0; i < root.getChildCount(); i++) {
            TreePath path = new TreePath(new Object[] { root, root.getChildAt(i) });
            configTree.expandPath(path);
        }

        configTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!configTree.isEnabled())
                    return;
                int selRow = configTree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    TreePath selPath = configTree.getPathForLocation(e.getX(), e.getY());
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    if (node.getUserObject() instanceof TagConfigNode) {
                        TagConfigNode configNode = (TagConfigNode) node.getUserObject();
                        configNode.setSelected(!configNode.isSelected());
                        if (node.getChildCount() > 0) {
                            for (int i = 0; i < node.getChildCount(); i++) {
                                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                                ((TagConfigNode) child.getUserObject()).setSelected(configNode.isSelected());
                            }
                        }
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        if (parent != null && parent.getUserObject() instanceof TagConfigNode) {
                            TagConfigNode parentConfig = (TagConfigNode) parent.getUserObject();
                            boolean allChildrenSelected = true;
                            for (int i = 0; i < parent.getChildCount(); i++) {
                                if (!((TagConfigNode) ((DefaultMutableTreeNode) parent.getChildAt(i)).getUserObject())
                                        .isSelected()) {
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

        configTree.setToolTipText("");
        configTree.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                TreePath path = configTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (nodeToConfigMap.containsKey(node)) {
                        configTree.setToolTipText(
                                "Source Path: " + nodeToConfigMap.get(node).get("sourcePath").getAsString());
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

    private JsonArray fetchTagConfigs() {
        try {
            TagCICDRPC rpc = ModuleRPCFactory.create(TagCICDConstants.MODULE_ID, TagCICDRPC.class);
            String configJson = rpc.getTagConfig();
            return gson.fromJson(configJson, JsonArray.class);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(context.getFrame(), "Failed to fetch tag configurations: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return new JsonArray();
        }
    }

    private void buildTreeModel(DefaultMutableTreeNode root, JsonArray configs) {
        Map<String, DefaultMutableTreeNode> providerNodes = new HashMap<>();
        for (JsonElement element : configs) {
            JsonObject config = element.getAsJsonObject();
            String provider = config.get("provider").getAsString();
            String baseTagPath = config.get("baseTagPath").getAsString();

            DefaultMutableTreeNode providerNode = providerNodes.computeIfAbsent(provider, k -> {
                TagConfigNode providerConfigNode = new TagConfigNode(provider, true);
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(providerConfigNode);
                root.add(newNode);
                return newNode;
            });

            String displayText = baseTagPath.isEmpty() ? "[Provider Root]" : baseTagPath;
            TagConfigNode configNode = new TagConfigNode(displayText, false);
            DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(configNode);
            providerNode.add(tagNode);
            nodeToConfigMap.put(tagNode, config);
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) root.getChildAt(i);
            TagConfigNode providerConfigNode = (TagConfigNode) providerNode.getUserObject();
            providerConfigNode.setSelected(true);
            for (int j = 0; j < providerNode.getChildCount(); j++) {
                ((TagConfigNode) ((DefaultMutableTreeNode) providerNode.getChildAt(j)).getUserObject())
                        .setSelected(true);
            }
        }
    }

    private void initComponents() {
        setSize(350, 470);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Select Configurations to " + operationType);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(headerLabel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(configTree);
        scrollPane.setBorder(
                new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(new Color(200, 200, 200), 1)));
        scrollPane.setPreferredSize(new Dimension(350, 150));
        add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        selectionPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

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

        statusLabel = new JLabel("");
        statusLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        southPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        confirmButton = new JButton(operationType);
        confirmButton.addActionListener(e -> performOperation());
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

    private void performOperation() {
        confirmed = true;
        selectedConfigs.clear();
        for (int i = 0; i < treeModel.getChildCount(treeModel.getRoot()); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) treeModel.getChild(treeModel.getRoot(), i);
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
                    "No configurations selected for " + operationType.toLowerCase() + ".", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        confirmButton.setEnabled(false);
        cancelButton.setEnabled(false);
        cancelButton.setText("Close");
        configTree.setEnabled(false);
        selectAllLabel.setEnabled(false);
        selectNoneLabel.setEnabled(false);
        statusLabel.setText("Processing...");

        if (operationType.equals("Export")) {
            startExport();
        } else if (operationType.equals("Import")) {
            startImport();
        }
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
                        String exportMode = config.get("exportMode").getAsString();
                        boolean excludeUdtDefinitions = config.has("excludeUdtDefinitions")
                                ? config.get("excludeUdtDefinitions").getAsBoolean()
                                : false;

                        Callable<String> exportTask = () -> rpc.exportTags(provider, baseTagPath, filePath, true, false,
                                exportMode, true, excludeUdtDefinitions);
                        Future<String> future = executor.submit(exportTask);

                        String result;
                        try {
                            result = future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (TimeoutException te) {
                            exportResults.addProperty(filePath,
                                    "Failed: Timeout after " + RPC_TIMEOUT_SECONDS + " seconds");
                            continue;
                        }
                        JsonObject exportResult = gson.fromJson(result, JsonObject.class);
                        exportResults.addProperty(filePath,
                                exportResult.get("success").getAsBoolean()
                                        ? "Exported successfully"
                                        : "Failed: " + exportResult.get("error").getAsString());
                    }
                } catch (Exception ex) {
                    for (JsonObject config : selectedConfigs) {
                        exportResults.addProperty(config.get("sourcePath").getAsString(), "Failed: " + ex.getMessage());
                    }
                } finally {
                    executor.shutdown();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updateStatus(exportResults);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(context.getFrame(), "Export error: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setVisible(false);
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
                        String exportMode = config.get("exportMode").getAsString();

                        Callable<String> importTask = () -> rpc.importTags(provider, baseTagPath, filePath,
                                collisionPolicy, exportMode);
                        Future<String> future = executor.submit(importTask);

                        String result;
                        try {
                            result = future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (TimeoutException te) {
                            importResults.addProperty(filePath,
                                    "Failed: Timeout after " + RPC_TIMEOUT_SECONDS + " seconds");
                            continue;
                        }
                        JsonObject importResult = gson.fromJson(result, JsonObject.class);
                        if (importResult.get("success").getAsBoolean()) {
                            JsonObject details = importResult.getAsJsonObject("details");
                            int created = details.has("created_tags") ? details.getAsJsonObject("created_tags").size()
                                    : 0;
                            int deleted = details.has("deleted_tags") ? details.getAsJsonObject("deleted_tags").size()
                                    : 0;
                            importResults.addProperty(filePath, "Created: " + created + ", Deleted: " + deleted);
                        } else {
                            importResults.addProperty(filePath, "Failed: " + importResult.get("error").getAsString());
                        }
                    }
                } catch (Exception ex) {
                    for (JsonObject config : selectedConfigs) {
                        importResults.addProperty(config.get("sourcePath").getAsString(), "Failed: " + ex.getMessage());
                    }
                } finally {
                    executor.shutdown();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updateStatus(importResults);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(context.getFrame(), "Import error: " + ex.getMessage(),
                            "Import Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setVisible(false);
                }
            }
        };
        importWorker.execute();
    }

    private void styleLinkLabel(JLabel label) {
        label.setForeground(new Color(0, 102, 204));
        label.setFont(label.getFont().deriveFont(11f));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (label.isEnabled())
                    label.setText("<html><u>" + label.getText() + "</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (label.isEnabled())
                    label.setText(label.getText().replaceAll("<html><u>|</u></html>", ""));
            }
        });
    }

    private void setAllSelected(boolean selected) {
        for (int i = 0; i < treeModel.getChildCount(treeModel.getRoot()); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) treeModel.getChild(treeModel.getRoot(), i);
            TagConfigNode providerConfigNode = (TagConfigNode) providerNode.getUserObject();
            providerConfigNode.setSelected(selected);
            for (int j = 0; j < providerNode.getChildCount(); j++) {
                ((TagConfigNode) ((DefaultMutableTreeNode) providerNode.getChildAt(j)).getUserObject())
                        .setSelected(selected);
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

    private void updateStatus(JsonObject results) {
        statusLabel.setText("");
        for (int i = 0; i < treeModel.getChildCount(treeModel.getRoot()); i++) {
            DefaultMutableTreeNode providerNode = (DefaultMutableTreeNode) treeModel.getChild(treeModel.getRoot(), i);
            for (int j = 0; j < providerNode.getChildCount(); j++) {
                DefaultMutableTreeNode tagNode = (DefaultMutableTreeNode) providerNode.getChildAt(j);
                String sourcePath = nodeToConfigMap.get(tagNode).get("sourcePath").getAsString();
                if (results.has(sourcePath)) {
                    String status = results.get(sourcePath).getAsString();
                    Color statusColor = status.contains("Failed") ? Color.RED : Color.GREEN;
                    nodeToStatusMap.put(tagNode, status);
                    TagConfigNode configNode = (TagConfigNode) tagNode.getUserObject();
                    configNode.setStatus(status);
                    configNode.setStatusColor(statusColor);
                }
            }
        }
        configTree.repaint();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<JsonObject> getSelectedConfigs() {
        return selectedConfigs;
    }

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
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panel.setOpaque(false);

            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(configNode.isSelected());
            checkBox.setOpaque(false);
            checkBox.setEnabled(tree.isEnabled());
            panel.add(checkBox);

            JLabel label = (JLabel) projectRenderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                    hasFocus);
            label.setText(configNode.toString());
            label.setForeground(configNode.getStatusColor());
            panel.add(label);

            return panel;
        }
    }
}