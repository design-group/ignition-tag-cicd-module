package dev.bwdesigngroup.ignition.tag_cicd.designer.actions;

import dev.bwdesigngroup.ignition.tag_cicd.designer.dialog.TagConfigManagerDialog;
import com.inductiveautomation.ignition.client.util.action.BaseAction;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

import javax.swing.*;
import java.awt.event.ActionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.inductiveautomation.ignition.common.BundleUtil.i18n;

/**
 * Action to open the Tag CICD Configuration Manager dialog.
 */
public class ConfigManagerAction extends BaseAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DesignerContext context;

    public ConfigManagerAction(DesignerContext context, Icon icon) {
        super(i18n("tagcicd.Action.ConfigManager.Name"), icon);
        this.context = context;
        putValue(SHORT_DESCRIPTION, i18n("tagcicd.Action.ConfigManager.Description"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        TagConfigManagerDialog dialog = new TagConfigManagerDialog(context);
        dialog.setVisible(true);
    }
}