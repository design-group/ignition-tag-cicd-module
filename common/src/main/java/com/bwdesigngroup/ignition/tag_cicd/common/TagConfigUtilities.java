/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.BasicTagPath;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

import java.util.*;

/**
 *
 * @author Keith Gamble
 */
public class TagConfigUtilities {
	private static final Logger logger = LoggerFactory.getLogger(TagConfigUtilities.class.getName());

	public static final String DEFAULT_PROVIDER = "default";
	public static final String UDT_TYPES_FOLDER = "_types_";

	/**
	 * Returns a tag configuration model for the given provider and tag path.
	 *
	 * @param provider the provider to retrieve tag configuration for.
	 * @param tagPath the base tag path to retrieve tag configuration for.
	 * @param recursive If true, will recursively search the `baseTagPath` for tags. If false, will only search for the direct children of `baseTagPath` for tags.
	 * @param localPropsOnly Set to True to only return configuration created by a user (aka no inherited properties). Useful for tag export and tag UI edits of raw JSON text.
	 * @return a tag configuration model for the given provider and tag path.
	 */
	public static TagConfigurationModel getTagConfigurationModel(GatewayTagManager tagManager, String provider, String tagPath, Boolean recursive, Boolean localPropsOnly) {
		TagPath baseTagPath;
		if (tagPath == null || tagPath.isEmpty()) {
			baseTagPath = new BasicTagPath(provider);
		} else {
			baseTagPath = new BasicTagPath(provider, List.of(tagPath));
		}
		
		logger.info("Requesting tag configuration for provider " + provider + " and tag path " + baseTagPath.toString() + " with recursive=" + recursive + " and localPropsOnly=" + localPropsOnly);

		TagConfigurationModel tagConfigurationModel = tagManager.getTagProvider(provider).getTagConfigsAsync(List.of(baseTagPath), recursive, localPropsOnly).join().get(0);

		return tagConfigurationModel;
	}


	/**
	 * Deletes all tags in the given tag configuration model, and returns a list of quality codes for the deleted tags.
	 *
	 * @param provider the tag provider for the configuration model
	 * @param baseTagPath the base tag path for the tags to be deleted
	 * @param tagConfigurationModel the configuration model for the tags to be deleted
	 * @return a list of quality codes for the deleted tags
	 */
	public static List<QualityCode> deleteTagsInConfigurationModel(GatewayTagManager tagManager, String provider, TagPath baseTagPath, TagConfigurationModel tagConfigurationModel) {
		List<TagConfigurationModel> configModelChildren = tagConfigurationModel.getChildren();
		List<TagPath> tagPaths =  new ArrayList<TagPath>();
		List<QualityCode> deleteResults = new ArrayList<QualityCode>();

		logger.info("Found " + configModelChildren.size() + " tags to delete from provider " + provider);

		for (TagConfigurationModel configModelChild : configModelChildren) {
			if (configModelChild.getPath().toString().equals(UDT_TYPES_FOLDER)) {
				// Delete every tag in the `_types_` folder
				logger.info("Found _types_ folder, deleting all tags in folder");
				deleteResults.addAll(deleteTagsInConfigurationModel(tagManager, provider, new BasicTagPath(provider, List.of(UDT_TYPES_FOLDER)), configModelChild));
				continue;
			}

			logger.info("Adding tag path " + configModelChild.getPath() + " to list of tags to delete");
			tagPaths.add(baseTagPath.getChildPath(configModelChild.getPath().toString()));
		}

		logger.info("Deleting tags from provider " + provider + " with paths: " + tagPaths.toString());
		deleteResults.addAll(tagManager.getTagProvider(provider).removeTagConfigsAsync(tagPaths).join());

		return deleteResults;
	}

	/**
	 * Converts a List of QualityCode objects to a JsonArray of QualityCode strings
	 *
	 * @param qualityCodes a List of QualityCode objects to convert to a JsonArray
	 * @return a JsonArray of QualityCode strings
	 */
	public static JsonArray convertQualityCodesToArray(List<QualityCode> qualityCodes) {
		JsonArray qualityCodesArray = new JsonArray();
		qualityCodes.forEach(code -> qualityCodesArray.add(code.toString()));
		return qualityCodesArray;
	}

	/**
	 * Adds a JsonObject of tags and their corresponding QualityCodes to the given JsonObject, 
	 * under the given key, if the JsonObject of tags is not empty.
	 *
	 * @param jsonObject the JsonObject to which the tags and QualityCodes should be added
	 * @param tags the JsonObject containing the tags and their corresponding QualityCodes
	 * @param key the key to use when adding the tags to the main JsonObject
	 */

	 public static void addQualityCodesToJsonObject(JsonObject jsonObject, JsonObject tags, String key) {
		if (tags.size() > 0) {
			jsonObject.add(key, tags);
		}
	}

	/**
     * Separates the UDT types from the regular tags in the given JsonObject,
     * sorts the UDT types based on their dependencies, and returns a new JsonObject
     * with the sorted UDT types and regular tags.
     *
     * @param tagsJson the JsonObject containing the tags and UDT types
     * @return a new JsonObject with the sorted UDT types and regular tags
     */
    public static JsonObject sortTagsAndUdtTypes(JsonObject tagsJson) {
		JsonObject sortedTagsJson = new JsonObject();
		JsonArray udtTypes = new JsonArray();
		JsonArray regularTags = new JsonArray();

		// Separate UDT types from regular tags and UDT instances
		separateUdtTypesRegularTagsAndInstances(tagsJson, udtTypes, regularTags);

		// Sort the UDT types based on their dependencies
		JsonArray sortedUdtTypes = sortUdtTypes(udtTypes);

		// Add the sorted UDT types and regular tags to the new JsonObject
		sortedTagsJson.add("udtTypes", sortedUdtTypes);
		sortedTagsJson.add("tags", regularTags);

		return sortedTagsJson;
	}

    /**
     * Separates the UDT types from the regular tags in the given JsonObject
     * and adds them to the respective arrays.
     *
     * @param json        the JsonObject to separate tags from
     * @param udtTypes    the array to store the UDT types
     * @param regularTags the array to store the regular tags
     */
    private static void separateUdtTypesRegularTagsAndInstances(JsonObject json, JsonArray udtTypes, JsonArray regularTags) {
		if (json.has("tags")) {
			JsonArray tags = json.getAsJsonArray("tags");
			for (JsonElement tagElement : tags) {
				JsonObject tag = tagElement.getAsJsonObject();
				String tagType = tag.get("tagType").getAsString();
				if (tagType.equals("UdtType")) {
					udtTypes.add(tag);
				} else if (tagType.equals("UdtInstance")) {
					// Recursively separate UDT instances and their children
					JsonArray udtInstanceTags = new JsonArray();
					separateUdtTypesRegularTagsAndInstances(tag, udtTypes, udtInstanceTags);
					regularTags.add(tag);
				} else if (tagType.equals("Folder")) {
					separateUdtTypesRegularTagsAndInstances(tag, udtTypes, regularTags);
				} else {
					regularTags.add(tag);
				}
			}
		}
	}

    /**
     * Sorts the UDT types in the given JsonArray based on their dependencies.
     *
     * @param udtTypesArray the JsonArray containing the UDT types
     * @return a sorted JsonArray of the UDT types
     */
    private static JsonArray sortUdtTypes(JsonArray udtTypesArray) {
        Map<String, JsonObject> udtTypesMap = new HashMap<>();
        Queue<JsonObject> udtTypesQueue = new ArrayDeque<>();

        // Extract UDT types from the array and add them to the map and queue
        for (JsonElement udtTypeElement : udtTypesArray) {
            JsonObject udtType = udtTypeElement.getAsJsonObject();
            String udtName = udtType.get("name").getAsString();
            udtTypesMap.put(udtName, udtType);
            udtTypesQueue.offer(udtType);
        }

        // Sort the UDT types based on their dependencies
        JsonArray sortedUdtTypes = new JsonArray();
        while (!udtTypesQueue.isEmpty()) {
            JsonObject udtType = udtTypesQueue.poll();
            sortUdtType(udtType, udtTypesMap, udtTypesQueue, sortedUdtTypes);
        }

        return sortedUdtTypes;
    }

    /**
     * Recursively sorts the dependencies of the given UDT type and adds it to the sortedUdtTypes array.
     *
     * @param udtType        the UDT type to sort
     * @param udtTypesMap    the map of UDT types by their name
     * @param udtTypesQueue  the queue of UDT types to process
     * @param sortedUdtTypes the array to store the sorted UDT types
     */
    private static void sortUdtType(JsonObject udtType, Map<String, JsonObject> udtTypesMap,
                                    Queue<JsonObject> udtTypesQueue, JsonArray sortedUdtTypes) {
        String udtName = udtType.get("name").getAsString();
        if (sortedUdtTypes.contains(udtType)) {
            return;
        }

        if (udtType.has("tags")) {
            JsonArray tags = udtType.getAsJsonArray("tags");
            for (JsonElement tagElement : tags) {
                JsonObject tag = tagElement.getAsJsonObject();
                String tagType = tag.get("tagType").getAsString();
                if (tagType.equals("UdtInstance")) {
                    String dependentUdtName = tag.get("typeId").getAsString();
                    JsonObject dependentUdtType = udtTypesMap.get(dependentUdtName);
                    if (dependentUdtType != null) {
                        sortUdtType(dependentUdtType, udtTypesMap, udtTypesQueue, sortedUdtTypes);
                    }
                }
            }
        }

        sortedUdtTypes.add(udtType);
    }
}
