package com.bwdesigngroup.ignition.tag_cicd.gateway;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagImportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagExportRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagImportRoutes;
import com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes.TagDeleteRoutes;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

/**
 * Class which is instantiated by the Ignition platform when the module is loaded in the gateway scope.
 */
public class TagCICDGatewayHook extends AbstractGatewayModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static GatewayContext context;
    private static final String CONFIG_FILE_PATH = "data/modules/com.bwdesigngroup.ignition.tag_cicd/import-config.json";

    @Override
    public void setup(GatewayContext context) {
        logger.info("Setting up TagCICDGatewayHook");
        TagCICDGatewayHook.context = context;
    }

    @Override
    public void startup(LicenseState activationState) {
        logger.info("Starting up TagCICDGatewayHook");
        performStartupImports();
    }

    private void performStartupImports() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            logger.info("No import config file found at " + CONFIG_FILE_PATH + ". Skipping startup imports.");
            return;
        }

        try {
            String configContent = new String(Files.readAllBytes(configFile.toPath()));
            Gson gson = new Gson();
            JsonArray importConfigs = gson.fromJson(configContent, JsonArray.class);

            if (importConfigs == null || importConfigs.size() == 0) {
                logger.info("Import config file is empty or invalid. Skipping startup imports.");
                return;
            }

            for (JsonElement element : importConfigs) {
                JsonObject config = element.getAsJsonObject();
                String sourcePath = config.get("sourcePath").getAsString();
                String provider = config.get("provider").getAsString();
                String baseTagPath = config.get("baseTagPath").getAsString();
                String collisionPolicy = config.get("collisionPolicy").getAsString();
                boolean individualFilesPerObject = config.get("individualFilesPerObject").getAsBoolean();

                logger.info("Processing startup import from " + sourcePath + " to provider " + provider + " at " + baseTagPath);

                JsonObject result = TagImportUtilities.importTagsFromSource(
                    context.getTagManager(), provider, baseTagPath, sourcePath, collisionPolicy, individualFilesPerObject
                );

                if (result.has("error")) {
                    logger.error("Failed to import tags from " + sourcePath + ": " + result.get("error").getAsString());
                } else {
                    logger.info("Successfully imported tags from " + sourcePath + " to " + provider + "/" + baseTagPath);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading import config file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error during startup tag import: " + e.getMessage(), e);
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
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
}