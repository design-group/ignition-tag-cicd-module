package dev.bwdesigngroup.ignition.tag_cicd.common.util;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import dev.bwdesigngroup.ignition.tag_cicd.common.strategy.TagExportImportStrategy;
import dev.bwdesigngroup.ignition.tag_cicd.common.strategy.TagExportImportStrategyFactory;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TagExportUtilities {
    private static final Logger logger = LoggerFactory.getLogger(TagExportUtilities.class.getName());

    public static JsonObject convertToJsonObject(TagConfigurationModel tagConfigurationModel) throws Exception {
        return TagUtilities.toJsonObject(tagConfigurationModel);
    }

    public static JsonObject removeUdtDefinitions(JsonObject tagsJson) {
        if (tagsJson.has("tags")) {
            JsonArray tags = tagsJson.getAsJsonArray("tags");
            JsonArray filteredTags = new JsonArray();

            for (JsonElement tag : tags) {
                JsonObject tagObject = tag.getAsJsonObject();
                if (tagObject.has("name") && tagObject.has("tagType")) {
                    String tagName = tagObject.get("name").getAsString();
                    String tagType = tagObject.get("tagType").getAsString();

                    if (!("_types_".equals(tagName) && "Folder".equals(tagType))) {
                        filteredTags.add(tagObject);
                    }
                } else {
                    filteredTags.add(tagObject);
                }
            }

            JsonObject filteredTagsJson = new JsonObject();
            filteredTagsJson.add("tags", filteredTags);
            return filteredTagsJson;
        }

        return tagsJson;
    }

    /**
     * Validates the tag path and export configuration before attempting export.
     * 
     * @param tagManager     the tag manager
     * @param provider       the provider name
     * @param baseTagPath    the base tag path to validate
     * @param recursive      whether the export is recursive
     * @param localPropsOnly whether to export only local properties
     * @throws IllegalArgumentException if the configuration is invalid
     * @throws IOException              if there's an error accessing the tags
     */
    public static void validateExportConfiguration(GatewayTagManager tagManager, String provider,
            String baseTagPath, boolean recursive, boolean localPropsOnly) throws IOException {

        // Check if provider exists
        if (tagManager.getTagProvider(provider) == null) {
            throw new IllegalArgumentException("Tag provider '" + provider + "' does not exist");
        }

        // Normalize the base tag path to prevent double slashes
        baseTagPath = normalizeTagPath(baseTagPath);

        // For _types_ paths, we'll be more permissive and let Ignition handle the
        // validation
        if (baseTagPath != null && baseTagPath.startsWith("_types_")) {
            logger.info("Skipping strict validation for UDT path: {}", baseTagPath);
            try {
                // Just do a basic check that we can access the path
                TagConfigurationModel testModel = TagConfigUtilities.getTagConfigurationModel(
                        tagManager, provider, baseTagPath, false, localPropsOnly);

                if (testModel == null) {
                    throw new IllegalArgumentException(
                            "Tag path '" + baseTagPath + "' does not exist in provider '" + provider + "'");
                }

                // Skip the Unknown tag type validation for _types_ paths
                // Let the export process handle it naturally
                logger.info("Basic validation passed for UDT path: {}", baseTagPath);
                return;

            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    throw e;
                }
                throw new IOException("Failed to access UDT path '" + baseTagPath + "' in provider '" + provider + "': "
                        + e.getMessage(), e);
            }
        }

        // Standard validation for non-_types_ paths
        try {
            TagConfigurationModel testModel = TagConfigUtilities.getTagConfigurationModel(
                    tagManager, provider, baseTagPath, false, localPropsOnly);

            // Check if we got a valid result
            if (testModel == null) {
                throw new IllegalArgumentException(
                        "Tag path '" + baseTagPath + "' does not exist in provider '" + provider + "'");
            }

            // Convert to JSON to check for Unknown tag types or empty results
            JsonObject testJson = convertToJsonObject(testModel);
            validateJsonStructure(testJson, baseTagPath, provider);

        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors as-is
        } catch (Exception e) {
            throw new IOException("Failed to validate tag path '" + baseTagPath + "' in provider '" + provider + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Normalizes a tag path by removing duplicate slashes and ensuring proper
     * format.
     * 
     * @param tagPath the tag path to normalize
     * @return the normalized tag path
     */
    private static String normalizeTagPath(String tagPath) {
        if (tagPath == null || tagPath.isEmpty()) {
            return "";
        }

        // Remove duplicate slashes and normalize the path
        String normalized = tagPath.replaceAll("/+", "/");

        // Remove leading slash if present
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Remove trailing slash if present
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * Validates the JSON structure for any Unknown tag types or invalid
     * configurations.
     * Special handling for _types_ folder exports.
     * 
     * @param json        the JSON to validate
     * @param baseTagPath the base tag path for context in error messages
     * @param provider    the provider name for better error messages
     * @throws IllegalArgumentException if invalid structure is found
     */
    private static void validateJsonStructure(JsonObject json, String baseTagPath, String provider) {
        logger.debug("Validating JSON structure for path: '{}' in provider: '{}'", baseTagPath, provider);
        logger.debug("JSON structure: {}", json.toString());

        // Special handling for _types_ folder exports
        if (baseTagPath != null && baseTagPath.startsWith("_types_")) {
            // For _types_ exports, we need to check if we got valid UDT definitions
            if (json.has("tagType") && "Unknown".equals(json.get("tagType").getAsString())) {
                throw new IllegalArgumentException(
                        "The path '" + baseTagPath +
                                "' does not exist or contains no UDT definitions in provider '" + provider + "'. " +
                                "Please verify the UDT folder path exists and contains UDT definitions.");
            }

            // Check if the _types_ export resulted in empty or invalid structure
            if (!json.has("tags")) {
                logger.debug("No 'tags' property found in JSON for _types_ export");
                throw new IllegalArgumentException(
                        "No tags found at path '" + baseTagPath + "' in provider '" + provider + "'. " +
                                "The export returned no tag structure.");
            }

            JsonArray tags = json.getAsJsonArray("tags");
            if (tags.size() == 0) {
                logger.debug("Empty tags array found for _types_ export");
                throw new IllegalArgumentException(
                        "No UDT definitions found at path '" + baseTagPath + "' in provider '" + provider + "'. " +
                                "Verify that UDT definitions exist in this location.");
            }

            // For _types_ exports, we should be more lenient about what constitutes valid
            // content
            // The export may contain folders that eventually contain UDT definitions
            logger.debug("Found {} tags in _types_ export", tags.size());

            // Log what we found for debugging
            for (JsonElement tagElement : tags) {
                if (tagElement.isJsonObject()) {
                    JsonObject tagObject = tagElement.getAsJsonObject();
                    String tagType = tagObject.has("tagType") ? tagObject.get("tagType").getAsString() : "unknown";
                    String tagName = tagObject.has("name") ? tagObject.get("name").getAsString() : "unnamed";
                    logger.debug("Found tag: name='{}', type='{}'", tagName, tagType);
                }
            }

            // Check if we have any UDT definitions or folders that might contain them
            boolean hasValidContent = false;
            for (JsonElement tagElement : tags) {
                if (tagElement.isJsonObject()) {
                    JsonObject tagObject = tagElement.getAsJsonObject();
                    if (tagObject.has("tagType")) {
                        String tagType = tagObject.get("tagType").getAsString();
                        if ("UdtType".equals(tagType)) {
                            hasValidContent = true;
                            break;
                        }
                        // Allow folders within _types_ as they may contain UDTs deeper down
                        if ("Folder".equals(tagType)) {
                            hasValidContent = true; // Assume folders in _types_ are legitimate
                            // You can make this more strict by recursively checking if needed
                            break;
                        }
                    }
                }
            }

            if (!hasValidContent) {
                logger.debug("No valid UDT content found in _types_ export");
                throw new IllegalArgumentException(
                        "No UDT definitions or folders found at path '" + baseTagPath + "' in provider '" + provider
                                + "'. " +
                                "The path exists but contains no UDT type definitions or folders. " +
                                "Make sure you're pointing to a folder that contains UDT definitions.");
            }

            logger.info("Successfully validated UDT definitions export from path: {}", baseTagPath);
            return; // Skip general validation for _types_ paths
        }

        // General validation for non-_types_ paths
        if (json.has("tagType") && "Unknown".equals(json.get("tagType").getAsString())) {
            String tagName = json.has("name") ? json.get("name").getAsString() : "unnamed";
            throw new IllegalArgumentException(
                    "Export resulted in Unknown tag type for '" + tagName + "'. " +
                            "This usually indicates an invalid tag path. " +
                            "Base tag path: '" + baseTagPath + "'");
        }

        // Check for tags array and validate recursively
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement tagElement : tags) {
                if (tagElement.isJsonObject()) {
                    JsonObject tagObject = tagElement.getAsJsonObject();
                    if (tagObject.has("tagType") && "Unknown".equals(tagObject.get("tagType").getAsString())) {
                        String tagName = tagObject.has("name") ? tagObject.get("name").getAsString() : "unnamed";
                        throw new IllegalArgumentException(
                                "Export contains Unknown tag type for '" + tagName + "'. " +
                                        "This indicates an invalid tag configuration. " +
                                        "Base tag path: '" + baseTagPath + "'");
                    }
                    // Recursively validate nested structures (but not for _types_ as we handle that
                    // above)
                    if (baseTagPath == null || !baseTagPath.startsWith("_types_")) {
                        validateJsonStructure(tagObject, baseTagPath, provider);
                    }
                }
            }
        }
    }

    /**
     * Recursively checks if a JSON object contains UDT definitions.
     * 
     * @param json the JSON object to check
     * @return true if UDT definitions are found
     */
    private static boolean containsUdtDefinitions(JsonObject json) {
        if (json.has("tagType") && "UdtType".equals(json.get("tagType").getAsString())) {
            return true;
        }

        if (json.has("tags") && json.get("tags").isJsonArray()) {
            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement tagElement : tags) {
                if (tagElement.isJsonObject() && containsUdtDefinitions(tagElement.getAsJsonObject())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static JsonObject exportTagsToJson(GatewayTagManager tagManager, String provider, String baseTagPath,
            boolean recursive, boolean localPropsOnly) throws Exception {
        if (provider == null) {
            provider = TagConfigUtilities.DEFAULT_PROVIDER;
        }
        if (baseTagPath == null) {
            baseTagPath = "";
        }

        // Normalize the base tag path
        baseTagPath = normalizeTagPath(baseTagPath);

        logger.info("Exporting tags from provider " + provider + " at " + baseTagPath + " (recursive=" + recursive +
                ", localPropsOnly=" + localPropsOnly + ")");

        // Validate configuration before proceeding
        validateExportConfiguration(tagManager, provider, baseTagPath, recursive, localPropsOnly);

        TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider,
                baseTagPath, recursive, localPropsOnly);
        JsonObject tagsJson = convertToJsonObject(tagConfigurationModel);

        // Final validation of the export result
        validateJsonStructure(tagsJson, baseTagPath, provider);

        return (JsonObject) FileUtilities.sortJsonElementRecursively(tagsJson);
    }

    public static void exportTagsToDisk(GatewayTagManager tagManager, String provider, String baseTagPath,
            boolean recursive, boolean localPropsOnly, String filePath, String exportMode,
            boolean deleteExisting, boolean excludeUdtDefinitions) throws IOException {

        // Normalize the base tag path
        if (baseTagPath == null) {
            baseTagPath = "";
        }
        baseTagPath = normalizeTagPath(baseTagPath);

        // Validate configuration before any file operations
        logger.info("Validating export configuration before proceeding...");
        validateExportConfiguration(tagManager, provider, baseTagPath, recursive, localPropsOnly);

        TagExportImportStrategy strategy = TagExportImportStrategyFactory.getInstance().getStrategy(exportMode);

        logger.info(
                "Starting tag export to disk using {} mode: provider={}, baseTagPath={}, filePath={}, recursive={}, localPropsOnly={}, deleteExisting={}, excludeUdtDefinitions={}",
                strategy.getExportMode().getDisplayName(), provider, baseTagPath, filePath, recursive, localPropsOnly,
                deleteExisting, excludeUdtDefinitions);

        try {
            strategy.exportTagsToDisk(tagManager, provider, baseTagPath, recursive, localPropsOnly, filePath,
                    deleteExisting, excludeUdtDefinitions);
        } catch (Exception e) {
            // If export fails, provide helpful error message
            if (e.getMessage().contains("Unknown")) {
                throw new IOException(
                        "Export failed due to Unknown tag type. This often occurs when trying to export " +
                                "from a non-existent path or when the path contains no valid tags. " +
                                "Please verify the tag path exists and contains the expected tags. " +
                                "Original error: " + e.getMessage(),
                        e);
            }
            throw e;
        }
    }

    /**
     * Provides suggestions for fixing common export path issues.
     * 
     * @param baseTagPath the problematic base tag path
     * @return a helpful suggestion message
     */
    public static String getExportPathSuggestion(String baseTagPath) {
        if (baseTagPath != null && baseTagPath.startsWith("_types_/")) {
            return "For UDT definition exports from _types_ subfolders:\n" +
                    "1. Verify the UDT folder exists and contains UDT definitions\n" +
                    "2. Use the exact folder path as shown in the tag browser\n" +
                    "3. Ensure UDT definitions exist in the specified location\n\n" +
                    "Alternative approaches:\n" +
                    "• Export entire provider (empty base path) with 'Include UDT Definitions'\n" +
                    "• Export from parent folders that contain the UDTs you need";
        }

        return "Ensure the base tag path points to a valid folder or tag within your provider.";
    }
}