package com.bwdesigngroup.ignition.tag_cicd.gateway;

import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDConstants;
import com.bwdesigngroup.ignition.tag_cicd.common.TagCICDRPC;
import com.bwdesigngroup.ignition.tag_cicd.common.TagExportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagImportUtilities;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TagCICDRPCHandler implements TagCICDRPC {
    private static final Logger logger = LoggerFactory.getLogger(TagCICDRPCHandler.class.getName());
    private final GatewayContext context;

    public TagCICDRPCHandler(GatewayContext context) {
        this.context = context;
    }

    @Override
    public String exportTags(String provider, String baseTagPath, String filePath, boolean recursive,
            boolean localPropsOnly, boolean individualFilesPerObject, boolean deleteExisting,
            boolean excludeUdtDefinitions) {
        JsonObject result = new JsonObject();
        try {
            logger.info("RPC exportTags called: provider={}, baseTagPath={}, filePath={}", provider, baseTagPath,
                    filePath);
            TagExportUtilities.exportTagsToDisk(context.getTagManager(), provider, baseTagPath, recursive,
                    localPropsOnly, filePath, individualFilesPerObject, deleteExisting, excludeUdtDefinitions);
            result.addProperty("success", true);
            result.addProperty("filePath", filePath);
            result.addProperty("details", "Exported tags to " + filePath);
        } catch (Exception e) {
            logger.error("Error exporting tags: {}", e.getMessage(), e);
            result.addProperty("success", false);
            result.addProperty("error", "Failed to export tags: " + e.getMessage());
        }
        return result.toString();
    }

    @Override
    public String importTags(String provider, String baseTagPath, String sourcePath, String collisionPolicy,
            boolean individualFilesPerObject) {
        JsonObject result = new JsonObject();
        try {
            logger.info("RPC importTags called: provider={}, baseTagPath={}, sourcePath={}", provider, baseTagPath,
                    sourcePath);
            JsonObject importResult = TagImportUtilities.importTagsFromSource(context.getTagManager(), provider,
                    baseTagPath, sourcePath, collisionPolicy, individualFilesPerObject);
            result.addProperty("success", true);
            result.add("details", importResult);
        } catch (Exception e) {
            logger.error("Error importing tags: {}", e.getMessage(), e);
            result.addProperty("success", false);
            result.addProperty("error", "Failed to import tags: " + e.getMessage());
        }
        return result.toString();
    }

    @Override
    public String getTagConfig() {
        JsonArray result = new JsonArray();
        Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);
        if (!Files.exists(configPath)) {
            logger.warn("No tag-cicd-config.json found at {}", configPath.toAbsolutePath());
            return result.toString();
        }

        try {
            String configContent = new String(Files.readAllBytes(configPath));
            result = new JsonParser().parse(configContent).getAsJsonArray();
        } catch (Exception e) {
            logger.error("Error retrieving tag config: {}", e.getMessage(), e);
        }
        return result.toString();
    }

    @Override
    public String exportTagsFromConfig() {
        JsonObject result = new JsonObject();
        Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);
        if (!Files.exists(configPath)) {
            logger.error("No tag-cicd-config.json found at {}", configPath.toAbsolutePath());
            result.addProperty("success", false);
            result.addProperty("error", "Config file not found at " + configPath.toAbsolutePath());
            return result.toString();
        }

        try {
            String configContent = new String(Files.readAllBytes(configPath));
            JsonArray configArray = new JsonParser().parse(configContent).getAsJsonArray();
            JsonObject exportResults = new JsonObject();

            for (JsonElement element : configArray) {
                JsonObject config = element.getAsJsonObject();
                String filePath = config.get("sourcePath").getAsString();
                String provider = config.get("provider").getAsString();
                String baseTagPath = config.get("baseTagPath").getAsString();
                boolean individualFilesPerObject = config.get("individualFilesPerObject").getAsBoolean();
                boolean excludeUdtDefinitions = config.has("excludeUdtDefinitions")
                        ? config.get("excludeUdtDefinitions").getAsBoolean()
                        : false;

                logger.info("Exporting tags from config: filePath={}, provider={}, baseTagPath={}", filePath, provider,
                        baseTagPath);
                TagExportUtilities.exportTagsToDisk(context.getTagManager(), provider, baseTagPath, true, false,
                        filePath, individualFilesPerObject, true, excludeUdtDefinitions);
                exportResults.addProperty(filePath, "Exported successfully");
            }
            result.addProperty("success", true);
            result.add("details", exportResults);
        } catch (Exception e) {
            logger.error("Error exporting tags from config: {}", e.getMessage(), e);
            result.addProperty("success", false);
            result.addProperty("error", "Failed to export tags from config: " + e.getMessage());
        }
        return result.toString();
    }

    @Override
    public String importTagsFromConfig() {
        JsonObject result = new JsonObject();
        Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);
        if (!Files.exists(configPath)) {
            logger.error("No tag-cicd-config.json found at {}", configPath.toAbsolutePath());
            result.addProperty("success", false);
            result.addProperty("error", "Config file not found at " + configPath.toAbsolutePath());
            return result.toString();
        }

        try {
            String configContent = new String(Files.readAllBytes(configPath));
            JsonArray configArray = new JsonParser().parse(configContent).getAsJsonArray();
            JsonObject importResults = new JsonObject();

            for (JsonElement element : configArray) {
                JsonObject config = element.getAsJsonObject();
                String sourcePath = config.get("sourcePath").getAsString();
                String provider = config.get("provider").getAsString();
                String baseTagPath = config.get("baseTagPath").getAsString();
                String collisionPolicy = config.get("collisionPolicy").getAsString();
                boolean individualFilesPerObject = config.get("individualFilesPerObject").getAsBoolean();

                logger.info("Importing tags from config: sourcePath={}, provider={}, baseTagPath={}", sourcePath,
                        provider, baseTagPath);
                JsonObject importResult = TagImportUtilities.importTagsFromSource(context.getTagManager(), provider,
                        baseTagPath, sourcePath, collisionPolicy, individualFilesPerObject);
                importResults.add(sourcePath, importResult);
            }
            result.addProperty("success", true);
            result.add("details", importResults);
        } catch (Exception e) {
            logger.error("Error importing tags from config: {}", e.getMessage(), e);
            result.addProperty("success", false);
            result.addProperty("error", "Failed to import tags from config: " + e.getMessage());
        }
        return result.toString();
    }
}