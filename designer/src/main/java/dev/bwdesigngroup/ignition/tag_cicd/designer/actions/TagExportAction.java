package dev.bwdesigngroup.ignition.tag_cicd.designer.actions;

import dev.bwdesigngroup.ignition.tag_cicd.designer.dialog.TagConfigOperationDialog;
import com.inductiveautomation.ignition.client.util.action.BaseAction;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;
import java.awt.event.ActionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.inductiveautomation.ignition.common.BundleUtil.i18n;

public class TagExportAction extends BaseAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DesignerContext context;

    public TagExportAction(DesignerContext context, Icon icon) {
        super(i18n("tagcicd.Action.ExportTags.Name"), icon);
        this.context = context;
        putValue(SHORT_DESCRIPTION, i18n("tagcicd.Action.ExportTags.Description"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        TagConfigOperationDialog dialog = new TagConfigOperationDialog(context, "Export");
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            logger.debug("Tag export canceled by user");
            return;
        }

        // The dialog handles the export operation internally and displays results
        logger.info("Export operation initiated for selected configurations");
    }
}