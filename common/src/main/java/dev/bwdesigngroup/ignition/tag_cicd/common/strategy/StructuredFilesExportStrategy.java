package dev.bwdesigngroup.ignition.tag_cicd.common.strategy;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.FileUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagConfigUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagExportUtilities;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.BasicTagPath;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Strategy for exporting/importing tags in a structured format with tags.json
 * and udts.json files in each folder.
 * 
 * @author Keith Gamble
 */
public class StructuredFilesExportStrategy implements TagExportImportStrategy {
    private static final Logger logger = LoggerFactory.getLogger(StructuredFilesExportStrategy.class.getName());
    private static final String TAGS_FILE_NAME = "tags.json";
    private static final String UDTS_FILE_NAME = "udts.json";

    @Override
    public void exportTagsToDisk(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            boolean recursive,
            boolean localPropsOnly,
            String filePath,
            boolean deleteExisting,
            boolean excludeUdtDefinitions) throws IOException {
        try {
            logger.info(
                    "Exporting tags in structured format: provider={}, baseTagPath={}, filePath={}, recursive={}, deleteExisting={}",
                    provider, baseTagPath, filePath, recursive, deleteExisting);

            // Get tag configuration model
            TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(
                    tagManager, provider, baseTagPath, recursive, localPropsOnly);

            // Convert to JSON
            JsonObject tagsJson = TagExportUtilities.convertToJsonObject(tagConfigurationModel);

            // Ensure directory exists
            String directoryPath = ensureDirectoryPath(filePath);
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
                }
            }

            // Handle deleteExisting for structured files mode
            if (deleteExisting) {
                logger.info("Cleaning existing structured files in directory: {}", directoryPath);
                FileUtilities.cleanStructuredFilesDirectory(directoryPath);
            }

            // Export tags in structured format
            exportTagsInStructuredFormat(tagsJson, directoryPath, excludeUdtDefinitions);
            logger.info("Successfully exported tags in structured format to: {}", directoryPath);
        } catch (Exception e) {
            logger.error("Error exporting tags in structured format: {}", e.getMessage(), e);
            throw new IOException("Failed to export tags in structured format: " + e.getMessage(), e);
        }
    }

    @Override
    public JsonObject importTagsFromSource(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            String sourcePath,
            String collisionPolicy) throws IOException {
        JsonObject responseObject = new JsonObject();
        JsonObject createdTags = new JsonObject();
        JsonObject deletedTags = new JsonObject();

        logger.info(
                "Importing tags from structured format: provider={}, baseTagPath={}, sourcePath={}, collisionPolicy={}",
                provider, baseTagPath, sourcePath, collisionPolicy);

        File directory = new File(sourcePath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Source path is not a valid directory: " + sourcePath);
        }

        // Handle delete-and-replace policy
        boolean deleteTags = "d".equalsIgnoreCase(collisionPolicy);
        CollisionPolicy policy = CollisionPolicy.fromString(
                deleteTags ? "o" : (collisionPolicy.isEmpty() ? "a" : collisionPolicy));

        if (deleteTags) {
            logger.info("Deleting existing tags at {}/{}", provider, baseTagPath);
            try {
                TagPath tagPath = new BasicTagPath(provider,
                        baseTagPath.isEmpty() ? List.of() : List.of(baseTagPath.split("/")));
                TagConfigurationModel baseTagsConfig = TagConfigUtilities.getTagConfigurationModel(
                        tagManager, provider, baseTagPath, true, false);
                List<QualityCode> deletedQualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(
                        tagManager, provider, tagPath, baseTagsConfig);
                deletedTags.add(baseTagPath, TagConfigUtilities.convertQualityCodesToArray(deletedQualityCodes));
            } catch (Exception e) {
                logger.error("Failed to delete existing tags: {}", e.getMessage(), e);
                throw new IOException("Failed to delete existing tags: " + e.getMessage(), e);
            }
        }

        try {
            // First check for _types_ folder and import UDT types
            File typesFolder = new File(sourcePath + "/_types_");
            if (typesFolder.exists() && typesFolder.isDirectory()) {
                String typesBasePath = baseTagPath.isEmpty() ? "_types_" : baseTagPath + "/_types_";
                TagPath typesPath = new BasicTagPath(provider, List.of(typesBasePath.split("/")));

                // Import UDT types if they exist
                File udtTypesFile = new File(typesFolder, UDTS_FILE_NAME);
                if (udtTypesFile.exists() && udtTypesFile.isFile()) {
                    String fileContent = new String(Files.readAllBytes(udtTypesFile.toPath()));
                    JsonObject udtTypesJson = TagUtilities.stringToJson(fileContent).getAsJsonObject();

                    List<QualityCode> qualityCodes = tagManager
                            .importTagsAsync(typesPath, fileContent, "json", policy)
                            .join();
                    createdTags.add(typesPath.toString(),
                            TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
                }
            }

            // Then import all tags recursively
            importStructuredFiles(tagManager, provider, baseTagPath, sourcePath, policy, createdTags);
        } catch (Exception e) {
            logger.error("Failed to import tags from structured format: {}", e.getMessage(), e);
            throw new IOException("Failed to import tags from structured format: " + e.getMessage(), e);
        }

        // Add results to response
        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
        return responseObject;
    }

    @Override
    public ExportMode getExportMode() {
        return ExportMode.STRUCTURED_FILES;
    }

    // Helper methods
    private String ensureDirectoryPath(String filePath) {
        if (filePath.contains(".") && !filePath.endsWith("/")) {
            return filePath.substring(0, filePath.lastIndexOf("/"));
        }
        return filePath.endsWith("/") ? filePath : filePath + "/";
    }

    private void exportTagsInStructuredFormat(JsonObject json, String basePath, boolean excludeUdtDefinitions)
            throws IOException {
        if (!json.has("tags")) {
            return;
        }

        // Split tags and UDT instances in the current folder
        JsonArray regularTags = new JsonArray();
        JsonArray udtInstances = new JsonArray();
        JsonArray subfolderTags = new JsonArray();

        JsonArray tags = json.getAsJsonArray("tags");
        for (JsonElement tagElement : tags) {
            JsonObject tagObject = tagElement.getAsJsonObject();
            String tagType = tagObject.get("tagType").getAsString();
            String tagName = tagObject.get("name").getAsString();

            // Handle _types_ folder specially
            if ("_types_".equals(tagName) && "Folder".equals(tagType)) {
                if (!excludeUdtDefinitions) {
                    String typesPath = basePath + "/" + tagName;
                    File typesDir = new File(typesPath);
                    typesDir.mkdirs();

                    // Export UDT definitions to the _types_ folder
                    JsonArray udtDefinitions = new JsonArray();
                    if (tagObject.has("tags")) {
                        JsonArray typesTags = tagObject.getAsJsonArray("tags");
                        for (JsonElement typeElement : typesTags) {
                            udtDefinitions.add(typeElement);
                        }
                    }

                    if (udtDefinitions.size() > 0) {
                        JsonObject udtDefinitionsJson = new JsonObject();
                        udtDefinitionsJson.add("tags", udtDefinitions);
                        udtDefinitionsJson = (JsonObject) FileUtilities.sortJsonElementRecursively(udtDefinitionsJson);
                        FileUtilities.saveJsonToFile(udtDefinitionsJson, typesPath + "/" + UDTS_FILE_NAME);
                    }
                }
                continue;
            }

            // Process regular folder
            if ("Folder".equals(tagType)) {
                String folderPath = basePath + "/" + tagName;
                File folder = new File(folderPath);
                folder.mkdirs();
                exportTagsInStructuredFormat(tagObject, folderPath, excludeUdtDefinitions);
                subfolderTags.add(tagObject);
            }
            // Process UDT instances
            else if ("UdtInstance".equals(tagType)) {
                udtInstances.add(tagObject);
            }
            // Process regular tags
            else {
                regularTags.add(tagObject);
            }
        }

        // Save regularTags to tags.json if there are any
        if (regularTags.size() > 0) {
            JsonObject tagsJson = new JsonObject();
            tagsJson.add("tags", regularTags);
            tagsJson = (JsonObject) FileUtilities.sortJsonElementRecursively(tagsJson);
            FileUtilities.saveJsonToFile(tagsJson, basePath + "/" + TAGS_FILE_NAME);
        }

        // Save UDT instances to udts.json if there are any
        if (udtInstances.size() > 0) {
            JsonObject udtsJson = new JsonObject();
            udtsJson.add("tags", udtInstances);
            udtsJson = (JsonObject) FileUtilities.sortJsonElementRecursively(udtsJson);
            FileUtilities.saveJsonToFile(udtsJson, basePath + "/" + UDTS_FILE_NAME);
        }
    }

    private void importStructuredFiles(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            String sourcePath,
            CollisionPolicy policy,
            JsonObject createdTags) throws IOException {
        // Create tag path
        List<String> pathComponents = new ArrayList<>();
        if (!baseTagPath.isEmpty()) {
            pathComponents.addAll(List.of(baseTagPath.split("/")));
        }
        TagPath basePath = new BasicTagPath(provider, pathComponents);

        // Import tags.json if it exists
        File tagsFile = new File(sourcePath, TAGS_FILE_NAME);
        if (tagsFile.exists() && tagsFile.isFile()) {
            String fileContent = new String(Files.readAllBytes(tagsFile.toPath()));
            JsonObject tagsJson = TagUtilities.stringToJson(fileContent).getAsJsonObject();

            List<QualityCode> qualityCodes = tagManager
                    .importTagsAsync(basePath, fileContent, "json", policy)
                    .join();
            createdTags.add(basePath.toString() + "/tags",
                    TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
        }

        // Import udts.json if it exists
        File udtsFile = new File(sourcePath, UDTS_FILE_NAME);
        if (udtsFile.exists() && udtsFile.isFile()) {
            String fileContent = new String(Files.readAllBytes(udtsFile.toPath()));
            JsonObject udtsJson = TagUtilities.stringToJson(fileContent).getAsJsonObject();

            List<QualityCode> qualityCodes = tagManager
                    .importTagsAsync(basePath, fileContent, "json", policy)
                    .join();
            createdTags.add(basePath.toString() + "/udts",
                    TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
        }

        // Recursively process subdirectories
        try (Stream<Path> paths = Files.list(Paths.get(sourcePath))) {
            List<Path> subdirectories = paths
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());

            for (Path subdirectory : subdirectories) {
                String folderName = subdirectory.getFileName().toString();
                // Skip _types_ folder as it's already handled separately
                if ("_types_".equals(folderName)) {
                    continue;
                }

                String childPath = baseTagPath.isEmpty() ? folderName : baseTagPath + "/" + folderName;
                importStructuredFiles(tagManager, provider, childPath, subdirectory.toString(), policy, createdTags);
            }
        }
    }
}