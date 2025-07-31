package dev.bwdesigngroup.ignition.tag_cicd.common.strategy;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.FileUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagConfigUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagExportUtilities;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy for exporting/importing tags as a single JSON file.
 * 
 * @author Keith Gamble
 */
public class SingleFileExportStrategy implements TagExportImportStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SingleFileExportStrategy.class.getName());

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
                    "Exporting tags as single file: provider={}, baseTagPath={}, filePath={}, recursive={}, deleteExisting={}",
                    provider, baseTagPath, filePath, recursive, deleteExisting);

            TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(
                    tagManager, provider, baseTagPath, recursive, localPropsOnly);
            JsonObject tagsJson = TagExportUtilities.convertToJsonObject(tagConfigurationModel);

            if (excludeUdtDefinitions) {
                tagsJson = TagExportUtilities.removeUdtDefinitions(tagsJson);
            }

            tagsJson = (JsonObject) FileUtilities.sortJsonElementRecursively(tagsJson);

            File file = new File(filePath);
            File parentDir = file.getParentFile();

            // Handle deleteExisting for single file mode
            if (deleteExisting && file.exists()) {
                logger.info("Deleting existing file: {}", file.getAbsolutePath());
                if (!file.delete()) {
                    logger.warn("Failed to delete existing file: {}", file.getAbsolutePath());
                }
            }

            // Create parent directory if it doesn't exist
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            FileUtilities.saveJsonToFile(tagsJson, filePath);
            logger.info("Successfully exported tags to single file: {}", filePath);
        } catch (Exception e) {
            logger.error("Error exporting tags to single file: {}", e.getMessage(), e);
            throw new IOException("Failed to export tags to single file: " + e.getMessage(), e);
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
                "Importing tags from single file: provider={}, baseTagPath={}, sourcePath={}, collisionPolicy={}",
                provider, baseTagPath, sourcePath, collisionPolicy);

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException("Source path is not a valid file: " + sourcePath);
        }

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
            String fileContent = FileUtilities.readFileAsString(sourceFile);
            JsonObject tagsJson = TagUtilities.stringToJson(fileContent).getAsJsonObject();

            JsonArray tagsArray = tagsJson.getAsJsonArray("tags");
            if (tagsArray != null) {
                // Step 1: Import UDT definitions from _types_
                JsonObject typesFolder = findTypesFolder(tagsJson);
                if (typesFolder != null) {
                    String typesBasePath = "_types_"; // Always import UDTs to _types_
                    TagPath typesPath = new BasicTagPath(provider, List.of(typesBasePath.split("/")));
                    JsonArray udtTypesArray = typesFolder.getAsJsonArray("tags");

                    if (udtTypesArray != null && udtTypesArray.size() > 0) {
                        List<JsonObject> sortedUdtTypes = sortUdtTypesByDependencies(udtTypesArray);

                        logger.debug("Importing UDTs to {}", typesPath);
                        for (JsonObject udtType : sortedUdtTypes) {
                            JsonObject singleUdtJson = new JsonObject();
                            singleUdtJson.add("tags", new JsonArray());
                            singleUdtJson.getAsJsonArray("tags").add(udtType);
                            List<QualityCode> qualityCodes = tagManager
                                    .importTagsAsync(typesPath, TagUtilities.jsonToString(singleUdtJson), "json",
                                            policy)
                                    .join();
                            String udtName = udtType.get("name").getAsString();
                            JsonArray qualityArray = TagConfigUtilities.convertQualityCodesToArray(qualityCodes);
                            createdTags.add(typesPath.toString() + "/" + udtName, qualityArray);
                            logger.debug("Imported UDT {} with result: {}", udtName, qualityArray);
                            if (!qualityArray.toString().contains("Good")) {
                                logger.warn("UDT {} import may have failed: {}", udtName, qualityArray);
                            }
                        }
                    }

                    // Remove _types_ from tagsArray
                    tagsArray.remove(typesFolder);
                }

                // Step 2: Import remaining tags with folder structure
                logger.debug("Importing remaining tags from {}", baseTagPath);
                importTagsRecursively(tagManager, provider, baseTagPath, policy, createdTags, tagsJson);
            } else {
                // Fallback for flat JSON
                TagPath basePath = new BasicTagPath(provider,
                        baseTagPath.isEmpty() ? List.of() : List.of(baseTagPath.split("/")));
                List<QualityCode> qualityCodes = tagManager
                        .importTagsAsync(basePath, fileContent, "json", policy)
                        .join();
                createdTags.add(basePath.toString(), TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
                logger.debug("Imported flat JSON to {} with result: {}", basePath, qualityCodes);
            }
        } catch (Exception e) {
            logger.error("Failed to import tags from file: {}", e.getMessage(), e);
            throw new IOException("Failed to import tags from file: " + e.getMessage(), e);
        }

        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
        return responseObject;
    }

    @Override
    public ExportMode getExportMode() {
        return ExportMode.SINGLE_FILE;
    }

    private JsonObject findTypesFolder(JsonObject tagsJson) {
        JsonArray tags = tagsJson.getAsJsonArray("tags");
        if (tags != null) {
            for (JsonElement tag : tags) {
                JsonObject tagObject = tag.getAsJsonObject();
                if ("_types_".equals(tagObject.get("name").getAsString()) &&
                        "Folder".equals(tagObject.get("tagType").getAsString())) {
                    return tagObject;
                }
            }
        }
        return null;
    }

    private List<JsonObject> sortUdtTypesByDependencies(JsonArray udtTypesArray) {
        Map<String, JsonObject> udtTypesMap = new HashMap<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        Set<String> allUdtNames = new HashSet<>();

        for (JsonElement element : udtTypesArray) {
            JsonObject udtType = element.getAsJsonObject();
            String udtName = udtType.get("name").getAsString();
            udtTypesMap.put(udtName, udtType);
            allUdtNames.add(udtName);
            Set<String> deps = new HashSet<>();
            findUdtDependencies(udtType, deps);
            dependencies.put(udtName, deps);
        }

        List<String> sortedNames = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> tempMarks = new HashSet<>();

        for (String udtName : allUdtNames) {
            if (!visited.contains(udtName)) {
                topologicalSortUdts(udtName, dependencies, visited, tempMarks, sortedNames);
            }
        }

        List<JsonObject> sortedUdts = new ArrayList<>();
        for (String name : sortedNames) {
            JsonObject udtObject = udtTypesMap.get(name);
            if (udtObject != null) {
                sortedUdts.add(udtObject);
            }
        }

        logger.debug("Sorted UDTs: {}", sortedUdts.stream().map(obj -> obj.get("name").getAsString()).toList());
        return sortedUdts;
    }

    private void findUdtDependencies(JsonObject jsonObject, Set<String> dependencies) {
        if (jsonObject.has("tagType") &&
                "UdtInstance".equals(jsonObject.get("tagType").getAsString()) &&
                jsonObject.has("typeId")) {
            String typeId = jsonObject.get("typeId").getAsString();
            if (!typeId.isEmpty()) {
                dependencies.add(typeId);
            }
        }

        if (jsonObject.has("tags") && jsonObject.get("tags").isJsonArray()) {
            JsonArray tags = jsonObject.getAsJsonArray("tags");
            for (JsonElement tagElement : tags) {
                if (tagElement.isJsonObject()) {
                    findUdtDependencies(tagElement.getAsJsonObject(), dependencies);
                }
            }
        }

        if (jsonObject.has("parameters")) {
            JsonElement parametersElement = jsonObject.get("parameters");

            // Handle both JsonArray and JsonObject cases for parameters
            if (parametersElement.isJsonArray()) {
                JsonArray parameters = parametersElement.getAsJsonArray();
                for (JsonElement paramElement : parameters) {
                    if (paramElement.isJsonObject()) {
                        JsonObject param = paramElement.getAsJsonObject();
                        if (param.has("value") && param.get("value").isJsonObject()) {
                            findUdtDependencies(param.get("value").getAsJsonObject(), dependencies);
                        }
                    }
                }
            } else if (parametersElement.isJsonObject()) {
                JsonObject parameters = parametersElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : parameters.entrySet()) {
                    JsonElement paramValue = entry.getValue();
                    if (paramValue.isJsonObject()) {
                        findUdtDependencies(paramValue.getAsJsonObject(), dependencies);
                    }
                }
            }
        }
    }

    private void topologicalSortUdts(
            String udtName,
            Map<String, Set<String>> dependencies,
            Set<String> visited,
            Set<String> tempMarks,
            List<String> sortedNames) {
        if (tempMarks.contains(udtName)) {
            logger.warn("Circular dependency detected in UDT types involving: {}", udtName);
            return;
        }

        if (visited.contains(udtName)) {
            return;
        }

        tempMarks.add(udtName);

        Set<String> deps = dependencies.getOrDefault(udtName, Collections.emptySet());
        for (String dep : deps) {
            if (dependencies.containsKey(dep)) {
                topologicalSortUdts(dep, dependencies, visited, tempMarks, sortedNames);
            }
        }

        tempMarks.remove(udtName);
        visited.add(udtName);
        sortedNames.add(udtName);
    }

    private void importTagsRecursively(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            CollisionPolicy collisionPolicy,
            JsonObject createdTags,
            JsonObject tagsJson) throws IOException {
        JsonArray tags = tagsJson.getAsJsonArray("tags");
        if (tags == null || tags.size() == 0) {
            return;
        }

        List<String> pathComponents = new ArrayList<>();
        if (!baseTagPath.isEmpty()) {
            pathComponents.addAll(List.of(baseTagPath.split("/")));
        }
        TagPath basePath = new BasicTagPath(provider, pathComponents);

        for (JsonElement tagElement : tags) {
            JsonObject tagObject = tagElement.getAsJsonObject();
            String tagType = tagObject.get("tagType").getAsString();
            String tagName = tagObject.get("name").getAsString();

            if ("Folder".equals(tagType)) {
                String folderPath = baseTagPath.isEmpty() ? tagName : baseTagPath + "/" + tagName;
                importTagsRecursively(tagManager, provider, folderPath, collisionPolicy, createdTags, tagObject);
            } else {
                JsonObject singleTagJson = new JsonObject();
                singleTagJson.add("tags", new JsonArray());
                singleTagJson.getAsJsonArray("tags").add(tagObject);

                try {
                    List<QualityCode> qualityCodes = tagManager
                            .importTagsAsync(basePath, TagUtilities.jsonToString(singleTagJson), "json",
                                    collisionPolicy)
                            .join();
                    createdTags.add(basePath.toString() + "/" + tagName,
                            TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
                    logger.debug("Imported tag {} to {} with result: {}", tagName, basePath, qualityCodes);
                } catch (Exception e) {
                    logger.error("Error importing tag {}: {}", tagName, e.getMessage(), e);
                }
            }
        }
    }
}