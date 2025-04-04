package dev.kgamble.ignition.tag_cicd.common.strategy;

import dev.kgamble.ignition.tag_cicd.common.model.ExportMode;
import dev.kgamble.ignition.tag_cicd.common.util.FileUtilities;
import dev.kgamble.ignition.tag_cicd.common.util.TagConfigUtilities;
import dev.kgamble.ignition.tag_cicd.common.util.TagExportUtilities;
import dev.kgamble.ignition.tag_cicd.common.util.TagImportUtilities;
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
 * Strategy for exporting/importing tags as individual files in a folder
 * structure.
 * 
 * @author You
 */
public class IndividualFilesExportStrategy implements TagExportImportStrategy {
    private static final Logger logger = LoggerFactory.getLogger(IndividualFilesExportStrategy.class.getName());

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
                    "Exporting tags as individual files: provider={}, baseTagPath={}, filePath={}, recursive={}",
                    provider, baseTagPath, filePath, recursive);

            TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(
                    tagManager, provider, baseTagPath, recursive, localPropsOnly);
            JsonObject tagsJson = TagExportUtilities.convertToJsonObject(tagConfigurationModel);

            String directoryPath = ensureDirectoryPath(filePath);
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
                }
            }

            if (deleteExisting) {
                FileUtilities.deleteExistingFiles(directoryPath, tagsJson);
            }

            saveTagsAsIndividualFiles(tagsJson, directoryPath, excludeUdtDefinitions);
            logger.info("Successfully exported tags as individual files to: {}", directoryPath);
        } catch (Exception e) {
            logger.error("Error exporting tags as individual files: {}", e.getMessage(), e);
            throw new IOException("Failed to export tags as individual files: " + e.getMessage(), e);
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
                "Importing tags from directory: provider={}, baseTagPath={}, sourcePath={}, collisionPolicy={}",
                provider, baseTagPath, sourcePath, collisionPolicy);

        File directory = new File(sourcePath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Source path is not a valid directory: " + sourcePath);
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
            JsonObject tagsJson = TagImportUtilities.readTagsFromDirectory(sourcePath, "");

            // Import UDT types first, sorted by dependencies
            JsonObject typesFolder = TagImportUtilities.findTypesFolder(tagsJson);
            if (typesFolder != null) {
                String typesBasePath = baseTagPath.isEmpty() ? "_types_" : baseTagPath + "/_types_";
                TagPath typesPath = new BasicTagPath(provider, List.of(typesBasePath.split("/")));

                List<JsonObject> sortedUdtTypes = sortUdtTypesByDependencies(typesFolder.getAsJsonArray("tags"));

                for (JsonObject udtTypeObject : sortedUdtTypes) {
                    String udtName = udtTypeObject.get("name").getAsString();
                    List<QualityCode> qualityCodes = tagManager
                            .importTagsAsync(typesPath, TagUtilities.jsonToString(udtTypeObject), "json", policy)
                            .join();
                    createdTags.add(typesPath.toString() + "/" + udtName,
                            TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
                }

                tagsJson.getAsJsonArray("tags").remove(typesFolder);
            }

            importTagsRecursively(tagManager, provider, baseTagPath, policy, createdTags, tagsJson);
        } catch (Exception e) {
            logger.error("Failed to import tags from directory: {}", e.getMessage(), e);
            throw new IOException("Failed to import tags from directory: " + e.getMessage(), e);
        }

        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
        return responseObject;
    }

    @Override
    public ExportMode getExportMode() {
        return ExportMode.INDIVIDUAL_FILES;
    }

    private String ensureDirectoryPath(String filePath) {
        if (filePath.contains(".") && !filePath.endsWith("/")) {
            return filePath.substring(0, filePath.lastIndexOf("/"));
        }
        return filePath.endsWith("/") ? filePath : filePath + "/";
    }

    private void saveTagsAsIndividualFiles(JsonObject json, String baseFilePath, boolean excludeUdtDefinitions)
            throws IOException {
        if (json.has("tags")) {
            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement tag : tags) {
                JsonObject tagObject = tag.getAsJsonObject();
                String tagType = tagObject.get("tagType").getAsString();
                String tagName = tagObject.get("name").getAsString();

                if (excludeUdtDefinitions && "_types_".equals(tagName) && "Folder".equals(tagType)) {
                    logger.info("Skipping _types_ folder due to excludeUdtDefinitions=true");
                    continue;
                }

                if ("_types_".equals(tagName) && "Folder".equals(tagType)) {
                    JsonArray typesTags = tagObject.getAsJsonArray("tags");
                    if (typesTags == null || typesTags.size() == 0) {
                        logger.info("Skipping _types_ folder because it is empty");
                        continue;
                    }
                }

                if ("Folder".equals(tagType) || "Provider".equals(tagType)) {
                    String folderPath = baseFilePath + tagName + "/";
                    File folder = new File(folderPath);
                    folder.mkdirs();
                    saveTagsAsIndividualFiles(tagObject, folderPath, excludeUdtDefinitions);
                } else {
                    tagObject = (JsonObject) FileUtilities.sortJsonElementRecursively(tagObject);
                    FileUtilities.saveJsonToFile(tagObject, baseFilePath + tagName + ".json");
                }
            }
        } else {
            FileUtilities.saveJsonToFile(json, baseFilePath + ".json");
        }
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

        return sortedUdts;
    }

    private void findUdtDependencies(JsonObject jsonObject, Set<String> dependencies) {
        if (jsonObject.has("tagType") &&
                jsonObject.get("tagType").getAsString().equals("UdtInstance") &&
                jsonObject.has("typeId")) {
            dependencies.add(jsonObject.get("typeId").getAsString());
        }

        if (jsonObject.has("tags")) {
            JsonArray tags = jsonObject.getAsJsonArray("tags");
            for (JsonElement tagElement : tags) {
                if (tagElement.isJsonObject()) {
                    findUdtDependencies(tagElement.getAsJsonObject(), dependencies);
                }
            }
        }

        if (jsonObject.has("parameters")) {
            JsonArray parameters = jsonObject.getAsJsonArray("parameters");
            for (JsonElement paramElement : parameters) {
                if (paramElement.isJsonObject()) {
                    JsonObject param = paramElement.getAsJsonObject();
                    if (param.has("value") && param.get("value").isJsonObject()) {
                        findUdtDependencies(param.get("value").getAsJsonObject(), dependencies);
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
            logger.warn("Circular dependency detected in UDT types involving: " + udtName);
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
            JsonObject tagsJson) {
        List<String> pathComponents = new ArrayList<>();
        if (!baseTagPath.isEmpty()) {
            pathComponents.addAll(List.of(baseTagPath.split("/")));
        }
        TagPath basePath = new BasicTagPath(provider, pathComponents);

        JsonArray tags = tagsJson.getAsJsonArray("tags");
        if (tags == null || tags.size() == 0) {
            return;
        }

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
                } catch (Exception e) {
                    logger.error("Error importing tag {}: {}", tagName, e.getMessage(), e);
                }
            }
        }
    }
}