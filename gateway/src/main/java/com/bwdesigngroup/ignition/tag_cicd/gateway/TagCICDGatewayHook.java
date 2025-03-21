package com.bwdesigngroup.ignition.tag_cicd.gateway;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagImportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDConstants;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagExportRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagImportRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagConfigRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagDeleteRoutes;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.clientcomm.ClientReqSession;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

/**
 * Class which is instantiated by the Ignition platform when the module is loaded in the gateway scope.
 */
public class TagCICDGatewayHook extends AbstractGatewayModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static GatewayContext context;

    @Override
    public void setup(GatewayContext context) {
        logger.info("Setting up TagCICDGatewayHook");
        TagCICDGatewayHook.context = context;
    }

    @Override
    public void startup(LicenseState activationState) {
        logger.info("Starting up TagCICDGatewayHook");
        performInitialTagImport();
    }

    private void performInitialTagImport() {
        Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);
        if (!Files.exists(configPath)) {
            logger.info("No tag-cicd-config.json found at " + configPath.toAbsolutePath() + ", skipping initial import.");
            return;
        }

        try {
            String configContent = new String(Files.readAllBytes(configPath));
            JsonArray configArray = new JsonParser().parse(configContent).getAsJsonArray();

            for (JsonElement element : configArray) {
                JsonObject config = element.getAsJsonObject();
                String sourcePath = config.get("sourcePath").getAsString();
                String provider = config.get("provider").getAsString();
                String baseTagPath = config.get("baseTagPath").getAsString();
                String collisionPolicy = config.get("collisionPolicy").getAsString();
                boolean individualFilesPerObject = config.get("individualFilesPerObject").getAsBoolean();

                logger.info("Importing tags from " + sourcePath + " to provider " + provider);
                JsonObject result = TagImportUtilities.importTagsFromSource(
                        context.getTagManager(), provider, baseTagPath, sourcePath,
                        collisionPolicy, individualFilesPerObject);
                logger.info("Import result: " + result.toString());
            }
        } catch (Exception e) {
            logger.error("Failed to perform initial tag import from " + configPath.toAbsolutePath(), e);
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down TagCICDGatewayHook");
    }

    @Override
    public void mountRouteHandlers(RouteGroup routes) {
        logger.info("Mounting route handlers for TagCICDGatewayHook");
        new TagExportRoutes(context, routes).mountRoutes();
        new TagImportRoutes(context, routes).mountRoutes();
        new TagDeleteRoutes(context, routes).mountRoutes();
        new TagConfigRoutes(context, routes).mountRoutes();
    }

    @Override
    public Object getRPCHandler(ClientReqSession session, String projectName) {
        logger.debug("Creating RPC Handler for session: " + session.getId() + ", project: " + projectName);
        return new TagCICDRPCHandler(context);
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
}