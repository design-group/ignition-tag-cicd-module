package com.bwdesigngroup.ignition.tag_cicd.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A utility class for importing tags.
 *
 * @author Keith Gamble
 */
public class TagImportUtilities {
    private static final Logger logger = LoggerFactory.getLogger(TagImportUtilities.class.getName());

    public static JsonObject importTagsFromSource(GatewayTagManager tagManager, String provider, String baseTagPath,
            String sourcePath, String collisionPolicy, boolean individualFilesPerObject) throws IOException {
        JsonObject responseObject = new JsonObject();
        JsonObject createdTags = new JsonObject();
        JsonObject deletedTags = new JsonObject();

        if (baseTagPath == null) {
            baseTagPath = "";
        }

        boolean deleteTags = "d".equalsIgnoreCase(collisionPolicy);
        CollisionPolicy policy = CollisionPolicy
                .fromString(deleteTags ? "o" : (collisionPolicy.isEmpty() ? "a" : collisionPolicy));

        if (deleteTags) {
            logger.info("Deleting all tags in provider " + provider + " at " + baseTagPath + " before importing");
            TagPath tagPath = new BasicTagPath(provider, baseTagPath.isEmpty() ? List.of() : List.of(baseTagPath));
            TagConfigurationModel baseTagsConfig = TagConfigUtilities.getTagConfigurationModel(tagManager, provider,
                    baseTagPath, true, false);
            List<QualityCode> deletedQualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(tagManager,
                    provider, tagPath, baseTagsConfig);
            deletedTags.add(baseTagPath, TagConfigUtilities.convertQualityCodesToArray(deletedQualityCodes));
        }

        if (individualFilesPerObject) {
            importTagsFromDirectory(tagManager, provider, baseTagPath, sourcePath, policy, createdTags);
        } else {
            importTagsFromFile(tagManager, provider, baseTagPath, sourcePath, policy, createdTags);
        }

        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
        TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");

        return responseObject;
    }

    private static void importTagsFromFile(GatewayTagManager tagManager, String provider, String baseTagPath,
            String filePath, CollisionPolicy collisionPolicy, JsonObject createdTags) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Source path is not a valid file: " + filePath);
        }

        String jsonContent = FileUtilities.readFileAsString(file);
        JsonObject tagConfigurationJson = (JsonObject) TagUtilities.stringToJson(jsonContent);
        importTagsRecursively(tagManager, provider, baseTagPath, collisionPolicy, createdTags, tagConfigurationJson);
    }

    private static void importTagsFromDirectory(GatewayTagManager tagManager, String provider, String baseTagPath,
            String directoryPath, CollisionPolicy collisionPolicy, JsonObject createdTags) throws IOException {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Source path is not a valid directory: " + directoryPath);
        }

        // Read the entire directory structure
        JsonObject tagsJson = readTagsFromDirectory(directoryPath, "");

        // Import UDT types first, sorted by dependencies
        JsonObject typesFolder = findTypesFolder(tagsJson);
        if (typesFolder != null) {
            String typesBasePath = baseTagPath.isEmpty() ? "_types_" : baseTagPath + "/_types_";
            TagPath typesPath = new BasicTagPath(provider, List.of(typesBasePath.split("/")));
            List<JsonObject> sortedUdtTypes = sortUdtTypesByDependencies(typesFolder.getAsJsonArray("tags"));

            for (JsonObject udtTypeObject : sortedUdtTypes) {
                String udtName = udtTypeObject.get("name").getAsString();
                List<QualityCode> qualityCodes = tagManager
                        .importTagsAsync(typesPath, TagUtilities.jsonToString(udtTypeObject), "json", collisionPolicy)
                        .join();
                createdTags.add(typesPath.toString() + "/" + udtName,
                        TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
            }
            // Remove _types_ from tagsJson to avoid re-importing
            tagsJson.getAsJsonArray("tags").remove(typesFolder);
        }

        // Import the remaining tags with folder structure
        importTagsRecursively(tagManager, provider, baseTagPath, collisionPolicy, createdTags, tagsJson);
    }

    private static List<JsonObject> sortUdtTypesByDependencies(JsonArray udtTypesArray) {
        // Map of UDT name to its JsonObject
        Map<String, JsonObject> udtMap = new HashMap<>();
        // Map of UDT name to its dependencies (typeIds it references)
        Map<String, Set<String>> dependencies = new HashMap<>();
        // All UDT names encountered
        Set<String> allUdtNames = new HashSet<>();

        // Populate the maps
        for (JsonElement element : udtTypesArray) {
            JsonObject udt = element.getAsJsonObject();
            String udtName = udt.get("name").getAsString();
            udtMap.put(udtName, udt);
            allUdtNames.add(udtName);

            Set<String> deps = new HashSet<>();
            findDependencies(udt, deps);
            dependencies.put(udtName, deps);
        }

        // Perform topological sort
        List<String> sortedNames = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> tempMark = new HashSet<>(); // For cycle detection

        for (String udtName : allUdtNames) {
            if (!visited.contains(udtName)) {
                topologicalSort(udtName, dependencies, visited, tempMark, sortedNames);
            }
        }

        // Convert sorted names back to JsonObjects
        List<JsonObject> sortedUdts = new ArrayList<>();
        for (String name : sortedNames) {
            sortedUdts.add(udtMap.get(name));
        }
        return sortedUdts;
    }

    private static void findDependencies(JsonObject udt, Set<String> dependencies) {
        // Check top-level typeId
        if (udt.has("typeId") && !udt.get("typeId").getAsString().isEmpty()) {
            dependencies.add(udt.get("typeId").getAsString());
        }

        // Recursively check nested tags
        JsonArray tags = udt.getAsJsonArray("tags");
        if (tags != null) {
            for (JsonElement tagElement : tags) {
                JsonObject tag = tagElement.getAsJsonObject();
                if (tag.has("typeId") && !tag.get("typeId").getAsString().isEmpty()) {
                    dependencies.add(tag.get("typeId").getAsString());
                }
                findDependencies(tag, dependencies); // Recursive call for nested UDTs
            }
        }
    }

    private static void topologicalSort(String udtName, Map<String, Set<String>> dependencies,
            Set<String> visited, Set<String> tempMark, List<String> sortedNames) {
        if (tempMark.contains(udtName)) {
            logger.warn("Circular dependency detected involving UDT: " + udtName);
            return; // Skip circular dependencies
        }
        if (visited.contains(udtName)) {
            return;
        }

        tempMark.add(udtName);
        Set<String> deps = dependencies.getOrDefault(udtName, new HashSet<>());
        for (String dep : deps) {
            if (dependencies.containsKey(dep)) { // Only sort known UDTs within _types_
                topologicalSort(dep, dependencies, visited, tempMark, sortedNames);
            }
        }
        tempMark.remove(udtName);
        visited.add(udtName);
        sortedNames.add(udtName);
    }

    private static void importTagsRecursively(GatewayTagManager tagManager, String provider, String baseTagPath,
            CollisionPolicy collisionPolicy, JsonObject createdTags, JsonObject tagsJson) {
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

                List<QualityCode> qualityCodes = tagManager
                        .importTagsAsync(basePath, TagUtilities.jsonToString(singleTagJson), "json", collisionPolicy)
                        .join();
                createdTags.add(basePath.toString() + "/" + tagName,
                        TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
            }
        }
    }

    public static JsonObject readTagsFromDirectory(String directoryPath, String relativePath) throws IOException {
        logger.trace("Reading tags from directory: " + directoryPath + " with relative path: " + relativePath);
        Path path = Paths.get(directoryPath);
        JsonObject folderObject = new JsonObject();
        JsonArray tagsArray = new JsonArray();

        try (Stream<Path> paths = Files.walk(path, 1)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String content = new String(Files.readAllBytes(file));
                            JsonObject tagObject = TagUtilities.stringToJson(content).getAsJsonObject();
                            tagsArray.add(tagObject);
                        } catch (IOException e) {
                            logger.error("Error reading file: " + file.toString(), e);
                        }
                    });

            Files.list(path)
                    .filter(Files::isDirectory)
                    .filter(subDir -> !subDir.equals(path))
                    .forEach(subDir -> {
                        try {
                            String subDirName = subDir.getFileName().toString();
                            String newRelativePath = relativePath.isEmpty() ? subDirName
                                    : relativePath + "/" + subDirName;
                            JsonObject subFolder = readTagsFromDirectory(subDir.toString(), newRelativePath);
                            subFolder.addProperty("name", subDirName);
                            subFolder.addProperty("tagType", "Folder");
                            tagsArray.add(subFolder);
                        } catch (IOException e) {
                            logger.error("Error reading directory: " + subDir.toString(), e);
                        }
                    });
        }

        folderObject.add("tags", tagsArray);
        return folderObject;
    }

    public static JsonObject findTypesFolder(JsonObject tagsJson) {
        JsonArray tags = tagsJson.getAsJsonArray("tags");
        if (tags != null) {
            for (JsonElement tag : tags) {
                JsonObject tagObject = tag.getAsJsonObject();
                if ("_types_".equals(tagObject.get("name").getAsString())
                        && "Folder".equals(tagObject.get("tagType").getAsString())) {
                    return tagObject;
                }
            }
        }
        return null;
    }
}