package dev.kgamble.ignition.tag_cicd.common.util;

import dev.kgamble.ignition.tag_cicd.common.model.ExportMode;
import dev.kgamble.ignition.tag_cicd.common.strategy.TagExportImportStrategy;
import dev.kgamble.ignition.tag_cicd.common.strategy.TagExportImportStrategyFactory;
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
        JsonObject tagsJson = convertToJsonObject(tagConfigurationModel);
        return (JsonObject) FileUtilities.sortJsonElementRecursively(tagsJson);
    }

    public static void exportTagsToDisk(GatewayTagManager tagManager, String provider, String baseTagPath,
            boolean recursive, boolean localPropsOnly, String filePath, String exportMode,
            boolean deleteExisting, boolean excludeUdtDefinitions) throws IOException {

        TagExportImportStrategy strategy = TagExportImportStrategyFactory.getInstance().getStrategy(exportMode);

        logger.info(
                "Starting tag export to disk using {} mode: provider={}, baseTagPath={}, filePath={}, recursive={}, localPropsOnly={}, deleteExisting={}, excludeUdtDefinitions={}",
                strategy.getExportMode().getDisplayName(), provider, baseTagPath, filePath, recursive, localPropsOnly,
                deleteExisting, excludeUdtDefinitions);

        strategy.exportTagsToDisk(tagManager, provider, baseTagPath, recursive, localPropsOnly, filePath,
                deleteExisting, excludeUdtDefinitions);
    }
}