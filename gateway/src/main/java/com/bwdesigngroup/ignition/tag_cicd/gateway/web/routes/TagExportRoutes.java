/*
 * Copyright 2022 Keith Gamble
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.FileUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagConfigUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.inductiveautomation.ignition.common.JsonUtilities;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.BasicTagPath;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

/**
 *
 * @author Keith Gamble
 */
public class TagExportRoutes {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final RouteGroup routes;
	private final GatewayTagManager tagManager;

	/**
	 * Creates a new instance of the TagExportRoutes class with the given
	 * GatewayContext and RouteGroup.
	 *
	 * @param context the GatewayContext to use
	 * @param group   the RouteGroup to use
	 */
	public TagExportRoutes(GatewayContext context, RouteGroup group) {
		this.routes = group;
		this.tagManager = context.getTagManager();
	}

	/**
	 * Mounts the routes for the TagCICD module.
	 */
	public void mountRoutes() {
		/*
		 * Export tag configuration
		 * This will be a GET request, with the tag configuration being returned as JSON
		 * 
		 * Example Usage: curl
		 * http://tag-cicd.localtest.me/data/tag-cicd/tags/export?provider=default&
		 * tagPath=&recursive=true&localPropsOnly=false
		 */
		this.routes.newRoute("/tags/export")
				.handler(this::getTagConfigurationAsString)
				.type(TYPE_JSON)
				.mount();

		/*
		 * Export tag configuration to a given filePath relative to the Ignition gateway
		 * This will be a POST request, with the tag configuration being returned as
		 * JSON
		 * 
		 * Example Usage: curl -X POST
		 * http://tag-cicd.localtest.me/data/tag-cicd/tags/export?provider=default&
		 * tagPath=&recursive=true&localPropsOnly=false&filePath=/workdir/
		 * tag_configuration.json
		 */
		this.routes.newRoute("/tags/export")
				.handler(this::saveTagConfigurationToDisc)
				.type(TYPE_JSON)
				.method(POST)
				.mount();
	}

	/**
	 * Returns the tag configuration as a JSON object.
	 *
	 * @param requestContext      the RequestContext for the request
	 * @param httpServletResponse the HttpServletResponse for the response
	 * @return the tag configuration as a JSON object
	 * @throws JSONException if there is an error creating the JSON object
	 */
	public JsonObject getTagConfigurationAsString(RequestContext requestContext,
			HttpServletResponse httpServletResponse) throws JSONException {
		String provider = requestContext.getParameter("provider");
		// If provider is not specified, default to DEFAULT_PROVIDER
		if (provider == null) {
			provider = TagConfigUtilities.DEFAULT_PROVIDER;
		}

		String tagPath = requestContext.getParameter("baseTagPath");
		// If tagPath is not specified, default to ""
		if (tagPath == null) {
			tagPath = "";
		}

		Boolean recursive = Boolean.parseBoolean(requestContext.getParameter("recursive"));

		Boolean localPropsOnly = Boolean.parseBoolean(requestContext.getParameter("localPropsOnly"));

		JsonObject json = new JsonObject();

		TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider,
				tagPath, recursive, localPropsOnly);

		try {
			json = TagUtilities.toJsonObject(tagConfigurationModel);
		} catch (Exception e) {
			logger.error("Error getting tag configuration", e);
			e.printStackTrace();
		}

		json = (JsonObject) JsonUtilities.createDeterministicCopy(json);

		return json;
	}

	/**
	 * Searches through the given json object for any `tags` arrays, if present it
	 * executes this function against each array.
	 * If the search hits a `tagType` that isn't `Provider` or `Folder`, it will
	 * execute saveJsonToFile on the tag.
	 * Whenever the function hits a `Folder` or `Provider`, it will create a folder
	 * with the name of the object, and then
	 * execute this function on the `tags` array of the object.
	 * 
	 * @param json         the json object to search through
	 * @param baseFilePath the base file path to save the json files to
	 */
	public void saveJsonFiles(JsonObject json, String baseFilePath) throws IOException {
		// If the json object has a `tags` array, execute this function on the array
		if (json.has("tags")) {
			JsonArray tags = json.getAsJsonArray("tags");
			for (JsonElement tag : tags) {
				JsonObject tagObject = tag.getAsJsonObject();
				// If the tag is a folder or provider, create a folder with the name of the
				// object, and then execute this function on the `tags` array of the object
				if (tagObject.get("tagType").getAsString().equals("Folder")
						|| tagObject.get("tagType").getAsString().equals("Provider")) {
					String folderName = tagObject.get("name").getAsString();
					String folderPath = baseFilePath + "/" + folderName;
					File folder = new File(folderPath);
					folder.mkdir();
					saveJsonFiles(tagObject, folderPath);
				} else {
					// If the tag is not a folder or provider, save the json to a file
					FileUtilities.saveJsonToFile(tagObject,
							baseFilePath + "/" + tagObject.get("name").getAsString() + ".json");
				}
			}
		} else {
			// If the json object does not have a `tags` array, save the json to a file
			FileUtilities.saveJsonToFile(json, baseFilePath + ".json");
		}
	}

	/**
	 * Gets the tag configuration for the given provider and tag path, and writes it
	 * to a file.
	 *
	 * @param requestContext      the RequestContext for the request
	 * @param httpServletResponse the HttpServletResponse for the response
	 * @return the tag configuration as a JSON object
	 * @throws JSONException if there is an error creating the JSON object
	 */
	public JsonObject saveTagConfigurationToDisc(RequestContext requestContext, HttpServletResponse httpServletResponse)
			throws JSONException {
		JsonObject json = getTagConfigurationAsString(requestContext, httpServletResponse);

		String filePath = requestContext.getParameter("filePath");
		logger.trace("filePath: " + filePath);
		// If filePath is not specified throw an error
		if (filePath == null) {
			throw new IllegalArgumentException("filePath parameter is required");
		}

		Boolean individualFilesPerObject = Boolean
				.parseBoolean(requestContext.getParameter("individualFilesPerObject"));
		logger.trace("individualFilesPerObject: " + individualFilesPerObject);

		String directoryPath = null;
		// If the filePath does not end in .json, it is a directory
		if (!filePath.endsWith(".json")) {
			directoryPath = filePath;
			// If the filePath does not end in a slash, add one
			if (!filePath.endsWith("/")) {
				directoryPath += "/";
			}
		}

		// If the filePath provided is a directory, make sure it exists. If it doesn't
		// exist, create it.
		if (directoryPath != null) {
			File directory = new File(directoryPath);
			if (!directory.exists()) {
				directory.mkdirs();
			}
		}

		try {
			if (individualFilesPerObject) {
				// If individualFilesPerObject is true, save each tag as a separate file
				saveJsonFiles(json, filePath);
			} else {
				// If individualFilesPerObject is false, save the entire tag configuration as a
				// single file
				FileUtilities.saveJsonToFile(json, filePath);
			}
		} catch (IOException e) {
			logger.error("Error saving tag configuration", e);
			e.printStackTrace();
			json = WebUtilities.getBadRequestError(httpServletResponse,
					"Error saving tag configuration: " + e.getMessage());
		}

		return json;
	}

	/**
	 * Imports a tag configuration from a JSON string in the request body
	 *
	 * @param requestContext      the context of the HTTP request
	 * @param httpServletResponse the HTTP response to send
	 * @return a JsonObject representing the results of the import operation
	 * @throws JSONException if there is an error parsing the JSON string
	 * @throws IOException   if there is an error reading the request body
	 */
	public JsonObject importTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse)
			throws JSONException, IOException {
		// Read the tag configuration from the request body
		String tagConfiguration = requestContext.readBody();

		// If the tag configuration is empty, return an error
		if (tagConfiguration.isEmpty()) {
			return WebUtilities.getBadRequestError(httpServletResponse, "Tag configuration is empty");
		}

		JsonObject tagConfigurationJson = (JsonObject) TagUtilities.stringToJson(tagConfiguration);
		// If tagConfiguration is not valid JSON, return an error
		if (!tagConfigurationJson.isJsonObject()) {
			return WebUtilities.getBadRequestError(httpServletResponse, "Tag configuration is not valid JSON");
		}

		// If the first `tagType` key is "Provider", then we can use that for the
		// provider
		// If not, we will use the provider specified in the request
		String provider = tagConfigurationJson.get("tagType").getAsString();
		if (!provider.equals("Provider")) {
			provider = requestContext.getParameter("provider");

			if (provider == null) {
				return WebUtilities.getBadRequestError(httpServletResponse,
						"Provider is not specified, and tag configuration does not contain a provider");
			}
		} else {
			provider = tagConfigurationJson.get("name").getAsString();
		}

		// If provider is not specified, default to DEFAULT_PROVIDER
		if (provider == null || provider.isEmpty()) {
			provider = TagConfigUtilities.DEFAULT_PROVIDER;
		}

		String collisionPolicyString = requestContext.getParameter("collisionPolicy");

		String basePath = requestContext.getParameter("baseTagPath");
		if (basePath == null) {
			basePath = "";
		}

		// If the `collisionPolicy` is "d" then we will delete the tags before importing
		// else we will use the collision policy specified in the request
		Boolean deleteTags = collisionPolicyString.equals("d");

		// If collisionPolicy is not specified, default to "a"
		if (collisionPolicyString.isEmpty()) {
			collisionPolicyString = "a";
		} else if (deleteTags) {
			collisionPolicyString = "o";
		}

		CollisionPolicy collisionPolicy = CollisionPolicy.fromString(collisionPolicyString);

		// Convert the List of QualityCodes to an array of strings
		JsonObject responseObject = new JsonObject();
		JsonObject createdTags = new JsonObject();
		JsonObject deletedTags = new JsonObject();

		// If we are importing at the base path, remove any tags currently there
		if (deleteTags) {
			logger.info("Deleting all tags in provider " + provider + " before importing");
			TagConfigurationModel baseTagsConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager,
					provider, basePath, true, false);
			List<QualityCode> deletedUdtQualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(tagManager,
					provider, new BasicTagPath(provider), baseTagsConfigurationModel);

			deletedTags.add(basePath, TagConfigUtilities.convertQualityCodesToArray(deletedUdtQualityCodes));
		}

		// For each tag object in the array tagConfigurationJson.get("tags")
		for (JsonElement tag : tagConfigurationJson.get("tags").getAsJsonArray()) {
			JsonObject tagObject = tag.getAsJsonObject();

			TagPath tagPath = new BasicTagPath(provider);

			if (basePath != null && !basePath.isEmpty()) {
				tagPath = tagPath.getChildPath(basePath);
			}

			List<QualityCode> createdQualityCodes = tagManager
					.importTagsAsync(tagPath, TagUtilities.jsonToString(tagObject), "json", collisionPolicy).join();
			createdTags.add(tagPath.toString(), TagConfigUtilities.convertQualityCodesToArray(createdQualityCodes));
		}

		// If there were deleted or created tags, add them to the response
		TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
		TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
		return responseObject;

	}
}
