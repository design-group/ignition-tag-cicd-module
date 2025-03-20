package com.bwdesigngroup.ignition.tag_cicd.gateway;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagExportRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagImportRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagDeleteRoutes;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.project.resource.adapter.ResourceTypeAdapter;
import com.inductiveautomation.ignition.common.project.resource.adapter.ResourceTypeAdapterRegistry;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.model.GatewayModuleHook;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.pages.config.overviewmeta.ConfigOverviewContributor;
import com.inductiveautomation.ignition.gateway.web.pages.status.overviewmeta.OverviewContributor;

/**
 * Class which is instantiated by the Ignition platform when the module is loaded in the gateway scope.
 */
public class TagCICDGatewayHook extends AbstractGatewayModuleHook {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static GatewayContext context;

    /**
     * Called to before startup. This is the chance for the module to add its extension points and update persistent
     * records and schemas. None of the managers will be started up at this point, but the extension point managers will
     * accept extension point types.
     */
    @Override
    public void setup(GatewayContext context) {
		logger.info("Setting up TagCICDGatewayHook");
		TagCICDGatewayHook.context = context;
    }

    /**
     * Called to initialize the module. Will only be called once. Persistence interface is available, but only in
     * read-only mode.
     */
    @Override
    public void startup(LicenseState activationState) {
		logger.info("Starting up TagCICDGatewayHook");
    }

    /**
     * Called to shutdown this module. Note that this instance will never be started back up - a new one will be created
     * if a restart is desired
     */
    @Override
    public void shutdown() {
		logger.info("Shutting down TagCICDGatewayHook");
    }

    /**
     * Provides a chance for the module to mount any route handlers it wants. These will be active at
     * <tt>/main/data/module-id/*</tt> See {@link RouteGroup} for details. Will be called after startup().
     */
    @Override
    public void mountRouteHandlers(RouteGroup routes) {
      logger.info("Mounting route handlers for TagCICDGatewayHook");
      new TagExportRoutes(context, routes).mountRoutes();
      new TagImportRoutes(context, routes).mountRoutes();
      new TagDeleteRoutes(context, routes).mountRoutes();
    }


    /**
     * @return {@code true} if this is a "free" module, i.e. it does not participate in the licensing system. This is
     * equivalent to the now defunct FreeModule attribute that could be specified in module.xml.
     */
    @Override
    public boolean isFreeModule() {
        return true;
    }
}
