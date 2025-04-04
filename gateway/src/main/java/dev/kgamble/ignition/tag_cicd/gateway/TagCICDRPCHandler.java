package dev.kgamble.ignition.tag_cicd.gateway;

import dev.kgamble.ignition.tag_cicd.common.model.ExportMode;
import dev.kgamble.ignition.tag_cicd.common.constants.TagCICDConstants;
import dev.kgamble.ignition.tag_cicd.common.TagCICDRPC;
import dev.kgamble.ignition.tag_cicd.common.util.TagExportUtilities;
import dev.kgamble.ignition.tag_cicd.common.util.TagImportUtilities;
import com.inductiveautomation.ignition.common.gson.Gson;
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
    private final Gson gson = new Gson();

    public TagCICDRPCHandler(GatewayContext context) {
        this.context = context;
    }

    @Override
    public String exportTags(String provider, String baseTagPath, String filePath, boolean recursive,
            boolean localPropsOnly, String exportMode, boolean deleteExisting, boolean excludeUdtDefinitions) {
        JsonObject result = new JsonObject();
        try {
            logger.info("RPC exportTags called: provider={}, baseTagPath={}, filePath={}, exportMode={}",
                    provider, baseTagPath, filePath, exportMode);
            TagExportUtilities.exportTagsToDisk(context.getTagManager(), provider, baseTagPath, recursive,
                    localPropsOnly, filePath, exportMode, deleteExisting, excludeUdtDefinitions);
            result.addProperty("success", true);
            result.addProperty("filePath", filePath);
            result.addProperty("exportMode", exportMode);
            result.addProperty("details", "Exported tags to " + filePath + " using " +
                    ExportMode.fromCode(exportMode).getDisplayName() + " mode");
        } catch (Exception e) {
            logger.error("Error exporting tags: {}", e.getMessage(), e);
            result.addProperty("success", false);
            result.addProperty("error", "Failed to export tags: " + e.getMessage());
        }
        return result.toString();
    }

    @Override
    public String importTags(String provider, String baseTagPath, String sourcePath, String collisionPolicy,
            String exportMode) {
        JsonObject result = new JsonObject();
        try {
            logger.info("RPC importTags called: provider={}, baseTagPath={}, sourcePath={}, exportMode={}",
                    provider, baseTagPath, sourcePath, exportMode);
            JsonObject importResult = TagImportUtilities.importTagsFromSource(context.getTagManager(), provider,
                    baseTagPath, sourcePath, collisionPolicy, exportMode);
            result.addProperty("success", true);
            result.addProperty("exportMode", exportMode);
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
            logger.warn("No export-config.json found at {}", configPath.toAbsolutePath());
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
    public String getExportModes() {
        JsonArray modesArray = new JsonArray();

        for (ExportMode mode : ExportMode.values()) {
            JsonObject modeObject = new JsonObject();
            modeObject.addProperty("code", mode.getCode());
            modeObject.addProperty("name", mode.getDisplayName());
            modesArray.add(modeObject);
        }

        return modesArray.toString();
    }

    @Override
    public String exportTagsFromConfig() {
        JsonObject result = new JsonObject();
        Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);
        if (!Files.exists(configPath)) {
            logger.error("No export-config.json found at {}", configPath.toAbsolutePath());
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
                String exportMode = config.get("exportMode").getAsString();
                boolean excludeUdtDefinitions = config.has("excludeUdtDefinitions")
                        ? config.get("excludeUdtDefinitions").getAsBoolean()
                        : false;

                logger.info("Exporting tags from config: filePath={}, provider={}, baseTagPath={}, exportMode={}",
                        filePath, provider, baseTagPath, exportMode);

                TagExportUtilities.exportTagsToDisk(context.getTagManager(), provider, baseTagPath, true, false,
                        filePath, exportMode, true, excludeUdtDefinitions);

                exportResults.addProperty(filePath, "Exported successfully using " +
                        ExportMode.fromCode(exportMode).getDisplayName() + " mode");
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
            logger.error("No export-config.json found at {}", configPath.toAbsolutePath());
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
                String exportMode = config.get("exportMode").getAsString();

                logger.info("Importing tags from config: sourcePath={}, provider={}, baseTagPath={}, exportMode={}",
                        sourcePath, provider, baseTagPath, exportMode);

                JsonObject importResult = TagImportUtilities.importTagsFromSource(context.getTagManager(), provider,
                        baseTagPath, sourcePath, collisionPolicy, exportMode);

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

    @Override
    public String getTagProviders() {
        JsonArray providersArray = new JsonArray();
        try {
            context.getTagManager().getTagProviders().forEach(provider -> {
                providersArray.add(provider.getName());
            });
        } catch (Exception e) {
            logger.error("Error getting tag providers: {}", e.getMessage(), e);
        }
        return providersArray.toString();
    }

    @Override
    public String saveTagConfig(String configJson) {
        JsonObject result = new JsonObject();
        Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);

        try {
            // Ensure parent directories exist
            Files.createDirectories(configPath.getParent());

            // Write the configuration file
            Files.write(configPath, configJson.getBytes());

            logger.info("Successfully saved tag configuration to {}", configPath);
            result.addProperty("success", true);
        } catch (Exception e) {
            logger.error("Error saving tag configuration: {}", e.getMessage(), e);
            result.addProperty("success", false);
            result.addProperty("error", "Failed to save tag configuration: " + e.getMessage());
        }

        return result.toString();
    }

    @Override
    public String getInstallDirectory() {
        return context.getSystemManager().getDataDir().toPath().toAbsolutePath().getParent().toString();
    }
}