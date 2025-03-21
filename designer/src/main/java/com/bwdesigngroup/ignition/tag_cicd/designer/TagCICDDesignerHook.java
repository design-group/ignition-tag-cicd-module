package com.bwdesigngroup.ignition.tag_cicd.designer;

import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDConstants;
import com.inductiveautomation.ignition.client.icons.VectorIcons;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.designer.gui.DesignerToolbar;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.jidesoft.action.CommandBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TagCICDDesignerHook extends AbstractDesignerModuleHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private DesignerContext context;

    @Override
    public void startup(DesignerContext context, LicenseState activationState) throws Exception {
        logger.info("Starting up Tag CICD Designer Module");
        this.context = context;
        BundleUtil.get().addBundle("tagcicd", this.getClass(), "designer");
    }

    @Override
    public List<CommandBar> getModuleToolbars() {
        List<CommandBar> toolbars = new ArrayList<>();
        DesignerToolbar toolbar = new DesignerToolbar("TagCICD", "tagcicd.Toolbar.Name");

        toolbar.addButton(new TagExportAction(context, VectorIcons.getInteractive("export")));
        toolbar.addButton(new TagImportAction(context, VectorIcons.getInteractive("import")));

        toolbars.add(toolbar);
        return toolbars;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down Tag CICD Designer Module");
    }
}