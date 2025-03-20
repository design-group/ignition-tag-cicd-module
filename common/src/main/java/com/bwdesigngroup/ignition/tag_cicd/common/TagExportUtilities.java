package com.bwdesigngroup.ignition.tag_cicd.common;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * A utility class for exporting tags.
 *
 * @author Keith Gamble
 */
public class TagExportUtilities {
    private static final Logger logger = LoggerFactory.getLogger(TagExportUtilities.class.getName());

    /**
     * Exports tag configuration to a JSON object.
     *
     * @param tagManager     The GatewayTagManager instance.
     * @param provider       The tag provider name.
     * @param baseTagPath    The base tag path to export (can be empty).
     * @param recursive      Whether to export tags recursively.
     * @param localPropsOnly Whether to export only local properties.
     * @return A JsonObject containing the exported tag configuration.
     * @throws Exception If an error occurs during export.
     */
    public static JsonObject exportTagsToJson(GatewayTagManager tagManager, String provider, String baseTagPath,
            boolean recursive, boolean localPropsOnly) throws Exception {
        if (provider == null) {
            provider = TagConfigUtilities.DEFAULT_PROVIDER;
        }
        if (baseTagPath == null) {
            baseTagPath = "";
        }

        logger.info("Exporting tags from provider " + provider + " at " + baseTagPath + " (recursive=" + recursive +
                ", localPropsOnly=" + localPropsOnly + ")");

        TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider,
                baseTagPath, recursive, localPropsOnly);
        JsonObject tagsJson = TagUtilities.toJsonObject(tagConfigurationModel);
        return (JsonObject) FileUtilities.sortJsonElementRecursively(tagsJson);
    }

    /**
     * Exports tag configuration to disk.
     *
     * @param tagManager               The GatewayTagManager instance.
     * @param provider                 The tag provider name.
     * @param baseTagPath              The base tag path to export (can be empty).
     * @param recursive                Whether to export tags recursively.
     * @param localPropsOnly           Whether to export only local properties.
     * @param filePath                 The file or directory path to save the
     *                                 export.
     * @param individualFilesPerObject Whether to save each tag as a separate file.
     * @param deleteExisting           Whether to delete existing files before
     *                                 export.
     * @throws IOException If an error occurs during file operations.
     */
    public static void exportTagsToDisk(GatewayTagManager tagManager, String provider, String baseTagPath,
            boolean recursive, boolean localPropsOnly, String filePath,
            boolean individualFilesPerObject, boolean deleteExisting) throws IOException {
        JsonObject tagsJson = null;
        try {
            tagsJson = exportTagsToJson(tagManager, provider, baseTagPath, recursive, localPropsOnly);
        } catch (Exception e) {
            throw new IOException("Failed to export tags to JSON: " + e.getMessage(), e);
        }

        String directoryPath = ensureDirectoryPath(filePath);
        File directory = new File(directoryPath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }

        if (deleteExisting) {
            logger.info("Deleting existing files in " + directoryPath);
            FileUtilities.deleteExistingFiles(directoryPath, tagsJson, individualFilesPerObject);
        }

        if (individualFilesPerObject) {
            saveTagsAsIndividualFiles(tagsJson, directoryPath);
        } else {
            FileUtilities.saveJsonToFile(tagsJson, filePath);
        }
    }

    private static String ensureDirectoryPath(String filePath) {
        if (filePath.contains(".") && !filePath.endsWith("/")) {
            return filePath.substring(0, filePath.lastIndexOf("/"));
        }
        return filePath.endsWith("/") ? filePath : filePath + "/";
    }

    private static void saveTagsAsIndividualFiles(JsonObject json, String baseFilePath) throws IOException {
        if (json.has("tags")) {
            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement tag : tags) {
                JsonObject tagObject = tag.getAsJsonObject();
                String tagType = tagObject.get("tagType").getAsString();
                String tagName = tagObject.get("name").getAsString();

                if ("Folder".equals(tagType) || "Provider".equals(tagType)) {
                    String folderPath = baseFilePath + tagName;
                    File folder = new File(folderPath);
                    folder.mkdir();
                    saveTagsAsIndividualFiles(tagObject, folderPath);
                } else {
                    FileUtilities.saveJsonToFile(tagObject, baseFilePath + tagName + ".json");
                }
            }
        } else {
            FileUtilities.saveJsonToFile(json, baseFilePath + ".json");
        }
    }
}