/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.IOException;
import java.util.List;
import java.io.File;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagConfigUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagImportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.FileUtilities;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonArray;
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
public class TagImportRoutes {
	private static final Logger logger = LoggerFactory.getLogger(TagImportRoutes.class.getName());
	private final RouteGroup routes;
	private final GatewayTagManager tagManager;

	/**
	 * Creates a new instance of the TagImportRoutes class with the given
	 * GatewayContext and RouteGroup.
	 *
	 * @param context the GatewayContext to use
	 * @param group   the RouteGroup to use
	 */
	public TagImportRoutes(GatewayContext context, RouteGroup group) {
		this.routes = group;
		this.tagManager = context.getTagManager();
	}

	/**
	 * Mounts the routes for the TagCICD module.
	 */
	public void mountRoutes() {
		/*
		* Import tag configuration
		* This will be a POST request, with the tag configuration in the body
		* 
		* Example Usage: curl -X POST -H "Content-Type: application/json" -d @tag_configuration.json http://tag-cicd.localtest.me/data/tag-cicd/tags/import?provider=default&baseTagPath=&collisionPolicy=o&importType=json
		*/ 
		this.routes.newRoute("/tags/import")
			.handler(this::importTagConfiguration)
			.type(TYPE_JSON)
			.method(POST)
			.mount();
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
	public JsonObject importTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse) {
		JsonObject responseObject = new JsonObject();
		try {
			String provider = getProvider(requestContext);
			String collisionPolicyString = requestContext.getParameter("collisionPolicy");
			String basePath = requestContext.getParameter("baseTagPath");
			if (basePath == null) {
				basePath = "";
			}

			Boolean deleteTags = collisionPolicyString.equals("d");
			CollisionPolicy collisionPolicy = getCollisionPolicy(collisionPolicyString, deleteTags);

			JsonObject createdTags = new JsonObject();
			JsonObject deletedTags = new JsonObject();

			if (deleteTags) {
				logger.info("Deleting all tags in provider " + provider + " before importing");
				TagConfigurationModel baseTagsConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider, basePath, true, false);
				List<QualityCode> deletedUdtQualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(tagManager, provider, new BasicTagPath(provider), baseTagsConfigurationModel);
				deletedTags.add(basePath, TagConfigUtilities.convertQualityCodesToArray(deletedUdtQualityCodes));
			}

			String filePath = requestContext.getParameter("filePath");
			Boolean individualFilesPerObject = Boolean.parseBoolean(requestContext.getParameter("individualFilesPerObject"));

			if (individualFilesPerObject) {
				File directory = new File(filePath);
				File[] files = directory.listFiles();
				if (files != null) {
					// Import the _types_ folder first, if present
					File typesFolder = FileUtilities.findTypesFolder(files);
					if (typesFolder != null) {
						JsonObject folderJson = TagImportUtilities.readTagsFromDirectory(typesFolder.getAbsolutePath());
						importTags(provider, basePath, collisionPolicy, createdTags, folderJson);
					}

					// Import the rest of the folders
					for (File file : files) {
						if (file.isDirectory() && !file.equals(typesFolder)) {
							JsonObject folderJson = TagImportUtilities.readTagsFromDirectory(file.getAbsolutePath());
							importTags(provider, basePath, collisionPolicy, createdTags, folderJson);
						}
					}
				}
			} else {
				JsonObject tagConfigurationJson = (JsonObject) TagUtilities.stringToJson(requestContext.readBody());
				importTags(provider, basePath, collisionPolicy, createdTags, tagConfigurationJson);
			}

			TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
			TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
		} catch (Exception e) {
			logger.error("Error importing tag configuration on line: " + e.toString(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
	}

	/**
	 * Imports tags from a JSON object
	 *
	 * @param provider        the provider to import the tags to
	 * @param basePath        the base tag path to import the tags to
	 * @param collisionPolicy the collision policy to use when importing the tags
	 * @param createdTags     a JsonObject to store the results of the import operation
	 * @param tagsJson        the JSON object containing the tags to import
	 */
	private void importTags(String provider, String basePath, CollisionPolicy collisionPolicy, JsonObject createdTags, JsonObject tagsJson) {
		TagPath baseTagPath = new BasicTagPath(provider);
		if (!basePath.isEmpty()) {
			baseTagPath = baseTagPath.getChildPath(basePath);
		}

		// Sort the tags and UDT types
		JsonObject sortedTagsJson = TagConfigUtilities.sortTagsAndUdtTypes(tagsJson);

		// Import the _types_ folder first, if present
		JsonArray udtTypes = sortedTagsJson.getAsJsonArray("udtTypes");
		if (udtTypes != null) {
			TagPath typesPath = baseTagPath.getChildPath(TagConfigUtilities.UDT_TYPES_FOLDER);
			for (JsonElement udtType : udtTypes) {
				JsonObject udtTypeObject = udtType.getAsJsonObject();
				List<QualityCode> qualityCodes = tagManager.importTagsAsync(typesPath, TagUtilities.jsonToString(udtTypeObject), "json", collisionPolicy).join();
				
				createdTags.add(typesPath.toString(), TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
			}
		}

		// Import the rest of the tags, including the folder structure
		JsonArray tags = sortedTagsJson.getAsJsonArray("tags");
		if (tags != null) {
			String tagsJsonString = TagUtilities.jsonToString(tagsJson);
			List<QualityCode> qualityCodes = tagManager.importTagsAsync(baseTagPath, tagsJsonString, "json", collisionPolicy).join();
			createdTags.add(baseTagPath.toString(), TagConfigUtilities.convertQualityCodesToArray(qualityCodes));
		}
	}

	/**
	 * Gets the provider from the request context
	 *
	 * @param requestContext the context of the HTTP request
	 * @return the provider to use
	 * @throws JSONException if there is an error parsing the JSON string
	 * @throws IOException   if there is an error reading the request body
	 */
	private String getProvider(RequestContext requestContext) throws JSONException, IOException {
		String provider = requestContext.getParameter("provider");
		if (provider == null) {
			JsonObject tagConfigurationJson = (JsonObject) TagUtilities.stringToJson(requestContext.readBody());
			String tagType = tagConfigurationJson.get("tagType").getAsString();
			if (tagType.equals("Provider")) {
				provider = tagConfigurationJson.get("name").getAsString();
			} else {
				provider = TagConfigUtilities.DEFAULT_PROVIDER;
			}
		}
		return provider;
	}

	/**
	 * Gets the collision policy from the request context
	 *
	 * @param collisionPolicyString the collision policy string to use
	 * @param deleteTags            whether to delete tags before importing
	 * @return the collision policy to use
	 */
	private CollisionPolicy getCollisionPolicy(String collisionPolicyString, Boolean deleteTags) {
		if (collisionPolicyString.isEmpty()) {
			collisionPolicyString = "a";
		} else if (deleteTags) {
			collisionPolicyString = "o";
		}
		return CollisionPolicy.fromString(collisionPolicyString);
	}
}  