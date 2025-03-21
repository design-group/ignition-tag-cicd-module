package com.bwdesigngroup.ignition.tag_cicd.designer;

import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDConstants;
import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDRPC;
import com.inductiveautomation.ignition.client.util.action.BaseAction;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.inductiveautomation.ignition.common.BundleUtil.i18n;

public class TagExportAction extends BaseAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DesignerContext context;
    private final Gson gson = new Gson();

    public TagExportAction(DesignerContext context, Icon icon) {
        super(i18n("tagcicd.Action.ExportTags.Name"), icon);
        this.context = context;
        putValue(SHORT_DESCRIPTION, i18n("tagcicd.Action.ExportTags.Description"));
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        logger.info("Export Tags button clicked");

        // Fetch the tag config
        JsonArray configs;
        try {
            TagCICDRPC rpc = ModuleRPCFactory.create(TagCICDConstants.MODULE_ID, TagCICDRPC.class);
            logger.debug("Fetching tag config via RPC");
            String configResult = rpc.getTagConfig();
            configs = gson.fromJson(configResult, JsonArray.class);
            logger.debug("Tag config fetched: {}", configResult);
        } catch (Exception ex) {
            logger.error("Error fetching tag config", ex);
            JOptionPane.showMessageDialog(context.getFrame(),
                    "Error fetching tag configurations: " + ex.getMessage(),
                    "Config Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (configs.size() == 0) {
            logger.warn("No tag configurations found in tag-cicd-config.json");
            JOptionPane.showMessageDialog(context.getFrame(),
                    "No tag configurations found in tag-cicd-config.json.",
                    "No Configurations", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create and show the dialog; the dialog will handle the export
        TagConfigSelectionDialog dialog = new TagConfigSelectionDialog(context, "Export", configs);
        dialog.setVisible(true);
        // No need to start the SwingWorker here; the dialog handles it
    }
}