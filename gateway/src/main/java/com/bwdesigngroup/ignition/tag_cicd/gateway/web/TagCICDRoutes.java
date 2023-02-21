package com.bwdesigngroup.ignition.tag_cicd.gateway.web;

/**
 * Endpoints for exporting and importing tag configurations
 */


import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class TagCICDRoutes {
	private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RouteGroup routes;
	private final GatewayContext context;

	private static final String DEFAULT_PROVIDER = "default";
	private static final String UDT_TYPES_FOLDER = "_types_";

	/**
	 * Creates a new instance of the TagCICDRoutes class with the given GatewayContext and RouteGroup.
	 *
	 * @param context the GatewayContext to use
	 * @param group the RouteGroup to use
	 */
	public TagCICDRoutes(GatewayContext context, RouteGroup group) {
        this.routes = group;
		this.context = context;
    }

	/**
	 * Mounts the routes for the TagCICD module.
	 */
	public void mountRoutes() {
		/*
		* Export tag configuration
		* This will be a GET request, with the tag configuration being returned as JSON
		* 
		* Example Usage: curl http://tag-cicd.localtest.me/data/tag-cicd/tags/export?provider=default&tagPath=&recursive=true&localPropsOnly=false
		*/ 
        this.routes.newRoute("/tags/export")
                .handler(this::getTagConfiguration)
                .type(TYPE_JSON)
                .mount();

		/*
		* Export tag configuration to a given filePath relative to the Ignition gateway
		* This will be a POST request, with the tag configuration being returned as JSON
		* 
		* Example Usage: curl -X POST http://tag-cicd.localtest.me/data/tag-cicd/tags/export?provider=default&tagPath=&recursive=true&localPropsOnly=false&filePath=/workdir/tag_configuration.json
		*/ 
        this.routes.newRoute("/tags/export")
                .handler(this::saveTagConfiguration)
                .type(TYPE_JSON)
				.method(POST)
                .mount();
		
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
	 * Returns a jsonObject to represent an HTTP error of status 400.
	 * 
	 * @param httpServletResponse the HttpServletResponse to set the status code on
	 * @param message the message to include in the error response
	 * @return a jsonObject to represent an HTTP error of status 400
	 */
	private JsonObject getBadRequestError(HttpServletResponse httpServletResponse, String message) {
		httpServletResponse.setStatus(400);
		JsonObject json = new JsonObject();
		json.addProperty("error", message);
		return json;
	}

	/**
	 * Returns a tag configuration model for the given provider and tag path.
	 *
	 * @param provider the provider to retrieve tag configuration for.
	 * @param tagPath the base tag path to retrieve tag configuration for.
	 * @param recursive If true, will recursively search the `baseTagPath` for tags. If false, will only search for the direct children of `baseTagPath` for tags.
	 * @param localPropsOnly Set to True to only return configuration created by a user (aka no inherited properties). Useful for tag export and tag UI edits of raw JSON text.
	 * @return a tag configuration model for the given provider and tag path.
	 */

	public TagConfigurationModel getTagConfigurationModel(String provider, String tagPath, Boolean recursive, Boolean localPropsOnly) {
		TagPath baseTagPath;
		if (tagPath == null || tagPath.isEmpty()) {
			baseTagPath = new BasicTagPath(provider);
		} else {
			baseTagPath = new BasicTagPath(provider, List.of(tagPath));
		}
		
		logger.info("Requesting tag configuration for (baseTagPath: " + baseTagPath + ", recursive: " + recursive + ", localPropsOnly: " + localPropsOnly + ")");

		TagConfigurationModel tagConfigurationModel = context.getTagManager().getTagProvider(provider).getTagConfigsAsync(List.of(baseTagPath), recursive, localPropsOnly).join().get(0);

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
	public List<QualityCode> deleteTagsInConfigurationModel(String provider, TagPath baseTagPath, TagConfigurationModel tagConfigurationModel) {
		List<TagConfigurationModel> configModelChildren = tagConfigurationModel.getChildren();
		List<TagPath> tagPaths =  new ArrayList<TagPath>();
		List<QualityCode> deleteResults = new ArrayList<QualityCode>();

		logger.info("Found " + configModelChildren.size() + " tags to delete from provider " + provider);

		for (TagConfigurationModel configModelChild : configModelChildren) {
			if (configModelChild.getPath().toString().equals(UDT_TYPES_FOLDER)) {
				// Delete every tag in the `_types_` folder
				logger.info("Found _types_ folder, deleting all tags in folder");
				deleteResults.addAll(deleteTagsInConfigurationModel(provider, new BasicTagPath(provider, List.of(UDT_TYPES_FOLDER)), configModelChild));
				continue;
			}

			logger.info("Adding tag path " + configModelChild.getPath() + " to list of tags to delete");
			tagPaths.add(baseTagPath.getChildPath(configModelChild.getPath().toString()));
		}

		logger.info("Deleting tags from provider " + provider + " with paths: " + tagPaths.toString());
		deleteResults.addAll(context.getTagManager().getTagProvider(provider).removeTagConfigsAsync(tagPaths).join());

		return deleteResults;
	}

	/**
	 * Converts a List of QualityCode objects to a JsonArray of QualityCode strings
	 *
	 * @param qualityCodes a List of QualityCode objects to convert to a JsonArray
	 * @return a JsonArray of QualityCode strings
	 */
	public JsonArray convertQualityCodesToArray(List<QualityCode> qualityCodes) {
		JsonArray qualityCodesArray = new JsonArray();
		qualityCodes.forEach(code -> qualityCodesArray.add(code.toString()));
		return qualityCodesArray;
	}
 
	/**
	 * Returns the tag configuration as a JSON object.
	 *
	 * @param requestContext the RequestContext for the request
	 * @param httpServletResponse the HttpServletResponse for the response
	 * @return the tag configuration as a JSON object
	 * @throws JSONException if there is an error creating the JSON object
	 */
	public JsonObject getTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse) throws JSONException {		
        String provider = requestContext.getParameter("provider");
		// If provider is not specified, default to DEFAULT_PROVIDER
		if (provider == null) {
			provider = DEFAULT_PROVIDER;
		}

		String tagPath = requestContext.getParameter("baseTagPath");
		// If tagPath is not specified, default to ""
		if (tagPath == null) {
			tagPath = "";
		}

		Boolean recursive = Boolean.parseBoolean(requestContext.getParameter("recursive"));

		Boolean localPropsOnly = Boolean.parseBoolean(requestContext.getParameter("localPropsOnly"));
		
		JsonObject json = new JsonObject();

		TagConfigurationModel tagConfigurationModel = getTagConfigurationModel(provider, tagPath, recursive, localPropsOnly);

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
	 * Gets the tag configuration for the given provider and tag path, and writes it to a file.
	 *
	 * @param requestContext the RequestContext for the request
	 * @param httpServletResponse the HttpServletResponse for the response
	 * @return the tag configuration as a JSON object
	 * @throws JSONException if there is an error creating the JSON object
	 */
	public JsonObject saveTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse) throws JSONException {		
		JsonObject json = getTagConfiguration(requestContext, httpServletResponse);

		String filePath = requestContext.getParameter("filePath");
		// If filePath is not specified throw an error
		if (filePath == null) {
			throw new IllegalArgumentException("filePath parameter is required");
		}

		try {
			FileWriter fileWriter = new FileWriter(filePath);
			fileWriter.write(json.toString());
			fileWriter.close();
		} catch (IOException e) {
			logger.error("Error saving tag configuration", e);
			e.printStackTrace();
		}

		return json;
	}


	/**
	 * Imports a tag configuration from a JSON string in the request body
	 *
	 * @param requestContext the context of the HTTP request
	 * @param httpServletResponse the HTTP response to send
	 * @return a JsonObject representing the results of the import operation
	 * @throws JSONException if there is an error parsing the JSON string
	 * @throws IOException if there is an error reading the request body
	 */
	public JsonObject importTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse) throws JSONException, IOException {
		// Read the tag configuration from the request body
		String tagConfiguration = requestContext.readBody();

		// If the tag configuration is empty, return an error
		if (tagConfiguration.isEmpty()) {
			return getBadRequestError(httpServletResponse, "Tag configuration is empty");
		}

		JsonObject tagConfigurationJson = (JsonObject) TagUtilities.stringToJson(tagConfiguration);
		// If tagConfiguration is not valid JSON, return an error
		if (!tagConfigurationJson.isJsonObject()) {
			return getBadRequestError(httpServletResponse, "Tag configuration is not valid JSON");
		}

		// If the first `tagType` key is "Provider", then we can use that for the provider
		// If not, we will use the provider specified in the request
		String provider = tagConfigurationJson.get("tagType").getAsString();
		if (!provider.equals("Provider")) {
			provider = requestContext.getParameter("provider");

			if (provider == null) {
				return getBadRequestError(httpServletResponse, "Provider is not specified, and tag configuration does not contain a provider");
			}
		} else {
			provider = tagConfigurationJson.get("name").getAsString();
		}

		// If provider is not specified, default to DEFAULT_PROVIDER
		if (provider == null || provider.isEmpty()) {
			provider = DEFAULT_PROVIDER;
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
		
		GatewayTagManager tagManager = context.getTagManager();

		// Convert the List of QualityCodes to an array of strings
		JsonObject responseObject = new JsonObject();
		JsonObject createdTags = new JsonObject();
		JsonObject deletedTags = new JsonObject();

		// If we are importing at the base path, remove any tags currently there
		if (deleteTags) {
			logger.info("Deleting all tags in provider " + provider + " before importing");
			TagConfigurationModel baseTagsConfigurationModel = getTagConfigurationModel(provider, basePath, true, false);
			List<QualityCode> deletedUdtQualityCodes = deleteTagsInConfigurationModel(provider, new BasicTagPath(provider), baseTagsConfigurationModel);

			deletedTags.add(basePath, convertQualityCodesToArray(deletedUdtQualityCodes));
		}


		// For each tag object in the array tagConfigurationJson.get("tags")
		for (JsonElement tag : tagConfigurationJson.get("tags").getAsJsonArray()) {
			JsonObject tagObject = tag.getAsJsonObject();

			TagPath tagPath = new BasicTagPath(provider);

			if (basePath != null && !basePath.isEmpty()) {
				tagPath = tagPath.getChildPath(basePath);
			}

			List<QualityCode> createdQualityCodes = tagManager.importTagsAsync(tagPath, TagUtilities.jsonToString(tagObject), "json", collisionPolicy).join();
			createdTags.add(tagPath.toString(), convertQualityCodesToArray(createdQualityCodes));
		}

		// If there were deleted or created tags, add them to the response
		addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
		addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
		return responseObject;
	}



	/**
	 * Adds a JsonObject of tags and their corresponding QualityCodes to the given JsonObject, 
	 * under the given key, if the JsonObject of tags is not empty.
	 *
	 * @param jsonObject the JsonObject to which the tags and QualityCodes should be added
	 * @param tags the JsonObject containing the tags and their corresponding QualityCodes
	 * @param key the key to use when adding the tags to the main JsonObject
	 */

	private void addQualityCodesToJsonObject(JsonObject jsonObject, JsonObject tags, String key) {
		if (tags.size() > 0) {
			jsonObject.add(key, tags);
		}
	}
	
}