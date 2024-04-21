/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.FileUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagConfigUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

/**
 *
 * @author Keith Gamble
 */
public class TagExportRoutes {
	private static final Logger logger = LoggerFactory.getLogger(TagExportRoutes.class.getName());
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
		JsonObject responseObject = new JsonObject();
		try {
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


			TagConfigurationModel tagConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider,
					tagPath, recursive, localPropsOnly);

			try {
				responseObject = TagUtilities.toJsonObject(tagConfigurationModel);
			} catch (Exception e) {
				logger.error("Error getting tag configuration", e);
				e.printStackTrace();
			}

			responseObject = (JsonObject) FileUtilities.sortJsonElementRecursively(responseObject);
		} catch (Exception e) {
			logger.error("Error exporting tag configuration to string: " + e.toString(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
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
		JsonObject responseObject = new JsonObject();
		try {
			responseObject = getTagConfigurationAsString(requestContext, httpServletResponse);

			String filePath = requestContext.getParameter("filePath");
			logger.trace("filePath: " + filePath);
			// If filePath is not specified throw an error
			if (filePath == null) {
				throw new IllegalArgumentException("filePath parameter is required");
			}

			Boolean individualFilesPerObject = Boolean
					.parseBoolean(requestContext.getParameter("individualFilesPerObject"));
			logger.trace("individualFilesPerObject: " + individualFilesPerObject);

			// Create the directory structure
			// We must make sure that the path isnt a file path, and either has no extension, or ends with a '/'
			String directoryPath = filePath;
			if (directoryPath.contains(".")) {
				directoryPath = directoryPath.substring(0, directoryPath.lastIndexOf("/"));
			}
			if (!directoryPath.endsWith("/")) {
				directoryPath += "/";
			}

			File directory = new File(directoryPath);
			if (!directory.exists()) {
				boolean created = directory.mkdirs();
				if (!created) {
					logger.error("Failed to create directory: " + directory.getAbsolutePath());
					responseObject = WebUtilities.getBadRequestError(httpServletResponse, "Failed to create directory: " + directory.getAbsolutePath());
					return responseObject;
				}
			}

			try {
				if (individualFilesPerObject) {
					// If individualFilesPerObject is true, save each tag as a separate file
					saveJsonFiles(responseObject, filePath);
				} else {
					// If individualFilesPerObject is false, save the entire tag configuration as a single file
					FileUtilities.saveJsonToFile(responseObject, filePath);
				}
			} catch (IOException e) {
				logger.error("Error saving tag configuration", e);
				e.printStackTrace();
				responseObject = WebUtilities.getBadRequestError(httpServletResponse,
						"Error saving tag configuration: " + e.getMessage());
			}
		} catch (Exception e) {
			logger.error("Error exporting tag configuration to disc: " + e.getMessage(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}

		return responseObject;
	}
}
