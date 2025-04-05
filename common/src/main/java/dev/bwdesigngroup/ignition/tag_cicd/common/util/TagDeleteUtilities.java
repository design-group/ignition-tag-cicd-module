package dev.bwdesigngroup.ignition.tag_cicd.common.util;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.BasicTagPath;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A utility class for deleting tags.
 *
 * @author Keith Gamble
 */
public class TagDeleteUtilities {
    private static final Logger logger = LoggerFactory.getLogger(TagDeleteUtilities.class.getName());

    /**
     * Deletes tags from a specified provider and path.
     *
     * @param tagManager  The GatewayTagManager instance.
     * @param provider    The tag provider name.
     * @param baseTagPath The base tag path to delete (can be empty).
     * @param recursive   Whether to delete tags recursively.
     * @return A JsonObject containing the deletion results.
     */
    public static JsonObject deleteTags(GatewayTagManager tagManager, String provider, String baseTagPath,
            boolean recursive) {
        JsonObject responseObject = new JsonObject();
        if (provider == null) {
            provider = TagConfigUtilities.DEFAULT_PROVIDER;
        }
        if (baseTagPath == null) {
            baseTagPath = "";
        }

        logger.info(
                "Deleting tags from provider " + provider + " at " + baseTagPath + " (recursive=" + recursive + ")");

        TagPath tagPath = new BasicTagPath(provider,
                baseTagPath.isEmpty() ? List.of() : List.of(baseTagPath.split("/")));
        TagConfigurationModel configModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider,
                baseTagPath, recursive, false);
        List<QualityCode> qualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(tagManager, provider,
                tagPath, configModel);

        responseObject.add(tagPath.toString(), TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
        return responseObject;
    }
}