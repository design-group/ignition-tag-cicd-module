package dev.bwdesigngroup.ignition.tag_cicd.designer.model;

import dev.bwdesigngroup.ignition.tag_cicd.common.TagCICDRPC;
import dev.bwdesigngroup.ignition.tag_cicd.common.constants.TagCICDConstants;
import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.GsonBuilder;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Data model for managing tag configurations.
 * This class handles loading, saving, and executing operations on tag
 * configurations.
 */
public class TagConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(TagConfigManager.class.getName());
    private static final int RPC_TIMEOUT_SECONDS = 30;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private JsonArray configArray = new JsonArray();
    private TagCICDRPC rpc;

    public TagConfigManager() {
        try {
            rpc = ModuleRPCFactory.create(TagCICDConstants.MODULE_ID, TagCICDRPC.class);
        } catch (Exception e) {
            logger.error("Failed to create RPC handler", e);
        }
    }

    /**
     * Loads configurations from the export-config.json file.
     * 
     * @throws Exception if an error occurs during loading
     */
    public void loadConfigurations() throws Exception {
        String configJson = rpc.getTagConfig();
        configArray = gson.fromJson(configJson, JsonArray.class);
    }

    /**
     * Saves configurations to the export-config.json file.
     * 
     * @throws Exception if an error occurs during saving
     */
    public void saveConfigurations() throws Exception {
        String configJson = gson.toJson(configArray);

        // Send to gateway via RPC
        String result = rpc.saveTagConfig(configJson);
        JsonObject resultObj = gson.fromJson(result, JsonObject.class);

        if (!resultObj.get("success").getAsBoolean()) {
            throw new Exception("Failed to save configurations: " +
                    (resultObj.has("error") ? resultObj.get("error").getAsString() : "Unknown error"));
        }

        logger.debug("Saved {} configurations to gateway", configArray.size());
    }

    /**
     * Gets the configuration at the specified index.
     * 
     * @param index the index of the configuration to get
     * @return the configuration at the specified index
     */
    public JsonObject getConfiguration(int index) {
        return configArray.get(index).getAsJsonObject();
    }

    /**
     * Gets the number of configurations.
     * 
     * @return the number of configurations
     */
    public int getConfigCount() {
        return configArray.size();
    }

    /**
     * Gets the configuration array.
     * 
     * @return the configuration array
     */
    public JsonArray getConfigArray() {
        return configArray;
    }

    /**
     * Adds a new configuration.
     * 
     * @param config the configuration to add
     * @throws Exception if an error occurs during saving
     */
    public void addConfiguration(JsonObject config) {
        configArray.add(config);
        try {
            saveConfigurations();
        } catch (Exception e) {
            logger.error("Failed to save configurations", e);
        }
    }

    /**
     * Updates an existing configuration.
     * 
     * @param index  the index of the configuration to update
     * @param config the new configuration
     * @throws Exception if an error occurs during saving
     */
    public void updateConfiguration(int index, JsonObject config) {
        configArray.set(index, config);
        try {
            saveConfigurations();
        } catch (Exception e) {
            logger.error("Failed to save configurations", e);
        }
    }

    /**
     * Deletes a configuration.
     * 
     * @param index the index of the configuration to delete
     * @throws Exception if an error occurs during saving
     */
    public void deleteConfiguration(int index) {
        configArray.remove(index);
        try {
            saveConfigurations();
        } catch (Exception e) {
            logger.error("Failed to save configurations", e);
        }
    }

    /**
     * Exports tags using the configuration at the specified index.
     * 
     * @param index the index of the configuration to use
     * @return the result of the export operation
     * @throws Exception if an error occurs during export
     */
    public String exportTags(int index) {
        JsonObject config = getConfiguration(index);

        try {
            String provider = config.get("provider").getAsString();
            String baseTagPath = config.get("baseTagPath").getAsString();
            String filePath = config.get("sourcePath").getAsString();
            String exportMode = config.get("exportMode").getAsString();

            // Convert includeUdtDefinitions to excludeUdtDefinitions (inverse logic)
            boolean excludeUdtDefinitions = config.has("excludeUdtDefinitions")
                    ? config.get("excludeUdtDefinitions").getAsBoolean()
                    : false;

            Future<String> future = executor.submit(() -> rpc.exportTags(
                    provider, baseTagPath, filePath, true, false, exportMode, true, excludeUdtDefinitions));

            return future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Export error", e);
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Imports tags using the configuration at the specified index.
     * 
     * @param index the index of the configuration to use
     * @return the result of the import operation
     * @throws Exception if an error occurs during import
     */
    public String importTags(int index) {
        JsonObject config = getConfiguration(index);

        try {
            String provider = config.get("provider").getAsString();
            String baseTagPath = config.get("baseTagPath").getAsString();
            String sourcePath = config.get("sourcePath").getAsString();
            String collisionPolicy = config.get("collisionPolicy").getAsString();
            String exportMode = config.get("exportMode").getAsString();

            Future<String> future = executor.submit(() -> rpc.importTags(
                    provider, baseTagPath, sourcePath, collisionPolicy, exportMode));

            return future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Import error", e);
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Gets the display name for the given export mode code.
     * 
     * @param code the export mode code
     * @return the display name
     */
    public String getExportModeDisplayName(String code) {
        for (ExportMode mode : ExportMode.values()) {
            if (mode.getCode().equals(code)) {
                return mode.getDisplayName();
            }
        }
        return code;
    }

    /**
     * Gets the display name for the given collision policy code.
     * 
     * @param code the collision policy code
     * @return the display name
     */
    public String getCollisionPolicyDisplayName(String code) {
        switch (code) {
            case "a":
                return "Abort";
            case "m":
                return "Merge";
            case "o":
                return "Overwrite";
            case "d":
                return "Delete & Replace";
            default:
                return code;
        }
    }

    /**
     * Gets the available tag providers.
     * 
     * @return an array of tag provider names
     */
    public String[] getTagProviders() {
        try {
            String providersStr = rpc.getTagProviders();
            String[] providers = gson.fromJson(providersStr, String[].class);
            return providers != null ? providers : new String[0];
        } catch (Exception e) {
            logger.error("Failed to fetch tag providers", e);
            return new String[0];
        }
    }

    /**
     * Checks if a configuration's export path overlaps with other configurations.
     * 
     * @param configIndex The index of the config to check, or -1 to check a new path
     * @param sourcePath The source path to check
     * @return true if there's an overlap, false otherwise
     */
    public boolean hasOverlappingPath(int configIndex, String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            return false;
        }
        
        File checkPath = new File(sourcePath);
        
        for (int i = 0; i < configArray.size(); i++) {
            // Skip comparing with itself
            if (i == configIndex) {
                continue;
            }
            
            JsonObject config = configArray.get(i).getAsJsonObject();
            String otherPath = config.get("sourcePath").getAsString();
            File otherFile = new File(otherPath);
            
            // Check if one path is a subdirectory of the other
            if (isPathOverlapping(checkPath, otherFile)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determines if two paths overlap (one is contained within the other).
     * 
     * @param path1 First path
     * @param path2 Second path
     * @return true if paths overlap, false otherwise
     */
    private boolean isPathOverlapping(File path1, File path2) {
        try {
            String canonical1 = path1.getCanonicalPath();
            String canonical2 = path2.getCanonicalPath();
            
            // Check if one path is a subdirectory of the other
            return canonical1.startsWith(canonical2 + File.separator) ||
                canonical2.startsWith(canonical1 + File.separator) ||
                canonical1.equals(canonical2);
        } catch (IOException e) {
            logger.warn("Error comparing paths: {} and {}", path1, path2, e);
            // If we can't determine, assume they might overlap to be safe
            return true;
        }
    }

    /**
     * Parses a result string into a JsonObject.
     * 
     * @param result the result string
     * @return the parsed JsonObject
     */
    public JsonObject parseResult(String result) {
        return gson.fromJson(result, JsonObject.class);
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}