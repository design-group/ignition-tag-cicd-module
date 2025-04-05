package dev.bwdesigngroup.ignition.tag_cicd.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.ignition.tag_cicd.common.strategy.TagExportImportStrategy;
import dev.bwdesigngroup.ignition.tag_cicd.common.strategy.TagExportImportStrategyFactory;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class TagImportUtilities {
    private static final Logger logger = LoggerFactory.getLogger(TagImportUtilities.class.getName());

    public static JsonObject importTagsFromSource(
            GatewayTagManager tagManager,
            String provider,
            String baseTagPath,
            String sourcePath,
            String collisionPolicy,
            String exportMode) throws IOException {

        TagExportImportStrategy strategy = TagExportImportStrategyFactory.getInstance().getStrategy(exportMode);

        logger.info(
                "Starting tag import using {} mode: provider={}, baseTagPath={}, sourcePath={}, collisionPolicy={}",
                strategy.getExportMode().getDisplayName(), provider, baseTagPath, sourcePath, collisionPolicy);

        return strategy.importTagsFromSource(tagManager, provider, baseTagPath, sourcePath, collisionPolicy);
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
                            JsonObject tagObject = com.inductiveautomation.ignition.common.tags.TagUtilities
                                    .stringToJson(content).getAsJsonObject();
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